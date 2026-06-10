# Scaletta Presentazione — Neuro-Semantic Investigator

---

## 1. L'OBIETTIVO E LA PREPARAZIONE: Risolvere Analogie con Reranking Semantico

### Il Rigore Teorico (Cosa spiegare)
> «Il nostro obiettivo è risolvere un'analogia strutturale $A : B = C : ?$ su un knowledge graph open-world. Prima di calcolare l'analogia nello spazio vettoriale, dobbiamo mappare in modo inequivocabile i termini $A$, $B$ e $C$ sui nodi del grafo (Entity Resolution).
>
> Per mitigare il "Popularity Bias" (es. "Lacrimosa" intesa come canzone o come fiume), non ci affidiamo al PageRank. Utilizziamo una proiezione in uno spazio semantico continuo a $768$ dimensioni (modello densamente addestrato). La disambiguazione avviene massimizzando un punteggio combinato $S$, calcolato tramite la Cosine Similarity tra gli embedding delle classi ontologiche:
>
> $$S = 0.60 \cdot \cos(E_{sourceClass}, E_{targetClass}) + 0.40 \cdot \text{Norm}(Sitelinks)$$
>
> Questo garantisce che l'entità target scelta appartenga allo stesso dominio concettuale della sorgente.»

### L'Aggancio al Codice (Cosa mostrare a livello logico)
* **Dove siamo:** Inizio del flusso nella classe `AppDis`.
* **L'Astrazione:** Mostra come l'interazione con Wikidata per scaricare le entità venga delegata, e come il modulo `OntologyTranslator` si occupi della matematica del reranking (la formula della similarità coseno). Solo dopo questa fase, il sistema genera il grafo RDF in memoria.

---

## 2. IL CORE VSA: Lo Spazio Iperdimensionale

### Il Rigore Teorico (Cosa spiegare)
> «La computazione vera e propria avviene in uno spazio iperdimensionale discreto $H = \{-1, +1\}^D$, dove $D=10000$. Non esiste un database: per mappare un nodo RDF in questo spazio utilizziamo una funzione di hash crittografica $H(URI)$ che fa da seed per un PRNG, garantendo una biiezione deterministica.
>
> L'algebra si fonda su tre operatori chiusi rispetto ad $H$:
> 1. **Bind ($\otimes$):** Prodotto di Hadamard. È l'operatore di associazione. Essendo su dominio bipolare, è un'involuzione auto-inversa: $\mathbf{a} \otimes \mathbf{b} \otimes \mathbf{b} = \mathbf{a}$.
> 2. **Bundle ($\oplus$):** Sovrapposizione di concetti. Si basa sulla somma vettoriale seguita dalla funzione signum (majority vote).
> 3. **Permute ($\rho^n$):** Shift circolare. Protegge la sintassi distruggendo temporaneamente la similarità. È invertibile: $\rho^{-n}(\rho^n(\mathbf{a})) = \mathbf{a}$.
>
> La metrica di distanza è il prodotto scalare normalizzato: $sim(\mathbf{a}, \mathbf{b}) = \frac{1}{D} \sum_{i=1}^D a_i b_i$.»

### L'Aggancio al Codice (Cosa mostrare a livello logico)
* **Dove siamo:** Sotto il cofano del sistema (`HDVectorMapB`).
* **L'Astrazione:** Spiega che l'infrastruttura Java implementa queste operazioni non con librerie di machine learning pesanti, ma con algoritmi primitivi sui `byte[]`. È algebra pura eseguita bare-metal, il che spiega la velocità di esecuzione (nessuna backpropagation).

---

## 3. L'INNOVAZIONE ARCHITETTURALE: Scalabilità tramite Simpkin Tree

### Il Rigore Teorico (Cosa spiegare)
> «Il limite teorico del Bundle ($\oplus$) è il collasso del rapporto segnale/rumore all'aumentare dei vettori sovrapposti (Capacity Limit). Per vettorizzare l'intero intorno di un'entità enciclopedica senza distruggere l'informazione, applichiamo l'approccio di Hierarchical Vector Chunking (Simpkin et al., 2018).
>
> Invece di un vettore piatto, costruiamo un albero gerarchico.
> - La tripla di base è protetta sintatticamente: $\mathbf{E} = \mathbf{v}_S \otimes \rho^1(\mathbf{v}_P) \otimes \rho^2(\mathbf{v}_O)$.
> - I blocchi di triple (Chunk) sono limitati a un $N_{max} = 30$: $\mathbf{C}_P = \bigoplus_{i=1}^{30} \mathbf{E}_i \oplus \mathbf{v}_{stop}$.
> - Il ramo completo è la codifica del chunk legato alla sua proprietà: $\mathbf{R}_P = \mathbf{C}_P \otimes \mathbf{v}_P \otimes \rho^{100}$.
> - other
> L'entità finale è il Bundle dei rami. Questo partizionamento confina l'interferenza costruttiva/distruttiva all'interno di blocchi limitati.»

