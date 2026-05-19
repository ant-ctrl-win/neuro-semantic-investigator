# Neuro-Semantic Investigator — Report Architetturale Completo

## 1. Scopo del Progetto

Il **Neuro-Semantic Investigator** è un motore di **ragionamento analogico** basato su due pilastri:
- **VSA (Vector Symbolic Architecture)** con codifica **MAP-B** a 10.000 dimensioni bipolari
- **Knowledge Graph** di Wikidata come memoria esterna (via SPARQL)

Obiettivo: dato un fatto noto `A : B` (es. "Apollo 11 : Neil Armstrong") e un'entità target `C` (es. "Vostok 1"), trovare l'analogo `D` (es. "Yuri Gagarin") tale che `A : B = C : D`.

Il sistema risolve l'analogia in 4 passi, senza training, senza database interno, usando Wikidata come sorgente dati on-the-fly.

---

## 2. Architettura MAP-B

### 2.1 Spazio Vettoriale

```
D = 10000 dimensioni
Bipolare: ogni elemento ∈ {-1, +1}
Implementazione: byte[] array in HDVectorMapB.java
```

### 2.2 Operazioni Algebriche

| Operazione | Implementazione | Significato |
|---|---|---|
| **Bind (⊗)** | Hadamard product: `a[i] * b[i]` | Associa due concetti in un vettore composito. È invertibile: `(a⊗b)⊗b ≈ a` |
| **Bundle (+)** | Somma + Signum (tie-break deterministico seed=42) | Sovrappone vettori in un unico "super-vettore" che risuona con ciascun componente |
| **Permute (ρⁿ)** | Shift ciclico di `n` posizioni | Crea posizioni uniche nello spazio vettoriale; `ρ⁻ⁿ(ρⁿ(v)) = v` |
| **Similarity** | Cosine = dot-product / D | `1.0` = identico, `0.0` = ortogonale (rumore puro), `-1.0` = opposto |

### 2.3 Persistenza Distribuita (Senza Database)

```
URI → UTF-8 bytes → UUID.nameUUIDFromBytes() → MD5 hash → 64-bit seed → java.util.Random(seed) LCG → 10000 bit bipolari
```

Stesso URI su qualsiasi JVM, qualsiasi OS, produce sempre lo stesso vettore. Completamente deterministico, nessuna persistenza richiesta.

### 2.4 Encoding di Triple (Simpkin Tree)

**Tripla:** `encode(S, P, O) = vS ⊗ ρ¹(vP) ⊗ ρ²(vO)`

Dove ρ¹ sposta il predicato di 1 posizione e ρ² sposta l'oggetto di 2 posizioni. Questo separa i ruoli semantici (soggetto/predicato/oggetto) nello spazio vettoriale.

**Costruzione dell'albero:**

```
Per ogni entità E:
  1. Raggruppa tutte le triple per predicato (bucket)
  2. Per ogni bucket (max 30 triple):
     - Bundle simultaneo delle triple → Chunk
     - Aggiungi StopVec (vsa:internal:stop) come delimitatore
     - Aggiungi vettore random per parity fix (se necessario)
  3. Rami: Chunk ⊗ vPredicato ⊗ ρ¹⁰⁰
  4. TreeVector(E) = Bundle(rami) + vIdentità(E) + random(parity)
```

**Estrazione (inversa):**
```
Ramo(P) = TreeVector(E) ⊗ ρ⁻¹⁰⁰(vP)  →  cleanUpChunk → Chunk pulito
Oggetto = Chunk ⊗ vE ⊗ ρ¹(vP) ⊗ ρ⁻²  →  cleanUpRelative → foglia
```

### 2.5 Clean-Up Statistico

```
Soglia Z-Score = log₁₀(D) = 4.0 σ (per D=10000)
Parametri: media e deviazione standard calcolate su TUTTI i candidati nel memory bank
Match valido se: (similarity_best - mean) / stdDev >= soglia
```

