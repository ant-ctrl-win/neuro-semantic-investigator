# TASK 2 — Cleanup Log

Repository: `C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator`
Branch: `integration/codex-simpkin`
Data: 2026-09-23

## 1. File modificati (righe toccate)

### C — Fix N+1 su `fetchLabelFromWikidata` (App.java)

| Voce | Intervento | Righe |
|---|---|---|
| C.1 | Aggiunto metodo `batchFetchLabels(Collection<String>)` (una sola query SPARQL `VALUES`) dopo `fetchLabelFromWikidata`. | nuovo blocco (~57 righe) |
| C.2 | Sostituito integralmente `extractPropertyLabels` con la versione batch: raccolta di tutte le URI (out+in), una chiamata `batchFetchLabels`, poi stampa. | ~34 righe sostituite da ~48 |
| C.3 | Nel loop top-3 finale: pre-passata `candidateKeys` -> `batchFetchLabels` -> `candidateLabels`; stampa da mappa. | ~7 righe sostituite da ~13 |
| C.4 | `fetchLabelFromWikidata` **non** rimosso né modificato. | — |

### D — Pulizia dead code

| File | Intervento | Righe |
|---|---|---|
| `jena/GraphManager.java` | Rimosso blocco legacy interamente commentato (vecchia classe duplicata). | 1-78 |
| `jena/GraphManager.java` | Rimosso metodo `getUnexploredNodes`. | ~134-153 |
| `jena/GraphManager.java` | Commento `direction`: `"mantenuto per retrocompatibilità dell'API"` -> `"attualmente ignorato; estrazione incoming in roadmap; l'estrazione attuale è 1-hop outgoing"`. | ~104 |
| `jena/TripleExtractor.java` | Rimossi import inutilizzati: `Resource`, `RDFNode`, `Statement`, `StmtIterator`, `Stream`, `ArrayList`, `List`. | 7-13 |
| `jena/TripleExtractor.java` | Rimosso `Direction.BOTH` dall'enum. | ~28 |
| `jena/TripleExtractor.java` | Commento di classe: `"in modo BIDIREZIONALE (Multi-Hop di base)"` -> `"in modo 1-hop outgoing"`. | 20 |
| `jena/TripleExtractor.java` | Commento del metodo: `"Il nuovo estrattore che fa Multi-Hop bidirezionale in un colpo solo."` -> `"Estrae il sotto-grafo 1-hop outgoing."`. | 32 |
| `engine/InvestigationEngine.java` | Rimossi campi `extractedNodeVectors` / `extractedTripleVectors`. | 17-18 |
| `engine/InvestigationEngine.java` | Rimosso record `Candidate`. | 20 |
| `engine/InvestigationEngine.java` | Rimossi costruttori a 2 e 3 argomenti; mantenuto solo quello a 4 argomenti. | 22-32 |
| `engine/InvestigationEngine.java` | Rimossi gli usi dei campi in `processTriple` (bind `tripleVector` + `.add(...)`). | 56-62 |
| `engine/InvestigationEngine.java` | In `expandAndProcess`: rimossa la chiamata commentata `expandNodeIncoming`, il loop di conteggio e la println `[DEBUG] LocalGraph contains...`. | 70-77 |
| `engine/InvestigationEngine.java` | Rimossi getter `getItemMemory`, `getNodeVectors`, `getTripleVectors`, `clearExtractedVectors`. | 95-110 |
| `embedding/OntologyTranslator.java` | Rimossa costante `EMBEDDING_DIMENSION`. | 15 |
| `embedding/OntologyTranslator.java` | Rimosso metodo `enrichLabelsWithExpectedTypes` (con javadoc). | 25-48 |
| `App.java` (nuovo) | Rimosso blocco `[DIAG]` commentato. | 130-155 |
| `App.java` (nuovo) | Rimosso vecchio STEP 1 commentato (`rawSimilarity > 0.05`). | 163-198 |
| `App.java` (nuovo) | Rimossa riga commentata `// ... (Sim: %.2f) ...`. | 268 |
| `App.java` (nuovo) | Commento soglia: rimosso riferimento al test inesistente "Nile/Big Bang"; nuova formulazione su "basin country vs place of publication, score 0.55". | 301-303 |
| `App.java` (nuovo) | D.6: entità di default ripristinate `Nile/Boat/cave` -> `Amleto/William Shakespeare/Lacrimosa`. | 32-34 |

`git diff --stat` (solo file toccati dal Task 2):
```text
 src/main/java/com/investigator/App.java            | 258 ++++++++++++++-------
 .../investigator/embedding/OntologyTranslator.java |  26 ---
 .../investigator/engine/InvestigationEngine.java   |  54 -----
 .../java/com/investigator/jena/GraphManager.java   | 105 +--------
 .../com/investigator/jena/TripleExtractor.java     |  15 +-
 5 files changed, 182 insertions(+), 276 deletions(-)
```

