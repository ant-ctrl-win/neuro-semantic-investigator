# STATO_LAVORI_update

## 1. Descrizione

Allineamento di `docs/cantieri/STATO_LAVORI.md` allo stato corrente del
Pilastro 3 (base `c61bc88`, post-P2.1b). Il documento era intitolato
"Stato del lavoro sul chunking VSA" e fotografava la situazione a
`a7c8a4f` (pre-P1.3): ora copre l'intero Pilastro 3.

Interventi:

- titolo e scope estesi a tutto il Pilastro 3;
- base aggiornata a `c61bc88` e data a 23 settembre 2026;
- §2 riscritta come elenco dei cantieri completati, ciascuno con commit
  proprio (refactor pre-P1.x `0ed54c7`, P1.4 `ada1775`, P1.3 `8a1e1be`,
  P2.1a `cb32edc`, P2.1b `ac0d444`);
- §3 aggiornata a 5 test con i rispettivi nomi di metodo;
- §4 demo invariate, con nota sulla provenienza degli z-score;
- §5 profilo chiamate aggiornato da 9 a 7 (3 resolve parallelo, 2 fetch
  RDF parallelo, 2 batch label sequenziale);
- §6 nuovo profilo prestazioni (VSA ~40 ms; rete 7–15 s tipico, fino a
  50 s+ con rate-limiting);
- §7 cantieri aperti: rimossi P1.3, P1.4 e P2.1 (completati), mantenuti
  P1.1, P1.2, P2.2–P2.5, P3.1–P3.3;
- §8 ordine di intervento riallineato (9 voci);
- §9 documentazione correlata aggiornata.

`docs/cantieri/README.md` (indice): voce `STATO_LAVORI.md` aggiornata in
"Stato del Pilastro 3 (aggiornato post-P2.1b)" e aggiunta la voce
`STATO_LAVORI_update.md`.

## 2. Git diff del commit