### L'Aggancio al Codice (Cosa mostrare a livello logico)
* **Dove siamo:** Modulo `TopologicalVectorUpdater` e `ItemMemory`.
* **L'Astrazione:** Mostra come l'albero si traduca nel codice in tre livelli di memoria separati: Atomi (foglie pure), Chunk (nodi intermedi), e Root (l'entità). Avere tre "livelli di cache" separati è il trucco software che ci permetterà di eliminare il rumore durante l'inferenza.

---

## 4. IL MOTORE DI INFERENZA: Unbinding e Z-Score (STEP 1)

### Il Rigore Teorico (Cosa spiegare)
> «L'inferenza è un processo di "unbinding" algebrico. Per dedurre il ruolo $P$ che lega $A$ a $B$, moltiplichiamo la radice $\mathbf{Root}_A$ per l'inverso della proprietà: $\mathbf{Root}_A \otimes \rho^{-100}(\mathbf{v}_P) \approx \mathbf{C}_P + \text{rumore}$.
>
> Il risultato è un vettore degradato. Per purificarlo, non usiamo una soglia di similarità statica (sarebbe arbitraria), ma un test statistico rigoroso. Calcoliamo la similarità del vettore degradato contro l'intera memoria dei chunk, estraendo la media $\mu$ e la deviazione standard $\sigma$ del rumore di fondo.
> Valutiamo il candidato migliore calcolando lo Z-Score:
>
> $$Z = \frac{sim_{best} - \mu}{\sigma}$$
>
> Se $Z \geq \log_{10}(D) = 4.0$, l'ipotesi è statisticamente significativa ($p \approx 3.1 \times 10^{-5}$). Ripetiamo l'unbinding da questo chunk puro per estrarre l'oggetto e verificare se coincide con $B$.»

### L'Aggancio al Codice (Cosa mostrare a livello logico)
* **Dove siamo:** Classe `AppDis`, Sezione "Step 1".
* **L'Astrazione:** Fai notare semplicemente che c'è un ciclo che testa le ipotesi. Il lavoro pesante di purificazione è delegato a `ItemMemory.cleanUpChunk`, che esegue esattamente la formula dello Z-Score proteggendo il main dalla logica statistica.

---

## 5. IL PONTE SEMANTICO: Fondere Spazio Sintattico e Continuo (STEP 2)

### Il Rigore Teorico (Cosa spiegare)
> «Il VSA opera in uno spazio altamente ortogonale e sintattico. Se il dominio target usa ontologie diverse (es. "author" in letteratura vs "composer" in musica), il VSA fallirebbe perché i vettori generati dai due URI sarebbero ortogonali per design.
>
> Integriamo quindi un approccio neuro-simbolico. Filtriamo la topologia del target tenendo solo gli archi relazionali compatibili strutturalmente. Successivamente, proiettiamo i concetti (le label delle proprietà) fuori dallo spazio VSA, immergendoli di nuovo nello spazio continuo a 768 dimensioni.
>
> Cerchiamo la transizione ottimale massimizzando la similarità tra il ruolo sorgente e le proprietà target: $\arg\max_Q \cos(E_P, E_Q)$. Una volta trovata la traduzione ("composer"), generiamo il suo vettore deterministico per rientrare nello spazio VSA.»

### L'Aggancio al Codice (Cosa mostrare a livello logico)
* **Dove siamo:** Classe `AppDis`, Sezione "Step 2".
* **L'Astrazione:** Mostra come l'API di Jena venga usata per scremare le foglie inutili (il Filtro Strutturale). Dopodiché, il flusso passa nuovamente a `OntologyTranslator` che esegue il "ponte" restituendo la nuova URI da esplorare.

---

## 6. PROIEZIONE OLOGRAFICA ED ESTRAZIONE (STEP 3 & 4)

### Il Rigore Teorico (Cosa spiegare)
> «A questo punto, possediamo la "chiave" algebrica tradotta per il dominio di arrivo. Applichiamo la stessa identica catena di operazioni inverse usate nello Step 1, ma sull'albero dell'entità $C$ (es. Lacrimosa).
>
> 1. Svincoliamo il ramo bersaglio con la chiave tradotta.
> 2. Passiamo il segnale degradato alla Clean-up Memory dei chunk.
> 3. Svincoliamo l'oggetto finale.
> 4. Eseguiamo un ultimo Z-Score sulla memoria atomica per recuperare l'identità pura del vettore incognito $D$.
>
> L'algebra ha risolto la proporzione.»

### L'Aggancio al Codice (Cosa mostrare a livello logico)
* **Dove siamo:** Classe `AppDis`, Sezione finale (Step 3 & 4).
* **L'Astrazione:** Evidenzia l'output stampato a terminale: la chiarezza del log, lo Z-Score confermato (es. 20.37 $\sigma$) e la risoluzione finale dell'analogia. Il codice qui fa semplicemente da collante per le deduzioni algoritmiche spiegate.