Verifica grep post-modifica su `src/**/*.java` per `[DEBUG]`, `Multi-Hop`, `BIDIREZIONALE`, `Nile/Big Bang`, `Direction.BOTH`: **nessuna occorrenza**.

## 2. Voci non applicate (con motivo)

| Voce | Motivo |
|---|---|
| **D.4 `ResolvedEntity`** | Già esclusa dal Task (campi valorizzati dal costruttore canonico in `EntityResolver` → fuori scope). `ResolvedEntity.java` non toccato. |
| **D.5 sottovoce `[LLM]` / "Neil Armstrong"** | Esplicitamente da saltare: in questo file non esistono (erano nel vecchio `App.java` eliminato in Task 1). Nessuna occorrenza trovata. |
| Import ora potenzialmente inutilizzati (non richiesti dal piano, **non rimossi**) | `GraphManager.java`: `java.util.HashSet`, `java.util.Set` (usati solo da `getUnexploredNodes`). `InvestigationEngine.java`: `RandomGenerationStrategy` (usato solo dal costruttore a 2 argomenti rimosso). Il piano non ne prevedeva la rimozione; lasciati e **segnalati** per decisione. |

## 3. Dubbi incontrati

- **Nessun dubbio bloccante.** Il codice C.1–C.3 è stato inserito verbatim come fornito.
- **σ demo leggermente diversi dagli attesi:** Süssmayr **27.54** (atteso ~27.55), Mozart **27.44** (atteso ~27.46). È una differenza di arrotondamento/varianza determinata dai dati live di Wikidata, non un errore di pipeline. Ordine e magnitudine confermati.
- **Mojibake in console:** i caratteri accentati (`è`, `σ`, `Süssmayr`) appaiono corrotti (`?`, `�`) per via della codepage della console Windows, non del codice.
- La demo richiede **rete** verso `query.wikidata.org` e i modelli in `models/` (presenti: `model.onnx` 435.811.539 byte, `tokenizer.json`).

## 4. Output `mvn clean compile`

```text
[INFO] --- clean:3.2.0:clean (default-clean) @ neuro-semantic-investigator ---
[INFO] Deleting C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\target
[INFO]
[INFO] --- compiler:3.13.0:compile (default-compile) @ neuro-semantic-investigator ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 15 source files with javac [debug target 21] to target\classes
[INFO] ...\jena\SparqlEndpoint.java uses or overrides a deprecated API.
[INFO] ...\jena\SparqlEndpoint.java: Recompile with -Xlint:deprecation for details.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.128 s
[INFO] Finished at: 2026-09-23T12:45:59+02:00
```

## 5. Output `mvn test`

```text
[INFO] --- surefire:3.2.5:test (default-test) @ neuro-semantic-investigator ---
[INFO] Running com.investigator.vsa.TopologicalVectorUpdaterTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.629 s -- in com.investigator.vsa.TopologicalVectorUpdaterTest
[INFO]
[INFO] Results:
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.199 s
[INFO] Finished at: 2026-09-23T12:46:08+02:00
```

## 6. Output demo `App.main`

Esecuzione: `java -cp "target/classes;$(Get-Content cp.txt)" com.investigator.App` (dopo `mvn dependency:build-classpath`).

