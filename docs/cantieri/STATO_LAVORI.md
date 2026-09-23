# Stato del lavoro sul chunking VSA

Data della revisione: 23 settembre 2026
Branch di lavoro: `integration/codex-simpkin`
Base corrente: `a7c8a4f`

## 1. Obiettivo

Il lavoro ha riallineato il chunking gerarchico della memoria VSA allo schema descritto da Simpkin et al. (2018), preservando la demo:

```text
Amleto : William Shakespeare = Lacrimosa : ?
```

Il problema iniziale riguardava soprattutto nodi Wikidata ad alto grado. Un bundle radice poteva contenere troppi rami e perdere il segnale dei singoli predicati nel rumore.

## 2. Modifiche completate

### 2.1 Permutazione posizionale cumulativa

`TopologicalVectorUpdater.encodeChunk` applica a ogni elemento lo shift dipendente dalla posizione e il prodotto cumulativo dei role vector:

```text
ρ(Zᵢ,i) ⊗ (p₀ ⊗ ... ⊗ pᵢ₋₁)
```

La codifica include `StopVec`, come nell'equazione 5 del paper.

### 2.2 Role vector posizionali

Le posizioni sono rappresentate da atomici deterministici con URI interne:

```text
vsa:internal:position:0
vsa:internal:position:1
...
```

La decodifica ricostruisce lo stesso prodotto cumulativo e applica la permutazione inversa.

### 2.3 Capacità configurabile

`TopologicalVectorUpdater` accetta la capacità nel costruttore. Il default rimane 30 per compatibilità. È disponibile anche `capacityForSigma(double)`; per `D=10000` e `3σ` restituisce 89.

Con capacità 30, ogni chunk contiene al massimo 29 elementi più `StopVec`.

### 2.4 Split ricorsivo dei rami

Quando un'entità possiede almeno 30 predicati, i rami vengono divisi in gruppi da 29 e ricombinati ricorsivamente.

Per 100 predicati:

```text
29 + 29 + 29 + 13
```

`BranchPath` conserva il ruolo di accesso alla radice e le posizioni necessarie per raggiungere il ramo foglia.

### 2.5 Padding deterministico

I vettori usati per risolvere i pareggi del bundling sono derivati dal contenuto ordinato del chunk. Ricostruzioni consecutive producono lo stesso vettore.

### 2.6 Correzione dei nodi con 1–29 predicati

Il ramo foglia era già legato al predicato, ma la radice diretta applicava nuovamente lo stesso binding. Nei binary spatter codes:

```text
(Bₚ ⊗ P) ⊗ P = Bₚ
```

Il secondo binding cancellava quindi il ruolo e rendeva impossibile il recupero. La radice diretta conserva ora il singolo binding.

### 2.7 Albero ricorsivo dei valori

La precedente implementazione partizionava i predicati con molti oggetti, ma non conservava una struttura percorribile. `ValueNode` registra ora:

- vettore puro;
- figli;
- numero di foglie discendenti.

Sono state aggiunte le API:

```java
recoverTriple(memory, entityUri, predicateUri, index)
recoverTriples(memory, entityUri, predicateUri)
```

Con 100 oggetti e capacità 30, il ramo contiene quattro sotto-chunk da `29 + 29 + 29 + 13`. Con capacità 5 vengono attraversati quattro livelli, verificando la ricorsione oltre il singolo livello intermedio.

### 2.8 Clean-up strutturale

Dopo la decodifica di ogni posizione, il vettore rumoroso viene confrontato con il figlio atteso. Il sotto-chunk puro viene ripristinato se:

```text
similarità ≥ 3/√D
```

Per `D=10000`, la soglia è `0,03`. La confidenza mostrata per il ramo è:

```text
z = similarità × √D
```

### 2.9 Uso dell'API ricorsiva nell'applicazione

`App` usa `recoverTriples` sia durante la scoperta del ruolo sorgente sia durante l'estrazione degli oggetti target. Non assume più che il ramo sia un unico chunk piatto.

## 3. Test aggiunti

`TopologicalVectorUpdaterTest` verifica:

1. determinismo di due ricostruzioni consecutive;
2. recupero di rami noti in un nodo con 100 predicati;
3. più livelli dei rami con capacità 5;
4. recupero diretto con 1, 5 e 29 predicati;
5. recupero degli oggetti 0, 28, 29 e 99 in un predicato con 100 oggetti;
6. albero dei valori a due livelli con capacità 30;
7. albero dei valori a quattro livelli con capacità 5;
8. capacità teorica pari a 89 a 3σ.

Ultima esecuzione:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
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

## 5. Profilo delle chiamate remote

Il percorso ordinario esegue circa nove richieste sequenziali:

| Fase | Chiamate |
|---|---:|
| risoluzione delle tre entità | 3 |
| espansione RDF di sorgente e target | 2 |
| etichetta del ruolo sorgente | 1 |
| etichette delle proprietà target | 1 batch |
| etichetta del ruolo target | 1 |
| etichette dei risultati finali | 1 batch |
| totale | 9 |

Le etichette delle proprietà e dei risultati sono già recuperate in batch: non viene eseguita una query per ciascuna proprietà. Rimane però una waterfall di chiamate sincrone. La latenza totale è la somma delle latenze di Wikidata e produce lo stesso problema pratico percepito di una struttura N+1.

