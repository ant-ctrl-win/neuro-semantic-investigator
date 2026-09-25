# Stato del lavoro — Neuro-Semantic Investigator (Pilastro 3)

Ultima revisione: 23 settembre 2026
Branch di lavoro: `integration/codex-simpkin`
Base corrente: `c61bc88` (post-P2.1b)

## 1. Obiettivo

Il lavoro copre il Pilastro 3 della strategia GitHub: un motore di
inferenza analogica su Wikidata basato su Vector Symbolic Architectures.
Lo scopo è produrre un prototipo di ricerca con output tracciabile e
z-score, senza training, basato su binary spatter codes (D=10000) e
sul chunking gerarchico di Simpkin et al. (2018).

## 2. Cantieri completati

### 2.1 Refactor chunking VSA (pre-P1.x, commit `0ed54c7`)

Il chunking gerarchico è stato riallineato allo schema di Simpkin et al.
(2018): permutazione posizionale cumulativa con `StopVec`, role vector
posizionali deterministici, capacità configurabile (default 30), split
ricorsivo dei rami oltre soglia, padding deterministico, correzione del
doppio binding sui nodi con 1–29 predicati, albero ricorsivo dei valori
e clean-up strutturale con soglia `3/√D`. La demo è stata preservata.

### 2.2 P1.4 — Eliminazione doppia traversata (commit `ada1775`)

Aggiunto l'overload `recoverTriples(memory, entityUri, predicateUri,
precomputedBranch)`. `App` non richiama più `recoverBranch` come gate
booleano prima di `recoverTriples`. Il ramo viene recuperato una sola
volta per fase e riusato. Comportamento osservabile invariato: il
metodo a tre argomenti resta disponibile come fallback.

### 2.3 P1.3 — Test combinato fat node + fat predicate (commit `8a1e1be`)

Aggiunto `recoverCombinedFatNodeFatPredicate` in
`TopologicalVectorUpdaterTest`. Copre nodo con 30 predicati e predicato
con 100 oggetti, con capacità 30 e 5. Esito: verde. P1.1 e P1.2 restano
teorici (non coperti da questo test).

### 2.4 P2.1a — Parallelizzazione fasi remote (commit `cb32edc`)

Le 3 resolve entità e i 2 fetch RDF sono in parallelo. Le scritture su
`GraphManager.localModel` restano sequenziali (Jena Model non thread-safe).
Aggiunte metriche di fase via `System.nanoTime()`.

### 2.5 P2.1b — Riduzione chiamate label e misura VSA (commit `ac0d444`)

Due chiamate Wikidata di label eliminate: riuso della label target
dall'inventario, fusione della label sorgente nella batch dell'inventario.
Chiamate remote: 9 → 7. Aggiunte metriche per STEP 1 e STEP 3&4 VSA.

## 3. Test

`TopologicalVectorUpdaterTest` verifica:

1. determinismo di due ricostruzioni consecutive (`sameNodeProducesSameVectorOnConsecutiveBuilds`);
2. recupero di rami noti in un nodo con 100 predicati (`hundredPropertiesAreChunkedAndKnownBranchesRecovered`);
3. recupero diretto con 1, 5 e 29 predicati, senza doppio binding (`branchesBelowChunkCapacityAreRecoveredWithoutDoubleBinding`);
4. recupero degli oggetti 0, 28, 29 e 99 in un predicato con 100 oggetti, con capacità 30 e 5 (`hundredObjectsAreRecoveredThroughTheRecursiveValueTree`);
5. caso combinato fat node (30 predicati) + fat predicate (100 oggetti), con capacità 30 e 5 (`recoverCombinedFatNodeFatPredicate`).

Ultima esecuzione:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 4. Demo verificate

### 4.1 Amleto / Shakespeare / Lacrimosa

```text
Ruolo sorgente: author
Ruolo target: composer
Z-score ramo: 22,80σ

#1 Franz Xaver Süssmayr      30,86σ
#2 Wolfgang Amadeus Mozart  30,85σ
```

### 4.2 Nile / Egypt / Mont Blanc

```text
Ruolo sorgente: country
Ruolo target: country
Z-score ramo: 15,58σ