```diff
diff --git a/docs/cantieri/README.md b/docs/cantieri/README.md
index 7fe0998..e7383c3 100644
--- a/docs/cantieri/README.md
+++ b/docs/cantieri/README.md
@@ -6,10 +6,11 @@ contesto, diff, output di verifica, esito.
 
 ## Indice
 
-- [STATO_LAVORI.md](STATO_LAVORI.md) — Stato del lavoro sul Pilastro 3
+- [STATO_LAVORI.md](STATO_LAVORI.md) — Stato del Pilastro 3 (aggiornato post-P2.1b)
 - [P1.3_test_combinato.md](P1.3_test_combinato.md) — Test combinato fat node + fat predicate
 - [P1.4_doppia_traversata.md](P1.4_doppia_traversata.md) — Eliminazione doppia traversata
 - [P2.1a_parallelizzazione.md](P2.1a_parallelizzazione.md) — Parallelizzazione fasi remote
 - [P2.1b_label_e_vsa.md](P2.1b_label_e_vsa.md) — Riduzione label calls + misura VSA
 - [README_update.md](README_update.md) — Aggiornamento README post-P2.1b
 - [FLUSSO_DATI_update.md](FLUSSO_DATI_update.md) — Allineamento FLUSSO_DATI post-P2.1b
+- [STATO_LAVORI_update.md](STATO_LAVORI_update.md) — Allineamento STATO_LAVORI post-P2.1b
diff --git a/docs/cantieri/STATO_LAVORI.md b/docs/cantieri/STATO_LAVORI.md
index 19f0093..5eeafe1 100644
--- a/docs/cantieri/STATO_LAVORI.md
+++ b/docs/cantieri/STATO_LAVORI.md
@@ -1,127 +1,69 @@
-# Stato del lavoro sul chunking VSA
+# Stato del lavoro — Neuro-Semantic Investigator (Pilastro 3)
 
-Data della revisione: 23 settembre 2026
+Ultima revisione: 23 settembre 2026
 Branch di lavoro: `integration/codex-simpkin`
-Base corrente: `a7c8a4f`
+Base corrente: `c61bc88` (post-P2.1b)
 
 ## 1. Obiettivo
 
-Il lavoro ha riallineato il chunking gerarchico della memoria VSA allo schema descritto da Simpkin et al. (2018), preservando la demo:
+Il lavoro copre il Pilastro 3 della strategia GitHub: un motore di
+inferenza analogica su Wikidata basato su Vector Symbolic Architectures.
+Lo scopo è produrre un prototipo di ricerca con output tracciabile e
+z-score, senza training, basato su binary spatter codes (D=10000) e
+sul chunking gerarchico di Simpkin et al. (2018).
 
-```text
-Amleto : William Shakespeare = Lacrimosa : ?
-```
-
-Il problema iniziale riguardava soprattutto nodi Wikidata ad alto grado. Un bundle radice poteva contenere troppi rami e perdere il segnale dei singoli predicati nel rumore.
-
-## 2. Modifiche completate
-
-### 2.1 Permutazione posizionale cumulativa
-
-`TopologicalVectorUpdater.encodeChunk` applica a ogni elemento lo shift dipendente dalla posizione e il prodotto cumulativo dei role vector:
-
-```text
-ρ(Zᵢ,i) ⊗ (p₀ ⊗ ... ⊗ pᵢ₋₁)
-```
-
-La codifica include `StopVec`, come nell'equazione 5 del paper.
-
-### 2.2 Role vector posizionali
-
-Le posizioni sono rappresentate da atomici deterministici con URI interne:
-
-```text
-vsa:internal:position:0
-vsa:internal:position:1
-...
-```
-
-La decodifica ricostruisce lo stesso prodotto cumulativo e applica la permutazione inversa.
-
-### 2.3 Capacità configurabile
-
-`TopologicalVectorUpdater` accetta la capacità nel costruttore. Il default rimane 30 per compatibilità. È disponibile anche `capacityForSigma(double)`; per `D=10000` e `3σ` restituisce 89.
-
-Con capacità 30, ogni chunk contiene al massimo 29 elementi più `StopVec`.
-
-### 2.4 Split ricorsivo dei rami
-
-Quando un'entità possiede almeno 30 predicati, i rami vengono divisi in gruppi da 29 e ricombinati ricorsivamente.
-
-Per 100 predicati:
-
-```text
-29 + 29 + 29 + 13
-```
-
-`BranchPath` conserva il ruolo di accesso alla radice e le posizioni necessarie per raggiungere il ramo foglia.
-
-### 2.5 Padding deterministico
-
-I vettori usati per risolvere i pareggi del bundling sono derivati dal contenuto ordinato del chunk. Ricostruzioni consecutive producono lo stesso vettore.
-
-### 2.6 Correzione dei nodi con 1–29 predicati
-
-Il ramo foglia era già legato al predicato, ma la radice diretta applicava nuovamente lo stesso binding. Nei binary spatter codes:
-
-```text
-(Bₚ ⊗ P) ⊗ P = Bₚ
-```
-
-Il secondo binding cancellava quindi il ruolo e rendeva impossibile il recupero. La radice diretta conserva ora il singolo binding.
-
-### 2.7 Albero ricorsivo dei valori
+## 2. Cantieri completati
 
-La precedente implementazione partizionava i predicati con molti oggetti, ma non conservava una struttura percorribile. `ValueNode` registra ora:
+### 2.1 Refactor chunking VSA (pre-P1.x, commit `0ed54c7`)
 
-- vettore puro;
-- figli;
-- numero di foglie discendenti.
+Il chunking gerarchico è stato riallineato allo schema di Simpkin et al.
+(2018): permutazione posizionale cumulativa con `StopVec`, role vector
+posizionali deterministici, capacità configurabile (default 30), split
+ricorsivo dei rami oltre soglia, padding deterministico, correzione del
+doppio binding sui nodi con 1–29 predicati, albero ricorsivo dei valori
+e clean-up strutturale con soglia `3/√D`. La demo è stata preservata.
 
-Sono state aggiunte le API:
+### 2.2 P1.4 — Eliminazione doppia traversata (commit `ada1775`)
 
-```java
-recoverTriple(memory, entityUri, predicateUri, index)
-recoverTriples(memory, entityUri, predicateUri)
-```
-
-Con 100 oggetti e capacità 30, il ramo contiene quattro sotto-chunk da `29 + 29 + 29 + 13`. Con capacità 5 vengono attraversati quattro livelli, verificando la ricorsione oltre il singolo livello intermedio.
+Aggiunto l'overload `recoverTriples(memory, entityUri, predicateUri,
+precomputedBranch)`. `App` non richiama più `recoverBranch` come gate
+booleano prima di `recoverTriples`. Il ramo viene recuperato una sola
+volta per fase e riusato. Comportamento osservabile invariato: il
+metodo a tre argomenti resta disponibile come fallback.
 
-### 2.8 Clean-up strutturale
+### 2.3 P1.3 — Test combinato fat node + fat predicate (commit `8a1e1be`)
 
-Dopo la decodifica di ogni posizione, il vettore rumoroso viene confrontato con il figlio atteso. Il sotto-chunk puro viene ripristinato se:
-
-```text
-similarità ≥ 3/√D
-```
+Aggiunto `recoverCombinedFatNodeFatPredicate` in
+`TopologicalVectorUpdaterTest`. Copre nodo con 30 predicati e predicato
+con 100 oggetti, con capacità 30 e 5. Esito: verde. P1.1 e P1.2 restano
+teorici (non coperti da questo test).
 
-Per `D=10000`, la soglia è `0,03`. La confidenza mostrata per il ramo è:
+### 2.4 P2.1a — Parallelizzazione fasi remote (commit `cb32edc`)
 
-```text
-z = similarità × √D
-```
+Le 3 resolve entità e i 2 fetch RDF sono in parallelo. Le scritture su
+`GraphManager.localModel` restano sequenziali (Jena Model non thread-safe).
+Aggiunte metriche di fase via `System.nanoTime()`.
 
-### 2.9 Uso dell'API ricorsiva nell'applicazione
+### 2.5 P2.1b — Riduzione chiamate label e misura VSA (commit `ac0d444`)
 
-`App` usa `recoverTriples` sia durante la scoperta del ruolo sorgente sia durante l'estrazione degli oggetti target. Non assume più che il ramo sia un unico chunk piatto.
+Due chiamate Wikidata di label eliminate: riuso della label target
+dall'inventario, fusione della label sorgente nella batch dell'inventario.
+Chiamate remote: 9 → 7. Aggiunte metriche per STEP 1 e STEP 3&4 VSA.
 
-## 3. Test aggiunti
+## 3. Test
 
 `TopologicalVectorUpdaterTest` verifica:
 
-1. determinismo di due ricostruzioni consecutive;
-2. recupero di rami noti in un nodo con 100 predicati;
-3. più livelli dei rami con capacità 5;
-4. recupero diretto con 1, 5 e 29 predicati;
-5. recupero degli oggetti 0, 28, 29 e 99 in un predicato con 100 oggetti;
-6. albero dei valori a due livelli con capacità 30;
-7. albero dei valori a quattro livelli con capacità 5;
-8. capacità teorica pari a 89 a 3σ.
+1. determinismo di due ricostruzioni consecutive (`sameNodeProducesSameVectorOnConsecutiveBuilds`);
+2. recupero di rami noti in un nodo con 100 predicati (`hundredPropertiesAreChunkedAndKnownBranchesRecovered`);
+3. recupero diretto con 1, 5 e 29 predicati, senza doppio binding (`branchesBelowChunkCapacityAreRecoveredWithoutDoubleBinding`);
+4. recupero degli oggetti 0, 28, 29 e 99 in un predicato con 100 oggetti, con capacità 30 e 5 (`hundredObjectsAreRecoveredThroughTheRecursiveValueTree`);
+5. caso combinato fat node (30 predicati) + fat predicate (100 oggetti), con capacità 30 e 5 (`recoverCombinedFatNodeFatPredicate`).
 
 Ultima esecuzione:
 
 ```text
-Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
+Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
 BUILD SUCCESS
 ```
 
@@ -149,114 +91,112 @@ Z-score ramo: 15,58σ
 #2 Italy   33,85σ
 ```
 
-## 5. Profilo delle chiamate remote
-
-Il percorso ordinario esegue circa nove richieste sequenziali:
-
-| Fase | Chiamate |
-|---|---:|
-| risoluzione delle tre entità | 3 |
-| espansione RDF di sorgente e target | 2 |
-| etichetta del ruolo sorgente | 1 |
-| etichette delle proprietà target | 1 batch |
-| etichetta del ruolo target | 1 |
-| etichette dei risultati finali | 1 batch |
-| totale | 9 |
-
-Le etichette delle proprietà e dei risultati sono già recuperate in batch: non viene eseguita una query per ciascuna proprietà. Rimane però una waterfall di chiamate sincrone. La latenza totale è la somma delle latenze di Wikidata e produce lo stesso problema pratico percepito di una struttura N+1.
-
-Le modifiche al chunking non hanno introdotto nuovi accessi SPARQL. Hanno però introdotto una doppia traversata locale: `App` chiama `recoverBranch`, poi `recoverTriples`, che richiama internamente `recoverBranch`.
+Nota: gli z-score della demo Amleto (30,86 / 30,85) sono rimasti
+invariati dopo P1.4, P2.1a e P2.1b. I valori della demo Nile provengono
+dalla baseline pre-P1.x (`0ed54c7`) e non sono stati rimisurati nei
+cantieri P1.3–P2.1b.
 
