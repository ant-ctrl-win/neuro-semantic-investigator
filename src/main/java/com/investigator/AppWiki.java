package com.investigator;

import com.investigator.llm.OntologyTranslator;
import com.investigator.vsa.*;
import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.jena.*;
import com.investigator.engine.InvestigationEngine;
import org.apache.jena.rdf.model.*;

import java.util.*;

public class AppWiki {
    public static void main(String[] args) {
        System.out.println("=== NEURO-SEMANTIC INVESTIGATOR: WIKI CONTEXT PROBE ===");
        System.out.println("=== Flusso: Sonda Descrittiva LLM -> Estrazione VSA ===\n");

        ItemMemory itemMemory = new ItemMemory(new RandomGenerationStrategy());
        OntologyTranslator translator = new OntologyTranslator();

        SparqlEndpoint wikidataEndpoint = new SparqlEndpoint("https://query.wikidata.org/sparql");
        GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
        TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();

        String apollo11Uri = "http://www.wikidata.org/entity/Q43653";  // Sorgente: Apollo 11
        String triesteUri = "http://www.wikidata.org/entity/Q850157";  // Target: Batiscafo Trieste

        Resource apolloResource = ModelFactory.createDefaultModel().createResource(apollo11Uri);
        Resource triesteResource = ModelFactory.createDefaultModel().createResource(triesteUri);

        InvestigationEngine engine = new InvestigationEngine(new HDVectorMapB(), graphManager, itemMemory, topologicalUpdater);

        System.out.println("[*] Ingestione e vettorizzazione grafi in corso...");
        engine.expandAndProcess(apolloResource);
        engine.expandAndProcess(triesteResource);
        Model localModel = engine.getGraphManager().getLocalModel();

        System.out.println("\n=======================================================");
        System.out.println("   FASE 1: ESTRAZIONE DEL CONTESTO E DELL'INVENTARIO   ");
        System.out.println("=======================================================");

        // 1. Estraiamo le descrizioni umane dal Web Semantico
        String apolloDesc = fetchDescriptionFromWikidata(apollo11Uri);
        String triesteDesc = fetchDescriptionFromWikidata(triesteUri);

        System.out.println("   [CONTESTO SORGENTE] Apollo 11: '" + apolloDesc + "'");
        System.out.println("   [CONTESTO BERSAGLIO] Trieste: '" + triesteDesc + "'\n");

        // 2. Estraiamo l'inventario del Trieste (sia Figli che Genitori)
        Map<String, String> triesteProperties = extractPropertyLabels(triesteResource, localModel);

        System.out.println("\n=======================================================");
        System.out.println("   FASE 2: ALLINEAMENTO ONTOLOGICO DESCRITTIVO (LLM)   ");
        System.out.println("=======================================================");
        System.out.println("   -> Uso l'INTERA DESCRIZIONE dell'Apollo 11 come 'sonda' per cercare punti di contatto nel Trieste...");

        // Abbassiamo la soglia a 0.35 perché paragoniamo una frase intera a concetti singoli
        String targetRoleUri = translator.findSemanticEquivalent(apolloDesc, triesteProperties, 0.35);

        if (targetRoleUri == null) {
            System.out.println("   [ERRORE] L'LLM non ha trovato nessun punto di contatto tra la descrizione e il bersaglio.");
            return;
        }
        String targetRoleLabel = fetchLabelFromWikidata(targetRoleUri);

        System.out.println("\n=======================================================");
        System.out.println("   FASE 3: ESTRAZIONE MATEMATICA (VSA MAP-B)           ");
        System.out.println("=======================================================");

        HDVector triesteRoot = itemMemory.getTreeVector(triesteUri);
        HDVector targetRole = itemMemory.getOrGenerate(targetRoleUri);

        System.out.println("   -> Uso la chiave identificata ('" + targetRoleLabel + "') per interrogare il vettore a 100.000 dimensioni...");

        // Estrazione del ramo
        HDVector noisyTriesteBranch = triesteRoot.permute(-100).bind(targetRole);
        HDVector cleanTriesteBranch = itemMemory.cleanUpChunk(noisyTriesteBranch);

        if (cleanTriesteBranch == null) {
            System.out.println("   [AVVISO] La VSA non ha trovato nulla in uscita per questo ramo.");
            System.out.println("   (Nota: Se la proprietà identificata era un [IN] Genitore, la VSA al Livello 1 non l'ha vettorizzata. Serve il Multi-Hop!)");
            return;
        }

        // Estrazione dell'oggetto
        HDVector noisyTargetObject = cleanTriesteBranch.bind(targetRole.permute(1)).permute(-2);
        HDVector finalAnalogue = itemMemory.cleanUpRelative(noisyTargetObject);

        if (finalAnalogue != null) {
            String humanReadableResult = fetchLabelFromWikidata(itemMemory.getLastBestKey());
            System.out.println("\n[!] SCOPERTA EFFETTUATA CON SUCCESSO:");
            System.out.println("    Il concetto descrittivo di Apollo 11 ('" + apolloDesc + "')");
            System.out.println("    ha attivato il nodo semantico: '" + targetRoleLabel + "'");
            System.out.println("    Il cui contenuto estratto matematicamente è: ===> " + humanReadableResult);
            System.out.println("    [Confidenza VSA]: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
        } else {
            System.out.println("\n[?] Ramo trovato, ma nessun individuo chiaro all'interno.");
        }
    }

    // ===================================================================================
    // METODI DI SUPPORTO (Estrazione Dati da Wikidata)
    // ===================================================================================

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
            // Silenzioso
        }
        return "No description found";
    }

