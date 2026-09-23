# TASK 1 — Cleanup Log

Repository: `C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator`
Branch: `integration/codex-simpkin`
Data: 2026-09-23

## 1. File eliminati

| File | Comando |
|---|---|
| `src/main/java/com/investigator/App.java` (versione precedente, superata) | `git rm` |

## 2. File rinominati

| Da | A | Comando |
|---|---|---|
| `src/main/java/com/investigator/AppDis.java` | `src/main/java/com/investigator/App.java` | `git mv` |

## 3. File modificati (righe toccate)

| File | Intervento | Righe |
|---|---|---|
| `src/main/java/com/investigator/App.java` | Rinominata classe `AppDis` -> `App`. Nessun altro riferimento testuale a "AppDis" presente nel file, quindi nessun commento/banner da aggiornare. | 13 |
| `src/main/java/com/investigator/vsa/strategy/SemanticEmbeddingStrategy.java` | Aggiunto blocco javadoc `@deprecated` e annotazione `@Deprecated` sulla classe. | 18-23 |
| `src/main/java/com/investigator/vsa/strategy/SemanticEmbeddingStrategy.java` | Rimosso riferimento modello sbagliato "all-MiniLM-L6-v2" (B.3). | 15 |
| `src/main/java/com/investigator/vsa/strategy/SemanticEmbeddingStrategy.java` | Rimosso "(pesa circa 22MB)" (B.3). | 35 |
| `src/main/java/com/investigator/vsa/strategy/SemanticEmbeddingStrategy.java` | "(384-D)" -> "(768-D)" (B.3). | 60 |
| `src/main/java/com/investigator/vsa/strategy/SemanticEmbeddingStrategy.java` | "100.000-D" -> "10.000-D" (B.3). | 63 |

### A.4 — Verifica chiamanti `AppDis`

`grep -r "AppDis" src/` (inclusi i file `.java`): **nessuna occorrenza**. Nessun chiamante residuo.

## 4. Output `mvn clean compile`

```text
[INFO] Scanning for projects...
[WARNING] Some problems were encountered while building the effective model for com.investigator:neuro-semantic-investigator:jar:1.0
[WARNING] 'dependencies.dependency.version' for org.testng:testng:jar is either LATEST or RELEASE (both of them are being deprecated) @ line 58, column 22
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.
[INFO] ------------< com.investigator:neuro-semantic-investigator >------------
[INFO] Building neuro-semantic-investigator 1.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[WARNING] Could not transfer metadata org.testng:testng/maven-metadata.xml from/to sjenar (https://maven.pkg.github.com/vaimee/sjenar): status code: 401, reason phrase: Unauthorized (401)
[WARNING] Could not transfer metadata org.testng:testng/maven-metadata.xml from/to sepa (https://maven.pkg.github.com/vaimee/SEPA): status code: 401, reason phrase: Unauthorized (401)
[INFO]
[INFO] --- clean:3.2.0:clean (default-clean) @ neuro-semantic-investigator ---
[INFO] Deleting C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\target
[INFO]
[INFO] --- resources:3.4.0:resources (default-resources) @ neuro-semantic-investigator ---
[INFO] Copying 2 resources from src\main\resources to target\classes
[INFO]
[INFO] --- compiler:3.13.0:compile (default-compile) @ neuro-semantic-investigator ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 15 source files with javac [debug target 21] to target\classes
[INFO] /C:/Users/Ion/IdeaProjects/Vaimee/VSA/neuro-semantic-investigator/src/main/java/com/investigator/jena/SparqlEndpoint.java: ...\jena\SparqlEndpoint.java uses or overrides a deprecated API.
[INFO] /C:/Users/Ion/IdeaProjects/Vaimee/VSA/neuro-semantic-investigator/src/main/java/com/investigator/jena/SparqlEndpoint.java: Recompile with -Xlint:deprecation for details.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.253 s
[INFO] Finished at: 2026-09-23T12:34:48+02:00
```

## 5. Output `mvn test`

```text
[INFO] --- surefire:3.2.5:test (default-test) @ neuro-semantic-investigator ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO]
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.investigator.vsa.TopologicalVectorUpdaterTest
   [ItemMemory] Inizializzata con D=10000. Soglia di clean-up calcolata: 4,00 σ
   [ItemMemory] Inizializzata con D=10000. Soglia di clean-up calcolata: 4,00 σ
   [ItemMemory] Inizializzata con D=10000. Soglia di clean-up calcolata: 4,00 σ
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.546 s -- in com.investigator.vsa.TopologicalVectorUpdaterTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.270 s
[INFO] Finished at: 2026-09-23T12:34:58+02:00
```

Esito: **BUILD SUCCESS**, **2 test passati**.

## 6. Dubbi incontrati

Nessun dubbio bloccante. Due note operative:

- Il warning di compilazione "SparqlEndpoint.java uses or overrides a deprecated API" era **preesistente** e non è stato introdotto da questo task (SparqlEndpoint.java non è tra i file toccati e non referenzia `SemanticEmbeddingStrategy`).
- La working directory contiene altre modifiche non committate **preesistenti al task** (`pom.xml`, `engine/InvestigationEngine.java`, `jena/GraphManager.java`, `vsa/HDVectorMapB.java`, `vsa/strategy/TopologicalVectorUpdater.java`) e file non tracciati (`HANDOFF.md`, `cleanup_plan.md`, `src/test/.../TopologicalVectorUpdaterTest.java`): non sono state toccate.
- Nessun commit e nessun push effettuati, come richiesto.
