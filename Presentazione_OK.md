# Scaletta Presentazione — Neuro-Semantic Investigator

---

## 1. L'OBIETTIVO E LA PREPARAZIONE: Risolvere Analogie con Reranking Semantico

### Concetto Teorico (Cosa dire)
> «Il sistema risolve proporzioni strutturali su knowledge graph open-world (es. Amleto : Shakespeare = Lacrimosa : ?). Ma prima di applicare l'algebra vettoriale, c'è un problema pratico: il "Popularity Bias". Se cerchiamo "Lacrimosa", Wikidata potrebbe restituirci 5 entità diverse.
>
> Per risolverlo, usiamo un modello di embedding locale a 768 dimensioni come **Reranker**. Chiediamo all'encoder di scegliere il candidato la cui classe ontologica (es. "musical work") è semanticamente più vicina alla classe dell'entità di partenza (es. "dramatic work" di Amleto). Una volta isolata l'entità corretta, il sistema ne scarica il grafo locale in tempo reale per iniziare il ragionamento vettoriale.»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **Entry point principale** | `AppDis.java` | `14` | `public static void main(String[] args)` |
| Entità di partenza (A e B) | `AppDis.java` | `30-31` | `resolver.resolve("Amleto", 1)` |
| Recupero candidati multipli (Target) | `AppDis.java` | `34` | `resolver.resolve("Lacrimosa", 5)` |
| **Disambiguazione (Reranker)** | `AppDis.java` | `46` | `translator.disambiguateCandidates(...)` |
| Controllo compatibilità classi | `AppDis.java` | `64` | `translator.areClassesCompatible(..., 0.35)` |
| Ingestione grafo in tempo reale | `AppDis.java` | `125-126` | `engine.expandAndProcess(...)` |

---

## 2. IL CORE VSA: Vettori Bipolari, Rigenerazione Deterministica, Operazioni

### Concetto Teorico (Cosa dire)
> «Come facciamo i calcoli sul grafo scaricato? I vettori VSA sono array di $D=10000$ valori bipolari $\{-1, +1\}$ (`byte[]`). Non li salviamo in un database: ogni URI Wikidata viene trasformata in vettore **deterministicamente** tramite hash MD5 → seed → generatore casuale. Stessa URI = stesso vettore, sempre.
>
> L'algebra si basa su operazioni native a bassissimo costo computazionale:
> - **Bind ($\otimes$)** = Prodotto di Hadamard. È auto-inverso: $a \otimes b \otimes b = a$.
> - **Bundle ($\oplus$)** = Somma componente per componente + majority vote.
> - **Permute ($\rho^n$)** = Shift circolare di $n$ posizioni per proteggere la direzionalità (soggetto/oggetto).
> - **Similarity** = Prodotto scalare / $D$ (cosine similarity).»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **Dimensionalità D=10000** | `HDVectorMapB.java` | `8` | `public static final int D = 10000;` |
| **Generazione deterministica** | `RandomGenerationStrategy.java` | `11-13` | `UUID.nameUUIDFromBytes(...).getMostSignificantBits()` |
| **Bind (Hadamard)** | `HDVectorMapB.java` | `63-70` | `result[i] = this.values[i] * o.values[i]` |
| **Bundle pairwise** | `HDVectorMapB.java` | `73-90` | `sum = a[i] + b[i]` → signum |
| **Permute (shift circolare)** | `HDVectorMapB.java` | `93-101` | `newIndex = (i + shift) % D;` |

---

## 3. L'INNOVAZIONE ARCHITETTURALE: Simpkin Tree e Scalabilità Olografica

### Concetto Teorico (Cosa dire)
> «Le VSA classiche hanno un limite matematico: sovrapponendo troppe triple (oltre ~89) in un vettore piatto, il rumore di fondo distrugge il segnale. Per mappare entità Wikidata con centinaia di proprietà senza perdere dati, abbiamo implementato l'approccio *Hierarchical Vector Chunking* (Simpkin et al., 2018).
>
> Invece di un calderone unico, costruiamo un albero (Simpkin Tree). Raggruppiamo le triple in sotto-blocchi ("chunk") da massimo 30 elementi. Salviamo questi livelli intermedi in una memoria a cascata (`ItemMemory`). Quando estraiamo un dato, non andiamo diretti al bersaglio: estraiamo prima il chunk (rumoroso), lo passiamo nella memoria intermedia per recuperare il chunk **perfettamente pulito**, e da lì estraiamo il dato finale. Questo "resetta" il rumore a ogni salto.»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **Codifica tripla RDF** | `TopologicalVectorUpdater.java` | `89-98` | `vS.bind(vP.permute(1)).bind(vO.permute(2))` |
| **Limite di sicurezza del rumore** | `TopologicalVectorUpdater.java` | `10` | `MAX_CHUNK_CAPACITY = 30;` |
| **Biforcazione gerarchica** | `TopologicalVectorUpdater.java` | `37-39` | `buildSimpleChunk()` vs `buildRecursiveSubTree()` |
| **Le 3 memorie (Atomi, Chunk, Albero)** | `ItemMemory.java` | `11-17` | `ConcurrentHashMap` separate per livello di astrazione |