Le modifiche al chunking non hanno introdotto nuovi accessi SPARQL. Hanno però introdotto una doppia traversata locale: `App` chiama `recoverBranch`, poi `recoverTriples`, che richiama internamente `recoverBranch`.

Maven esegue inoltre richieste ai metadati TestNG perché `pom.xml` usa la versione dinamica `RELEASE`. Nei log queste richieste raggiungono repository GitHub Packages e ricevono `401 Unauthorized`.

## 6. Bug e modifiche ancora necessarie

### P1 — Correttezza e regressioni

#### P1.1 Limitare il clean-up finale agli oggetti validi

`cleanUpRelativeTopK` confronta il vettore recuperato con tutta `atomicMemory`, che contiene anche soggetti, predicati, ruoli posizionali e URI interne. I candidati finali devono essere limitati agli oggetti effettivi del predicato target.

#### P1.2 Rendere atomico `recoverTriples`

Se una foglia non supera il clean-up, l'implementazione corrente la omette e restituisce le altre. Il chiamante non può distinguere un recupero completo da uno parziale. L'API deve restituire tutte le triple oppure un risultato esplicito con errori e cardinalità.

#### P1.3 Aggiungere il test combinato

Manca un caso che combini entrambe le dimensioni critiche:

```text
almeno 30 predicati
└── almeno un predicato con 100 oggetti
```

Il test deve verificare sia il percorso nel tree dei rami sia la discesa nel tree dei valori.

#### P1.4 Eliminare la doppia traversata del ramo

`App` recupera esplicitamente il ramo e subito dopo richiama `recoverTriples`, che lo recupera nuovamente. Serve un'unica API che restituisca ramo, confidenza e triple, oppure un overload che accetti il ramo già purificato.

### P2 — Prestazioni e robustezza

#### P2.1 Ridurre la waterfall Wikidata

Interventi proposti:

1. eseguire in parallelo le tre risoluzioni indipendenti;
2. eseguire in parallelo le due estrazioni RDF in modelli separati e unirli dopo il completamento;
3. usare una cache di etichette condivisa;
4. riutilizzare l'etichetta del ruolo target già presente nell'inventario;
5. raccogliere metriche di durata per ogni fase remota;
6. valutare una query combinata per le etichette dei ruoli.

#### P2.2 Rimuovere le richieste Maven ai metadati TestNG

TestNG e JUnit 4 non sono usati dai test correnti. Possono essere rimossi; in alternativa, TestNG deve avere una versione esplicita anziché `RELEASE`.

#### P2.3 Validare la CLI prima dell'ONNX

Il modello viene caricato prima di analizzare gli argomenti. `--help` inizializza inutilmente ONNX e un numero non valido per `--top` o `--candidates` produce `NumberFormatException`.

#### P2.4 Rimuovere chunk obsoleti

La ricostruzione elimina i percorsi correnti da `TopologicalVectorUpdater`, ma non elimina da `ItemMemory.chunkMemory` i chunk di livelli non più presenti. Aggiornamenti ripetuti possono far crescere la memoria e contaminare le API legacy di clean-up globale.

#### P2.5 Allineare runtime e configurazione Maven

Il progetto è Java 21. L'esecuzione con JDK 25 produce il warning ONNX relativo a `System.load`. Va usato JDK 21 oppure l'opzione:

```text
--enable-native-access=ALL-UNNAMED
```

Il compilatore Maven dovrebbe usare `maven.compiler.release=21` per impedire dipendenze involontarie da API Java successive.

### P3 — Chiarezza del codice

#### P3.1 Allineare nome e comportamento dell'estrattore

`extractBidirectional` estrae solamente archi outgoing e `GraphManager.expandNode` ignora `Direction`. Il nome, i commenti e l'API devono descrivere il comportamento reale oppure va implementata l'estrazione incoming.

#### P3.2 Semplificare `InvestigationEngine.processTriple`

Il metodo genera tre atomici ma conserva i risultati solo tramite l'effetto laterale di `getOrGenerate`. Le variabili locali non vengono utilizzate.

#### P3.3 Separare le responsabilità di `App`

`App` contiene CLI, query SPARQL, orchestrazione, filtri, ranking, label lookup e output. Dopo aver stabilizzato gli aspetti P1 e P2, queste responsabilità possono essere estratte in componenti verificabili separatamente.

## 7. Ordine di intervento proposto

1. test combinato fat node + fat predicate;
2. API unica per ramo, confidenza e triple;
3. dominio candidato limitato agli oggetti target;
4. recupero atomico, senza risultati parziali silenziosi;
5. rimozione della waterfall di rete e delle richieste Maven superflue;
6. validazione CLI e pulizia dei chunk obsoleti;
7. riallineamento dell'estrattore e refactoring di `App`.

## 8. Documentazione correlata

- `FLUSSO_DATI.md`: flusso completo dei dati e formule VSA.
- `README.md`: presentazione pubblica, setup, uso, esempi e limiti.
- `src/test/java/com/investigator/vsa/TopologicalVectorUpdaterTest.java`: regressioni automatiche del chunking.
