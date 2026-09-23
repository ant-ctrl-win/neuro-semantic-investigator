# HANDOFF — Neuro-Semantic Investigator

Documento auto-contenuto di passaggio consegne. Non richiede accesso a chat precedenti.
Repository: `C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator`
Branch corrente: `integration/codex-simpkin`
Data redazione: 2026-09-23

---

## Sezione 1 — Stato del repository

### 1.1 `git log --oneline -5`

```text
b678c93 refactor: stabilize NSI for public release
65c804e Pulizia file .md ed eliminazione praphify. Creazione file Presentazione_XXX.md
bc22d44 Pulizia file .md ed eliminazione praphify. Creazione file Presentazione_XXX.md
c17b723 Pulizia file .md ed eliminazione praphify. Creazione file Presentazione_XXX.md
a9ac0dd Pulizia file .md ed eliminazione praphify. Creazione file Presentazione_XXX.md
```

### 1.2 `git branch -a`

```text
  archive/legacy-automaton
  fix/simpkin-fidelity
* integration/codex-simpkin
  main
  refactor/dbpedia-engine
  remotes/origin/HEAD -> origin/main
  remotes/origin/archive/legacy-automaton
  remotes/origin/fix/simpkin-fidelity
  remotes/origin/main
  remotes/origin/refactor/dbpedia-engine
```

### 1.3 `git status`

```text
On branch integration/codex-simpkin
Changes not staged for commit:
  (use "git add <file>..." to update what will be committed)
  (use "git restore <file>..." to discard changes in working directory)
	modified:   pom.xml
	modified:   src/main/java/com/investigator/AppDis.java
	modified:   src/main/java/com/investigator/vsa/HDVectorMapB.java
	modified:   src/main/java/com/investigator/vsa/strategy/TopologicalVectorUpdater.java

Untracked files:
  (use "git add <file>..." to include in what will be committed)
	src/test/java/com/investigator/vsa/TopologicalVectorUpdaterTest.java

no changes added to commit (use "git add" and/or "git commit -a")
```

### 1.4 File modificati rispetto a `main`

`git diff main --name-status`:

```text
M	pom.xml
M	src/main/java/com/investigator/AppDis.java
M	src/main/java/com/investigator/vsa/HDVectorMapB.java
M	src/main/java/com/investigator/vsa/strategy/TopologicalVectorUpdater.java
```

`git diff main --stat`:

```text
 pom.xml                                            |   5 +
 src/main/java/com/investigator/AppDis.java         | 116 +++++-------
 .../java/com/investigator/vsa/HDVectorMapB.java    |  17 +-
 .../vsa/strategy/TopologicalVectorUpdater.java     | 198 ++++++++++++++-------
 4 files changed, 188 insertions(+), 148 deletions(-)
```

File nuovo non tracciato (non compare in `git diff`): `src/test/java/com/investigator/vsa/TopologicalVectorUpdaterTest.java` (65 righe, test JUnit 5).

> **NOTA CRITICA.** `git rev-parse` mostra che `main`, `integration/codex-simpkin`, `fix/simpkin-fidelity`, `origin/main` e `origin/fix/simpkin-fidelity` puntano **tutti allo stesso commit** `b678c93`. Quindi:
> - `git log main..integration/codex-simpkin` è **vuoto**;
> - **tutto il lavoro del branch è ancora non committato** nella working directory;
> - `git diff main` riflette esattamente le modifiche non committate elencate sopra.

---

## Sezione 2 — Storia del lavoro su questo branch