---

## 4. IL MOTORE DI INFERENZA: Unbinding Sorgente e Test Z-Score (STEP 1)

### Concetto Teorico (Cosa dire)
> «Inizia l'inferenza vera e propria sulla Sorgente (es. Amleto). Vogliamo capire *quale* proprietà lega Amleto a Shakespeare, senza saperlo a priori. L'algoritmo fa un "unbinding" algebrico:
>
> Prova ogni proprietà conosciuta. Estrae il ramo, applica la pulizia intermedia del chunk, estrae l'oggetto e ne calcola la similarità col target noto.
> Non usiamo soglie fisse, ma un test **Z-Score** formale. Calcoliamo media e varianza del rumore: se la similarità dista più di $4.0 \sigma$ (soglia dinamica derivata da $\log_{10}(D)$) dalla media, accettiamo l'intuizione. Il sistema scopre matematicamente che il legame è "author".»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **Estrazione del ramo (rumoroso)** | `AppDis.java` | `139` | `apolloRoot.permute(-100).bind(candidateRole)` |
| **Estrazione oggetto pulito** | `AppDis.java` | `143` | `cleanBranch.bind(subjectVector).bind(...).permute(-2)` |
| Verifica similarità matematica | `AppDis.java` | `144` | `noisyObject.similarity(armstrongVector)` |
| **Clean-up statistico (motore)** | `ItemMemory.java` | `73-113` | `performStatisticalCleanUp(...)` |
| **Formula Z-Score e Soglia 4.0 σ** | `ItemMemory.java` | `35` e `101` | `Math.log10(HDVectorMapB.D)` / Calcolo deviazione standard |

---

## 5. IL PONTE SEMANTICO: Filtro Strutturale ed Embedding 768-dim (STEP 2)

### Concetto Teorico (Cosa dire)
> «Sappiamo che la relazione è "author". Ma sul Nilo o nel dominio musicale, "author" non esiste. Prima applichiamo un **Filtro Strutturale RDF**: interroghiamo il grafo locale per scartare a priori le proprietà del target che puntano a stringhe o date, tenendo solo quelle che puntano a entità.
>
> Poi usiamo il nostro modello di embedding (ONNX 768-dim) per calcolare la Cosine Similarity tra "author" e i candidati rimasti. Il sistema trova da solo che "composer" è l'equivalente corretto.»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **Filtro Strutturale RDF (scrematura)** | `AppDis.java` | `154-173` | Iterazione sulle proprietà: controlla se l'oggetto `isResource()` |
| **Traduzione Semantica (Ponte)** | `AppDis.java` | `182` | `translator.findSemanticEquivalent(sourceRoleLabel, ..., 0.30)` |
| Algoritmo Cosine Similarity | `OntologyTranslator.java` | `146` | `CosineSimilarity.between(sourceEmbedding, targetEmbedding)` |

---

## 6. PROIEZIONE ED ESTRAZIONE SUL TARGET (STEP 3 & 4)

### Concetto Teorico (Cosa dire)
> «Torniamo al VSA. Proiettiamo la relazione tradotta ("composer") sul vettore di "Lacrimosa".
> Estraiamo il ramo, lo purifichiamo col Simpkin clean-up, svincoliamo l'oggetto e facciamo l'ultimo Z-Score contro la memoria atomica per trovare il nome preciso. Il sistema restituisce l'incognita finale: Franz Xaver Süssmayr.»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| Proiezione sul bersaglio target | `AppDis.java` | `198` | `targetRoot.permute(-100).bind(targetRole)` |
| **Clean-up intermedio a cascata** | `AppDis.java` | `201` | `itemMemory.cleanUpChunk(noisyTargetBranch)` |
| Svincolo finale dell'oggetto | `AppDis.java` | `212` | `pureTargetBranch.bind(targetSubject).bind(...).permute(-2)` |
| **Clean-up atomico finale (Il Risultato)**| `AppDis.java` | `215` | `itemMemory.cleanUpRelative(noisyTargetObject)` |

---

## RIEPILOGO RAPIDO: Traccia di Navigazione

*Tieni questo riepilogo sottomano: ti dice esattamente dove scorrere il file `AppDis.java` in tempo reale durante la presentazione.*

| Fase Logica | Scorri `AppDis.java` fino a: | Righe |
|---|---|---|
| **1. Disambiguazione (Reranker)** | Il blocco `resolver.resolve` e `disambiguateCandidates` | 30 - 46 |
| **2. Motore VSA e Simpkin** | Apri brevemente `HDVectorMapB.java` e `ItemMemory.java` | — |
| **3. STEP 1 (Unbinding Sorgente)** | Il ciclo `for (Property prop : apolloProps)` | 134 - 149 |
| **4. STEP 2 (Ponte Semantico)** | Il blocco "Filtro Strutturale RDF" e `findSemanticEquivalent` | 154 - 183 |
| **5. STEP 3 & 4 (Estrazione Target)** | Il commento `FASE 1: Estrazione e Purificazione` fino in fondo | 195 - 217 |