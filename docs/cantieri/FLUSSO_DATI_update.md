# FLUSSO_DATI — Allineamento post-P2.1b

## 1. Descrizione

Aggiornamento testuale di `FLUSSO_DATI.md`, il documento tecnico interno in
italiano che descrive il flusso dati del sistema. Nessuna modifica al codice.

Gli interventi allineano la documentazione allo stato del repository dopo i
cantieri P2.1a (parallelizzazione delle fasi remote) e P2.1b (riduzione delle
chiamate di etichetta e misura delle fasi VSA), più i fix precedenti P1.4
(overload di `recoverTriples`) e P2.1b (etichetta del ruolo sorgente nella
stessa query `VALUES` del target).

Sono state toccate solo le sezioni elencate dall'utente, con patch chirurgiche.
Le formule VSA delle sezioni §6, §7, §8, §9 e §10 non sono state modificate.

## 2. Git diff del commit

```diff
diff --git a/FLUSSO_DATI.md b/FLUSSO_DATI.md
index 6224387..9fdf95e 100644
--- a/FLUSSO_DATI.md
+++ b/FLUSSO_DATI.md
@@ -94,23 +94,29 @@ Se la similarità è inferiore, `App` cerca su Wikidata eventi o nodi collegati
 
 ## 5. Ingestione del grafo RDF
 
-`InvestigationEngine.expandAndProcess` viene chiamato per la sorgente e per il target.
+`InvestigationEngine.expandAndProcess` è stato diviso in due fasi: `fetchOutgoing`
+esegue il fetch HTTP puro, mentre `processExpanded` codifica in VSA il grafo già
+presente nel modello locale. `expandAndProcess` resta come composizione delle due.
 
 Il flusso effettivo è:
 
 ```text
 URI dell'entità
     ↓
-TripleExtractor.extractBidirectional(...)
+TripleExtractor.extractBidirectional(...)  [fetch HTTP, in parallelo per sorgente e target]
     ↓
 query CONSTRUCT 1-hop outgoing
     ↓
-GraphManager.localModel
+GraphManager.localModel.add(...)  [inserimento sequenziale, Jena Model non è thread-safe]
+    ↓
+InvestigationEngine.processExpanded(...)
     ↓
 generazione degli atomici e costruzione degli alberi VSA
 ```
 
-Nonostante il nome `extractBidirectional`, la query corrente acquisisce solamente triple in uscita:
+Il fetch RDF delle due entità è parallelo (P2.1a), ma l'inserimento nel
+`localModel` resta sequenziale per thread-safety di Jena. Il nome
+`extractBidirectional` resta fuorviante, perché estrae solo triple in uscita:
 
 ```text
 <entità> ?predicato ?oggetto
@@ -249,6 +255,15 @@ decode(C,k) = ρ⁻¹(C ⊗ (p₀ ⊗ ... ⊗ pₖ), k+1)
 
 `recoverBranch` percorre l'albero dei rami fino al predicato richiesto. `recoverTriple` e `recoverTriples` percorrono poi l'albero dei valori.
 
+`recoverTriples` dispone di un overload che accetta il ramo già purificato:
+
+```java
+recoverTriples(memory, entityUri, predicateUri, precomputedBranch)
+```
+
+L'overload evita di ripercorrere `recoverBranch` due volte (fix P1.4). Il metodo
+a 3 argomenti resta come fallback e continua a calcolare internamente il ramo.
+
 A ogni passaggio:
 
 1. viene decodificata la posizione del figlio;
@@ -291,6 +306,10 @@ Nella demo il ruolo sorgente risultante è `author`.
 
 Il sistema raccoglie le proprietà del target e mantiene quelle che puntano a entità Wikidata. Le etichette vengono recuperate in batch.
 
+L'etichetta del ruolo sorgente viaggia nella stessa query `VALUES` delle
+etichette delle proprietà del target (fix P2.1b). Viene così risparmiata una
+chiamata remota dedicata.
+
 `OntologyTranslator` codifica:
 
 - l'etichetta del ruolo sorgente;
@@ -309,7 +328,7 @@ Questa fase usa il modello ONNX. La VSA continua invece a usare vettori bipolari
 Una volta noto il ruolo target:
 
 1. `recoverBranch` recupera il ramo puro del target;
-2. `recoverTriples` attraversa l'intero albero dei valori;
+2. `recoverTriples` viene chiamato con l'overload che riusa `pureTargetBranch` già recuperato al passo 1 (P1.4);
 3. soggetto e predicato vengono svincolati da ogni tripla;
 4. `ItemMemory.cleanUpRelativeTopK` confronta il risultato con la memoria atomica;
 5. per ogni URI viene mantenuto il punteggio migliore;
@@ -334,9 +353,16 @@ Il risultato della demo corrente è:
 | traduzione ruolo | soglia `0,60` | nessun ruolo target accettato |
 | ramo VSA | soglia strutturale 3σ | ramo rifiutato |
 | clean-up finale | memoria atomica e soglia dinamica | nessun candidato finale |
