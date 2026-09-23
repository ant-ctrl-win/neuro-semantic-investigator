# CLI log — Aggiunta CLI ad App.java + exec-maven-plugin

Branch: `integration/codex-simpkin`

## [DUBBIO] risolto

`pom.xml` in `HEAD` conteneva **già** un `exec-maven-plugin` con `<mainClass>com.investigator.InvestigatorCli</mainClass>`, ma la classe `InvestigatorCli` non esiste nel repo. Aggiungere un secondo blocco avrebbe creato una dichiarazione duplicata (Maven fa merge, vince l'ultima). Su indicazione del Tech Lead ho scelto: **sostituire** il `mainClass` in `com.investigator.App` e **riposizionare** il blocco prima di `maven-surefire-plugin`.

## File modificati

### `src/main/java/com/investigator/App.java`
- **A.1** — sostituite le 3 righe hardcoded `resolver.resolve("Amleto"/"William Shakespeare"/"Lacrimosa", ...)` con il parsing `parseArgs(args)` + `opts.get(...)`, più 2 righe di stampa della query e dei parametri (`targetCandidates`, `topK`).
- **A.2** — nel loop finale, `.limit(3)` → `.limit(topK)`.
- **A.3** — aggiunti i metodi statici privati `parseArgs(String[])` e `printUsage()` in fondo alla classe.

### `pom.xml`
- Il blocco `exec-maven-plugin` esistente (in fondo, `com.investigator.InvestigatorCli`) è stato rimosso.
- Nuovo blocco `exec-maven-plugin` 3.2.0 con `<mainClass>com.investigator.App</mainClass>` inserito **prima** di `maven-surefire-plugin`.

## V1 — `mvn clean compile`

```
[INFO] Compiling 15 source files with javac [debug target 21] to target\classes
[INFO] .../jena/SparqlEndpoint.java uses or overrides a deprecated API.
[INFO] BUILD SUCCESS
[INFO] Total time:  3.179 s
```

## V2 — `mvn test`

```
[INFO] Running com.investigator.vsa.TopologicalVectorUpdaterTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.560 s
[INFO] BUILD SUCCESS
```

## V3 — esecuzione default (`mvn exec:java`)

```
[*] Query: Amleto : William Shakespeare = Lacrimosa : ?
[*] Target candidates: 5, top-K: 3
   SORGENTE  : Hamlet                     Tipo: dramatic work
   OGG. NOTO : William Shakespeare        Tipo: human
   TARGET 1  : Requiem                    Tipo: musical work/composition
...
[!] ANALOGIA RISOLTA CON SUCCESSO:
    William Shakespeare sta a  Hamlet
    COME
    #1  Franz Xaver Süssmayr      sta a  Requiem            (σ = 27,54)
    #2  Wolfgang Amadeus Mozart   sta a  Requiem            (σ = 27,44)
    [Logica Applicata]: author ===> composer
```

Attesi Süssmayr + Mozart → **confermati**.

## V4 — `mvn exec:java -Dexec.args="--source Nile --known Egypt --target Danube"`

```
[*] Query: Nile : Egypt = Danube : ?
[*] Target candidates: 5, top-K: 3
...
[!] ANALOGIA RISOLTA CON SUCCESSO:
    Egypt           sta a  Nile
    COME
    #1  Switzerland               sta a  Danube             (σ = 18,01)
    #2  Bosnia and Herzegovina    sta a  Danube             (σ = 17,48)
    #3  Serbia                    sta a  Danube             (σ = 17,43)
    [Logica Applicata]: basin country ===> basin country
```

Attesi Switzerland + Serbia → **confermati** (entrambi in top-3; #2 Bosnia in mezzo).

## V5 — `mvn exec:java -Dexec.args="--help"`

```
Neuro-Semantic Investigator
Uso: App [--source <nome>] [--known <nome>] [--target <nome>]
         [--candidates <n>] [--top <n>] [--help]

  --source      Entità sorgente (default: Amleto)
  --known       Entità nota (default: William Shakespeare)
  --target      Entità target (default: Lacrimosa)
  --candidates  Candidati per disambiguazione target (default: 5)
  --top         Numero di risultati finali da mostrare (default: 3)
  --help, -h    Mostra questo messaggio
```

Exit code = **0**.

## Note
- Nessun commit effettuato.
- `exec:java` esegue in-process: con `--help` il `System.exit(0)` termina correttamente con codice 0.
