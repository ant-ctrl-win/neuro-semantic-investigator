# Scaletta Presentazione — Neuro-Semantic Investigator

---

## 1. L'OBIETTIVO: Risolvere Analogie su Wikidata Senza Addestramento

### Concetto Teorico (Cosa dire)
> «Date due coppie di entità A:B note (es. Amazon:Brazil), e una terza entità C (es. Nile), il sistema trova D tale che A:B = C:D — tutto senza training, senza backpropagation, senza database di vettori pre-calcolati. È pura algebra lineare in uno spazio vettoriale iperdimensionale (VSA / Hyperdimensional Computing). Non "impariamo" embedding: codifichiamo la topologia del knowledge graph in vettori e usiamo l'algebra per navigarlo.»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga |
|---|---|---|
| Entry point (pipeline base) | `App.java` | `14` (`main`) |
| Entry point (con disambiguazione) | `AppDis.java` | `16` (`main`) |
| Entità hardcoded da modificare | `App.java` | `30-36` (righe con `resolver.resolve(...)`) |
| Orchestratore che coordina tutto | `InvestigationEngine.java` | `63` (`expandAndProcess`) |

---

## 2. IL CORE VSA: Vettori Bipolari, Rigenerazione Deterministica, Operazioni Algebriche

### Concetto Teorico (Cosa dire)
> «I vettori VSA sono array di **D=10000** valori bipolari {-1, +1} (`byte[]`). Non li salviamo da nessuna parte: ogni URI Wikidata viene trasformata in vettore **deterministicamente** tramite `UUID.nameUUIDFromBytes` (hash MD5) → seed a 64 bit → `Random(seed)` LCG → 10000 bit casuali ma **riproducibili**. Stessa URI = stesso vettore su qualsiasi JVM, senza database. Le quattro operazioni algebriche fondamentali sono:
> - **Bind (⊗)** = Prodotto di Hadamard componente per componente. È auto-inverso: `a ⊗ b ⊗ b = a`.
> - **Bundle (⊕)** = Somma componente per componente + signum. Tie-break deterministico posizionale (`i%2`).
> - **Bundle N-ario (⊕ₙ)** = Bundle simultaneo con tie-break basato su XOR-hash dei vettori.
> - **Permute (ρⁿ)** = Shift circolare di n posizioni. Protegge il ruolo sintattico di ogni componente.
> - **Similarity** = Prodotto scalare / D (cosine similarity normalizzato). Range [-1, +1].»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **Dimensionalità D=10000** | `HDVectorMapB.java` | `8` | `public static final int D = 10000;` |
| Interfaccia VSA | `HDVector.java` | `3-7` | `bind()`, `bundle()`, `permute()`, `similarity()` |
| **Generazione deterministica da URI** | `RandomGenerationStrategy.java` | `11-13` | `UUID.nameUUIDFromBytes(uri.getBytes(...)).getMostSignificantBits()` → `generateSeeded(seed)` |
| Costruttore seeded | `HDVectorMapB.java` | `28-35` | `new Random(seed)` → `rnd.nextBoolean() ? 1 : -1` |
| **Bind (Hadamard)** | `HDVectorMapB.java` | `63-70` | `result[i] = this.values[i] * o.values[i]` |
| **Bundle pairwise** | `HDVectorMapB.java` | `73-90` | `sum = a[i] + b[i]` → signum; tie-break `(i%2==0)?1:-1` |
| **Bundle N-ario** | `HDVectorMapB.java` | `37-60` | `Arrays.hashCode(bv.values)` XOR cumulativo come seed del tie-breaker |
| **Permute (shift circolare)** | `HDVectorMapB.java` | `93-101` | `newIndex = (i + shift) % D; if(newIndex<0) newIndex+=D` |
| **Similarity (cosine)** | `HDVectorMapB.java` | `104-111` | `dotProduct += this[i] * o[i]; return dotProduct / D` |

---

## 3. IL SIMPKIN TREE: Come Comprimiamo le Triple RDF in Chunk e Rami Vettoriali