+| disponibilità Wikidata | endpoint pubblico | risposte 5xx transitorie, rate-limiting per IP |
 
 Il progetto non persiste la memoria tra esecuzioni. Grafo, vettori atomici, chunk, percorsi e radici vengono ricostruiti a ogni avvio.
 
+L'endpoint pubblico di Wikidata è soggetto a risposte `5xx` transitorie
+(es. `502 Bad Gateway`) e a rate-limiting per IP. Sotto carico, una
+singola esecuzione può richiedere decine di secondi o fallire la
+risoluzione delle etichette. Il sistema non maschera questi errori:
+i risultati possono variare tra esecuzioni e condizioni di rete.
+
 ## 16. Sequenza completa della demo
 
 ```mermaid
@@ -349,11 +375,18 @@ sequenceDiagram
     participant V as Memoria VSA
 
     U->>A: Amleto : Shakespeare = Lacrimosa : ?
-    A->>W: risoluzione delle tre entità
+    par 3 resolve in parallelo
+        A->>W: resolve(Amleto)
+        A->>W: resolve(Shakespeare)
+        A->>W: resolve(Lacrimosa)
+    end
     W-->>A: URI, classi, sitelink
     A->>E: confronto classi e reranking
     E-->>A: target Requiem
-    A->>W: triple outgoing di Amleto e Requiem
+    par 2 fetch RDF in parallelo
+        A->>W: CONSTRUCT outgoing Amleto
+        A->>W: CONSTRUCT outgoing Requiem
+    end
     W-->>G: modelli RDF 1-hop
     G->>V: atomici, triple, chunk e alberi
     A->>V: cerca William Shakespeare nei rami di Amleto
@@ -365,3 +398,19 @@ sequenceDiagram
     A->>W: etichette dei candidati finali
     W-->>U: Süssmayr, Mozart
 ```
+
+## 17. Profilo prestazioni
+
+Il sistema separa il calcolo strutturale (VSA) dalla rete:
+
+- **VSA** (STEP 1 + STEP 3&4): ~40 ms totali. La deduzione del ruolo
+  sorgente e l'estrazione del target sono operazioni in memoria su
+  vettori bipolari, non dominano il tempo di esecuzione.
+- **Rete**: 7 richieste Wikidata, near-minimal. Il minimo teorico è 6;
+  la settima è il batch di etichette dei candidati finali, non
+  fondibile perché i candidati esistono solo dopo la proiezione VSA.
+- **Demo end-to-end**: tipicamente 7–15 s su rete domestica, con picchi
+  fino a 50 s o più quando Wikidata è lenta o applica rate-limiting.
+  Il tempo totale è dominato dalla latenza del provider.
+
+Le metriche di fase sono stampate a fine esecuzione dalla CLI.
```

## 3. Sezioni toccate

| Sezione | Tipo intervento | Contenuto |
|---|---|---|
| §5 Ingestione del grafo RDF | riscrittura flusso + nota | `fetchOutgoing` / `processExpanded`, inserimento sequenziale nel `localModel`, fetch RDF parallelo (P2.1a) |
| §11 Decodifica ricorsiva e clean-up | nota | overload `recoverTriples(..., precomputedBranch)`, fallback a 3 argomenti (P1.4) |
| §13 Traduzione del ruolo tra domini | nota | etichetta ruolo sorgente nella stessa query `VALUES` del target (P2.1b) |
| §14 Recupero e ranking dell'oggetto finale | aggiornamento passo 2 | riuso di `pureTargetBranch` tramite overload (P1.4) |
| §15 Dipendenze esterne e punti di errore | riga tabella + paragrafo | disponibilità Wikidata: 5xx transitori e rate-limiting per IP |
| §16 Sequenza completa della demo | diagramma Mermaid | blocchi `par` per 3 resolve parallele e 2 fetch RDF paralleli |
| §17 Profilo prestazioni | nuova sezione | VSA ~40 ms, 7 richieste near-minimal, demo 7–15 s con picchi |

## 4. Ambiguità o disallineamenti

- Nessun disallineamento di numerazione: §5, §11, §13, §14, §15 e §16
  corrispondono al contenuto atteso prima delle modifiche.
- §17 era libera: il documento terminava alla §16, senza appendici.
- Le sezioni non elencate non sono state riscritte; in particolare le formule
  VSA di §6, §7, §8, §9 e §10 sono rimaste invariate.
- Il blocco `par` di Mermaid è supportato dalla sintassi `sequenceDiagram`;
  i due gruppi (`par 3 resolve in parallelo` / `end`, `par 2 fetch RDF in
  parallelo` / `end`) sono correttamente bilanciati e non annidati.

## 5. Verifica di coerenza numerica

- Chiamate remote: **7** richieste Wikidata (§16 implicito, §17 esplicito).
- Costo VSA: **~40 ms** totali (§17).
- Tempo demo: **7–15 s** tipico, picchi fino a 50 s (§17).
- Coerenza con §14: ranking demo invariato (`Süssmayr σ = 30,86`,
  `Mozart σ = 30,85`).