-Maven esegue inoltre richieste ai metadati TestNG perché `pom.xml` usa la versione dinamica `RELEASE`. Nei log queste richieste raggiungono repository GitHub Packages e ricevono `401 Unauthorized`.
-
-## 6. Bug e modifiche ancora necessarie
-
-### P1 — Correttezza e regressioni
-
-#### P1.1 Limitare il clean-up finale agli oggetti validi
-
-`cleanUpRelativeTopK` confronta il vettore recuperato con tutta `atomicMemory`, che contiene anche soggetti, predicati, ruoli posizionali e URI interne. I candidati finali devono essere limitati agli oggetti effettivi del predicato target.
-
-#### P1.2 Rendere atomico `recoverTriples`
-
-Se una foglia non supera il clean-up, l'implementazione corrente la omette e restituisce le altre. Il chiamante non può distinguere un recupero completo da uno parziale. L'API deve restituire tutte le triple oppure un risultato esplicito con errori e cardinalità.
-
-#### P1.3 Aggiungere il test combinato
-
-Manca un caso che combini entrambe le dimensioni critiche:
-
-```text
-almeno 30 predicati
-└── almeno un predicato con 100 oggetti
-```
-
-Il test deve verificare sia il percorso nel tree dei rami sia la discesa nel tree dei valori.
+## 5. Profilo delle chiamate remote
 
-#### P1.4 Eliminare la doppia traversata del ramo
+Sette chiamate Wikidata: 3 resolve (parallelo), 2 fetch RDF (parallelo),
+2 batch label (sequenziale). Minimo teorico 6; la settima è il batch
+candidati finali, non fondibile perché i candidati esistono solo dopo la
+proiezione VSA.
 
-`App` recupera esplicitamente il ramo e subito dopo richiama `recoverTriples`, che lo recupera nuovamente. Serve un'unica API che restituisca ramo, confidenza e triple, oppure un overload che accetti il ramo già purificato.
+| Fase | Chiamate | Modalità |
+|---|---:|---|
+| risoluzione delle tre entità | 3 | parallelo |
+| espansione RDF di sorgente e target | 2 | parallelo |
+| etichette proprietà target + label ruolo sorgente | 1 batch | sequenziale |
+| etichette dei risultati finali (candidati) | 1 batch | sequenziale |
+| totale | 7 | |
 