### Concetto Teorico (Cosa dire)
> «Ogni entità Wikidata diventa un **Simpkin Tree**: una gerarchia vettoriale a 3 livelli. Partendo dalle triple RDF (Soggetto, Predicato, Oggetto):
>
> **Livello 1 — Foglia (Tripla):** `v(S) ⊗ ρ¹(vP) ⊗ ρ²(vO)` — codifichiamo soggetto, predicato e oggetto con shift protettivi.
>
> **Livello 2 — Chunk:** Raggruppiamo fino a **30 triple** per predicato. Facciamo il bundle di tutte le triple codificate + un vettore "STOP" (`vsa:internal:stop`) come delimitatore. Se ci sono più di 30 triple, splittiamo ricorsivamente in sotto-chunk.
>
> **Livello 3 — Radice (Tree):** Per ogni predicato P dell'entità, creiamo un **ramo**: `Chunk ⊗ vP ⊗ ρ¹⁰⁰`. Poi facciamo il bundle di tutti i rami + il vettore dell'identità pura dell'entità. Il risultato è il **Mega-Vettore** (radice) che rappresenta l'intera entità.
>
> Le **3 memorie** (`ItemMemory`) memorizzano separatamente foglie, chunk e radici per permettere il clean-up mirato durante l'inferenza.»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **Codifica tripla: S⊗ρ¹(P)⊗ρ²(O)** | `TopologicalVectorUpdater.java` | `89-98` | `vS.bind(vP.permute(1)).bind(vO.permute(2))` |
| Stessa codifica (duplicata nell'engine) | `InvestigationEngine.java` | `50-52` | `vS.bind(vP.permute(1)).bind(vO.permute(2))` |
| **MAX_CHUNK_CAPACITY = 30** | `TopologicalVectorUpdater.java` | `10` | `private static final int MAX_CHUNK_CAPACITY = 30;` |
| Costruzione chunk semplice | `TopologicalVectorUpdater.java` | `63-74` | `buildSimpleChunk()` → bundle triple + stop vector + parity |
| Costruzione sotto-albero ricorsivo | `TopologicalVectorUpdater.java` | `76-87` | `buildRecursiveSubTree()` → split in gruppi da 30 |
| **Ramo = Chunk ⊗ Predicato ⊗ ρ¹⁰⁰** | `TopologicalVectorUpdater.java` | `48` | `branchContent.bind(predicateVector).permute(100)` |
| Identità pura al livello radice | `TopologicalVectorUpdater.java` | `53` | `macroBranches.add(pureIdentity)` |
| **Radice = bundle simultaneo di tutti i rami** | `TopologicalVectorUpdater.java` | `59` | `HDVectorMapB.bundleSimultaneous(macroBranches)` |
| Salvataggio radice nella memoria | `TopologicalVectorUpdater.java` | `60` | `memory.saveTreeVector(entityUri, rootVector)` |
| **Memoria atomica (foglie)** | `ItemMemory.java` | `11` | `ConcurrentHashMap<String, HDVector> atomicMemory` |
| **Memoria chunk (intermedia)** | `ItemMemory.java` | `14` | `ConcurrentHashMap<String, HDVector> chunkMemory` |
| **Memoria albero (radici)** | `ItemMemory.java` | `17` | `ConcurrentHashMap<String, HDVector> treeMemory` |
| Salva chunk | `ItemMemory.java` | `58-60` | `saveChunkVector(chunkUri, chunk)` |
| Recupera radice (con fallback) | `ItemMemory.java` | `53-55` | `getTreeVector(uri)` → `treeMemory.getOrDefault(uri, getOrGenerate(uri))` |
| Generazione on-demand da URI | `ItemMemory.java` | `40-42` | `getOrGenerate(uri)` → `atomicMemory.computeIfAbsent(uri, k -> strategy.generate(uri))` |
| **Aggiornamento topologico gerarchico** | `TopologicalVectorUpdater.java` | `21-61` | `updateNodeVectorHierarchical(memory, node, model)` |

---

## 4. IL MOTORE DI INFERENZA: Unbinding Algebrico e Test Z-Score

### Concetto Teorico (Cosa dire)
> «L'inferenza è pura **manipolazione algebrica** dei vettori, senza reti neurali. Il processo si articola in 4 passi:
>
> **STEP 1 — Deduzione del Ruolo Sorgente:** Per ogni proprietà P di A, facciamo l'unbinding:
> 1. `Tree(A) ⊗ ρ⁻¹⁰⁰(vP)` → estrae (rumorosamente) il chunk associato a P
> 2. `cleanUpChunk(noisyChunk)` → cerca nel `chunkMemory` il chunk puro più simile tramite **Z-Score**
> 3. `cleanChunk ⊗ vS ⊗ ρ¹(vP) ⊗ ρ⁻²` → estrae l'oggetto candidato
> 4. `similarity(candidateObject, vB)` → se > 0.05, P è il ruolo che lega A a B
>
> **Il Test Z-Score:** Non usiamo una soglia fissa di similarità, ma un test statistico. Calcoliamo media μ e deviazione standard σ di tutte le similarità nel memory bank. Se `(bestSimilarity − μ) / σ ≥ soglia`, accettiamo il match. La soglia è dinamica: `log₁₀(D)` = **4.0 σ** per D=10000. Questo filtra il rumore di fondo in modo adattivo.
>
> **STEP 2 — Ponte Analogico:** (vedi Sezione 5)
>
> **STEP 3 & 4 — Proiezione ed Estrazione sul Target:**
> 1. `Tree(C) ⊗ ρ⁻¹⁰⁰(vP_target)` → estrae il ramo corrispondente sul target
> 2. `cleanUpChunk(noisyChunk)` → pulisce il chunk
> 3. `cleanChunk ⊗ vC ⊗ ρ¹(vP_target) ⊗ ρ⁻²` → estrae l'oggetto target
> 4. `cleanUpRelative(noisyObject)` → cerca nell'`atomicMemory` il vettore foglia puro → **D trovato!**»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **STEP 1: Unbinding del ramo** | `App.java` | `159` | `apolloRoot.permute(-100).bind(candidateRole)` |
| STEP 1: Clean-up chunk | `App.java` | `160` | `itemMemory.cleanUpChunk(noisyBranch)` |
| **STEP 1: Estrazione oggetto candidato** | `App.java` | `163` | `cleanBranch.bind(subjectVector).bind(candidateRole.permute(1)).permute(-2)` |
| STEP 1: Verifica similarità con B | `App.java` | `164` | `noisyObject.similarity(armstrongVector)` |
| STEP 1: Soglia raw 0.05 | `App.java` | `166` | `rawSimilarity > 0.05` |
| **STEP 3: Proiezione su target** | `App.java` | `206` | `targetRoot.permute(-100).bind(targetRole)` |
| STEP 3: Clean-up chunk target | `App.java` | `207` | `itemMemory.cleanUpChunk(noisyTargetBranch)` |
| **STEP 4: Estrazione oggetto target** | `App.java` | `214` | `cleanTargetBranch.bind(targetSubject).bind(targetRole.permute(1)).permute(-2)` |
| STEP 4: Clean-up finale (atomi) | `App.java` | `215` | `itemMemory.cleanUpRelative(noisyTargetObject)` |
| Risultato umano | `App.java` | `222` | `fetchLabelFromWikidata(itemMemory.getLastBestKey())` |
| **Clean-up statistico (motore centrale)** | `ItemMemory.java` | `73-113` | `performStatisticalCleanUp(noisyVector, memoryBank, thresholdSigma)` |
| Iterazione su tutti i candidati | `ItemMemory.java` | `81-90` | `for (entry : memoryBank)` → `noisyVector.similarity(candidate)` |
| **Calcolo media e varianza** | `ItemMemory.java` | `92-99` | `mean = sum/size; variance = Σ(s-mean)²/size; stdDev = sqrt(variance)` |
| **Formula Z-Score** | `ItemMemory.java` | `101` | `sigmaFromMean = (bestSimilarity - mean) / stdDev` |
| **Condizione di accettazione** | `ItemMemory.java` | `109` | `if (stdDev > 0 && sigmaFromMean >= thresholdSigma) return bestMatch` |
| **Soglia dinamica** | `ItemMemory.java` | `35` | `dynamicThresholdSigma = Math.log10(HDVectorMapB.D)` = **4.0 σ** |
| Clean-up per chunk (memoria intermedia) | `ItemMemory.java` | `63-65` | `cleanUpChunk(noisyChunk)` → `performStatisticalCleanUp(noisyChunk, chunkMemory, thresholdSigma)` |
| Clean-up per atomi (memoria foglie) | `ItemMemory.java` | `68-70` | `cleanUpRelative(noisyVector)` → `performStatisticalCleanUp(noisyVector, atomicMemory, thresholdSigma)` |

---

## 5. IL PONTE SEMANTICO: Cosine Similarity su Embedding 768-dim per Tradurre Relazioni tra Domini

### Concetto Teorico (Cosa dire)
> «Il VSA lavora in uno spazio **sintattico** (D=10000). Ma quando due domini sono diversi (es. "astronauta su missione spaziale" vs "esploratore su fiume"), il nome delle proprietà cambia: `crew member (P1029)` su Apollo 11 vs `significant person (P3342)` sul Nilo. Qui entra in gioco il **Ponte Semantico**:
>
> Usiamo un modello ONNX locale **bge-base-en-v1.5** (768 dimensioni) per calcolare la **Cosine Similarity** tra le label in inglese delle proprietà. Per ogni proprietà sorgente, embeddiamo la sua label e cerchiamo la proprietà target con la massima similarità coseno sopra una soglia (tipicamente 0.30-0.40).
>
> Il sistema arricchisce le label con i **type constraint** di Wikidata (es. `crew member (value type: human)`) per migliorare la precisione del matching. Per la **disambiguazione** di entità con nome ambiguo, usiamo uno scoring combinato: 60% similarità coseno tra classi + 40% sitelinks normalizzati.»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **Dimensione embedding = 768** | `OntologyTranslator.java` | `15` | `public static final int EMBEDDING_DIMENSION = 768;` |
| Caricamento modello ONNX | `OntologyTranslator.java` | `21` | `new OnnxEmbeddingModel("models/model.onnx")` |
| **findSemanticEquivalent (core matching)** | `OntologyTranslator.java` | `116-168` | Itera proprietà target, embedda, calcola cosine |
| Embedding della sorgente | `OntologyTranslator.java` | `133` | `encoder.embed(enrichedSource).content()` |
| Embedding del target candidato | `OntologyTranslator.java` | `145` | `encoder.embed(targetLabel).content()` |
| **Cosine Similarity** | `OntologyTranslator.java` | `146` | `CosineSimilarity.between(sourceEmbedding, targetEmbedding)` |
| Soglia di accettazione | `OntologyTranslator.java` | `159` | `if (bestScore >= threshold) return bestUriMatch` |
| **Arricchimento con type constraint** | `OntologyTranslator.java` | `28-47` | `enrichLabelsWithExpectedTypes()` → `"label (value type: human)"` |
| Fetch batch dei tipi attesi (SPARQL) | `OntologyTranslator.java` | `49-110` | `batchFetchExpectedTypes()` → query P2302 + wikibase:propertyType |
| **Disambiguazione entità (reranker)** | `OntologyTranslator.java` | `180-211` | `disambiguateCandidates(targetClassLabel, candidates)` |
| **Formula combinata: 60% class + 40% sitelinks** | `OntologyTranslator.java` | `200` | `combined = classSim * 0.60 + sitelinksScore * 0.40` |
| Verifica compatibilità classi | `OntologyTranslator.java` | `170-178` | `areClassesCompatible(classA, classB, threshold)` |
| **Chiamata dal pipeline principale** | `App.java` | `189` | `translator.findSemanticEquivalent(sourceRoleLabel, targetProperties, 0.40)` |
| Disambiguazione in AppDis | `AppDis.java` | `46` | `translator.disambiguateCandidates(apolloEntity.classLabel(), targetResults)` |
| Compatibilità classi in AppDis | `AppDis.java` | `64` | `translator.areClassesCompatible(..., 0.35)` |

---

## 6. L'INNOVAZIONE ARCHITETTURALE: Scalabilità Olografica via Simpkin Tree

### Concetto Teorico (Cosa dire — come punto di forza)
> «Le VSA classiche hanno un limite matematico noto: sovrapponendo troppi elementi (oltre ~89 su D=10000), il rumore di fondo distrugge il segnale. Come possiamo allora mappare un'entità enciclopedica di Wikidata con centinaia di proprietà senza perdere l'informazione?
>
> Abbiamo risolto il problema implementando l'approccio di *Hierarchical Vector Chunking* proposto da Simpkin et al. (2018). Invece di comprimere tutto in un unico calderone piatto, costruiamo un albero. Raggruppiamo le triple in sotto-blocchi ("chunk") con una capacità massima (es. 30 triple).
>
> La vera magia avviene grazie alla **Clean-up Memory a livelli intermedi**. Quando interroghiamo l'entità, non estraiamo direttamente il dato finale: estraiamo prima il chunk (che sarà rumoroso), lo passiamo attraverso la memoria intermedia (`chunkMemory`) per recuperare il chunk **perfettamente pulito**, e solo a quel punto estraiamo il dato finale. Questo meccanismo a cascata "resetta" il rumore a ogni salto, garantendo un'estrazione esatta anche su grafi immensi.»

### Riferimento nel Codice (Dove puntare il dito)
| Cosa | File | Riga | Operazione Java |
|---|---|---|---|
| **Limite di sicurezza del rumore** | `TopologicalVectorUpdater.java` | `10` | `MAX_CHUNK_CAPACITY = 30;` (Evita il collasso del SNR) |
| **Biforcazione gerarchica** | `TopologicalVectorUpdater.java` | `37-39` | Se le triple sono ≤ 30 crea un chunk semplice, altrimenti `buildRecursiveSubTree()` |
| **Clean-up Memory intermedia** | `ItemMemory.java` | `14` | `ConcurrentHashMap<String, HDVector> chunkMemory` |
| **Estrazione e pulizia a cascata** | `App.java` | `159-160` | Prima unbinda il ramo: `bind(candidateRole)` → Poi resetta il rumore: `itemMemory.cleanUpChunk(noisyBranch)` |
| **L'estrazione sicura dal chunk pulito** | `App.java` | `163` | `cleanBranch.bind(subjectVector)...` (Lavora sul segnale puro, non rumoroso) |