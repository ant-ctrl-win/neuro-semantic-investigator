# Neuro-Semantic Investigator: Technical Description

## 1. Panoramica dell'Architettura

Il Neuro-Semantic Investigator risolve il problema dell'**Analogia Strutturale** su Knowledge Graph Open-World: date due entità sorgente `A : B` (es. *"Amleto sta a William Shakespeare"*) e un'entità target `C` (es. *"Lacrimosa"*), il sistema deve inferire l'entità incognita `D` tale che `A : B = C : D` (es. *"Mozart"*), percorrendo path non esplicitamente materializzati nel grafo.

### Pipeline di Esecuzione (AppDis.java)

```
┌─────────────────────────────────────────────────────────────────┐
│ FASE 0: Entity Resolution                                        │
│   Input testuale → EntityResolver (wikibase:mwapi) → ResolvedEntity │
├─────────────────────────────────────────────────────────────────┤
│ FASE 0.5: Reranking & Compatibilità Ontologica                  │
│   OntologyTranslator.disambiguateCandidates()                   │
│   OntologyTranslator.areClassesCompatible()  (cosine ≥ 0.35)    │
│   → Allineamento SPARQL se incompatibili                        │
├─────────────────────────────────────────────────────────────────┤
│ Graph Ingestion                                                  │
│   GraphManager + TripleExtractor → CONSTRUCT SPARQL su Wikidata  │
│   → Model RDF locale (bidirezionale: outgoing + incoming)       │
├─────────────────────────────────────────────────────────────────┤
│ VSA Encoding (Simpkin Tree)                                      │
│   InvestigationEngine.expandAndProcess()                        │
│   → TopologicalVectorUpdater: tripla → vettore → chunk → albero │
│   → ItemMemory: 3-tier memory (atomic/chunk/tree)               │
├─────────────────────────────────────────────────────────────────┤
│ STEP 1: Role Deduction (Dominio Sorgente)                        │
│   Per ogni proprietà P di A:                                    │
│     noisyBranch = Tree(A) ⊗ ρ⁻¹⁰⁰(vP)                          │
│     cleanBranch = cleanUpChunk(noisyBranch)                     │
│     noisyObject = cleanBranch ⊗ vA ⊗ ρ¹(vP) ⊗ ρ⁻²              │
│     sim = cosine(noisyObject, vB)                               │
│   → Scopre QUALE proprietà lega A a B                           │
├─────────────────────────────────────────────────────────────────┤
│ STEP 2: Semantic Bridge (Cambio di Dominio)                      │
│   OntologyTranslator.findSemanticEquivalent()                   │
│   → Embedding MiniLM del ruolo sorgente vs. proprietà target    │
│   → Traduce il ruolo dal dominio A al dominio C                  │
├─────────────────────────────────────────────────────────────────┤
│ STEP 3 & 4: Projection + Extraction (Dominio Target)            │
│   branch = Tree(C) ⊗ ρ⁻¹⁰⁰(targetRole)                         │
│   chunk = cleanUpChunk(branch)                                  │
│   object = chunk ⊗ vC ⊗ ρ¹(targetRole) ⊗ ρ⁻²                   │
│   finalAnswer = cleanUpRelative(object)                         │
└─────────────────────────────────────────────────────────────────┘
```

### Mappa dei Moduli

| Modulo | Classe | Ruolo |
|--------|--------|-------|
| **Entity Resolution** | `EntityResolver.java` | Converte testo → URI Wikidata via `wikibase:mwapi` |
| **Graph Ingestion** | `GraphManager.java`, `TripleExtractor.java`, `SparqlEndpoint.java` | Scarica e accumula triple RDF da Wikidata |
| **VSA Core** | `HDVectorMapB.java`, `HDVector.java` | Operazioni algebriche su vettori bipolari D=10000 |
| **VSA Memory** | `ItemMemory.java` | 3-tier memory + clean-up statistico (Z-Score) |
| **VSA Encoding** | `TopologicalVectorUpdater.java` | Costruzione Simpkin Tree da modello RDF |
| **VSA Generation** | `RandomGenerationStrategy.java`, `SemanticEmbeddingStrategy.java` | Generazione deterministica di vettori atomici |
| **Semantic Bridge** | `OntologyTranslator.java` | Embedding MiniLM per matching cross-ontologia |
| **Orchestrator** | `InvestigationEngine.java` | Coordina ingestion, encoding, aggiornamento topologico |
| **Entry Points** | `App.java`, `AppDis.java` | Pipeline complete (duplicate al ~90%) |