1. Il branch riallinea l'algebra VSA alla formulazione di Simpkin et al. (2018), rendendo la memoria iperdimensionale deterministica e invertibile.
2. `HDVectorMapB.generateRandom()` non usa più `new Random()` (seed da orologio) ma un seed fisso `generateSeeded(0x53494d504b494eL)`.
3. Aggiunto l'overload `HDVectorMapB.generateRandom(List<HDVector> content)`: seed derivato dall'hash ordinato del contenuto (padding "content-addressed" per i chunk di cardinalità pari).
4. `TopologicalVectorUpdater` è stato riscritto: capacità di chunk configurabile (`chunkCapacity`, default 30), ricombinazione gerarchica ricorsiva (`buildTripleTree`, `encodeChunk`).
5. Introdotta la codifica posizionale cumulativa (Simpkin eq. 5): `content[i] ⊗ permute(i+1) ⊗ ∏ role_j`, con `StopVec` legato al ruolo cumulativo.
6. Aggiunte le API pubbliche inverse `decodeChunkElement(chunk, index, memory)` e `recoverBranch(memory, entityUri, predicateUri)`, più `capacityForSigma(sigma)`.
7. Aggiunto un registro d'istanza `branchPaths` (`entityUri␀predicateUri` → catena di indici) per navigare l'albero ricombinato.
8. `AppDis` ora recupera il ramo via `recoverBranch` e decodifica il chunk **posizione per posizione** (sia lato sorgente sia lato target), invece del vecchio `permute(-100).bind(role)` + `cleanUpChunk` a colpo singolo.
9. Lato target, `AppDis` raccoglie tutti i candidati delle varie posizioni in una `candidateMap` e sceglie i top-3 per σ tramite `ItemMemory.cleanUpRelativeTopK`/`ScoredMatch`.
10. Aggiunto il plugin `maven-surefire-plugin` 3.2.5 in `pom.xml` e il test `TopologicalVectorUpdaterTest` (determinismo, chunking a capacità 30/5, formula `capacityForSigma(3) == 89`).

---

## Sezione 3 — Progetti / branch paralleli (contesto esterno)

### 3.1 `C:\Users\Ion\IdeaProjects\Vaimee\VSA\nsi-experimental`

Clone di lavoro parallelo, con il branch `integration/codex-simpkin` attivo (verificato: `git -C ... branch -a` mostra `* integration/codex-simpkin`). Rispetto alla repository corrente:
- HEAD è `65c804e` (una commit **dietro** `b678c93`).
- Contiene le **stesse** 4 modifiche non committate ai sorgenti (`pom.xml`, `AppDis.java`, `HDVectorMapB.java`, `TopologicalVectorUpdater.java`) e lo stesso test non tracciato. `git -C ... diff --stat` riporta i medesimi conteggi delle modifiche (con lievi differenze di conteggio su `AppDis`, perché in quel clone il file corrisponde a una base leggermente diversa).
- Contiene report documentali di sessione (`codex_report.md`, `integration_report.md`) e il modello binario `models/model.onnx` (415 MB), non presenti come file tracciati qui.
- `origin/HEAD` in quel clone punta a `origin/fix/simpkin-fidelity` (in questa repo punta a `origin/main`).

**Relazione:** è la copia in cui sono state generate/integrate le modifiche "Codex" già confluite qui. Le modifiche sorgente sono già presenti in questa working directory; none è committata in nessuna delle due.

### 3.2 Branch `fix/simpkin-fidelity` (locale e `remotes/origin/fix/simpkin-fidelity`)

Versione precedente dello stesso lavoro. In questa repo **punta allo stesso commit `b678c93` di `main`** (`git rev-parse` verificato); non contiene commit aggiuntivi. `git diff main fix/simpkin-fidelity --stat` è **vuoto**. Il suo storico è quello condiviso con `main` (i commit `Pulizia file .md ...`). Va considerato il ramo di riferimento storico del fix Simpkin, ormai allineato al punto di partenza del branch corrente.

### 3.3 Altri branch

- `archive/legacy-automaton`, `refactor/dbpedia-engine` (locale e `origin`): branch collaterali, non correlati alle modifiche correnti.

---

## Sezione 4 — Problemi aperti (da risolvere)

### 4.1 Soglia hardcoded `0.05` in `AppDis` STEP 1

Nel file `src/main/java/com/investigator/AppDis.java` la ricerca del ruolo sorgente usa una soglia fissa `0.05`:

- riga 160: `if (rawSimilarity > 0.05 && rawSimilarity > bestHypothesisScore) {`
- riga 168: `if (sourceRoleUri == null || bestHypothesisScore < 0.05) {`

Questa soglia va sostituita con uno **z-score adattivo** (calcolato sulla distribuzione delle similarità osservate), coerentemente con il resto della pipeline che già usa σ (`ItemMemory`, `getLastBestSigma`, `cleanUpRelativeTopK`). Attualmente è un doppio uso dello stesso magic number, non giustificato statisticamente.

### 4.2 Caso "Africa" fallisce

La query analogica `Nile : Africa = Danube : ?` **non trova nessun ruolo sorgente** (STEP 1 fallisce con "[ERRORE] La VSA non riesce a trovare l'Oggetto Noto nel vettore della Sorgente"). La query analoga `Nile : Egypt = Danube : ?` funziona correttamente. Serve un'indagine: probabilmente la relazione `Nile → Africa` (parte di / continente) ha cardinalità/struttura del chunk diversa da `Nile → Egypt` e la decodifica posizionale o `recoverBranch` non la espone tra i candidati.

**Stato del codice (verificato al 2026-09-23):** `AppDis.main` ha attualmente le entità hardcoded proprio sul caso fallito:

```java
List<ResolvedEntity> apolloResults   = resolver.resolve("Nile", 1);
List<ResolvedEntity> armstrongResults = resolver.resolve("Africa", 1); //"William Shakespeare"
List<ResolvedEntity> targetResults   = resolver.resolve("Danubio", 5);
```

---

## Sezione 5 — Prossimi passi

1. **Sostituire la soglia `0.05`** in `AppDis` STEP 1 con uno z-score adattivo (analisi della distribuzione delle `rawSimilarity` da fare).
2. **Investigare il caso Africa** (`Nile : Africa = Danube : ?`), confrontandolo con il caso funzionante `Nile : Egypt = Danube : ?`.
3. **Commit finale e merge su `main`** (ricordando che oggi nessuna modifica è committata).

---

## Sezione 6 — Comandi di verifica attuali

Eseguiti nella root della repository (Maven su PATH, nessun wrapper `mvnw`; Java target 21).

- `mvn clean compile` → **BUILD SUCCESS** (verificato 2026-09-23).
- `mvn test` → **2 test passati** (`TopologicalVectorUpdaterTest`, `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`, surefire 3.2.5). Verificato 2026-09-23.
- `AppDis.main` con la demo Amleto → Süssmayr **σ=27.55**, Mozart **σ=27.46**.
- `AppDis.main` con Nile/Egypt → Switzerland **σ=18.07**, Serbia **σ=17.58**.

> Le due esecuzioni `AppDis.main` richiedono **rete** verso `https://query.wikidata.org/sparql` e i modelli in `models/` (`model.onnx` 435.811.539 byte e `tokenizer.json` presenti). Le entità sono **hardcoded** dentro `main()`: i valori σ sopra riportati valgono per le entità usate al momento della misurazione, non per l'attuale `Nile/Africa/Danubio` (caso 4.2, che fallisce).
> Non esiste `exec-maven-plugin`: per eseguire `AppDis` usare l'IDE oppure `java -cp "target/classes;$(Get-Content cp.txt)" com.investigator.AppDis` dopo `mvn dependency:build-classpath "-Dmdep.outputFile=cp.txt"`.

---

## Riepilogo esecutivo

Lavoro = portare `AppDis`/`TopologicalVectorUpdater` a una gestione Simpkin-fedele dei chunk VSA (determinismo + decodifica posizionale + recupero gerarchico dei rami). Tutto il codice è **non committato**; `main`, `integration/codex-simpkin` e `fix/simpkin-fidelity` sono allo stesso commit. Restano due problemi funzionali: soglia `0.05` da rendere adattiva e fallimento del caso `Nile : Africa`. Poi commit e merge.