```text
=== NEURO-SEMANTIC INVESTIGATOR: LA VERA ANALOGIA ===
=== Flusso: Risoluzione -> Reranking -> Scoperta VSA -> Encoder-Embedding -> Proiezione Olografica ===

   [ItemMemory] Inizializzata con D=10000. Soglia di clean-up calcolata: 4,00 σ
[*] FASE 0: Risoluzione entità e profilazione ontologica...
   [RERANKER] Disambiguazione di 5 candidati (class similarity 60% + sitelinks 40%):
   [RERANKER] -> Scelto: Requiem (Combinato: 0,835)

=======================================================
   REPORT ONTOLOGICO PRELIMINARE
=======================================================
   SORGENTE  : Hamlet                     Tipo: dramatic work
   OGG. NOTO : William Shakespeare        Tipo: human
   TARGET 1  : Requiem                    Tipo: musical work/composition
=======================================================

   [COMPAT CHECK] 'dramatic work' vs 'musical work/composition' -> Similarità: 0,726 (Soglia: 0,35)
[*] Ingestione e vettorizzazione grafi in corso...
   [GraphManager] 1-hop expansion: http://www.wikidata.org/entity/Q41567
   [GraphManager] 1-hop expansion: http://www.wikidata.org/entity/Q207875

=======================================================
   STEP 1: DEDUZIONE DEL RUOLO SORGENTE (VSA)
=======================================================
   -> DEDUZIONE COMPLETATA: L'oggetto è legato tramite 'author' (z=14,60σ)

=======================================================
   STEP 2: IL PONTE ANALOGICO (Embedding + Filtro Strutturale)
=======================================================
   [INDAGINE ONTOLOGICA] Inventario delle proprietà del target:
   --- PROPRIETÀ IN USCITA (I Figli) ---
      - [OUT] .../P935  --->  Label: 'Commons gallery'
      - [OUT] .../P10765  --->  Label: 'Muziekweb composition ID'
      - [OUT] .../P7937  --->  Label: 'form of creative work'
      - [OUT] .../P1476  --->  Label: 'title'
      - [OUT] .../P1994  --->  Label: 'AllMusic composition ID'
      - [OUT] .../P214  --->  Label: 'VIAF cluster ID'
      - [OUT] .../P577  --->  Label: 'publication date'
      - [OUT] .../P571  --->  Label: 'inception'
      - [OUT] .../P6216  --->  Label: 'copyright status'
      - [OUT] .../P6058  --->  Label: 'Larousse ID'
      - [OUT] .../P826  --->  Label: 'tonality'
      - [OUT] .../P51  --->  Label: 'audio'
      - [OUT] .../P227  --->  Label: 'GND ID'
      - [OUT] .../P1617  --->  Label: 'BBC Things ID'
      - [OUT] .../P839  --->  Label: 'IMSLP ID'
      - [OUT] .../P910  --->  Label: "topic's main category"
      - [OUT] .../P435  --->  Label: 'MusicBrainz work ID'
      - [OUT] .../P86  --->  Label: 'composer'
      - [OUT] .../P1417  --->  Label: 'Encyclopædia Britannica Online ID'
      - [OUT] .../P407  --->  Label: 'language of work or name'
      - [OUT] .../P528  --->  Label: 'catalog code'
      - [OUT] .../P646  --->  Label: 'Freebase ID'
      - [OUT] .../P31  --->  Label: 'instance of'
      - [OUT] .../P1552  --->  Label: 'has characteristic'
      - [OUT] .../P2000  --->  Label: 'CPDL ID'
      - [OUT] .../P1191  --->  Label: 'date of first performance'
   --- PROPRIETÀ IN ENTRATA (I Genitori) ---
   [FILTRO STRUTTURALE RDF] Scrematura delle proprietà incompatibili...
   -> Proprietà rimaste dopo il filtro strutturale: 8 su 26
   -> Chiedo all'Embedding di tradurre 'author' tra i candidati rimasti...

   [Encoder] Analisi semantica per: 'author'...
      -> Confronto con 'tonality': Similarità 0,540
      -> Confronto con 'language of work or name': Similarità 0,596
      -> Confronto con "topic's main category": Similarità 0,556
      -> Confronto con 'has characteristic': Similarità 0,569
      -> Confronto con 'form of creative work': Similarità 0,570
      -> Confronto con 'instance of': Similarità 0,539
      -> Confronto con 'composer': Similarità 0,623
      -> Confronto con 'copyright status': Similarità 0,589

   [Encoder] -> MATCH VINCENTE! 'composer' è l'equivalente di 'author' (Score: 0,62)

=======================================================
    STEP 3 & 4: PROIEZIONE E ESTRAZIONE TARGET (VSA)
=======================================================
   -> Uso la chiave tradotta ('composer') per aprire il vettore bersaglio...
   -> [SIMPKIN CLEAN-UP] Recupero del ramo intermedio (Chunk)...
   -> Ramo recuperato con successo! Z-Score Ramo: 8,74 σ
   -> Svincolo l'oggetto dal ramo purificato...

[!] ANALOGIA RISOLTA CON SUCCESSO:
    William Shakespeare sta a  Hamlet
    COME

    #1  Franz Xaver Süssmayr      sta a  Requiem            (σ = 27,54)
    #2  Wolfgang Amadeus Mozart   sta a  Requiem            (σ = 27,44)

    [Logica Applicata]: author ===> composer
```

### Checklist V3

| Verifica | Esito |
|---|---|
| Output senza `[DEBUG]` | OK (nessuna occorrenza) |
| Output senza `Multi-Hop` | OK (nessuna occorrenza) |
| Una riga `[GraphManager] 1-hop expansion` per nodo | OK (2 righe per 2 nodi: Q41567, Q207875) |
| Süssmayr + Mozart con σ attesi | OK — **Süssmayr 27,54** / **Mozart 27,44** |

Nessun commit e nessun push effettuati, come richiesto.