//    private static Map<String, String> extractPropertyLabels(Resource entity, Model localGraph) {
//        Map<String, String> labels = new HashMap<>();
//        Set<Property> outgoingProps = new HashSet<>();
//        Set<Property> incomingProps = new HashSet<>();
//
//        // 1. FIGLI (Outgoing edges)
//        localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(s -> outgoingProps.add(s.getPredicate()));
//
//        // 2. GENITORI (Incoming edges)
//        localGraph.listStatements(null, null, entity).forEachRemaining(s -> incomingProps.add(s.getPredicate()));
//
//        System.out.println("   [INDAGINE ONTOLOGICA] Inventario delle proprietà del target:");
//
//        System.out.println("   --- PROPRIETÀ IN USCITA (I Figli) ---");
//        for (Property prop : outgoingProps) {
//            String uri = prop.getURI();
//            if (isMetadata(uri)) continue;
//
//            String label = fetchLabelFromWikidata(uri);
//            labels.put(uri, label);
//            System.out.println("      - [OUT] " + uri + "  --->  Label: '" + label + "'");
//        }
//
//        System.out.println("   --- PROPRIETÀ IN ENTRATA (I Genitori) ---");
//        for (Property prop : incomingProps) {
//            String uri = prop.getURI();
//            if (isMetadata(uri)) continue;
//
//            if (!labels.containsKey(uri)) {
//                String label = fetchLabelFromWikidata(uri);
//                labels.put(uri, label);
//            }
//            System.out.println("      - [IN]  " + uri + "  --->  Label: '" + labels.get(uri) + "'");
//        }
//
//        return labels;
//    }
private static Map<String, String> extractPropertyLabels(Resource entity, Model localGraph) {
    Map<String, String> semanticVocabulary = new HashMap<>();

    System.out.println("   [INDAGINE ONTOLOGICA] Esplorazione profonda del target (Livello 1 & Livello 2):");

    // Analizziamo tutti gli Statement (Soggetto -> Predicato -> Oggetto) dove l'entità è il Soggetto
    localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(stmt -> {
        Property prop = stmt.getPredicate();
        String propUri = prop.getURI();

        // 1. FILTRO NAMESPACE: Teniamo SOLO le proprietà "direct" (che contengono i dati veri)
        // e ignoriamo metadati o proprietà di sistema.
        if (!propUri.startsWith("http://www.wikidata.org/prop/direct/") || isMetadata(propUri)) {
            return;
        }

        // Traduciamo la Proprietà (Livello 1)
        String propLabel = fetchLabelFromWikidata(propUri);
        semanticVocabulary.put(propUri, propLabel);

        // 2. ESPLORAZIONE LIVELLO 2 (Gli Oggetti Collegati)
        RDFNode objectNode = stmt.getObject();
        if (objectNode.isResource()) {
            String objUri = objectNode.asResource().getURI();
            // Se l'oggetto è un'altra entità Wikidata (es. un Evento, una Persona, un Luogo)
            if (objUri != null && objUri.startsWith("http://www.wikidata.org/entity/")) {
                // Traduciamo l'Entità (Livello 2)
                String objLabel = fetchLabelFromWikidata(objUri);
                // Creiamo una chiave composita per l'LLM, es: "significant event: Project Nekton"
                String compositeKey = propUri + "->" + objUri;
                String compositeLabel = propLabel + ": " + objLabel;
                semanticVocabulary.put(compositeKey, compositeLabel);
                System.out.println("      - [L2 Espanso] " + compositeLabel);
            }
        } else {
            System.out.println("      - [L1 Diretto] " + propLabel + " (Valore letterale)");
        }
    });

    return semanticVocabulary;
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
        // Ho aggiunto P137 e P2067 tra i filtri se vogliamo ignorare dati tecnici per forzare scoperte semantiche,
        // ma puoi modificarlo come preferisci.
        return uri.contains("P18") || uri.contains("P373") || uri.contains("P2002") || uri.contains("P2013");
    }
}