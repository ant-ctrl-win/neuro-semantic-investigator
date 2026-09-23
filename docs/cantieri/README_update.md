# Cantiere — Aggiornamento README post-P2.1b

- **Data**: 2026-09-23
- **Branch**: `integration/codex-simpkin`
- **Commit**: `docs: update README after P2.1b (perf, references, limitations)`
- **File modificati**: `README.md`, `docs/cantieri/README.md` (indice), `docs/cantieri/README_update.md` (questo report)

## 1. Descrizione del fix

Intervento puramente documentale su `README.md`, allineato allo stato reale
della pipeline dopo P2.1a e P2.1b. Nessuna modifica a codice, `pom.xml`,
`FLUSSO_DATI.md`, `AGENTS.md` o al contenuto preesistente di `docs/cantieri/`.

I cinque interventi richiesti:

1. **References** — sostituito l'intero elenco con le quattro voci richieste:
   Simpkin et al. (2018), Gayler (2003), Schlegel et al. (2022), Kanerva
   (2009). Rimossi i due riferimenti precedenti non richiesti (Neubert et
   al. 2019, Karunaratne et al. 2021) e riordinati secondo la lista fornita.
2. **Processing pipeline** — il paragrafo finale passa da "about nine remote
   requests in sequence" a **sette richieste Wikidata** (tre ricerche
   entità, due espansioni grafo, due batch-label), con ricerca ed espansioni
   emesse **in parallelo** e chiamate rimanenti sequenziali.
3. **Nuova sezione "Performance profile"** — inserita tra "Processing
   pipeline" e "Stack": VSA ~40 ms, rete 7 richieste (minimo teorico 6), demo
   end-to-end ~6–7 s, timing per fase stampati a fine run.
4. **Current limitations** — la voce "Sequential network waterfall" è
   diventata **"Network latency"** (sette richieste, minimo teorico sei) e in
   fondo è stata aggiunta la voce **"Transient Wikidata errors"** (5xx, es.
   502, che degradano la risoluzione delle label).
5. **Next engineering steps** — rimossi i punti completati (test combinato,
   doppia traversata, parallelizzazione + timing) e rinumerati i nove punti
   rimanenti nell'ordine indicato. Rimane inoltre rimosso il rimando finale
   a `STATO_LAVORI.md`.

Inoltre è stata **rimossa ogni menzione di `STATO_LAVORI.md`** dal README
(era citato due volte), perché il file è stato spostato in `docs/cantieri/` e
non è più documentazione pubblica. L'indice `docs/cantieri/README.md` è stato
aggiornato con la voce di questo report.

## 2. Git diff delle modifiche

Il diff copre `README.md` e `docs/cantieri/README.md` (indice). Il presente
report è esso stesso un artefatto del commit e non compare nel proprio diff.