Il clean-up opera su due memorie separate:
- **chunkMemory**: per recuperare rami (macro-bucket di proprietà)
- **atomicMemory**: per recuperare foglie (entità individuali)

---

## 3. Pipeline di Ragionamento

### 3.1 FASE 0: Entity Resolution & Reranking

**EntityResolver.java** — Converte testo libero in URI Wikidata usando `wikibase:mwapi` (EntitySearch interno di Wikidata):

```sparql
SELECT ?entity ?entityLabel ?class ?classLabel ?sitelinks WHERE {
  SERVICE wikibase:mwapi {
      bd:serviceParam wikibase:endpoint "www.wikidata.org";
                      wikibase:api "EntitySearch";
                      mwapi:search "Apollo 11";
                      mwapi:language "en".
  }
  OPTIONAL { ?entity wdt:P31 ?class . }
  OPTIONAL { ?entity wikibase:sitelinks ?sitelinks . }
  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
} ORDER BY DESC(?sitelinks)
```

**ResolvedEntity** (record Java) contiene:
- `searchKeyword` — keyword originale
- `entityUri` — URI Wikidata completo
- `entityLabel` — etichetta in inglese
- `classUri` — URI del P31 (instance of)
- `classLabel` — etichetta del P31
- `sitelinks` — numero di edizioni Wikipedia (proxy di notorietà)

**Reranker** (`disambiguateCandidates` in OntologyTranslator):
```
combinedScore = classSimilarity × 0.60 + sitelinksNorm × 0.40
```

Dove `classSimilarity` è la cosine similarity tra gli embedding MiniLM (384-d) del `classLabel` sorgente e del `classLabel` candidato. I sitelinks sono normalizzati rispetto al massimo del lotto. Il peso 60/40 bilancia rilevanza semantica e notorietà.

### 3.2 Compatibilità Ontologica

Prima di procedere, il sistema verifica che le classi ontologiche (P31) di sorgente e target siano semanticamente compatibili tramite `areClassesCompatible()`:

```
cosine(embed(sorgente.P31), embed(target.P31)) >= 0.35
```

Soglia 0.35 scelta per permettere analogie cross-classe ma correlate (es. "dramatic work" vs "musical work/composition" = 0.397).

Se le classi sono incompatibili, parte una procedura di allineamento ontologico che:
1. Interroga Wikidata per nodi collegati al target via P793 (significant event), P2283 (uses), P516 (equipment)
2. Usa l'embedding per trovare il candidato con classe più simile alla sorgente

### 3.3 Graph Ingestion

**GraphManager + TripleExtractor** — Scarica il grafo bidirezionale:

```sparql
CONSTRUCT {
  <entityUri> ?pOut ?o .
  ?s ?pIn <entityUri> .
} WHERE {
  { SELECT ?pOut ?o WHERE { <entityUri> ?pOut ?o . } }
  UNION
  { SELECT ?s ?pIn WHERE { ?s ?pIn <entityUri> . } LIMIT 500 }
}
```

Estrae sia le frecce in uscita (proprietà dell'entità) che quelle in entrata (chi punta all'entità).

### 3.4 STEP 1: VSA Role Deduction (Dominio Sorgente)

```
Per ogni proprietà P dell'entità sorgente A:
  noisyBranch = TreeVector(A) ⊗ ρ⁻¹⁰⁰(vP)
  cleanBranch = cleanUpChunk(noisyBranch)     // Z-Score > 4.0 σ
  
  Se cleanBranch != null:
    noisyObject = cleanBranch ⊗ vA ⊗ ρ¹(vP) ⊗ ρ⁻²
    sim = cosine(noisyObject, vB)          // B = oggetto noto
    
    Se sim > 0.05 AND sim > best:
      sourceRoleUri = URI di P
      best = sim

Output: RUOLO SCOPERTO (es. "crew members")
```