---

## 2. Rappresentazione della Conoscenza (VSA Encoding)

### 2.1 Spazio Vettoriale e Dimensionalità

Il sistema opera in uno spazio vettoriale ipercubo di **D = 10000 dimensioni**, con vettori **bipolari** i cui elementi appartengono all'insieme `{-1, +1}`. La scelta di vettori bipolari (anziché binari `{0,1}`) garantisce che l'operatore di binding sia auto-inverso: `bind(a, bind(a, b)) = b`.

L'implementazione utilizza array di `byte[]` (Java):

```java
// HDVectorMapB.java:8-9
public static final int D = 10000;
private final byte[] values;
```

Ogni elemento occupa 1 byte (anche se il dominio è {-1, +1}), per un footprint di **10 KB per vettore**. Nessuna libreria esterna per algebra lineare: tutte le operazioni sono implementate direttamente con cicli `for` su array primitivi.

### 2.2 Codifica delle Triple RDF (Struttura MAP-B)

Il sistema utilizza lo schema **MAP-B** (Multiply-Add-Permute, variante Bipolare) per codificare triple RDF. Il pattern fondamentale è:

**`encode(S, P, O) = vS ⊗ ρ¹(vP) ⊗ ρ²(vO)`**

```java
// TopologicalVectorUpdater.java:97
return vS.bind(vP.permute(1)).bind(vO.permute(2));
```

Dove:
- `⊗` (bind) è il **prodotto di Hadamard** (moltiplicazione elemento per elemento)
- `ρⁿ` (permute) è lo **shift ciclico** di `n` posizioni

