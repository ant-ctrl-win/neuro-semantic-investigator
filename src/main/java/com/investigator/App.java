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
import com.investigator.automaton.GaylerIntersection;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ModelFactory;

public class App {

    public static void main(String[] args) {
        System.out.println("=== AVVIO NEURO-SEMANTIC INVESTIGATOR ===\n");

        ItemMemory itemMemory = new ItemMemory(new RandomGenerationStrategy());

        // 1. DEFINIAMO IL TARGET: Cerchiamo la Germania (Q183) tramite la proprietà Paese (P17)
        String p17Uri = "http://www.wikidata.org/prop/direct/P17";
        String q183Uri = "http://www.wikidata.org/entity/Q183";

        HDVector vP17 = itemMemory.getOrGenerate(p17Uri);
        HDVector vQ183 = itemMemory.getOrGenerate(q183Uri);

        // Identikit: P(1) ⊗ O(2)
        HDVector contextTarget = vP17.permute(1).bind(vQ183.permute(2));

        System.out.println("[*] Target Semantico: [Qualcosa] -> P17 -> Q183 (Germania)");

        // 2. Setup Motore
        SparqlEndpoint wikidataEndpoint = new SparqlEndpoint("https://query.wikidata.org/sparql");
        GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
        TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();

        InvestigationEngine engine = new InvestigationEngine(contextTarget, graphManager, itemMemory, topologicalUpdater);

        // 3. TARGET TEST: BERLINO (Q64) invece del Muro o delle Piombatoie
        // Q64 è un'entità "ricca" che DEVE avere P17 -> Q183
        String targetQ = "http://www.wikidata.org/entity/Q64";
        Resource targetEntity = ModelFactory.createDefaultModel().createResource(targetQ);

        System.out.println("[*] Analisi Topologica di: " + targetQ);
        engine.expandAndProcess(targetEntity);

        // === DUMP TRIPLE SCARICATE PER Q64 ===
        System.out.println("\n=== DUMP TRIPLE SCARICATE PER " + targetQ + " ===");
        graphManager.getLocalModel().listStatements(targetEntity, null, (org.apache.jena.rdf.model.RDFNode)null)
            .forEachRemaining(stmt -> {
                System.out.println("   P: " + stmt.getPredicate().getURI() + " -> O: " + stmt.getObject());
            });

        // 4. VERIFICA MATEMATICA: UNBINDING SUL VETTORE DI BERLINO
        System.out.println("\n=== DECRIPTazione VETTORIALE (UNBINDING) ===");
        HDVector vBerlino = itemMemory.getOrGenerate(targetQ);

        // Proviamo a estrarre l'oggetto usando il predicato: (V_mosaico ⊗ ρ1(P)) ⊗ ρ-2
        // Se la topologia ha funzionato, 'extracted' deve essere simile a vQ183 (Germania)
        HDVector extracted = vBerlino.bind(vP17.permute(1)).permute(-2);
        HDVector cleaned = itemMemory.cleanUpRelative(extracted);
        double discoverySim = cleaned.similarity(vQ183);

        System.out.printf("   - Similarità (Berlino vs Germania) BASE: %.4f\n", vBerlino.similarity(vQ183));
        System.out.printf("   - RISONANZA ESTRATTA (Pattern P17->Q183, dopo Clean-up): %.4f\n", discoverySim);

        if (discoverySim > 0.1) {
            System.out.println("   [SUCCESS] Il segnale topologico è stato trovato!");
        } else {
            System.out.println("   [FAILURE] Il segnale è ancora assente. Verificare URIs o Bundling.");
        }

        // 5. CLASSIFICA FINALE
        System.out.println("\n=== CLASSIFICA RISONANZA TOPOLOGICA ===");
        itemMemory.getAllVectors().entrySet().stream()
                .filter(entry -> entry.getKey().contains("/entity/Q"))
                .sorted((e1, e2) -> Double.compare(e2.getValue().similarity(contextTarget), e1.getValue().similarity(contextTarget)))
                .limit(5)
                .forEach(entry -> {
                    String label = entry.getKey().substring(entry.getKey().lastIndexOf("/") + 1);
                    System.out.printf("   %s: %.4f\n", label, entry.getValue().similarity(contextTarget));
                });

        // 6. TEST ANALOGIA: Relational Analogy (Pattern-based)
        // Test: Berlino : Germania = Parigi : X
        System.out.println("\n=== TEST ANALOGIA RELAZIONALE ===");
        System.out.println("   Pattern: [Qualcosa] --P17--> Q183 (Germania)");
        System.out.println("   Atteso: Parigi --P17--> Q142 (Francia)");

        String q90Uri = "http://www.wikidata.org/entity/Q90";  // Paris
        String q142Uri = "http://www.wikidata.org/entity/Q142"; // France

        Resource parisEntity = ModelFactory.createDefaultModel().createResource(q90Uri);
        System.out.println("[*] Analisi Topologica di: " + q90Uri);
        engine.expandAndProcess(parisEntity);

        HDVector vGermania = vQ183;
        HDVector vFrancia = itemMemory.getOrGenerate(q142Uri);

        // Re-fetch del vettore aggiornato dopo l'espansione topologica
        HDVector vParigi = itemMemory.getOrGenerate(q90Uri);
        System.out.printf("   [*] vParigi ricaricato dalla memory dopo expand\n");

        // ============================================================
        // SELECTIVE RELATIONAL QUERY (Attenzione Selettiva)
        // Usa il predificato P17 scoperto come "sonda" per interrogare Parigi
        // ============================================================
        // P17 è un vettore PURO (non derivato da bundle), quindi il rumore è minimo
        // Chiediamo a Parigi: "Qual è l'oggetto che occupa il ruolo P17 dentro di te?"
        // resultRaw = vParigi ⊗ ρ¹(P17) ⊗ ρ⁻²
        HDVector resultRaw = vParigi.bind(vP17.permute(1)).permute(-2);
        System.out.println("   [*] Query diretta su Parigi con P17 come sonda");

        System.out.printf("   [*] Similarità RAW con Francia (Q142): %.4f\n",
            resultRaw.similarity(vFrancia));

        // Clean-up finale per agganciare il risultato al vettore più simile
        HDVector analogyCleaned = itemMemory.cleanUpRelative(resultRaw);
        double analogySim = analogyCleaned.similarity(vFrancia);

        // Statistiche Z-Score
        double mean = itemMemory.getLastMean();
        double stdDev = itemMemory.getLastStdDev();
        double sigma = itemMemory.getLastBestSigma();
        String bestKey = itemMemory.getLastBestKey();

        System.out.printf("   [*] Statistiche Z-Score:\n");
        System.out.printf("   [*]   Media rumore di fondo (μ): %.6f\n", mean);
        System.out.printf("   [*]   Deviazione Standard (σ): %.6f\n", stdDev);
        System.out.printf("   [*]   Sigma dalla media: %.2f σ\n", sigma);
        System.out.printf("   [*]   Best match: %s\n", bestKey != null ? bestKey.substring(bestKey.lastIndexOf("/") + 1) : "N/A");

        String analogyLabel = (stdDev > 0 && sigma > 6) ? "Francia (Outlier 6σ)" : "Sconosciuto";
        System.out.printf("   [*] Risultato: %s\n", analogyLabel);

        if (sigma > 6) {
            System.out.println("   [SUCCESS] La Francia è un OUTLIER statistico (>6σ)!");
        } else {
            System.out.println("   [INFO] La Francia non è un outlier significativo. Debug in corso...");
            itemMemory.getAllVectors().entrySet().stream()
                    .filter(entry -> entry.getKey().contains("/entity/Q"))
                    .sorted((e1, e2) -> Double.compare(e2.getValue().similarity(resultRaw), e1.getValue().similarity(resultRaw)))
                    .limit(5)
                    .forEach(entry -> {
                        String label = entry.getKey().substring(entry.getKey().lastIndexOf("/") + 1);
                        double sim = entry.getValue().similarity(resultRaw);
                        double entrySigma = (stdDev > 0) ? (sim - mean) / stdDev : 0;
                        System.out.printf("   %s: %.4f (%.2f σ)\n", label, sim, entrySigma);
                    });
        }

        if (analogySim > 0.1) {
            System.out.println("   [SUCCESS] L'analogia strutturale è confermata!");
        } else {
            System.out.println("   [INFO] Il sistema non ha raggiunto la soglia. Debug in corso...");
            itemMemory.getAllVectors().entrySet().stream()
                    .filter(entry -> entry.getKey().contains("/entity/Q"))
                    .sorted((e1, e2) -> Double.compare(e2.getValue().similarity(resultRaw), e1.getValue().similarity(resultRaw)))
                    .limit(3)
                    .forEach(entry -> {
                        String label = entry.getKey().substring(entry.getKey().lastIndexOf("/") + 1);
                        System.out.printf("   Top candidate: %s: %.4f\n", label, entry.getValue().similarity(resultRaw));
                    });
        }
    }
}