### 3.5 STEP 2: LLM Semantic Bridge (Cambio di Dominio)

**Problema:** Il ruolo scoperto nel dominio sorgente (es. "crew members" in Apollo 11) potrebbe avere un nome diverso nel dominio target (es. sempre "crew members" in Vostok 1, ma potrebbe anche essere "astronaut" o "participant").

**Soluzione (3 sotto-fasi):**

**A) Arricchimento con Tipi Attesi**

Per ogni proprietà del target, il sistema interroga Wikidata per il vincolo di range (P2302 → P2308):

```sparql
SELECT ?prop ?expectedTypeLabel WHERE {
  VALUES ?prop { <P1029> <P50> ... }
  ?prop p:P2302 ?stmt.
  ?stmt ps:P2302 wd:Q21510865.     # "value-type constraint"
  ?stmt pq:P2308 ?expectedType.
  ?expectedType rdfs:label ?expectedTypeLabel.
}
```

Risultato: label arricchite come `"composer [expects: human]"` invece di `"composer"`.

**B) Arricchimento del Ruolo Sorgente**

Anche il ruolo sorgente viene arricchito se ha un expected type:
`"crew members"` → `"crew members [expects: human]"`

**C) Matching via Embedding**

Cosine similarity tra l'embedding MiniLM (384-d) del ruolo sorgente arricchito e ogni proprietà target arricchita. Soglia 0.35 (ridotta per compensare la complessità delle label arricchite).

Esempio: `"crew members"` vs `"crew members"` = 1.000 → match perfetto.

### 3.6 STEP 3 & 4: VSA Projection + Extraction (Dominio Target)

```
// STEP 3 — Proiezione
noisyBranch = TreeVector(C) ⊗ ρ⁻¹⁰⁰(targetRole)

// "Holographic Bypass": si salta il clean-up intermedio del ramo
// perché il segnale nei grafi piccoli è troppo debole per superare 4.0 σ

// STEP 4 — Estrazione
noisyObject = noisyBranch ⊗ vC ⊗ ρ¹(targetRole) ⊗ ρ⁻²
finalMatch = cleanUpRelative(noisyObject)

Se finalMatch != null (Z-Score > 4.0 σ):
  RISULTATO: Yuri Gagarin è l'analogo di Neil Armstrong nel dominio Vostok
```

**Nota critica (BUG corrente):** L'Holographic Bypass è stato introdotto perché il clean-up intermedio del ramo (cleanUpChunk) falliva quasi sempre, interrompendo la pipeline. Senza questo passo intermedio, il rumore si accumula e il clean-up finale (cleanUpRelative) produce Z-Score troppo bassi. Vedere Sezione 5.

---

## 4. Componenti Software

### 4.1 Package `vsa/`

| Classe | Ruolo |
|---|---|
| `HDVector.java` | Interfaccia: bind, bundle, permute, similarity |
| `HDVectorMapB.java` | Implementazione MAP-B: D=10000, byte[] bipolare, Hadamard, signum |
| `ItemMemory.java` | 3-tier memory: atomicMemory, chunkMemory, treeMemory + Z-Score clean-up |
| `strategy/VectorGenerationStrategy.java` | Interfaccia per generazione vettori |
| `strategy/RandomGenerationStrategy.java` | Generazione deterministica: URI → seed → vettore |
| `strategy/SemanticEmbeddingStrategy.java` | Generazione neuro-simbolica: MiniLM → LSH → vettore bipolare |
| `strategy/TopologicalVectorUpdater.java` | Costruzione Simpkin Tree da grafo RDF |

### 4.2 Package `engine/`

| Classe | Ruolo |
|---|---|
| `InvestigationEngine.java` | Orchestratore: ingestion + encodifica triple + aggiornamento topologico |

### 4.3 Package `jena/`