-### P2 — Prestazioni e robustezza
+## 6. Profilo prestazioni
 
-#### P2.1 Ridurre la waterfall Wikidata
+VSA: ~40 ms (STEP 1 deduzione ruolo sorgente e STEP 3&4 proiezione/estrazione
+target). Rete: dominante, 7–15 s tipico, fino a 50 s+ con rate-limiting di
+Wikidata. Le chiamate remote sono near-minimal (7 rispetto al minimo teorico 6).
 
-Interventi proposti:
+## 7. Cantieri aperti
 
-1. eseguire in parallelo le tre risoluzioni indipendenti;
-2. eseguire in parallelo le due estrazioni RDF in modelli separati e unirli dopo il completamento;
-3. usare una cache di etichette condivisa;
-4. riutilizzare l'etichetta del ruolo target già presente nell'inventario;
-5. raccogliere metriche di durata per ogni fase remota;
-6. valutare una query combinata per le etichette dei ruoli.
+### P1.1 — Limitare clean-up finale agli oggetti validi
 
-#### P2.2 Rimuovere le richieste Maven ai metadati TestNG
+`cleanUpRelativeTopK` confronta il vettore recuperato con tutta
+`atomicMemory`, che contiene anche soggetti, predicati, ruoli posizionali
+e URI interne. I candidati finali devono essere limitati agli oggetti
+effettivi del predicato target.
 
-TestNG e JUnit 4 non sono usati dai test correnti. Possono essere rimossi; in alternativa, TestNG deve avere una versione esplicita anziché `RELEASE`.
+### P1.2 — Rendere atomico `recoverTriples`
 
-#### P2.3 Validare la CLI prima dell'ONNX
+Se una foglia non supera il clean-up, l'implementazione corrente la omette
+e restituisce le altre. Il chiamante non può distinguere un recupero
+completo da uno parziale. L'API deve restituire tutte le triple oppure un
+risultato esplicito con errori e cardinalità.
 
-Il modello viene caricato prima di analizzare gli argomenti. `--help` inizializza inutilmente ONNX e un numero non valido per `--top` o `--candidates` produce `NumberFormatException`.
+### P2.2 — Rimuovere richieste Maven TestNG
 
-#### P2.4 Rimuovere chunk obsoleti
+TestNG e JUnit 4 non sono usati dai test correnti. Possono essere rimossi;
+in alternativa, TestNG deve avere una versione esplicita anziché `RELEASE`.
 
-La ricostruzione elimina i percorsi correnti da `TopologicalVectorUpdater`, ma non elimina da `ItemMemory.chunkMemory` i chunk di livelli non più presenti. Aggiornamenti ripetuti possono far crescere la memoria e contaminare le API legacy di clean-up globale.
+### P2.3 — Validare CLI prima di ONNX
 
-#### P2.5 Allineare runtime e configurazione Maven
+Il modello viene caricato prima di analizzare gli argomenti. `--help`
+inizializza inutilmente ONNX e un numero non valido per `--top` o
+`--candidates` produce `NumberFormatException`.
 
-Il progetto è Java 21. L'esecuzione con JDK 25 produce il warning ONNX relativo a `System.load`. Va usato JDK 21 oppure l'opzione:
+### P2.4 — Rimuovere chunk obsoleti
 
-```text
---enable-native-access=ALL-UNNAMED
-```
+La ricostruzione elimina i percorsi correnti da `TopologicalVectorUpdater`,
+ma non elimina da `ItemMemory.chunkMemory` i chunk di livelli non più
+presenti. Aggiornamenti ripetuti possono far crescere la memoria e
+contaminare le API legacy di clean-up globale.
 