#1 France  34,00σ
#2 Italy   33,85σ
```

Nota: gli z-score della demo Amleto (30,86 / 30,85) sono rimasti
invariati dopo P1.4, P2.1a e P2.1b. I valori della demo Nile provengono
dalla baseline pre-P1.x (`0ed54c7`) e non sono stati rimisurati nei
cantieri P1.3–P2.1b.

## 5. Profilo delle chiamate remote

Sette chiamate Wikidata: 3 resolve (parallelo), 2 fetch RDF (parallelo),
2 batch label (sequenziale). Minimo teorico 6; la settima è il batch
candidati finali, non fondibile perché i candidati esistono solo dopo la
proiezione VSA.

| Fase | Chiamate | Modalità |
|---|---:|---|
| risoluzione delle tre entità | 3 | parallelo |
| espansione RDF di sorgente e target | 2 | parallelo |
| etichette proprietà target + label ruolo sorgente | 1 batch | sequenziale |
| etichette dei risultati finali (candidati) | 1 batch | sequenziale |
| totale | 7 | |

## 6. Profilo prestazioni

VSA: ~40 ms (STEP 1 deduzione ruolo sorgente e STEP 3&4 proiezione/estrazione
target). Rete: dominante, 7–15 s tipico, fino a 50 s+ con rate-limiting di
Wikidata. Le chiamate remote sono near-minimal (7 rispetto al minimo teorico 6).

## 7. Cantieri aperti

### P1.1 — Limitare clean-up finale agli oggetti validi

`cleanUpRelativeTopK` confronta il vettore recuperato con tutta
`atomicMemory`, che contiene anche soggetti, predicati, ruoli posizionali
e URI interne. I candidati finali devono essere limitati agli oggetti
effettivi del predicato target.

### P1.2 — Rendere atomico `recoverTriples`

Se una foglia non supera il clean-up, l'implementazione corrente la omette
e restituisce le altre. Il chiamante non può distinguere un recupero
completo da uno parziale. L'API deve restituire tutte le triple oppure un
risultato esplicito con errori e cardinalità.

### P2.2 — Rimuovere richieste Maven TestNG

TestNG e JUnit 4 non sono usati dai test correnti. Possono essere rimossi;
in alternativa, TestNG deve avere una versione esplicita anziché `RELEASE`.

### P2.3 — Validare CLI prima di ONNX

Il modello viene caricato prima di analizzare gli argomenti. `--help`
inizializza inutilmente ONNX e un numero non valido per `--top` o
`--candidates` produce `NumberFormatException`.

### P2.4 — Rimuovere chunk obsoleti

La ricostruzione elimina i percorsi correnti da `TopologicalVectorUpdater`,
ma non elimina da `ItemMemory.chunkMemory` i chunk di livelli non più
presenti. Aggiornamenti ripetuti possono far crescere la memoria e
contaminare le API legacy di clean-up globale.

### P2.5 — Allineare runtime e configurazione Maven

Il progetto è Java 21. L'esecuzione con JDK 25 produce il warning ONNX
relativo a `System.load`. Va usato JDK 21 oppure l'opzione
`--enable-native-access=ALL-UNNAMED`. Il compilatore Maven dovrebbe usare
`maven.compiler.release=21` per impedire dipendenze involontarie da API
Java successive.

### P3.1 — Allineare nome e comportamento dell'estrattore

`extractBidirectional` estrae solamente archi outgoing e
`GraphManager.expandNode` ignora `Direction`. Il nome, i commenti e l'API
devono descrivere il comportamento reale oppure va implementata
l'estrazione incoming.

### P3.2 — Semplificare `InvestigationEngine.processTriple`

Il metodo genera tre atomici ma conserva i risultati solo tramite
l'effetto laterale di `getOrGenerate`. Le variabili locali non vengono
utilizzate.

### P3.3 — Separare le responsabilità di `App`

`App` contiene CLI, query SPARQL, orchestrazione, filtri, ranking, label
lookup e output. Dopo aver stabilizzato gli aspetti P1 e P2, queste
responsabilità possono essere estratte in componenti verificabili
separatamente.

## 8. Ordine di intervento proposto

1. P1.1
2. P1.2
3. P2.2
4. P2.3
5. P2.4
6. P2.5
7. P3.1
8. P3.2
9. P3.3

## 9. Documentazione correlata

- `README.md`: presentazione pubblica
- `FLUSSO_DATI.md`: flusso dati e formule VSA
- `docs/cantieri/README.md`: indice dei cantieri
- `P1.3_test_combinato.md`, `P1.4_doppia_traversata.md`,
  `P2.1a_parallelizzazione.md`, `P2.1b_label_e_vsa.md`: report di cantiere
- `src/test/java/com/investigator/vsa/TopologicalVectorUpdaterTest.java`:
  regressioni automatiche