| Classe | Ruolo |
|---|---|
| `SparqlEndpoint.java` | Connessione HTTP a Wikidata (deprecated, quasi inutilizzato) |
| `TripleExtractor.java` | Estrattore bidirezionale con CONSTRUCT SPARQL |
| `GraphManager.java` | Gestione modello RDF locale (accumulativo) |
| `EntityResolver.java` | Entity Search via wikibase:mwapi + sitelinks ranking |
| `ResolvedEntity.java` | Record: keyword, URI, label, P31 class, sitelinks |

### 4.4 Package `llm/`

| Classe | Ruolo |
|---|---|
| `OntologyTranslator.java` | Ponte semantico: embedding MiniLM, class similarity, reranking, property type enrichment, semantic equivalent finder |

### 4.5 Entry Points

| Classe | Descrizione |
|---|---|
| `App.java` | Pipeline base (Amazon/Brazil/Nile — hardcoded, con allineamento ontologico SPARQL) |
| `AppDis.java` | Pipeline con disambiguazione (Apollo 11/Neil Armstrong/Vostok 1 — test principale) |

---

## 5. Problemi Noti

### 5.1 Holographic Bypass e SNR Insufficiente

Il clean-up del ramo intermedio (`cleanUpChunk`) fallisce quasi sempre perché:
- I grafi di entità come Apollo 11 o Vostok 1 hanno ~30-50 proprietà
- Il bundle simultaneo di tutte le triple per ogni proprietà crea un rumore composito
- Con D=10000 e 30+ proprietà, il segnale di un singolo ramo è spesso sotto 4.0 σ

L'Holographic Bypass (saltare cleanUpChunk) è un workaround che però non risolve il problema a monte: senza pulizia intermedia, il rumore si propaga e il clean-up finale fallisce.

**Possibili soluzioni:**
1. Aumentare D a 100.000 (10x più spazio → migliore separazione dei segnali)
2. Ridurre la soglia Z-Score per chunk piccoli (soglia adattiva basata sul numero di candidati)
3. Usare SemanticEmbeddingStrategy invece di RandomGenerationStrategy (vettori semanticamente correlati invece che ortogonali puri)

### 5.2 SemanticEmbeddingStrategy Non Usata in Default

La `SemanticEmbeddingStrategy` genera vettori non random ma basati su embedding MiniLM proiettati via LSH in spazio bipolare. Questo renderebbe vettori come "Apollo 11" e "Vostok 1" naturalmente più simili (entrambi sono "human spaceflight"), migliorando la risonanza analogica. Tuttavia non è usata nelle App correnti (che usano RandomGenerationStrategy).

### 5.3 Due App con Logica Duplicata

`App.java` e `AppDis.java` condividono ~90% della logica (STEP 1-4 identici) ma differiscono in:
- App.java: allineamento ontologico via SPARQL (hardcoded per missioni)
- AppDis.java: reranker + compatibilità embedding + type enrichment

Questa duplicazione va consolidata.

### 5.4 Chunk Parity Fix Inconsistente

In `TopologicalVectorUpdater`, alla riga 56: se `macroBranches.size() % 2 == 0`, viene aggiunto un `HDVectorMapB.generateRandom()`. Tuttavia `generateRandom()` usa `new Random()` (senza seed, non deterministico). Questo rompe la riproducibilità distribuita perché il vettore di parity fix è diverso a ogni run. Andrebbe usato `generateSeeded(someDeterministicSeed)`.

---

## 6. Dipendenze

```
Apache Jena 5.0.0     — RDF model, SPARQL query/construct
LangChain4j 0.27.1    — Embedding MiniLM-L6-v2 (384 dimensioni)
SLF4J + Logback       — Logging
JUnit 5.10.2          — Test
Java 17               — Compilazione (pom.xml) / Java 21 (Maven properties)
```

## 7. Build & Esecuzione

```bash
mvn clean compile                          # Compilazione
mvn test -Dtest=ClassName                  # Test singolo
mvn exec:java -Dexec.mainClass=com.investigator.AppDis   # Esecuzione
```