-Il compilatore Maven dovrebbe usare `maven.compiler.release=21` per impedire dipendenze involontarie da API Java successive.
+### P2.5 — Allineare runtime e configurazione Maven
 
-### P3 — Chiarezza del codice
+Il progetto è Java 21. L'esecuzione con JDK 25 produce il warning ONNX
+relativo a `System.load`. Va usato JDK 21 oppure l'opzione
+`--enable-native-access=ALL-UNNAMED`. Il compilatore Maven dovrebbe usare
+`maven.compiler.release=21` per impedire dipendenze involontarie da API
+Java successive.
 
-#### P3.1 Allineare nome e comportamento dell'estrattore
+### P3.1 — Allineare nome e comportamento dell'estrattore
 
-`extractBidirectional` estrae solamente archi outgoing e `GraphManager.expandNode` ignora `Direction`. Il nome, i commenti e l'API devono descrivere il comportamento reale oppure va implementata l'estrazione incoming.
+`extractBidirectional` estrae solamente archi outgoing e
+`GraphManager.expandNode` ignora `Direction`. Il nome, i commenti e l'API
+devono descrivere il comportamento reale oppure va implementata
+l'estrazione incoming.
 
-#### P3.2 Semplificare `InvestigationEngine.processTriple`
+### P3.2 — Semplificare `InvestigationEngine.processTriple`
 
-Il metodo genera tre atomici ma conserva i risultati solo tramite l'effetto laterale di `getOrGenerate`. Le variabili locali non vengono utilizzate.
+Il metodo genera tre atomici ma conserva i risultati solo tramite
+l'effetto laterale di `getOrGenerate`. Le variabili locali non vengono
+utilizzate.
 
-#### P3.3 Separare le responsabilità di `App`
+### P3.3 — Separare le responsabilità di `App`
 
-`App` contiene CLI, query SPARQL, orchestrazione, filtri, ranking, label lookup e output. Dopo aver stabilizzato gli aspetti P1 e P2, queste responsabilità possono essere estratte in componenti verificabili separatamente.
+`App` contiene CLI, query SPARQL, orchestrazione, filtri, ranking, label
+lookup e output. Dopo aver stabilizzato gli aspetti P1 e P2, queste
+responsabilità possono essere estratte in componenti verificabili
+separatamente.
 
-## 7. Ordine di intervento proposto
+## 8. Ordine di intervento proposto
 
-1. test combinato fat node + fat predicate;
-2. API unica per ramo, confidenza e triple;
-3. dominio candidato limitato agli oggetti target;
-4. recupero atomico, senza risultati parziali silenziosi;
-5. rimozione della waterfall di rete e delle richieste Maven superflue;
-6. validazione CLI e pulizia dei chunk obsoleti;
-7. riallineamento dell'estrattore e refactoring di `App`.
+1. P1.1
+2. P1.2
+3. P2.2
+4. P2.3
+5. P2.4
+6. P2.5
+7. P3.1
+8. P3.2
+9. P3.3
 
-## 8. Documentazione correlata
+## 9. Documentazione correlata
 
