package com.investigator;

import com.investigator.llm.OntologyTranslator;
import com.investigator.vsa.*;
import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.jena.*;
import com.investigator.engine.InvestigationEngine;
import org.apache.jena.rdf.model.*;

import java.util.*;

public class AppAnalogyChain {
    public static void main(String[] args) {
        System.out.println("=== NEURO-SEMANTIC INVESTIGATOR: LA VERA ANALOGIA ===");
        System.out.println("=== Flusso: Scoperta VSA -> Traduzione LLM -> Proiezione VSA ===\n");

        ItemMemory itemMemory = new ItemMemory(new RandomGenerationStrategy());
        OntologyTranslator translator = new OntologyTranslator();

        SparqlEndpoint wikidataEndpoint = new SparqlEndpoint("https://query.wikidata.org/sparql");
        GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
        TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();

        String apollo11Uri = "http://www.wikidata.org/entity/Q43653";  // Sorgente: Apollo 11
        String armstrongUri = "http://www.wikidata.org/entity/Q1615";  // Oggetto noto: Neil Armstrong
        //String operatorProp = "http://www.wikidata.org/entity/P137";
        //String crewMemberProp = "http://www.wikidata.org/entity/P1029";
        String triesteUri = "http://www.wikidata.org/entity/Q850157";  // Target: Batiscafo Trieste

        Resource apolloResource = ModelFactory.createDefaultModel().createResource(apollo11Uri);
        Resource triesteResource = ModelFactory.createDefaultModel().createResource(triesteUri);

        InvestigationEngine engine = new InvestigationEngine(new HDVectorMapB(), graphManager, itemMemory, topologicalUpdater);

        System.out.println("[*] Ingestione e vettorizzazione grafi in corso...");
        engine.expandAndProcess(apolloResource);
        engine.expandAndProcess(triesteResource);
        Model localModel = engine.getGraphManager().getLocalModel();

        HDVector apolloRoot = itemMemory.getTreeVector(apollo11Uri);
        HDVector armstrongVector = itemMemory.getOrGenerate(armstrongUri);

        System.out.println("\n=======================================================");
        System.out.println("   STEP 1: DEDUZIONE DEL RUOLO SORGENTE (VSA)          ");
        System.out.println("=======================================================");
        System.out.println("   Obiettivo: Scoprire matematicamente come Neil Armstrong è legato all'Apollo 11.");

        String sourceRoleUri = null;
        double bestHypothesisScore = -1.0;

        // Estraiamo tutte le proprietà dell'Apollo per testarle
        Set<Property> apolloProps = new HashSet<>();
        localModel.listStatements(apolloResource, null, (RDFNode) null).forEachRemaining(s -> apolloProps.add(s.getPredicate()));

        // La VSA testa tutte le "chiavi" (Predicati) per vedere quale apre il Chunk che contiene Armstrong
        for (Property prop : apolloProps) {
            if (isMetadata(prop.getURI())) continue;

            HDVector candidateRole = itemMemory.getOrGenerate(prop.getURI());
            HDVector noisyBranch = apolloRoot.permute(-100).bind(candidateRole);
            HDVector cleanBranch = itemMemory.cleanUpChunk(noisyBranch);

            if (cleanBranch != null) {
                // Disfiamo il ramo per estrarre l'oggetto
                HDVector extractedObject = cleanBranch.bind(candidateRole.permute(1)).permute(-2);

                // Misuriamo quanto questo oggetto assomiglia ad Armstrong
                double similarity = extractedObject.similarity(armstrongVector);
                if (similarity > bestHypothesisScore) {
                    bestHypothesisScore = similarity;
                    sourceRoleUri = prop.getURI();
                }
            }
        }

        if (sourceRoleUri == null || bestHypothesisScore < 0.1) {
            System.out.println("   [ERRORE] La VSA non riesce a trovare Armstrong nel vettore dell'Apollo 11.");
            return;
        }

        String sourceRoleLabel = fetchLabelFromWikidata(sourceRoleUri);
        System.out.printf("   -> DEDUZIONE COMPLETATA: Armstrong è legato tramite '%s' (Similarità del vettore: %.2f)\n", sourceRoleLabel, bestHypothesisScore);

        System.out.println("\n=======================================================");
        System.out.println("   STEP 2: IL PONTE ANALOGICO (LLM)                    ");
        System.out.println("=======================================================");

        Map<String, String> triesteProperties = extractPropertyLabels(triesteResource, localModel);
        System.out.println("   -> Chiedo all'LLM di tradurre '" + sourceRoleLabel + "' nel dominio del Trieste...");

        String targetRoleUri = translator.findSemanticEquivalent(sourceRoleLabel, triesteProperties, 0.40);

        if (targetRoleUri == null) {
            System.out.println("   [ERRORE] Il traduttore non ha trovato un equivalente per '" + sourceRoleLabel + "'. Analogia fallita.");
            return;
        }
        String targetRoleLabel = fetchLabelFromWikidata(targetRoleUri);

        System.out.println("\n=======================================================");
        System.out.println("   STEP 3 & 4: PROIEZIONE E ESTRAZIONE TARGET (VSA)    ");
        System.out.println("=======================================================");

        HDVector triesteRoot = itemMemory.getTreeVector(triesteUri);
        HDVector targetRole = itemMemory.getOrGenerate(targetRoleUri);

        System.out.println("   -> Uso la chiave tradotta ('" + targetRoleLabel + "') per aprire il vettore del Trieste...");
        HDVector noisyTriesteBranch = triesteRoot.permute(-100).bind(targetRole);
        HDVector cleanTriesteBranch = itemMemory.cleanUpChunk(noisyTriesteBranch);

        if (cleanTriesteBranch == null) {
            System.out.println("   [ERRORE] Il ramo estratto si è disintegrato nel rumore.");
            return;
        }

        HDVector noisyTargetObject = cleanTriesteBranch.bind(targetRole.permute(1)).permute(-2);
        HDVector finalAnalogue = itemMemory.cleanUpRelative(noisyTargetObject);

        if (finalAnalogue != null) {
            System.out.println("\n[!] ANALOGIA RISOLTA CON SUCCESSO:");
            System.out.println("    Neil Armstrong  sta a  Apollo 11");
            System.out.println("    COME");

            // Facciamo la query finale a Wikidata per avere il nome umano del risultato (Step 4 della tua domanda precedente)
            String humanReadableResult = fetchLabelFromWikidata(itemMemory.getLastBestKey());
            System.out.println("    " + humanReadableResult + "  sta a  Batiscafo Trieste");
            System.out.println("\n    [Logica Applicata]: " + sourceRoleLabel + " ===> " + targetRoleLabel);
            System.out.println("    [Confidenza VSA]: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
        } else {
            System.out.println("\n[?] Ramo trovato, ma nessun individuo chiaro all'interno.");
        }
    }

//    private static Map<String, String> extractPropertyLabels(Resource entity, Model localGraph) {
//        Map<String, String> labels = new HashMap<>();
//        Set<Property> actualProperties = new HashSet<>();
//        localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(s -> actualProperties.add(s.getPredicate()));
//
//        for (Property prop : actualProperties) {
//            String uri = prop.getURI();
//            if (isMetadata(uri)) continue;
//            labels.put(uri, fetchLabelFromWikidata(uri));
//        }
//        return labels;
//    }

    private static String fetchDescriptionFromWikidata(String entityUri) {
        String sparqlQuery =
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT ?desc WHERE { " +
                        "  <" + entityUri + "> schema:description ?desc . " +
                        "  FILTER (lang(?desc) = 'en') " +
                        "} LIMIT 1";

        try {
            org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
            try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
                    .query(query)
                    .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
                    .build()) {

                org.apache.jena.query.ResultSet results = qexec.execSelect();
                if (results.hasNext()) {
                    return results.nextSolution().getLiteral("desc").getString();
                }
            }
        } catch (Exception e) {
            System.err.println("   [AVVISO] Errore nell'estrazione della descrizione: " + e.getMessage());
        }
        return "";
    }

    private static Map<String, String> extractPropertyLabels(Resource entity, Model localGraph) {
        Map<String, String> labels = new HashMap<>();
        Set<Property> outgoingProps = new HashSet<>();
        Set<Property> incomingProps = new HashSet<>();

        // 1. FIGLI (Outgoing edges: Entità -> Predicato -> Oggetto)
        localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(s -> outgoingProps.add(s.getPredicate()));

        // 2. GENITORI (Incoming edges: Soggetto -> Predicato -> Entità)
        // Mettiamo 'null' sul Soggetto e sul Predicato, e l'entità come Oggetto.
        localGraph.listStatements(null, null, entity).forEachRemaining(s -> incomingProps.add(s.getPredicate()));

        System.out.println("   [INDAGINE ONTOLOGICA] Inventario delle proprietà del target:");

        System.out.println("   --- PROPRIETÀ IN USCITA (I Figli) ---");
        for (Property prop : outgoingProps) {
            String uri = prop.getURI();
            if (isMetadata(uri)) continue;

            String label = fetchLabelFromWikidata(uri);
            labels.put(uri, label);
            System.out.println("      - [OUT] " + uri + "  --->  Label: '" + label + "'");
        }

        System.out.println("   --- PROPRIETÀ IN ENTRATA (I Genitori) ---");
        for (Property prop : incomingProps) {
            String uri = prop.getURI();
            if (isMetadata(uri)) continue;

            // Se l'abbiamo già tradotta, non rifacciamo la chiamata a Wikidata
            if (!labels.containsKey(uri)) {
                String label = fetchLabelFromWikidata(uri);
                labels.put(uri, label);
            }
            System.out.println("      - [IN]  " + uri + "  --->  Label: '" + labels.get(uri) + "'");
        }

        return labels;
    }

    private static String fetchLabelFromWikidata(String uri) {
        if (!uri.startsWith("http://www.wikidata.org/")) {
            return uri.replace("_", " ");
        }

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
                if (results.hasNext()) return results.nextSolution().getLiteral("label").getString();
            }
        } catch (Exception e) { /* Silenzioso */ }

        String[] parts = uri.split("/");
        return parts[parts.length - 1];
    }

    private static boolean isMetadata(String uri) {
        return uri.contains("P18") || uri.contains("P373") || uri.contains("P2002") || uri.contains("P2013") || uri.contains("P137");
    }
}