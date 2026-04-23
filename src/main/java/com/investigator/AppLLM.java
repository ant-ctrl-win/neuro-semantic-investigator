package com.investigator;

import com.investigator.llm.OntologyTranslator;
import com.investigator.vsa.*;
import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.jena.*;
import com.investigator.engine.InvestigationEngine;
import org.apache.jena.rdf.model.*;

import java.util.*;

public class AppLLM {
    public static void main(String[] args) {
        System.out.println("=== NEURO-SEMANTIC INVESTIGATOR: MISSION ANALOGY ===");
        System.out.println("=== ARCHITETTURA IBRIDA A DUE LIVELLI (LLM + VSA) ===\n");

        // 1. IL RITORNO ALLA MATEMATICA PURA: Vettori casuali ortogonali (zero rumore)
        ItemMemory itemMemory = new ItemMemory(new RandomGenerationStrategy());

        // 2. IL CERVELLO SEMANTICO: Inizializziamo il traduttore LLM
        OntologyTranslator translator = new OntologyTranslator();

        SparqlEndpoint wikidataEndpoint = new SparqlEndpoint("https://query.wikidata.org/sparql");
        GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
        TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();

        String apollo11Uri = "http://www.wikidata.org/entity/Q43653";  // Apollo 11
        String triesteUri = "http://www.wikidata.org/entity/Q850157";  // Batiscafo Trieste
        String armstrongUri = "http://www.wikidata.org/entity/Q1615";  // Neil Armstrong

        Resource apolloResource = ModelFactory.createDefaultModel().createResource(apollo11Uri);
        Resource triesteResource = ModelFactory.createDefaultModel().createResource(triesteUri);

        InvestigationEngine engine = new InvestigationEngine(new HDVectorMapB(), graphManager, itemMemory, topologicalUpdater);

        System.out.println("[*] Ingestione Grafo Apollo 11...");
        engine.expandAndProcess(apolloResource);

        System.out.println("[*] Ingestione Grafo Batiscafo Trieste...");
        engine.expandAndProcess(triesteResource);

        Model localModel = engine.getGraphManager().getLocalModel();

        System.out.println("\n=======================================================");
        System.out.println("   FASE 1: TRADUZIONE ONTOLOGICA (LLM)                 ");
        System.out.println("=======================================================");

        // Mappiamo tutte le proprietà del Trieste con le loro etichette umane (Label)
        Map<String, String> triesteProperties = extractPropertyLabels(triesteResource, localModel);


        String sourceConcept = "maker";
        System.out.println("[1] Cerco il ponte semantico per l'attributo " +sourceConcept + "...");

        // Il traduttore scavalca la rigidità di Wikidata e trova l'URI equivalente nel Trieste
        String targetRoleUri = translator.findSemanticEquivalent(sourceConcept, triesteProperties, 0.45);

        if (targetRoleUri == null) {
            System.out.println("   [ERRORE] Il traduttore non ha trovato un ruolo equivalente nel target.");
            return;
        }

        System.out.println("\n=======================================================");
        System.out.println("   FASE 2: UNBINDING TOPOLOGICO PURO (VSA MAP-B)       ");
        System.out.println("=======================================================");

        HDVector triesteRoot = itemMemory.getTreeVector(triesteUri);
        HDVector targetRole = itemMemory.getOrGenerate(targetRoleUri);

        System.out.println("[2] Estrazione puramente matematica del ramo dal Trieste usando il ruolo trovato...");
        // Disfiamo il mega-vettore usando la chiave appena tradotta
        HDVector noisyTriesteBranch = triesteRoot.permute(-100).bind(targetRole);

        // Poiché usiamo vettori Random ortogonali, il rumore è ZERO. Lo Z-score sarà altissimo.
        HDVector cleanTriesteBranch = itemMemory.cleanUpChunk(noisyTriesteBranch);

        if (cleanTriesteBranch == null) {
            System.out.println("   [ERRORE] Il ramo estratto non è sopravvissuto al clean-up matematico.");
            return;
        }
        System.out.println("   -> Ramo estratto e ripulito con successo! (Z-Score: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ)");

        System.out.println("\n[3] Interrogo il ramo per estrarre gli individui all'interno...");
        // Ricordiamo la tripla: T = P(1) ⊗ O(2). Disfiamo P(1) e shiftiamo di -2.
        HDVector noisyAnalogueObject = cleanTriesteBranch.bind(targetRole.permute(1)).permute(-2);

        // CLEAN-UP FINALE SULLE FOGLIE
        HDVector finalAnalogue = itemMemory.cleanUpRelative(noisyAnalogueObject);

        if (finalAnalogue != null) {
            System.out.println("\n[!] RISULTATO ESTRATTO CON SUCCESSO:");
            System.out.println("    Concetto cercato: '" + sourceConcept + "'");
            System.out.println("    Valore nel Target: " + itemMemory.getLastBestKey());
            System.out.println("    Confidenza Strutturale:  " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
        } else {
            System.out.println("\n[?] Ramo trovato, ma nessun valore chiaro all'interno.");
        }
    }

    /**
     * Utility per estrarre le proprietà e risolvere le loro etichette interrogando Wikidata
     * con il bypass anti-bot per garantire che l'LLM legga parole e non codici.
     */
//    private static Map<String, String> extractPropertyLabels(Resource entity, Model localGraph) {
//        Map<String, String> labels = new HashMap<>();
//        Set<Property> actualProperties = new HashSet<>();
//
//        localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(s -> actualProperties.add(s.getPredicate()));
//
//        for (Property prop : actualProperties) {
//            String uri = prop.getURI();
//            if (isMetadata(uri)) continue;
//
//            String label = fetchLabelFromWikidata(uri);
//            labels.put(uri, label);
//        }
//        return labels;
//    }
//
//
//    private static String fetchLabelFromWikidata(String uri) {
//        String sparqlQuery =
//                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
//                        "SELECT ?label WHERE { " +
//                        "  <" + uri + "> rdfs:label ?label . " +
//                        "  FILTER (lang(?label) = 'en') " +
//                        "} LIMIT 1";
//
//        try {
//            org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
//            try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
//                    .query(query)
//                    .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
//                    .build()) {
//
//                org.apache.jena.query.ResultSet results = qexec.execSelect();
//                if (results.hasNext()) {
//                    return results.nextSolution().getLiteral("label").getString();
//                }
//            }
//        } catch (Exception e) {
//            // Silenzioso per evitare inquinamento dei log
//        }
//        // Fallback sul nome locale se la query fallisce
//        String[] parts = uri.split("/");
//        return parts[parts.length - 1];
//    }


    private static Map<String, String> extractPropertyLabels(Resource entity, Model localGraph) {
        Map<String, String> labels = new HashMap<>();
        Set<Property> actualProperties = new HashSet<>();

        localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(s -> actualProperties.add(s.getPredicate()));

        System.out.println("   [INDAGINE ONTOLOGICA] Inventario delle proprietà del target:");
        for (Property prop : actualProperties) {
            String uri = prop.getURI();
            if (isMetadata(uri)) continue;

            String label = fetchLabelFromWikidata(uri);
            labels.put(uri, label);
            // STAMPA DI DEBUG PER VEDERE COSA ESTRAE
            System.out.println("      - Trovata proprietà: " + uri + "  --->  Label: '" + label + "'");
        }
        return labels;
    }

    private static String fetchLabelFromWikidata(String uri) {
        // IL TRUCCO DI WIKIDATA: Convertiamo l'URI dal namespace "direct" a quello "entity" prima di chiedere la label!
        String entityUri = uri.replace("/prop/direct/", "/entity/");

        String sparqlQuery =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "SELECT ?label WHERE { " +
                        "  <" + entityUri + "> rdfs:label ?label . " +
                        "  FILTER (lang(?label) = 'en') " +
                        "} LIMIT 1";

        try {
            org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
            try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
                    .query(query)
                    .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
                    .build()) {

                org.apache.jena.query.ResultSet results = qexec.execSelect();
                if (results.hasNext()) {
                    return results.nextSolution().getLiteral("label").getString();
                }
            }
        } catch (Exception e) {
            // Silenzioso
        }

        // Fallback
        String[] parts = uri.split("/");
        return parts[parts.length - 1];
    }

    private static boolean isMetadata(String uri) {
        return uri.contains("P18") || uri.contains("P373") || uri.contains("P2002") || uri.contains("P2013");
    }
}