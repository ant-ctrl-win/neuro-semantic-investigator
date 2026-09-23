## Sezione 1 — Codice morto da rimuovere

Metodo di verifica: per ogni voce è stato eseguito `git grep -n "<simbolo>" -- src` (equivalente a `grep -r`). Tutti i risultati riportano **solo la definizione** o riferimenti commentati; nessun chiamante attivo.

| File | Righe | Tipo di codice | Perché è morto | Rischio di rimozione |
|---|---|---|---|---|
| `jena/GraphManager.java` | 1–78 | Blocco legacy interamente commentato (vecchia classe duplicata) | È una copia commentata della stessa classe; il codice attivo inizia alla riga 80 | Basso — nessun riferimento |
| `jena/TripleExtractor.java` | 7–13 | Import inutilizzati (`Resource`, `RDFNode`, `Statement`, `StmtIterator`, `Stream`, `ArrayList`, `List`) | Nessuno di questi simboli è usato nel corpo (solo `Model`, `ModelFactory`, `Query`, `QueryExecution`) | Basso — rimozione sicura |
| `jena/SparqlEndpoint.java` | 21–23 | `getEndpointUrl()` | Nessun chiamante | Basso |
| `jena/SparqlEndpoint.java` | 25–31 | `queryOutgoing(Resource)` | Solo riferimento commentato in `GraphManager.java:27` | Basso |
| `jena/SparqlEndpoint.java` | 33–39 | `queryIncoming(Resource)` | Solo riferimento commentato in `GraphManager.java:29` | Basso |
| `jena/GraphManager.java` | 94 (e 99) | Campo `sparqlEndpoint` | Assegnato nel costruttore, **mai letto** (il costruttore riceve l'oggetto ma `expandNode` usa un URL hardcoded) | Basso — ma richiede aggiornare i call site se si rimuove anche il parametro |
| `engine/InvestigationEngine.java` | 22–26 | Costruttore `InvestigationEngine(HDVector, GraphManager)` | `new InvestigationEngine` compare solo 2 volte (App:130, AppDis:120) e **entrambe usano il costruttore a 4 argomenti** | Basso |
| `engine/InvestigationEngine.java` | 28–32 | Costruttore `InvestigationEngine(HDVector, GraphManager, ItemMemory)` | Idem: mai invocato | Basso |
| `engine/InvestigationEngine.java` | 20 | `record Candidate(...)` | Nessun riferimento nel progetto | Basso |
| `engine/InvestigationEngine.java` | 95–97 | `getItemMemory()` | Nessun chiamante | Basso |
| `engine/InvestigationEngine.java` | 99–101 | `getNodeVectors()` | Nessun chiamante | Basso |
| `engine/InvestigationEngine.java` | 103–105 | `getTripleVectors()` | Nessun chiamante | Basso |
| `engine/InvestigationEngine.java` | 107–110 | `clearExtractedVectors()` | Nessun chiamante | Basso |
| `engine/InvestigationEngine.java` | 17–18, 56–62 | Campi `extractedNodeVectors` / `extractedTripleVectors` e loro popolamento in `processTriple` | Vengono scritti ma letti **solo** dai getter morti sopra → vettori accumulati inutilmente | Basso — rimuovere insieme ai getter |
| `embedding/OntologyTranslator.java` | 29–48 | `enrichLabelsWithExpectedTypes(...)` | Nessun chiamante | Basso |
| `embedding/OntologyTranslator.java` | 15 | `EMBEDDING_DIMENSION` | Nessun riferimento nel progetto | Basso |
| `App.java` | 231–255 | `fetchDescriptionFromWikidata(...)` | Nessun chiamante (in `AppDis` è già stato rimosso) | Basso |
| `jena/GraphManager.java` | 134–153 | `getUnexploredNodes(...)` | Nessun chiamante (residuo del pianificatore multi-hop) | Basso |
| `jena/TripleExtractor.java` | 28 | `Direction.BOTH` | Mai referenziato (si usano solo `OUTGOING`/`INCOMING`) | Basso |
| `vsa/strategy/SemanticEmbeddingStrategy.java` | 1–151 | Intera classe | `new SemanticEmbeddingStrategy` non compare mai; è implementata ma non istanziata da nessun entry point (confermato da AGENTS.md: "not used by default") | Medio — è una strategia alternativa completa; valutare se verrà riattivata |
| `jena/ResolvedEntity.java` | 4, 7 | Campi `searchKeyword` e `classUri` | Vengono valorizzati (`EntityResolver:65`) ma **mai letti** | Basso |

### Evidenze grep (output riassuntivo)

```text
git grep -n "SparqlEndpoint" -- src
  App.java:21, AppDis.java:24                 -> new SparqlEndpoint(...)
  GraphManager.java:94,99                     -> campo/assegnazione (mai letto)
  GraphManager.java:15,18,20,27,29            -> commenti
  SparqlEndpoint.java:13,17                   -> definizione/costruttore

git grep -n "queryOutgoing" -- src
  SparqlEndpoint.java:25 (def)  +  GraphManager.java:27 (commento)

git grep -n "getUnexploredNodes" -- src
  GraphManager.java:134 (def)   +  GraphManager.java:54 (commento)

git grep -n "new InvestigationEngine" -- src
  App.java:130, AppDis.java:120  -> entrambe chiamata a 4 argomenti

git grep -n "getNodeVectors\|getTripleVectors\|clearExtractedVectors\|getItemMemory\|Candidate" -- src
  -> solo definizioni in InvestigationEngine.java

git grep -n "enrichLabelsWithExpectedTypes" -- src
  OntologyTranslator.java:29 (def)  (nessun chiamante)

git grep -n "EMBEDDING_DIMENSION" -- src
  OntologyTranslator.java:15 (def)  (nessun riferimento)

git grep -n "fetchDescriptionFromWikidata" -- src
  App.java:231 (def)  (nessun chiamante)

git grep -n "new SemanticEmbeddingStrategy" -- src
  (nessuna occorrenza)

git grep -n "Direction.BOTH" -- src
  (nessuna occorrenza)

git grep -n "searchKeyword\|classUri" -- src
  ResolvedEntity.java:4,7 (campi) + EntityResolver.java:51,54,66 (solo scrittura)
```

---

## Sezione 2 — Codice sospeso (funzionalità incomplete)

| File | Righe | Cosa dovrebbe fare | Stato attuale | Cosa manca per completarlo |
|---|---|---|---|---|
| `jena/GraphManager.java` | 118–120 | Espandere anche le triple **in entrata** del nodo (incoming edges) | `expandNodeIncoming` è definito ma il chiamante in `InvestigationEngine.java:70` è **commentato**; il metodo delega a `expandNode`, che ignora la direzione | Implementare una query CONSTRUCT per le triple entranti in `TripleExtractor` e collegarla; rimuovere il commento in InvestigationEngine |
| `jena/TripleExtractor.java` | 20, 32–43 | Estrazione **bidirezionale** (i commenti la chiamano "Multi-Hop bidirezionale") | Il CONSTRUCT è solo outgoing: `<entityUri> ?pOut ?o` | Aggiungere la parte `?s ?p <entityUri>` e fare merge nel `Model`; oppure rimuovere la promessa "bidirezionale" dai commenti/firma |
| `jena/GraphManager.java` | 105–112 | Usare il parametro `direction` per scegliere out/in | Il parametro è accettato ma **ignorato**; si chiama sempre `extractBidirectional` (che è outgoing-only) | Implementare lo switch su `Direction` |
| `embedding/OntologyTranslator.java` | 117–131 | Arricchire la label sorgente col tipo atteso della proprietà (range constraint) | Il ramo esiste ma **non è mai esercitato**: l'unico call site attivo con l'overload a 4 argomenti passa `sourcePropertyUri = null` (`AppDis.java:304-305`) | Passare la URI reale della proprietà sorgente; attualmente `batchFetchExpectedTypes` è dormiente |
| `embedding/OntologyTranslator.java` | 50–111 | Fetch dei tipi attesi delle proprietà via SPARQL | Implementato ma usato solo dal ramo precedente (mai attivo) e da `enrichLabelsWithExpectedTypes` (morto) | Decidere se cablare il meccanismo o rimuoverlo |
| `App.java` | 150–228 | Pipeline analogica completa con la nuova API Simpkin (`recoverBranch`/`decodeChunkElement`) | Usa ancora il percorso **superato** `getTreeVector(...).permute(-100).bind(...)` + `itemMemory.cleanUpChunk(...)` (righe 159–160, 206–207, 215), come `AppDis` prima del refactor | Allineare `App` al flusso di `AppDis` (recoverBranch + decodifica posizione-per-posizione) o dichiararlo deprecato |

---

## Sezione 3 — Collo di bottiglia prestazionali

| File | Righe | Problema | Impatto stimato | Fix suggerito |
|---|---|---|---|---|
| `AppDis.java` | 390–409 | `fetchLabelFromWikidata(uri)` invocata **dentro il loop** su ogni proprietà outgoing/incoming → una chiamata di rete SPARQL per proprietà (N+1) | Alto: su entità ricche di proprietà sono decine di round-trip sequenziali, ognuno con latenza di rete | Recuperare tutte le label in **una sola query** con `VALUES` (come già fa `batchFetchExpectedTypes` in OntologyTranslator) e popolare `labels` |
| `App.java` | 271–292 | Stesso N+1 di `AppDis` (codice duplicato) | Alto (se `App` viene usato) | Stessa soluzione, o riuso del metodo di `AppDis` |
| `embedding/OntologyTranslator.java` | 140–158 | `encoder.embed(targetLabel)` eseguito per **ogni** proprietà target, senza cache degli embedding; inoltre stampa una riga per ogni similarità > 0.3 | Alto: decine/centinaia di inferenze ONNX ripetute (anche le stesse label tra chiamate diverse) + spam di log | Introdurre una cache `Map<String, Embedding>` (simile a `embeddingCache` di `SemanticEmbeddingStrategy`) e batching; abbassare/rimuovere la stampa per-elemento |
| `AppDis.java` | 366 | `fetchLabelFromWikidata(match.key())` per ciascuno dei top-3 candidati | Basso (3 chiamate) | Risolvere le 3 label in una sola query `VALUES` |
| `AppDis.java` | 219 e 341 | `localModel.listStatements(...).toList().size()` eseguito dentro i loop per contare gli oggetti | Medio: materializza di nuovo tutte le statement ad ogni iterazione | Calcolare il conteggio una volta fuori dal loop, oppure iterare direttamente lo `StmtIterator` per posizione |
| `AppDis.java` | 396, 408 | Stampa di una riga per **ogni** proprietà nel loop di inventario ontologico | Basso-medio: output molto lungo, I/O su centinaia di righe | Rendere la diagnostica opzionale (flag) o riassumerla in un conteggio |
| `App.java` | 278, 291 | Stesso spam di `AppDis` (codice duplicato) | Basso-medio | Come sopra |
| `jena/GraphManager.java` | 105–119 | `expandNodeOutgoing` e `expandNodeIncoming` eseguono **la stessa** `extractBidirectional`; se l'incoming venisse riattivato si avrebbero due richieste identiche per lo stesso nodo | Attualmente nullo (riga 70 commentata); **rischio latente** di chiamata di rete duplicata | Far sì che `Direction` cambi davvero la query, oppure unificare in un'unica chiamata bidirezionale |
| `AppDis.java` | 211–254 | Doppia passata su `allHypotheses` per calcolare μ/σ (necessaria) ma ogni `recoverBranch` esegue clean-up sulla memoria dei chunk | Medio: dipende dal numero di proprietà e chunk; accettabile ma da monitorare | Nessuna azione immediata; eventualmente cache dei rami già recuperati |

---

## Sezione 4 — Cosmetico fuorviante

| File | Righe | Testo attuale | Perché è fuorviante | Correzione proposta |
|---|---|---|---|---|
| `jena/TripleExtractor.java` | 20 | "Estrae le triple ... in modo BIDIREZIONALE (Multi-Hop di base)" | La query CONSTRUCT è **solo outgoing** e single-hop | "Estrae le triple outgoing (1-hop)" |
| `jena/TripleExtractor.java` | 32 | "Il nuovo estrattore che fa Multi-Hop bidirezionale in un colpo solo" | Stessa discrepanza: non è né bidirezionale né multi-hop | Correggere o implementare davvero (vedi Sezione 2) |
| `jena/GraphManager.java` | 104 | "Il parametro `direction` è mantenuto per retrocompatibilità dell'API" | Il parametro non ha effetto: è più corretto dire che è ignorato | Documentare che è attualmente ignorato, o implementarlo |
| `App.java` | 64 | "// LA MAGIA: ALLINEAMENTO ONTOLOGICO AUTOMATICO (Graph + LLM)" | Non c'è alcun LLM: il confronto è a embedding (`OntologyTranslator`). Inoltre "LA MAGIA" è illeggibile | "Allineamento ontologico automatico (Graph + Embedding)" |
| `App.java` | 33 | "// CHIEDIAMO AL RESOLVER DI TROVARE L'APOLLO 13!" | Entità risolte sono Amazon/Brazil/Nile: commento obsoleto | Rimuovere o aggiornare |
| `App.java` | 34 | "// USS Enterprise" | Residuo storico, non inerente al codice | Rimuovere |
| `App.java` | 45, 48 | `ResolvedEntity triesteEntity = targetResults.get(0); ResolvedEntity targetEntity = triesteEntity;` | Variabile chiamata "trieste" e alias immediato inutile, fuorviante | Rinominare direttamente in `targetEntity` |
| `App.java` | 70–71 | Commento su P793/P2283/P516 "collegati al Trieste" | Residuo della demo Trieste | Aggiornare a "target" generico |
| `App.java` | 182 | STEP 2 banner "(LLM)" | Il componente è un embedding encoder, non un LLM | "(Embedding)" |
| `App.java` | 111, 117 | Commenti "[LLM] Valuto..." / "L'LLM ha isolato..." | Idem: nessun LLM | "[Embedding] ..." |
| `App.java` | 219 | `System.out.println("    Neil Armstrong  sta a  Apollo 11");` | Output **hardcoded**: stampa sempre Armstrong/Apollo 11 qualunque siano le entità (in `AppDis` questi nomi sono già stati corretti) | Usare `armstrongEntity.entityLabel()` / `apolloEntity.entityLabel()` |
| `AppDis.java` | 130–155 | Blocco `[DIAG]` commentato su Nile/Africa/P30/P17 | Residuo di debug obsoleto che oscura il codice | Rimuovere |
| `AppDis.java` | 163–198 | Vecchio STEP 1 commentato con `rawSimilarity > 0.05` | Codice superato: l'attivo (righe 200–254) usa già lo z-score adattivo. Il doppio blocco è fuorviante | Rimuovere il blocco commentato |
| `AppDis.java` | 268 | `//  ... (Sim: %.2f) ...` commentato | Residuo della stampa precedente | Rimuovere |
| `AppDis.java` | 301–303 | "Soglia alzata da 0.30 a 0.60 ... (vedi test Nile/Big Bang ...)" | Fa riferimento a un test "Nile/Big Bang" che **non esiste** nel repo (nessun test con quel nome) | Riformulare senza riferimento a test inesistenti, o aggiungere il test citato |
| `AppDis.java` | 19 vs `App.java:16` | Banner "Encoder-Embedding" vs "LLM" | Incoerenza di terminologia tra i due entry point quasi identici | Uniformare la terminologia |
| `vsa/strategy/SemanticEmbeddingStrategy.java` | 14, 29 | "Usa ... (all-MiniLM-L6-v2)" / "il modello locale (pesa circa 22MB)" | Il modello caricato è `models/model.onnx` (bge-base-en-v1.5, ~415 MB) | Aggiornare i commenti (o rimuovere la classe, Sezione 1) |
| `vsa/strategy/SemanticEmbeddingStrategy.java` | 54 | "embedding denso reale (384-D)" | `embeddingDim = 768` | Correggere in 768-D |
| `vsa/strategy/SemanticEmbeddingStrategy.java` | 57 | "Proiezione LSH -> 100.000-D Bipolare" | `HDVectorMapB.D = 10000` | Correggere in 10.000-D |
| `AppDis.java` | 447 | `// Proprietà P-numeriche note come metadata` | Commento corretto (nessun problema) — citato solo per completezza di ispezione | — |

---

## Sezione 5 — Riepilogo esecutivo

- **File coinvolti:** 9 (8 di produzione + 1 record):
  `App.java`, `AppDis.java`, `engine/InvestigationEngine.java`, `jena/GraphManager.java`, `jena/TripleExtractor.java`, `jena/SparqlEndpoint.java`, `jena/ResolvedEntity.java`, `embedding/OntologyTranslator.java`, `vsa/strategy/SemanticEmbeddingStrategy.java`.
- **Righe rimosse stimate:** ~430–500, di cui:
  - ~170 righe di codice commentato (`GraphManager:1–78`, `AppDis:130–155` e `163–198`, `AppDis:268`, `InvestigationEngine:70`);
  - ~150 righe della classe morta `SemanticEmbeddingStrategy`;
  - ~110 righe di metodi/costruttori/getter/import morti (`SparqlEndpoint`, `InvestigationEngine`, `OntologyTranslator`, `App.fetchDescriptionFromWikidata`, `GraphManager.getUnexploredNodes`, import inutilizzati).
- **Righe modificate stimate:** ~40–60 (commenti/label di `App`, banner e commenti di `AppDis`, aggiornamento dei call site se si rimuovono `SparqlEndpoint`/`sparqlEndpoint`, e correzione delle etichette fuorvianti).
- **Beneficio stimato:**
  - **Leggibilità:** eliminazione di blocchi commentati (oltre 170 righe) e di una classe non usata; flusso di `AppDis` nuovamente lineare.
  - **Prestazioni:** il fix N+1 su `fetchLabelFromWikidata` (Sezione 3) è il guadagno maggiore: da decine di round-trip di rete a 1–2 query; la cache degli embedding elimina inferenze ONNX ripetute.
  - **Chiarezza:** terminologia coerente (Embedding vs LLM), nomi di variabili corretti, niente output hardcoded, commenti allineati all'implementazione.
- **Rischi noti dell'applicazione del piano:**
  1. `SemanticEmbeddingStrategy` è morta **oggi**, ma è una strategia alternativa completa: rimuoverla definitivamente va confermato col Tech Lead (potrebbe servire in futuro).
  2. La rimozione di `SparqlEndpoint` e del campo `sparqlEndpoint` obbliga ad aggiornare i call site in `App.java:21-22` e `AppDis.java:24-25`; il costruttore di `GraphManager` cambierebbe firma (o diventerebbe senza argomenti), con impatto sui due entry point.
  3. L'eliminazione dei getter/campi di `InvestigationEngine` cambia l'API pubblica (nessun chiamante attuale, ma valutare eventuali usi esterni/IDE).
  4. `App.java` è attualmente **superato** rispetto a `AppDis`: allinearlo o deprecarlo è una decisione di prodotto, non solo di pulizia.
  5. Il fix sull'estrazione incoming (Sezione 2) tocca la semantica della rete/grafo: da fare con test dedicati, non come semplice pulizia.
  6. **Vincoli rispettati:** nessuna voce riguarda `ItemMemory.java`, `HDVectorMapB.java`, `TopologicalVectorUpdater.java` né la logica di chunking/branch (Simpkin eq. 5).