```diff
diff --git a/README.md b/README.md
index 4617358..4fb2a97 100644
--- a/README.md
+++ b/README.md
@@ -133,13 +133,30 @@ The main stages are:
 7. traverse the target tree and rank recovered objects;
 8. fetch display labels in batches.
 
-The ordinary path currently performs about nine remote requests in
-sequence. Property and final-result labels are batched, but the remaining
-request waterfall makes runtime sensitive to Wikidata latency.
+The ordinary path performs seven Wikidata requests: three entity
+searches, two graph expansions, and two batch-label requests. The
+three searches and the two expansions are issued in parallel; the
+remaining calls are sequential. Property and final-result labels are
+batched.
 
 Detailed internals are documented in [FLUSSO_DATI.md](FLUSSO_DATI.md).
-Completed work and remaining defects are tracked in
-[STATO_LAVORI.md](STATO_LAVORI.md).
+
+---
+
+## Performance profile
+
+The engine separates structural computation from remote retrieval:
+
+- **VSA operations** (source-role discovery and target extraction):
+  ~40 ms total.
+- **Network**: 7 Wikidata requests, near-minimal. The theoretical
+  minimum is 6; the seventh is the batch label for final candidates,
+  which cannot be merged because candidates only exist after the VSA
+  projection.
+- **End-to-end demo**: ~6–7 s on a domestic connection, dominated by
+  Wikidata latency.
+
+Per-stage timings are printed at the end of every CLI run.
 
 ---
 
@@ -332,13 +349,15 @@ that rule-based systems cannot express and pure LLMs cannot justify.
 
 ## Current limitations
 
-### Sequential network waterfall
+### Network latency
 
-The normal path performs approximately nine sequential Wikidata
-requests: three entity searches, two graph expansions, two individual
-role-label lookups, and two batch-label requests. This is a fixed
-waterfall rather than a query per RDF property, but its latency is still
-the sum of all remote calls.
+The normal path performs seven Wikidata requests: three entity
+searches, two graph expansions, and two batch-label requests. The
+three searches and the two expansions are issued in parallel; the
+remaining calls are sequential. The number of calls is close to the
+theoretical minimum of six (the seventh is the label batch for final
+candidates, which cannot be merged because candidates only exist after
+the VSA projection). Runtime remains dominated by Wikidata latency.
 
 ### One-hop outgoing graph only
 
@@ -376,27 +395,28 @@ Results depend on live Wikidata content and availability. Semantic role
 alignment requires the local ONNX model and tokenizer. Console output is
 currently in Italian.
 
+### Transient Wikidata errors
+
+Occasional `5xx` responses (e.g. `502 Bad Gateway`) from the Wikidata
+endpoint can degrade label resolution and change the outcome of a run.
+The system does not mask these errors; results may vary between runs.
+
 ---
 
 ## Next engineering steps
 
 The immediate work is intentionally narrower than a product roadmap:
 
-1. add a combined regression test with at least 30 predicates and one
-   predicate containing 100 objects;
-2. remove the duplicate branch traversal between `recoverBranch` and
-   `recoverTriples`;
-3. restrict final candidates to objects of the selected target role;
-4. make recursive recovery report incomplete results explicitly;
-5. parallelize independent Wikidata requests and add per-stage timing;
-6. remove unused test frameworks and the dynamic TestNG version;
-7. validate CLI input before initializing ONNX;
-8. remove obsolete chunks when rebuilding a node;
-9. align graph-extraction names with outgoing-only behavior or implement
-   incoming extraction.
-
-See [STATO_LAVORI.md](STATO_LAVORI.md) for the evidence and detailed
-order of intervention.
+1. restrict final candidates to objects of the selected target role;
+2. make recursive recovery report incomplete results explicitly;
+3. remove unused test frameworks and the dynamic TestNG version;
+4. validate CLI input before initializing ONNX;
+5. remove obsolete chunks when rebuilding a node;
+6. align the Maven runtime and set `maven.compiler.release=21`;
+7. align graph-extraction names with outgoing-only behavior or
+   implement incoming extraction;
+8. simplify `InvestigationEngine.processTriple`;
+9. separate the responsibilities of `App`.
 
 ---
 
@@ -407,14 +427,20 @@ order of intervention.
   Chunking for Decentralized Workflows.* IEEE International Conference
   on Semantic Computing (ICSC). — *Foundational reference for the
   hierarchical chunking scheme implemented in this project.*
-- Kanerva, P. (2009). *Hyperdimensional Computing: An Introduction to
-  Computing in Distributed Representation with High-Dimensional Random
-  Vectors.* Cognitive Computation, 1(2), 139–159.
-- Neubert, P., Schubert, S., & Protzel, P. (2019). *An Introduction to
-  Hyperdimensional Computing for Robotics.* Chemnitz University of
-  Technology.
-- Karunaratne, G. et al. (2021). *Robust High-dimensional Memory-augmented
-  Neural Networks.* IBM Research – Zurich.
+- Gayler, R. W. (2003). *Vector Symbolic Architectures answer
+  Jackendoff's challenges for cognitive neuroscience.* In Proceedings
+  of the Joint International Conference on Cognitive Science
+  (ICCS/ASCS'03), 133–138. University of New South Wales, Sydney.
+  arXiv:cs/0412059 — *Foundational reference for Vector Symbolic
+  Architectures.*
+- Schlegel, K., Neubert, P., & Protzel, P. (2022). *A comparison of
+  vector symbolic architectures.* Artificial Intelligence Review, 55,
+  4523–4555. — *Reference for the MAP-B binary model used in this
+  project.*
+- Kanerva, P. (2009). *Hyperdimensional Computing: An Introduction
+  to Computing in Distributed Representation with High-Dimensional
+  Random Vectors.* Cognitive Computation, 1(2), 139–159. — *Background
+  on hyperdimensional computing.*
 
 ---
 
diff --git a/docs/cantieri/README.md b/docs/cantieri/README.md
index 4dc2843..45e6435 100644
--- a/docs/cantieri/README.md
+++ b/docs/cantieri/README.md
@@ -11,3 +11,4 @@ contesto, diff, output di verifica, esito.
 - [P1.4_doppia_traversata.md](P1.4_doppia_traversata.md) — Eliminazione doppia traversata
 - [P2.1a_parallelizzazione.md](P2.1a_parallelizzazione.md) — Parallelizzazione fasi remote
 - [P2.1b_label_e_vsa.md](P2.1b_label_e_vsa.md) — Riduzione label calls + misura VSA
+- [README_update.md](README_update.md) — Aggiornamento README post-P2.1b
```