La permutazione differenziata per ruolo (+1 per il predicato, +2 per l'oggetto) garantisce che i tre componenti occupino posizioni distinte e non commutino: `encode(S, P, O) ≠ encode(P, S, O)`. Questo è essenziale per preservare la direzionalità del grafo RDF.

#### Operazioni Algebriche nel Dettaglio

**Bind (⊗) — Prodotto di Hadamard:**
```java
// HDVectorMapB.java:63-70
public HDVector bind(HDVector other) {
    HDVectorMapB o = (HDVectorMapB) other;
    byte[] result = new byte[D];
    for (int i = 0; i < D; i++) {
        result[i] = (byte) (this.values[i] * o.values[i]);
    }
    return new HDVectorMapB(result);
}
```
Proprietà chiave: `(a ⊗ b) ⊗ b = a` (auto-inversione), poiché nel dominio {-1,+1} la moltiplicazione per due volte per lo stesso elemento restituisce l'identità.

**Bundle (+) — Somma con Segno (Majority Vote):**
```java
// HDVectorMapB.java:73-89
public HDVector bundle(HDVector other) {
    HDVectorMapB o = (HDVectorMapB) other;
    byte[] result = new byte[D];
    for (int i = 0; i < D; i++) {
        int sum = this.values[i] + o.values[i];
        if (sum > 0)        result[i] = 1;
        else if (sum < 0)   result[i] = -1;
        else                result[i] = (byte) ((i % 2 == 0) ? 1 : -1);
    }
    return new HDVectorMapB(result);
}
```
Il bundle di due vettori usa un tie-breaker **posizionale deterministico** (parità dell'indice `i % 2`), eliminando ogni fonte di non-determinismo.

**Bundle Simultaneo (N vettori):**
```java
// HDVectorMapB.java:37-60
public static HDVector bundleSimultaneous(List<HDVector> vectors) {
    int[] sum = new int[D];
    long localSeed = 0;
    for (HDVector v : vectors) {
        HDVectorMapB bv = (HDVectorMapB) v;
        localSeed ^= Arrays.hashCode(bv.values);  // seed da XOR degli hash
        for (int i = 0; i < D; i++) sum[i] += bv.values[i];
    }
    byte[] result = new byte[D];
    Random tieBreaker = new Random(localSeed != 0 ? localSeed : 42L);
    for (int i = 0; i < D; i++) {
        if (sum[i] > 0)       result[i] = 1;
        else if (sum[i] < 0)  result[i] = -1;
        else                  result[i] = (byte) (tieBreaker.nextBoolean() ? 1 : -1);
    }
    return new HDVectorMapB(result);
}
```
Il seed deterministico è lo XOR degli hash code di tutti i vettori partecipanti: **stesso insieme di vettori → stesso risultato**, garantendo riproducibilità assoluta.

**Permute (ρⁿ) — Shift Ciclico:**
```java
// HDVectorMapB.java:93-101
public HDVector permute(int shift) {
    byte[] result = new byte[D];
    for (int i = 0; i < D; i++) {
        int newIndex = (i + shift) % D;
        if (newIndex < 0) newIndex += D;
        result[newIndex] = this.values[i];
    }
    return new HDVectorMapB(result);
}
```
Lo shift di +100 posizioni per i macro-branch differenzia ciascun ramo nell'albero. Poiché `D = 10000`, lo shift "+100" preserva la quasi-ortogonalità: `cosine(v, ρ¹⁰⁰(v)) ≈ 0`.

**Similarity — Cosine su Vettori Bipolari:**
```java
// HDVectorMapB.java:104-111
public double similarity(HDVector other) {
    HDVectorMapB o = (HDVectorMapB) other;
    int dotProduct = 0;
    for (int i = 0; i < D; i++) {
        dotProduct += this.values[i] * o.values[i];
    }
    return (double) dotProduct / D;
}
```
Valori: `1.0` = identici, `0.0` = ortogonali (rumore), `-1.0` = opposti. La similarità attesa tra due vettori generati indipendentemente è `0.0` con deviazione standard `1/√D ≈ 0.01`.

### 2.3 Generazione dei Vettori Atomici

I vettori di base per entità e proprietà sono generati in modo completamente **deterministico e riproducibile** tramite hashing crittografico:

```java
// RandomGenerationStrategy.java:12-13
long seed = UUID.nameUUIDFromBytes(uri.getBytes(StandardCharsets.UTF_8))
    .getMostSignificantBits();
return HDVectorMapB.generateSeeded(seed);
```

Pipeline: `URI → UTF-8 bytes → UUID.nameUUIDFromBytes (MD5) → 64-bit seed → java.util.Random(seed) LCG → 10000 bit bipolari`

Questo garantisce tre proprietà fondamentali:
1. **Stesso URI → stesso vettore** su qualsiasi JVM, OS, o esecuzione
2. **Nessun database**: i vettori sono rigenerati on-the-fly, non persistiti
3. **URI diversi → vettori quasi-ortogonali** con probabilità `1 - 2⁻⁶⁴` di collisione

### 2.4 Codifica Gerarchica: Il Simpkin Tree

Il sistema organizza le triple di un'entità in una struttura ad albero ispirata al **Simpkin Tree** dell'HDC:

```
Entità E (es. "Amleto")
├── Ramo: proprietà P1 (es. "author") ⊗ ρ¹⁰⁰
│   └── Chunk P1 = bundle(triple₁, triple₂, ..., tripleₙ, stopVec, [parity])
├── Ramo: proprietà P2 (es. "genre") ⊗ ρ¹⁰⁰
│   └── Chunk P2 = bundle(triple₁, ..., stopVec, [parity])
├── ...
├── Identità pura: vE
└── Parity fix (se necessario)
         ↓ bundle simultaneo
    TreeVector(E) = radice dell'albero
```

```java
// TopologicalVectorUpdater.java:21-61
private void updateNodeVectorHierarchical(ItemMemory memory, Resource node, Model model) {
    String entityUri = node.getURI();
    HDVector pureIdentity = memory.getOrGenerate(entityUri);

    // Raggruppa triple per predicato
    Map<Property, List<Statement>> buckets = allStatements.stream()
            .collect(Collectors.groupingBy(Statement::getPredicate));

    List<HDVector> macroBranches = new ArrayList<>();
    for (Map.Entry<Property, List<Statement>> entry : buckets.entrySet()) {
        Property predicate = entry.getKey();
        List<Statement> triples = entry.getValue();
        HDVector predicateVector = memory.getOrGenerate(predicate.getURI());

        // Chunk: bundle delle triple per questo predicato
        HDVector branchContent = (triples.size() <= MAX_CHUNK_CAPACITY) ?
                buildSimpleChunk(triples, memory) :
                buildRecursiveSubTree(triples, memory);

        // Salva il chunk nella memoria per clean-up futuro
        String chunkKey = entityUri + ":chunk:" + predicate.getURI();
        memory.saveChunkVector(chunkKey, branchContent);

        // Ramo = Chunk ⊗ Predicato ⊗ ρ¹⁰⁰
        macroBranches.add(branchContent.bind(predicateVector).permute(100));
    }

    // Aggiunge identità pura e parity fix
    macroBranches.add(pureIdentity);
    if (macroBranches.size() % 2 == 0) {
        macroBranches.add(memory.getOrGenerate("vsa:internal:parity_fix"));
    }

    // Bundle simultaneo di tutti i rami = TreeVector(E)
    HDVector rootVector = HDVectorMapB.bundleSimultaneous(macroBranches);
    memory.saveTreeVector(entityUri, rootVector);
}
```

Caratteristiche architetturali del Simpkin Tree:
- **Capacità massima per chunk**: 30 triple (`MAX_CHUNK_CAPACITY`). Oltre questa soglia, il chunk viene ricorsivamente suddiviso in sotto-chunk.
- **Stop vector**: Un delimitatore semantico (`vsa:internal:stop`) separa i chunk, migliorando il recupero in fase di clean-up.
- **Parity fix**: Se il numero di componenti nel bundle (o di macro-branch nell'albero) è pari, viene aggiunto un vettore di parità per garantire che la somma non collassi a zero su nessuna dimensione.
- **Permutazione +100**: Ogni ramo è shiftato di 100 posizioni per occupare una regione distinta dello spazio vettoriale, prevenendo interferenza tra rami diversi.

### 2.5 Memoria a Tre Livelli (ItemMemory)

```java
// ItemMemory.java
// 1. Foglie — Vettori atomici puri (entità, proprietà)
private final Map<String, HDVector> atomicMemory = new ConcurrentHashMap<>();

// 2. Chunk — Bundle di triple per singolo predicato
private final Map<String, HDVector> chunkMemory = new ConcurrentHashMap<>();

// 3. Radici — Simpkin Tree completi (TreeVector)
private final Map<String, HDVector> treeMemory = new ConcurrentHashMap<>();
```

La separazione in tre livelli è critica per il clean-up: quando si tenta di estrarre un chunk da un TreeVector rumoroso, si cerca nella `chunkMemory` (non nella `atomicMemory`), perché il segnale del chunk (bundle di triple) ha una firma statistica diversa da quella di un vettore atomico.

### 2.6 Integrazione Semantica

Il sistema dispone di due strategie di generazione vettoriale:

| Strategia | Classe | Meccanismo | Usata di default? |
|-----------|--------|------------|-------------------|
| **Random** | `RandomGenerationStrategy` | URI → UUID → seed → LCG → bipolare | **Sì** (entrambe le App) |
| **Semantic** | `SemanticEmbeddingStrategy` | URI → MiniLM embedding (384-d) → LSH → bipolare 10000-d | No |

La `SemanticEmbeddingStrategy` proietta embedding semantici densi nello spazio bipolare tramite una matrice di proiezione LSH fissa (seed=42), preservando le relazioni di similarità semantica: entità simili (es. "Apollo 11" e "Vostok 1") ricevono vettori con similarità > 0, anziché essere quasi-ortogonali come nella strategia random. Questa strategia non è abilitata di default, ma il suo utilizzo migliorerebbe la risonanza analogica tra domini affini.

---

## 3. Meccanismo di Ragionamento (Reasoning)

### 3.1 Principio dell'Unbinding

Il ragionamento avviene per **algebra lineare inversa**: grazie all'auto-inversione del binding bipolare e all'inversione della permutazione ciclica, è possibile estrarre selettivamente componenti da un vettore composito.

**Teorema di Estrazione dal Simpkin Tree:**

Dato `Tree(E) = bundle(..., Chunk_P ⊗ vP ⊗ ρ¹⁰⁰, ...)` dove `Chunk_P = bundle(encode(S₁,P,O₁), ..., encode(Sₙ,P,Oₙ))`:

```
Tree(E) ⊗ ρ⁻¹⁰⁰(vP) ≈ Chunk_P + rumore
```

Il risultato è una versione rumorosa del chunk, dove il rumore proviene dagli altri rami dell'albero che, essendo quasi-ortogonali, contribuiscono con similarità attesa ≈ 0.

### 3.2 Clean-Up Statistico (Z-Score)

Il clean-up è il meccanismo che separa il segnale dal rumore. Opera in due fasi:

**Fase 1: Ricerca del miglior candidato.**
Per ogni vettore `candidate` nel memory bank, calcola `sim = cosine(noisyVector, candidate)`.

**Fase 2: Test di significatività statistica.**
Calcola media e deviazione standard di tutte le similarità, poi verifica:

```
Z-Score = (bestSimilarity - mean) / stdDev ≥ threshold
```

```java
// ItemMemory.java:73-113
private HDVector performStatisticalCleanUp(HDVector noisyVector,
        Map<String, HDVector> memoryBank, double thresholdSigma) {
    if (memoryBank.isEmpty()) return null;

    List<Double> similarities = new ArrayList<>();
    double bestSimilarity = -1.0;
    HDVector bestMatch = null;
    String bestKey = null;

    for (Map.Entry<String, HDVector> entry : memoryBank.entrySet()) {
        double sim = noisyVector.similarity(entry.getValue());
        similarities.add(sim);
        if (sim > bestSimilarity) {
            bestSimilarity = sim; bestMatch = entry.getValue();
            bestKey = entry.getKey();
        }
    }

    // Calcolo Z-Score
    double mean = similarities.stream().mapToDouble(d -> d).average().orElse(0);
    double variance = similarities.stream()
        .mapToDouble(s -> (s - mean) * (s - mean)).average().orElse(0);
    double stdDev = Math.sqrt(variance);
    double sigmaFromMean = (stdDev > 0) ? (bestSimilarity - mean) / stdDev : 0;

    if (stdDev > 0 && sigmaFromMean >= thresholdSigma) return bestMatch;
    return null;
}
```

La soglia è dinamica: `threshold = log₁₀(D) = 4.0 σ` (per D=10000). Questo è un design intenzionale: a D=100000 la soglia salirebbe automaticamente a 5.0 σ, adattandosi alla maggiore capacità dello spazio.

### 3.3 Pipeline di Inferenza Completa

**STEP 1 — Scoperta del Ruolo nel Dominio Sorgente:**

```
Input: A (Apollo 11), B (Neil Armstrong), Tree(A)

Per ogni proprietà P ∈ proprietà(A):
  noisyBranch  = Tree(A) ⊗ ρ⁻¹⁰⁰(vP)      // 1. Estrai il ramo
  cleanBranch  = cleanUpChunk(noisyBranch)  // 2. Purifica il chunk
  if (cleanBranch == null) continue;        //    Ramo perso nel rumore
  
  noisyObject  = cleanBranch ⊗ vA ⊗ ρ¹(vP) ⊗ ρ⁻²  // 3. Unbind soggetto e predicato
  sim          = cosine(noisyObject, vB)            // 4. Confronta con B

Output: sourceRole = argmax_P sim  (es. "crew members" P1029)
```

Questo step determina *quale relazione* connette A a B, senza che questa informazione sia esplicitamente nota al sistema.

**STEP 2 — Ponte Semantico (Traduzione Cross-Ontologia):**

```
Input:  sourceRoleLabel ("crew members"), proprietà(targetEntity C)

Per ogni proprietà Q ∈ proprietà(C):
  sim = cosine(embed(sourceRoleLabel), embed(Q.label))
  
Output: targetRole = argmax_Q sim  (es. "crew members" in Vostok 1)
```

Il matching è puramente basato su similarità coseno tra embedding MiniLM (384-d). Se la similarità supera la soglia configurata (tipicamente 0.30-0.40), la proprietà viene selezionata. Opzionalmente, le label vengono arricchite con i type constraint di Wikidata (es. "crew members (value type: human)") per migliorare la precisione del matching.

**STEP 3 & 4 — Proiezione ed Estrazione nel Dominio Target:**

```
Input: C (Vostok 1), targetRole (P1029), Tree(C)

noisyBranch   = Tree(C) ⊗ ρ⁻¹⁰⁰(targetRole)     // 1. Estrai ramo del target
cleanBranch   = cleanUpChunk(noisyBranch)         // 2. Purifica chunk
noisyObject   = cleanBranch ⊗ vC ⊗ ρ¹(targetRole) ⊗ ρ⁻²  // 3. Unbind
finalAnalogue = cleanUpRelative(noisyObject)      // 4. Clean-up finale

Output: D = finalAnalogue → label(D)  (es. "Yuri Gagarin")
```

### 3.4 Gestione dei Path Multi-Hop

Il sistema non esegue esplicitamente path multi-hop. Tuttavia, il meccanismo di unbinding dal Simpkin Tree realizza implicitamente un reasoning a due hop:

```
Hop 1: E → (Tree(E) ⊗ ρ⁻¹⁰⁰(vP)) → Chunk_P
Hop 2: Chunk_P → (Chunk_P ⊗ vE ⊗ ρ¹(vP) ⊗ ρ⁻²) → Oggetto O
```

Dove il passaggio intermedio (Chunk_P) è un bundle di *tutte* le triple con predicato P, e l'estrazione dell'oggetto specifico avviene proprio grazie all'unbinding del soggetto: `Chunk_P ⊗ vA` sopprime tutti gli oggetti che non appartengono ad A, facendo emergere solo l'oggetto desiderato.

---

## 4. Tolleranza all'Incompletezza (Open-World Assumption)

### 4.1 Principio Olografico della VSA

La proprietà fondamentale che abilita il reasoning open-world è la **natura olografica** dei vettori VSA. In un vettore olografico:

1. **L'informazione è distribuita** su tutte le D dimensioni. Non esiste una corrispondenza localizzata "dimensione i = entità X". Ogni dimensione contribuisce a rappresentare ogni entità.

2. **La sovrapposizione è non-distruttiva**. Il bundle di N vettori produce un vettore che risuona con *ciascuno* dei componenti originali, anche se il rapporto segnale-rumore (SNR) si degrada come `1/√N`.

3. **Le operazioni sono associative e commutative dove appropriato**, permettendo di riordinare le query senza cambiare il risultato.

### 4.2 Implicazioni per l'Open-World QA

**Entità non viste nel training:**
Il sistema non ha una fase di training. Ogni URI viene convertita in un vettore deterministico al volo. Un'entità mai incontrata prima (es. un nuovo film su Wikidata) riceve immediatamente un vettore, e il suo Simpkin Tree viene costruito scaricando il grafo locale in tempo reale. Non esiste alcuna distinzione tra "entità viste" ed "entità nuove".

**Relazioni incomplete:**
La similarità tra vettori atomici nello spazio a 10000 dimensioni permette di inferire relazioni non esplicitamente presenti. Se due entità hanno vettori simili (es. entrambe sono "missioni spaziali"), il sistema può trasferire pattern relazionali tra di esse anche in assenza di una connessione diretta nel grafo.

**Path non materializzati:**
Il clean-up statistico è il meccanismo chiave. Anche se il segnale di unbinding è parzialmente degradato dal rumore (SNR basso), il test Z-Score permette di distinguere un match genuino dal rumore di fondo. La soglia `log₁₀(D)` fornisce un intervallo di confidenza formale: con D=10000, un match a 4.0 σ ha probabilità di falso positivo inferiore a `3.2 × 10⁻⁵`.

### 4.3 Limiti dell'Approccio Corrente

L'analisi del codice rivela una criticità documentata nel `PROJECT_REPORT.md`: il **clean-up intermedio dei chunk fallisce frequentemente** quando il numero di proprietà per entità è elevato (30-50), perché il bundle di molti rami degrada l'SNR di ciascun ramo. Il workaround ("Holographic Bypass") salta il clean-up intermedio, ma questo propaga il rumore allo step successivo. Possibili soluzioni esplorate nel report:
- Aumentare `D` a 100000 (10× capacità)
- Soglia di clean-up adattiva inversamente proporzionale al numero di candidati
- Utilizzare `SemanticEmbeddingStrategy` per ridurre l'ortogonalità tra entità correlate

---

## 5. Ruolo dell'LLM

### 5.1 Natura del "LLM" nel Sistema

**Il sistema non effettua alcuna chiamata API a un Large Language Model esterno.** Il package `llm/` e il nome della classe `OntologyTranslator` sono fuorvianti: ciò che il codice chiama "LLM" è in realtà:

```java
// OntologyTranslator.java:18-19
private final EmbeddingModel llm;
this.llm = new AllMiniLmL6V2EmbeddingModel();
```

Il modello `all-MiniLM-L6-v2` è un **encoder embedding** locale (non generativo) di 384 dimensioni, eseguito interamente in-process tramite LangChain4j. Il download del modello avviene automaticamente al primo utilizzo (~22 MB).

### 5.2 Ruoli Specifici dell'Embedding Model

L'embedding model (non un LLM generativo) viene utilizzato per tre compiti esclusivamente basati su **cosine similarity**:

1. **Disambiguazione entità (Reranker):**
```java
// OntologyTranslator.java:178-209
public ResolvedEntity disambiguateCandidates(String targetClassLabel,
        List<ResolvedEntity> candidates) {
    // combinedScore = classSimilarity × 0.60 + sitelinksNorm × 0.40
    // classSimilarity = cosine(embed(targetClass), embed(candidate.classLabel))
}
```
Date più entità candidate con lo stesso nome (es. "Nile" può essere il fiume, un film, una band), il reranker seleziona quella la cui classe ontologica (P31) è semanticamente più vicina alla classe dell'entità sorgente.

2. **Verifica compatibilità ontologica:**
```java
// OntologyTranslator.java:168-176
public boolean areClassesCompatible(String classLabelA, String classLabelB,
        double threshold) {
    double sim = CosineSimilarity.between(
        llm.embed(classLabelA).content(), llm.embed(classLabelB).content());
    return sim >= threshold;  // default: 0.35
}
```
Esempio: `cosine(embed("human spaceflight"), embed("space mission")) = 0.73 ≥ 0.35 → compatibili`.

3. **Traduzione cross-ontologica (Semantic Bridge):**
```java
// OntologyTranslator.java:114-166
public String findSemanticEquivalent(String sourceLabel, String sourcePropertyUri,
        Map<String, String> targetProperties, double threshold) {
    Embedding sourceEmbedding = llm.embed(enrichedSource).content();
    for (Map.Entry<String, String> entry : targetProperties.entrySet()) {
        Embedding targetEmbedding = llm.embed(entry.getValue()).content();
        double similarity = CosineSimilarity.between(sourceEmbedding, targetEmbedding);
        if (similarity > bestScore) { bestScore = similarity; bestUriMatch = entry.getKey(); }
    }
    return (bestScore >= threshold) ? bestUriMatch : null;
}
```

### 5.3 Numero di Chiamate di Embedding per Query

Per una singola query analogica, il numero di embedding calcolati è:

| Fase | Chiamate di embedding | Note |
|------|----------------------|------|
| Reranking | `N_candidates` embedding (class label) | Tipicamente 1-5 |
| Compatibilità | 2 embedding (class sorgente + class target) | |
| Ponte semantico | 1 embedding (ruolo sorgente) + `M_proprietà` (proprietà target) | M ≈ 30-50 per entità Wikidata |
| **Totale** | **~35-60 embedding** | Tutti in-process, nessuna API esterna |

Tutti gli embedding sono calcolati localmente e cachati in `ConcurrentHashMap` per evitare ricalcoli.

### 5.4 Arricchimento con Type Constraints

Opzionalmente, le label delle proprietà vengono arricchite con il tipo atteso (value-type constraint di Wikidata) prima dell'embedding:

```java
// OntologyTranslator.java:26-44
// Esempio: "composer" → "composer (value type: human)"
enriched.put(uri, label + " (value type: " + expectedType + ")");
```

Questo migliora la precisione del matching cross-ontologia, specialmente quando due domini usano nomi di proprietà diversi per lo stesso concetto astratto.

---

## 6. Efficienza e Complessità Computazionale

### 6.1 Complessità Temporale (Big-O)

| Operazione | Complessità | Note |
|-----------|-------------|-------|
| **Generazione vettore atomico** | `O(D)` | Hashing + LCG, one-shot per URI |
| **Bind (⊗)** | `O(D)` | Moltiplicazione element-wise |
| **Bundle (2 vettori)** | `O(D)` | Somma + signum |
| **Bundle (K vettori)** | `O(K·D)` | Somma + signum + tie-break |
| **Permute (ρⁿ)** | `O(D)` | Copia con shift ciclico |
| **Similarity** | `O(D)` | Dot product normalizzato |
| **Clean-up (M candidati)** | `O(M·D)` | M similarità + media/varianza |
| **Costruzione Simpkin Tree** | `O(P·C·D)` | P proprietà, C chunk build, D operazioni |
| **Role Deduction (STEP 1)** | `O(P·(M_chunk·D + M_atom·D))` | Per ogni proprietà P: unbind + 2 clean-up |
| **Semantic Bridge (STEP 2)** | `O(Q·E)` | Q proprietà target, E = dim embedding (384) |
| **Extraction (STEP 3-4)** | `O(M_chunk·D + M_atom·D)` | Unbind + 2 clean-up |

Dove:
- `D = 10000` (dimensionalità VSA)
- `P =` numero di proprietà dell'entità sorgente (~30-50)
- `C =` dimensione media di un chunk (≤ 30 triple)
- `M_chunk ≈ P` (numero di chunk nel memory bank)
- `M_atom =` numero di vettori atomici generati (~100-200)
- `E = 384` (dimensionalità embedding MiniLM)

### 6.2 Complessità Spaziale (Big-O)

| Struttura | Footprint | Note |
|-----------|-----------|------|
| **Vettore HD (1×)** | `D × 1 byte = 10 KB` | byte[] di 10000 elementi |
| **atomicMemory** | `O(N_atom · D)` | ~100-200 vettori → 1-2 MB |
| **chunkMemory** | `O(E·P · D)` | E entità, P proprietà → ~5-15 MB |
| **treeMemory** | `O(E · D)` | ~10 KB per entità |
| **MiniLM Model** | 22 MB | Caricato una volta in RAM |
| **projectionMatrix** (LSH) | `E × D × 4 bytes = 384 × 10000 × 4 = 15.36 MB` | Solo se SemanticEmbeddingStrategy è attiva |
| **Totale stimato** | **~40-60 MB** | Per un'esecuzione tipica |

### 6.3 Assenza di Training

Il sistema opera interamente in **forward-pass algebrico**:

- **Nessuna backpropagation**: non ci sono pesi addestrabili. Tutti i vettori sono generati deterministicamente o calcolati tramite operazioni algebriche.
- **Nessuna discesa del gradiente**: l'ottimizzazione è sostituita dal clean-up statistico (Z-Score).
- **Nessun dataset di training**: la conoscenza proviene interamente da Wikidata in tempo reale.
- **Nessun fine-tuning**: il modello MiniLM è usato off-the-shelf, solo per embedding.

Questo rende il sistema adatto a scenari dove i dati cambiano frequentemente e il retraining non è praticabile.

### 6.4 Profilo di Esecuzione

Per una tipica query analogica (es. Amleto:Shakespeare = Lacrimosa:?), il costo dominante è:

1. **3 query SPARQL a Wikidata** (entity resolution + 2× graph ingestion): latenza di rete, ~500ms-2s ciascuna
2. **~50 embedding MiniLM**: computazione locale, ~10-50ms totali
3. **Costruzione Simpkin Tree (2 entità)**: O(P·D) ~ 30×10000 = 300K operazioni, < 1ms
4. **Role Deduction (30 proprietà)**: O(30·(30×10000 + 100×10000)) = O(39M) operazioni, ~20-50ms
5. **Extraction finale**: O(30×10000 + 100×10000) = O(1.3M) operazioni, ~1-5ms

**Latenza totale stimata**: 2-6 secondi, dominata dalle chiamate HTTP a Wikidata.

---

## 7. Riepilogo delle Innovazioni Architetturali

1. **Ragionamento puramente algebrico**: L'intera pipeline di inferenza è composta da moltiplicazioni, somme, shift e dot product — operazioni lineari senza training.

2. **Memoria olografica senza database**: I vettori non sono memorizzati ma rigenerati deterministicamente da URI. Il sistema scala orizzontalmente senza overhead di sincronizzazione.

3. **Clean-up statistico adattivo**: La soglia Z-Score `log₁₀(D)` si adatta automaticamente alla dimensionalità, fornendo un framework formale per il controllo dei falsi positivi.

4. **Ponte semantico neuro-simbolico**: La fusione di VSA (struttura) e embedding (semantica) permette il reasoning cross-ontologia senza richiedere un allineamento manuale degli schemi.

5. **Open-world by design**: Ogni entità è istanziabile al volo. Non esiste un insieme chiuso di entità o relazioni. Il sistema può ragionare su qualsiasi nodo Wikidata senza pre-processing.
