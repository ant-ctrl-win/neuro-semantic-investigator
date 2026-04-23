package com.investigator;

import com.investigator.vsa.*;
import com.investigator.vsa.strategy.SemanticEmbeddingStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.jena.*;
import com.investigator.engine.InvestigationEngine;
import com.investigator.automaton.*;
import org.apache.jena.rdf.model.*;

import java.util.*;

public class AppSemantic {
    public static void main(String[] args) {
        System.out.println("=== NEURO-SEMANTIC INVESTIGATOR: MISSION ANALOGY ===");
        System.out.println("=== MODE: NEURO-SYMBOLIC (LLM Embeddings + HDC) ===\n");

        System.out.println("[*] Inizializzazione modello LLM (all-MiniLM-L6-v2) in corso...");
        long startTime = System.currentTimeMillis();

        // IL CAMBIO DI PARADIGMA: Usiamo l'Embedding Strategy al posto di quella Random
        ItemMemory itemMemory = new ItemMemory(new SemanticEmbeddingStrategy());

        System.out.println("[*] Modello caricato in " + (System.currentTimeMillis() - startTime) + " ms.");

        SparqlEndpoint wikidataEndpoint = new SparqlEndpoint("https://query.wikidata.org/sparql");
        GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
        TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();

        String apollo11Uri = "http://www.wikidata.org/entity/Q43653";  // Apollo 11
        String triesteUri = "http://www.wikidata.org/entity/Q850157";  // Batiscafo Trieste

        Resource apolloResource = ModelFactory.createDefaultModel().createResource(apollo11Uri);
        Resource triesteResource = ModelFactory.createDefaultModel().createResource(triesteUri);

        InvestigationEngine engine = new InvestigationEngine(new HDVectorMapB(), graphManager, itemMemory, topologicalUpdater);

        System.out.println("\n[*] Ingestione Apollo 11...");
        engine.expandAndProcess(apolloResource);

        System.out.println("\n[*] Ingestione Batiscafo Trieste...");
        engine.expandAndProcess(triesteResource);

        Model localModel = engine.getGraphManager().getLocalModel();

        // 1. ESTRAZIONE E PULIZIA DEI RAMI
        System.out.println("\n[*] Estrazione e validazione Chunk...");
        List<HDVector> apolloBranches = extractCleanMacroBranches(itemMemory, apollo11Uri, localModel);
        List<HDVector> triesteBranches = extractCleanMacroBranches(itemMemory, triesteUri, localModel);

        System.out.printf("    -> Rami puliti estratti: Apollo (%d), Trieste (%d)\n", apolloBranches.size(), triesteBranches.size());

        // 2. AUTOMA CELLULARE E MAPPING
        System.out.println("\n[*] Inizializzazione Gayler Mapping e Rilassamento Automa...");
        CellularAutomaton ca = new CellularAutomaton(new HDVectorMapB(), engine, itemMemory);

        ca.initializeAnalogicalState(
                itemMemory.getTreeVector(apollo11Uri),
                itemMemory.getTreeVector(triesteUri),
                apolloBranches,
                triesteBranches
        );

        for (int i = 0; i < 50; i++) ca.step();
        HDVector x_t = ca.getState();

        System.out.println("\n=======================================================");
        System.out.println("   TRADUZIONE ANALOGICA: UNBINDING STEP-BY-STEP        ");
        System.out.println("=======================================================");

        HDVector apolloRoot = itemMemory.getTreeVector(apollo11Uri);

        // --- STEP 1: ESTRAZIONE RAMO SORGENTE ---
        System.out.println("[1] Troviamo l'URI corretta per l'equipaggio e estraiamo il ramo...");

        String actualCrewProp = null;
        for(Statement s : localModel.listStatements(apolloResource, null, (RDFNode)null).toList()) {
            String predUri = s.getPredicate().getURI();
            if (predUri.contains("P1029") || predUri.contains("P527")) {
                actualCrewProp = predUri;
                break;
            }
        }

        if (actualCrewProp == null) {
            System.out.println("   [CRITICO] Nessuna proprietà per l'equipaggio trovata nel grafo locale dell'Apollo 11.");
            return;
        }

        System.out.println("   -> URI Predicato trovata: " + actualCrewProp);
        HDVector roleCrew = itemMemory.getOrGenerate(actualCrewProp);
        HDVector noisyApolloCrew = apolloRoot.permute(-100).bind(roleCrew);

        HDVector cleanApolloCrew = itemMemory.cleanUpChunk(noisyApolloCrew);

        if (cleanApolloCrew == null) {
            System.out.println("   [ERRORE] Ramo perso nel rumore.");
            return;
        }
        System.out.println("   -> Ramo estratto e ripulito con successo! (Z-Score: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ)");

        // --- STEP 2: TRADUZIONE NEL DOMINIO TARGET ---
        System.out.println("\n[2] Traduco il ramo nel dominio Trieste usando lo stato dell'automa...");
        HDVector noisyTriesteCrew = x_t.bind(cleanApolloCrew);

        System.out.println("   [DIAGNOSTICA] Analizzo la proiezione verso i rami del Trieste...");
        double bestSim = -1.0;

        for (HDVector tBranch : triesteBranches) {
            double sim = noisyTriesteCrew.similarity(tBranch);
            if (sim > bestSim) {
                bestSim = sim;
            }
        }
        System.out.printf("   -> Il vettore tradotto punta al miglior candidato grezzo con similarità: %.4f\n", bestSim);

        HDVector cleanTriesteCrew = itemMemory.cleanUpChunk(noisyTriesteCrew);

        if (cleanTriesteCrew == null) {
            System.out.println("   [ERRORE] Il segnale è sotto la soglia (Z-Score: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ)");
            System.out.println("   [INDAGINE] Il Trieste non possiede un ramo semanticamente e strutturalmente affine a 'crew member'.");
            return;
        }

        System.out.println("   -> Ramo analogo semantico trovato e validato nel Trieste! (Z-Score: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ)");
        System.out.println("   -> Chiave del ramo target: " + itemMemory.getLastBestKey());

        // --- STEP 3: UNBINDING DELLA FOGLIA ---
        System.out.println("\n[3] Interrogo il ramo analogo per estrarre l'analogo di Armstrong...");
        // Disfiamo P(1) e shiftiamo di -2
        HDVector noisyAnalogueObject = cleanTriesteCrew.bind(roleCrew.permute(1)).permute(-2);

        // --- STEP 4: CLEAN-UP FINALE ---
        HDVector finalAnalogue = itemMemory.cleanUpRelative(noisyAnalogueObject);

        if (finalAnalogue != null) {
            System.out.println("\n[!] RISULTATO ANALOGICO TROVATO:");
            System.out.println("    Neil Armstrong (Apollo)  ===>  " + itemMemory.getLastBestKey() + " (Trieste)");
            System.out.println("    Confidenza Statistica:   " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
        } else {
            System.out.println("\n[?] Ramo trovato, ma nessun individuo chiaro all'interno (Z-Score: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ)");
        }
    }

    private static List<HDVector> extractCleanMacroBranches(ItemMemory memory, String entityUri, Model localGraph) {
        Resource entityRes = localGraph.getResource(entityUri);
        HDVector megaVector = memory.getTreeVector(entityUri);
        List<HDVector> cleanBranches = new ArrayList<>();

        Set<Property> actualProperties = new HashSet<>();
        localGraph.listStatements(entityRes, null, (RDFNode) null).forEachRemaining(s -> {
            if (!isMetadata(s.getPredicate().getURI())) actualProperties.add(s.getPredicate());
        });

        for (Property prop : actualProperties) {
            HDVector role = memory.getOrGenerate(prop.getURI());
            HDVector noisyBranchContent = megaVector.permute(-100).bind(role);
            HDVector cleanChunk = memory.cleanUpChunk(noisyBranchContent);

            if (cleanChunk != null) {
                cleanBranches.add(cleanChunk);
            }
        }
        return cleanBranches;
    }

    private static boolean isMetadata(String uri) {
        return uri.contains("P18") || uri.contains("P373") || uri.contains("P2002") || uri.contains("P2013");
    }
}