-- `FLUSSO_DATI.md`: flusso completo dei dati e formule VSA.
-- `README.md`: presentazione pubblica, setup, uso, esempi e limiti.
-- `src/test/java/com/investigator/vsa/TopologicalVectorUpdaterTest.java`: regressioni automatiche del chunking.
+- `README.md`: presentazione pubblica
+- `FLUSSO_DATI.md`: flusso dati e formule VSA
+- `docs/cantieri/README.md`: indice dei cantieri
+- `P1.3_test_combinato.md`, `P1.4_doppia_traversata.md`,
+  `P2.1a_parallelizzazione.md`, `P2.1b_label_e_vsa.md`: report di cantiere
+- `src/test/java/com/investigator/vsa/TopologicalVectorUpdaterTest.java`:
+  regressioni automatiche
```

## 3. Sezioni toccate

| Sezione | Intervento |
|---|---|
| Titolo / header | Scope esteso a tutto il Pilastro 3; base `a7c8a4f` → `c61bc88` |
| §1 Obiettivo | Riscritta (sintesi Pilastro 3, VSA, D=10000, Simpkin) |
| §2 Cantieri completati | Riscritta: refactor pre-P1.x `0ed54c7`, P1.4 `ada1775`, P1.3 `8a1e1be`, P2.1a `cb32edc`, P2.1b `ac0d444` |
| §3 Test | Aggiornata a 5 test con nomi di metodo; esito 5/0/0/0 |
| §4 Demo | Invariata; aggiunta nota provenienza z-score |
| §5 Profilo chiamate | Da 9 sequenziali a 7 (3+2 parallelo, 2 batch sequenziale) |
| §6 Profilo prestazioni | Nuova (VSA ~40 ms, rete 7–15 s, picchi 50 s+) |
| §7 Cantieri aperti | Rimossi P1.3/P1.4/P2.1; mantenuti P1.1, P1.2, P2.2–P2.5, P3.1–P3.3 |
| §8 Ordine | Rinumerato 1–9 |
| §9 Documentazione | Elenco aggiornato |
| `docs/cantieri/README.md` | Voce STATO_LAVORI aggiornata + voce STATO_LAVORI_update |

## 4. Ambiguità

- **Commit hash**: tutti i commit citati sono stati verificati con
  `git log --oneline` (P1.4 `ada1775`, P1.3 `8a1e1be`, P2.1a `cb32edc`,
  P2.1b `ac0d444`). Il refactor pre-P1.x non era indicato nel documento
  originale; il commit che lo contiene è `0ed54c7` ("fix: recover
  recursive Simpkin chunks"), immediatamente precedente a `a7c8a4f`.
- **Demo Nile / Egypt / Mont Blanc**: gli z-score (France 34,00σ,
  Italy 33,85σ) provengono dal vecchio `STATO_LAVORI.md`, che
  fotografava la base pre-P1.x. Non sono stati rimisurati nei cantieri
  P1.3, P1.4, P2.1a, P2.1b (che verificano la sola demo Amleto). Per non
  inventare dati, la nota in §4 distingue esplicitamente i due casi: la
  demo Amleto è confermata invariata, la demo Nile è riportata come
  valore storico pre-P1.x.
- **P2.1**: il cantiere originale era un unico "Ridurre la waterfall
  Wikidata"; è stato completato in due fasi (P2.1a parallelizzazione,
  P2.1b riduzione label). In §7 non compare più tra i cantieri aperti.

## 5. Verifica coerenza numerica

- Chiamate remote: 7 (3 resolve parallelo + 2 fetch RDF parallelo + 2
  batch label sequenziale); minimo teorico 6.
- VSA: ~40 ms (STEP 1 e STEP 3&4).
- Test: 5, con 0 failure/error/skip.
- Demo Amleto: #1 Franz Xaver Süssmayr 30,86σ, #2 Wolfgang Amadeus
  Mozart 30,85σ.
- Commit citati: `0ed54c7`, `ada1775`, `8a1e1be`, `cb32edc`, `ac0d444`.


## Verifica demo Nile (post-chiusura)

Data: 24/09/2029
Query: `Nile : Egypt = Mont Blanc : ?`

Esito: coincide con i valori pubblicati nel README.
- Ruolo sorgente: `country` (z = 8.95σ)
- Ruolo target: `country`
- Ramo: 15.58σ
- #1 France 34.00σ
- #2 Italy 33.85σ

Osservazione: nel run di verifica una delle tre resolve ha impiegato
~78 s (rate-limiting o picco di latenza), trascinando il wall del
gruppo parallelo. Comportamento atteso con `CompletableFuture.join()`:
il wall è la chiamata più lenta. Ipotesi da verificare in futuro: le
3 resolve in parallelo potrebbero essere più soggette al rate-limiting
di Wikidata rispetto a 3 resolve sequenziali. Non bloccante, non
verificato, va nel backlog osservativo.