## 3. Sezioni toccate

| # | Sezione README | Tipo di modifica |
|---|---|---|
| 1 | `## Performance profile` (nuova) | Aggiunta, tra "Processing pipeline" e "Stack" |
| 2 | `## Processing pipeline` — ultimo paragrafo | 9 → 7 richieste; ricerca/espansioni in parallelo; rimando a `STATO_LAVORI.md` rimosso |
| 3 | `## Current limitations` → voce "Network latency" | Sostituita "Sequential network waterfall" |
| 4 | `## Current limitations` → voce "Transient Wikidata errors" | Aggiunta in fondo alla sezione |
| 5 | `## Next engineering steps` | Rimossi i 3 punti completati; rinumerati 1–9; rimando a `STATO_LAVORI.md` rimosso |
| 6 | `## References` | Riscritta integralmente (Simpkin, Gayler, Schlegel, Kanerva) |
| 7 | `docs/cantieri/README.md` (indice) | Aggiunta voce `README_update.md` |

## 4. Verifica delle citazioni

- **Schlegel et al. (2022)** — Verificata tramite **Crossref API**
  (`api.crossref.org/works?query.bibliographic=A comparison of vector
  symbolic architectures`). Risultato:
  - titolo: *A comparison of vector symbolic architectures*
  - autori: Kenny Schlegel, Peer Neubert, Peter Protzel
  - contenitore: *Artificial Intelligence Review*
  - volume: **55**
  - pagine: **4523–4555**
  - data di pubblicazione (online): 2021-12-15 (numero 2022)
  - DOI: `10.1007/s10462-021-10110-3`

  I numeri forniti nel prompt (**vol. 55, pp. 4523–4555**) sono quindi
  **corretti**: la citazione è stata inserita così com'era, senza aggiungere
  il fallback "(see publisher for exact pagination)".
- **Simpkin et al. (2018)**, **Gayler (2003)**, **Kanerva (2009)** — citazioni
  fornite dal committente e riportate verbatim, senza verifica indipendente
  (nessun dubbio segnalato).

## 5. Verifiche finali

- Nessun riferimento residuo a `STATO_LAVORI.md` in `README.md`
  (`grep` su `README.md`: 0 occorrenze).
- Coerenza dei numeri: 7 richieste / ~40 ms / ~6–7 s presenti e concordi in
  "Processing pipeline", "Performance profile" e "Network latency".
- Markdown: nessun blocco di codice rotto, nessun link morto
  (`FLUSSO_DATI.md` resta l'unico rimando interno).
- `git status --short` pulito dopo il commit.
- `git log --oneline -2` e `ls docs/cantieri/` come da richiesta.

## 6. Ambiguità incontrate

- **Contraddizione vincoli vs deliverable**: i vincoli richiedevano "solo
  `README.md`, nessun altro file" e "non toccare `docs/cantieri/`", mentre il
  deliverable chiedeva di creare `docs/cantieri/README_update.md`. Chiarito
  con il committente: il vincolo riguarda i file pubblici/di codice
  (FLUSSO_DATI.md, contenuto esistente di docs/cantieri/, AGENTS.md, codice
  Java, pom.xml); il report di cantiere è un artefatto e va creato come
  richiesto, incluso l'aggiornamento dell'indice.
- **Indice `docs/cantieri/README.md`**: aggiunta della voce richiesta
  (`- [README_update.md](README_update.md) — Aggiornamento README post-P2.1b`).
  Convenzione confermata per i cantieri futuri (FLUSSO_DATI update,
  STATO_LAVORI update).
