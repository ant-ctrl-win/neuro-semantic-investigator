package com.investigator;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;
import com.investigator.vsa.ItemMemory;
import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.jena.SparqlEndpoint;
import com.investigator.jena.TripleExtractor;
import com.investigator.jena.GraphManager;
import com.investigator.engine.InvestigationEngine;
import com.investigator.automaton.CellularAutomaton;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ModelFactory;
import java.util.List;

public class App {

    // Definiamo l'URI interno per il vettore posizionale dell'albero
    public static final String TREE_POSITION_URI = "vsa:internal:tree_position";

    public static void main(String[] args) {
        System.out.println("=== AVVIO NEURO-SEMANTIC INVESTIGATOR (Fase 2: Simpkin Tree) ===\n");

        ItemMemory itemMemory = new ItemMemory(new RandomGenerationStrategy());

        // 1. INIZIALIZZAZIONE VETTORI CHIAVE E STRUTTURALI
        String p17Uri = "http://www.wikidata.org/prop/direct/P17";
        String q183Uri = "http://www.wikidata.org/entity/Q183";

        HDVector vP17 = itemMemory.getOrGenerate(p17Uri);
        HDVector vQ183 = itemMemory.getOrGenerate(q183Uri);

        // Generiamo e fissiamo il vettore strutturale dell'albero
        HDVector vTree = itemMemory.getOrGenerate(TREE_POSITION_URI);

        HDVector contextTarget = vP17.permute(1).bind(vQ183.permute(2));
        System.out.println("[*] Target Semantico: [Qualcosa] -> P17 -> Q183 (Germania)");

        // 2. SETUP MOTORE
        SparqlEndpoint wikidataEndpoint = new SparqlEndpoint("https://query.wikidata.org/sparql");
        GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
        TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();

        InvestigationEngine engine = new InvestigationEngine(contextTarget, graphManager, itemMemory, topologicalUpdater);

        // =====================================================================
        // 3. TARGET TEST: BERLINO (Q64)
        // =====================================================================
        String targetQ = "http://www.wikidata.org/entity/Q64";
        Resource targetEntity = ModelFactory.createDefaultModel().createResource(targetQ);

        System.out.println("\n[*] Analisi Topologica e Costruzione Albero per: " + targetQ);
        engine.expandAndProcess(targetEntity);

        System.out.println("\n=== DECRIPTAZIONE VETTORIALE (UNBINDING ALBERO) SU BERLINO ===");

//        HDVector megaVettoreBerlino = itemMemory.getOrGenerate(targetQ);
        HDVector megaVettoreBerlino = itemMemory.getTreeVector(targetQ);
        double bestBerlinSim = -1.0;
        int winningBerlinBranch = -1;
        double winningBerlinSigma = 0.0;
        String winningBerlinKey = null;

        int maxBranches = 8; // Assumiamo un massimo di 8 rami

        System.out.println("   [*] Navigazione dell'albero vettoriale di Berlino con sonda P17");

        for (int i = 0; i < maxBranches; i++) {
            // STEP 1: Svincoliamo il ramo i-esimo dall'albero radice
            HDVector extractedChunk = megaVettoreBerlino.bind(vTree.permute(i + 1));

            // STEP 2: Svincoliamo il target relazionale (Oggetto) dal ramo
            HDVector extractedTarget = extractedChunk.bind(vP17.permute(1)).permute(-2);

            // Clean-up statistico
            HDVector cleaned = itemMemory.cleanUpRelative(extractedTarget);
            double sim = cleaned.similarity(vQ183);

            // Stampiamo i rami per vedere cosa contiene davvero Berlino
            if (itemMemory.getLastBestSigma() > 1.0) {
                String bestMatchLabel = itemMemory.getLastBestKey() != null
                        ? itemMemory.getLastBestKey().substring(itemMemory.getLastBestKey().lastIndexOf("/") + 1)
                        : "N/A";
                System.out.printf("   [*] Ramo %d - Z-Score: %.2f σ (Best match grezzo: %s)\n",
                        i, itemMemory.getLastBestSigma(), bestMatchLabel);
            }

            if (sim > bestBerlinSim) {
                bestBerlinSim = sim;
                winningBerlinBranch = i;
                winningBerlinSigma = itemMemory.getLastBestSigma();
                winningBerlinKey = itemMemory.getLastBestKey();
            }
        }

        System.out.printf("\n   [*] Risultato Finale su Berlino:\n");
        if (winningBerlinSigma > 5.0 && winningBerlinKey != null && winningBerlinKey.contains("Q183")) {
            System.out.printf("   [SUCCESS] Germania (Q183) estratta dal Ramo %d come OUTLIER (%.2f σ)!\n", winningBerlinBranch, winningBerlinSigma);
            System.out.printf("   [SUCCESS] Similarità finale con il vettore puro della Germania: %.4f\n", bestBerlinSim);
        } else {
            System.out.println("   [FAILURE] Segnale non trovato. È probabile che P17 non sia stata estratta dalla query SPARQL (LIMIT 100).");
        }

        // =====================================================================
        // 4. TEST ANALOGIA: PARIGI (Q90)
        // =====================================================================
        System.out.println("\n=== TEST ANALOGIA RELAZIONALE ===");
        System.out.println("   Pattern: [Qualcosa] --P17--> Q183 (Germania)");
        System.out.println("   Atteso: Parigi --P17--> Q142 (Francia)");

        String q90Uri = "http://www.wikidata.org/entity/Q90";  // Paris
        String q142Uri = "http://www.wikidata.org/entity/Q142"; // France

        Resource parisEntity = ModelFactory.createDefaultModel().createResource(q90Uri);
        System.out.println("\n[*] Analisi Topologica e Costruzione Albero per: " + q90Uri);
        engine.expandAndProcess(parisEntity);

        HDVector vFrancia = itemMemory.getOrGenerate(q142Uri);
//        HDVector megaVettoreParigi = itemMemory.getOrGenerate(q90Uri);
        HDVector megaVettoreParigi = itemMemory.getTreeVector(q90Uri);

        System.out.println("   [*] Navigazione dell'albero vettoriale di Parigi con sonda P17");

        double bestChunkSim = -1.0;
        int winningBranch = -1;
        double winningSigma = 0.0;
        String winningKey = null;

        for (int i = 0; i < maxBranches; i++) {
            // STEP 1: Isoliamo il ramo (Chunk)
            HDVector extractedChunk = megaVettoreParigi.bind(vTree.permute(i + 1));

            // STEP 2: Isoliamo l'entità target dal ramo
            HDVector resultRaw = extractedChunk.bind(vP17.permute(1)).permute(-2);

            // STEP 3: Filtraggio Z-Score
            HDVector analogyCleaned = itemMemory.cleanUpRelative(resultRaw);
            double analogySim = analogyCleaned.similarity(vFrancia);

            // Stampiamo solo i rami che mostrano un minimo segno di reazione (> 1.0 sigma)
            if (itemMemory.getLastBestSigma() > 1.0) {
                String bestMatchLabel = itemMemory.getLastBestKey() != null
                        ? itemMemory.getLastBestKey().substring(itemMemory.getLastBestKey().lastIndexOf("/") + 1)
                        : "N/A";
                System.out.printf("   [*] Ramo %d - Z-Score: %.2f σ (Best match grezzo: %s)\n",
                        i, itemMemory.getLastBestSigma(), bestMatchLabel);
            }

            if (analogySim > bestChunkSim) {
                bestChunkSim = analogySim;
                winningBranch = i;
                winningSigma = itemMemory.getLastBestSigma();
                winningKey = itemMemory.getLastBestKey();
            }
        }

        System.out.printf("\n   [*] Risultato Finale sull'Analogia:\n");
        if (winningSigma > 5.0 && winningKey != null && winningKey.contains("Q142")) {
            System.out.printf("   [SUCCESS] Francia (Q142) estratta dal Ramo %d come OUTLIER (%.2f σ)!\n", winningBranch, winningSigma);
            System.out.printf("   [SUCCESS] Similarità finale con il vettore puro della Francia: %.4f\n", bestChunkSim);
        } else {
            System.out.println("   [FAILURE] Nessun ramo ha superato la soglia di significatività statistica.");
        }


//        // =====================================================================
//        // 5. DUAL-GRAPH ANALOGICAL MAPPING (Fase 2: Simpkin Branches)
//        // =====================================================================
//        System.out.println("\n=== DUAL-GRAPH ANALOGICAL MAPPING ===");
//        System.out.println("   Source: Berlino (Q64)");
//        System.out.println("   Target: Parigi (Q90)");
//
//        System.out.println("[*] Recupero degli Alberi Vettoriali e scomposizione in rami...");
//
//        // Prepariamo le liste per contenere i rami svincolati
//        List<HDVector> sourceBranches = new java.util.ArrayList<>();
//        List<HDVector> targetBranches = new java.util.ArrayList<>();
//
//        // Estraiamo i rami da Berlino (Source)
//        for (int i = 0; i < maxBranches; i++) {
//            HDVector branch = megaVettoreBerlino.bind(vTree.permute(i + 1));
//            // Filtriamo il puro rumore: se il ramo non somiglia nemmeno a se stesso bundlato, è vuoto
//            if (branch.similarity(branch) > 0.9) {
//                sourceBranches.add(branch);
//            }
//        }
//
//        // Estraiamo i rami da Parigi (Target)
//        for (int i = 0; i < maxBranches; i++) {
//            HDVector branch = megaVettoreParigi.bind(vTree.permute(i + 1));
//            if (branch.similarity(branch) > 0.9) {
//                targetBranches.add(branch);
//            }
//        }
//
//        System.out.printf("   [*] Source branches (Berlino): %d rami isolati.\n", sourceBranches.size());
//        System.out.printf("   [*] Target branches (Parigi): %d rami isolati.\n", targetBranches.size());
//
//        // Inizializziamo l'automa passandogli direttamente l'investigationEngine di Parigi (o dummy)
//        HDVector dummyInitial = new HDVectorMapB();
//        CellularAutomaton analogicalCA = new CellularAutomaton(dummyInitial, engine, itemMemory);
//
//        System.out.println("[*] Inizializzazione stato analogico (Gayler + Simpkin)...");
//        // Passiamo le Radici e i Rami per una costruzione matematica senza collasso
//        analogicalCA.initializeAnalogicalState(megaVettoreBerlino, megaVettoreParigi, sourceBranches, targetBranches);
//
//        System.out.printf("   [*] Matrice W calcolata con %d incroci macroscopici (zero rumore di fondo).\n", analogicalCA.getAssociationMatrix().getCardinality());
//
////        System.out.println("[*] Esecuzione automa cellulare...");
////        for (int i = 0; i < 3; i++) {
////            analogicalCA.step(); // Chiamiamo lo step matematico puro (l'intersezione Sigma-Pi)
////
////            // Verifichiamo se lo stato emergente x_t si sta avvicinando al nostro target causale noto
////            double resonance = analogicalCA.getState().similarity(contextTarget);
////            System.out.printf("   [*] Step %d completato. Risonanza dello stato x_t con [Qualcosa]->P17->Q183: %.4f\n", i + 1, resonance);
////        }
////
////        System.out.println("\n   [INFO] Mappatura analogica eseguita e completata.");
//
//        System.out.println("[*] Esecuzione automa cellulare e Test Analogico...");
//        for (int i = 0; i < 3; i++) {
//            analogicalCA.step();
//
//            // IL VERO TEST: Interroghiamo lo stato analogico x_t
//            // Chiediamo: "Secondo la mappa strutturale corrente, chi è l'analogo della Germania (Q183)?"
//            HDVector analogOfGermany = analogicalCA.getState().bind(vQ183);
//
//            // Ripuliamo il risultato per trovare a quale concetto atomico corrisponde
//            HDVector cleanedAnalog = itemMemory.cleanUpRelative(analogOfGermany);
//            double simToFrance = cleanedAnalog.similarity(vFrancia);
//
//            String bestMatchLabel = itemMemory.getLastBestKey() != null
//                    ? itemMemory.getLastBestKey().substring(itemMemory.getLastBestKey().lastIndexOf("/") + 1)
//                    : "N/A";
//
//            System.out.printf("   [*] Step %d completato.\n", i + 1);
//            System.out.printf("       - Equivalente trovato: %s (Z-Score: %.2f σ)\n", bestMatchLabel, itemMemory.getLastBestSigma());
//            System.out.printf("       - Similarità vettoriale con la Francia (Q142): %.4f\n", simToFrance);
//        }
//
//        System.out.println("\n   [INFO] Mappatura analogica eseguita e completata.");

        // =====================================================================
        // 5. HOLOGRAPHIC ANALOGICAL TRANSLATION (Simpkin + VSA)
        // =====================================================================
        System.out.println("\n=== HOLOGRAPHIC ANALOGICAL TRANSLATION ===");
        System.out.println("   Source: Berlino (Q64) | Target: Parigi (Q90)");

        System.out.println("[*] Estrazione dei Rami (Simpkin Branches)...");
        List<HDVector> sourceBranches = new java.util.ArrayList<>();
        List<HDVector> targetBranches = new java.util.ArrayList<>();

        // Estraiamo gli 8 rami dai Mega Vettori
        for (int i = 0; i < 8; i++) {
            sourceBranches.add(megaVettoreBerlino.bind(vTree.permute(i + 1)));
            targetBranches.add(megaVettoreParigi.bind(vTree.permute(i + 1)));
        }

        // Costruiamo la Matrice di Traduzione W
        System.out.println("[*] Costruzione della Matrice di Traduzione Olografica W...");
        com.investigator.automaton.AssociationMatrix wMatrix = new com.investigator.automaton.AssociationMatrix(itemMemory);
        wMatrix.computeAnalogicalW(sourceBranches, targetBranches);
        HDVector W = wMatrix.getW();

        System.out.println("[*] Esecuzione della Traduzione Analogica...");

        // 1. Prendiamo il ramo di Berlino che sappiamo contenere la Germania (Ramo 1 dal log)
        int ramoTargetIndex = 1;
        HDVector ramoBerlinoConGermania = sourceBranches.get(ramoTargetIndex);

        // 2. LA MAGIA VSA: Traduciamo il ramo di Berlino nel dominio di Parigi!
        System.out.println("   [*] Applico W al Ramo Source (Translating...)");
        HDVector ramiParigiTradotti = W.bind(ramoBerlinoConGermania);

        // 3. Interroghiamo il risultato della traduzione con la sonda P17
        System.out.println("   [*] Interrogo il risultato della traduzione con la sonda P17...");
        HDVector traduzioneGrezza = ramiParigiTradotti.bind(vP17.permute(1)).permute(-2);

        // 4. Clean-up statistico
        HDVector risultatoFinale = itemMemory.cleanUpRelative(traduzioneGrezza);
        double analogSim = risultatoFinale.similarity(vFrancia);

        String analogMatch = itemMemory.getLastBestKey() != null
                ? itemMemory.getLastBestKey().substring(itemMemory.getLastBestKey().lastIndexOf("/") + 1)
                : "N/A";

        System.out.printf("\n   [*] Risultato della Traduzione Analogica:\n");
        if (itemMemory.getLastBestSigma() > 5.0 && analogMatch.equals("Q142")) {
            System.out.printf("   [SUCCESS] L'analogo trovato è la Francia (Q142) con %.2f σ!\n", itemMemory.getLastBestSigma());
            System.out.printf("   [SUCCESS] Similarità vettoriale: %.4f\n", analogSim);
        } else {
            System.out.printf("   [FAILURE] Traduzione fallita. Match grezzo: %s (%.2f σ)\n", analogMatch, itemMemory.getLastBestSigma());
        }

    }
}