package com.investigator;

import com.investigator.llm.OntologyTranslator;
import com.investigator.vsa.*;
import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.jena.*;
import com.investigator.engine.InvestigationEngine;
import org.apache.jena.rdf.model.*;

import java.util.*;

public class AppDis {
    public static void main(String[] args) {
        System.out.println("=== NEURO-SEMANTIC INVESTIGATOR: LA VERA ANALOGIA ===");
        System.out.println("=== Flusso: Risoluzione -> Reranking -> Scoperta VSA -> Encoder-Embedding -> Proiezione Olografica ===\n");

        ItemMemory itemMemory = new ItemMemory(new RandomGenerationStrategy());
        OntologyTranslator translator = new OntologyTranslator();

        SparqlEndpoint wikidataEndpoint = new SparqlEndpoint("https://query.wikidata.org/sparql");
        GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
        TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();

        EntityResolver resolver = new EntityResolver();

        System.out.println("[*] FASE 0: Risoluzione entità e profilazione ontologica...");

        // SORGENTE e OGGETTO NOTO
        List<ResolvedEntity> apolloResults = resolver.resolve("Amleto", 1);
        List<ResolvedEntity> armstrongResults = resolver.resolve("William Shakespeare", 1);

        // IL TARGET: Tiriamo giù i TOP 5 per il Nilo per evitare il Popularity Bias!
        List<ResolvedEntity> targetResults = resolver.resolve("Lacrimosa", 5);

        if (apolloResults.isEmpty() || armstrongResults.isEmpty() || targetResults.isEmpty()) {
            System.out.println("   [ERRORE CRITICO] Una o più entità non sono state trovate dal motore di ricerca.");
            return;
        }

        ResolvedEntity apolloEntity = apolloResults.get(0);
        ResolvedEntity armstrongEntity = armstrongResults.get(0);

        // LA MAGIA DEL RERANKER VETTORIALE:
        // Chiediamo all'encoder di scegliere il Nilo che più assomiglia all'ontologia della sorgente (es. "river")
        ResolvedEntity targetEntity = translator.disambiguateCandidates(apolloEntity.classLabel(), targetResults);

        System.out.println("\n=======================================================");
        System.out.println("   REPORT ONTOLOGICO PRELIMINARE");
        System.out.println("=======================================================");
        System.out.printf("   SORGENTE  : %-25s  Tipo: %s%n", apolloEntity.entityLabel(), apolloEntity.classLabel());
        System.out.printf("   OGG. NOTO : %-25s  Tipo: %s%n", armstrongEntity.entityLabel(), armstrongEntity.classLabel());
        System.out.printf("   TARGET 1  : %-25s  Tipo: %s%n", targetEntity.entityLabel(), targetEntity.classLabel());
        System.out.println("=======================================================\n");

        String apollo11Uri = apolloEntity.entityUri();
        String armstrongUri = armstrongEntity.entityUri();
        String currentTargetUri = targetEntity.entityUri();
        String currentTargetLabel = targetEntity.entityLabel();

        // ------------------------------------------------------------------------------------------------
        // ALLINEAMENTO ONTOLOGICO AUTOMATICO (Graph + Encoder-Embedding)
        // ------------------------------------------------------------------------------------------------
        if (!translator.areClassesCompatible(apolloEntity.classLabel(), targetEntity.classLabel(), 0.35)) {
            System.out.println("\n   [ALLARME ASIMMETRIA] Le ontologie sono semanticamente distanti ('" + apolloEntity.classLabel() + "' vs '" + targetEntity.classLabel() + "').");
            System.out.println("   L'Investigatore interroga l'inconscio collettivo (Wikidata) per trovare un nodo affine...");

            String sparqlQuery =
                    "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " +
                            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                            "SELECT DISTINCT ?event ?eventLabel ?classLabel WHERE { " +
                            "  { <" + currentTargetUri + "> wdt:P793 ?event . } " +
                            "  UNION { ?event wdt:P2283 <" + currentTargetUri + "> . } " +
                            "  UNION { ?event wdt:P516 <" + currentTargetUri + "> . } " +
                            "  OPTIONAL { ?event wdt:P31 ?class . ?class rdfs:label ?classLabel . FILTER(lang(?classLabel)='en') } " +
                            "  ?event rdfs:label ?eventLabel . FILTER(lang(?eventLabel)='en') " +
                            "} LIMIT 20";

            Map<String, String> candidateOntologies = new HashMap<>();
            Map<String, String> candidateLabels = new HashMap<>();

            try {
                org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
                try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
                        .query(query)
                        .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
                        .build()) {
                    org.apache.jena.query.ResultSet results = qexec.execSelect();
                    while (results.hasNext()) {
                        org.apache.jena.query.QuerySolution soln = results.next();
                        String evUri = soln.getResource("event").getURI();
                        String evLabel = soln.getLiteral("eventLabel").getString();
                        String clLabel = soln.contains("classLabel") ? soln.getLiteral("classLabel").getString() : "event";

                        candidateOntologies.put(evUri, clLabel + ": " + evLabel);
                        candidateLabels.put(evUri, evLabel);
                    }
                }
            } catch (Exception e) {
                System.out.println("   [ERRORE SPARQL] " + e.getMessage());
            }

            if (!candidateOntologies.isEmpty()) {
                System.out.println("   [Embedding] Valuto " + candidateOntologies.size() + " candidati topologici rispetto a '" + apolloEntity.classLabel() + "'...");

                String bestEventUri = translator.findSemanticEquivalent(apolloEntity.classLabel(), candidateOntologies, 0.20);

                if (bestEventUri != null) {
                    currentTargetUri = bestEventUri;
                    currentTargetLabel = candidateLabels.get(bestEventUri);
                    System.out.println("   [COMPENSAZIONE RIUSCITA] L'Embedding ha isolato l'evento topologicamente corretto!");
                    System.out.printf("   NUOVO TARGET: %-25s%n", currentTargetLabel);
                } else {
                    System.out.println("   [FALLIMENTO] L'Embedding non ha trovato nessun evento candidato affine a " + apolloEntity.classLabel());
                    return;
                }
            } else {
                System.out.println("   [FALLIMENTO] Il grafo locale non contiene collegamenti a eventi o missioni noti.");
                return;
            }
        }

        InvestigationEngine engine = new InvestigationEngine(new HDVectorMapB(), graphManager, itemMemory, topologicalUpdater);

        System.out.println("[*] Ingestione e vettorizzazione grafi in corso...");
        Resource apolloResource = ModelFactory.createDefaultModel().createResource(apollo11Uri);
        Resource targetResource = ModelFactory.createDefaultModel().createResource(currentTargetUri);

        engine.expandAndProcess(apolloResource);
        engine.expandAndProcess(targetResource);
        Model localModel = engine.getGraphManager().getLocalModel();

        HDVector apolloRoot = itemMemory.getTreeVector(apollo11Uri);
        HDVector armstrongVector = itemMemory.getOrGenerate(armstrongUri);

        System.out.println("\n=======================================================");
        System.out.println("   STEP 1: DEDUZIONE DEL RUOLO SORGENTE (VSA)          ");
        System.out.println("=======================================================");

        String sourceRoleUri = null;
        double bestHypothesisScore = -1.0;

        HDVector subjectVector = itemMemory.getOrGenerate(apollo11Uri);
        Set<Property> apolloProps = new HashSet<>();
        localModel.listStatements(apolloResource, null, (RDFNode) null).forEachRemaining(s -> apolloProps.add(s.getPredicate()));

        for (Property prop : apolloProps) {
            String propUri = prop.getURI();
            if (isMetadata(propUri)) continue;

            HDVector candidateRole = itemMemory.getOrGenerate(propUri);
            HDVector noisyBranch = apolloRoot.permute(-100).bind(candidateRole);
            HDVector cleanBranch = itemMemory.cleanUpChunk(noisyBranch);

            if (cleanBranch != null) {
                HDVector noisyObject = cleanBranch.bind(subjectVector).bind(candidateRole.permute(1)).permute(-2);
                double rawSimilarity = noisyObject.similarity(armstrongVector);

                if (rawSimilarity > 0.05 && rawSimilarity > bestHypothesisScore) {
                    bestHypothesisScore = rawSimilarity;
                    sourceRoleUri = propUri;
                }
            }
        }

        if (sourceRoleUri == null || bestHypothesisScore < 0.05) {
            System.out.println("   [ERRORE] La VSA non riesce a trovare l'Oggetto Noto nel vettore della Sorgente.");
            return;
        }

        String sourceRoleLabel = fetchLabelFromWikidata(sourceRoleUri);
        System.out.printf("   -> DEDUZIONE COMPLETATA: L'oggetto è legato tramite '%s' (Sim: %.2f)\n", sourceRoleLabel, bestHypothesisScore);

        System.out.println("\n=======================================================");
        System.out.println("   STEP 2: IL PONTE ANALOGICO (Embedding)                    ");
        System.out.println("=======================================================");

        Map<String, String> targetProperties = extractPropertyLabels(targetResource, localModel);

        // Arricchiamo le label con il tipo atteso della proprietà (es. "composer [expects: human]")
        Map<String, String> enrichedProperties = translator.enrichLabelsWithExpectedTypes(targetProperties);

        System.out.println("   -> Chiedo all'Embedding di tradurre '" + sourceRoleLabel + "' nel dominio di " + currentTargetLabel + "...");
        System.out.println("   -> (Proprietà arricchite con tipo atteso per matching strutturale)");

        String targetRoleUri = translator.findSemanticEquivalent(
                sourceRoleLabel, sourceRoleUri, enrichedProperties, 0.35);

        if (targetRoleUri == null) {
            System.out.println("   [ERRORE] Il traduttore non ha trovato un equivalente per '" + sourceRoleLabel + "'. Analogia fallita.");
            return;
        }
        String targetRoleLabel = fetchLabelFromWikidata(targetRoleUri);

        System.out.println("\n=======================================================");
        System.out.println("   STEP 3 & 4: PROIEZIONE E ESTRAZIONE TARGET (VSA)    ");
        System.out.println("=======================================================");

        HDVector targetRoot = itemMemory.getTreeVector(currentTargetUri);
        HDVector targetRole = itemMemory.getOrGenerate(targetRoleUri);
        HDVector targetSubject = itemMemory.getOrGenerate(currentTargetUri);

        System.out.println("   -> Uso la chiave tradotta ('" + targetRoleLabel + "') per aprire il vettore bersaglio...");

        // LA MAGIA DELL'HOLOGRAPHIC BYPASS:
        // Estraiamo il ramo rumoroso SENZA FARE IL CLEANUP INTERMEDIO per non perdere il segnale debole!
        HDVector noisyTargetBranch = targetRoot.permute(-100).bind(targetRole);

        System.out.println("   -> [HOLOGRAPHIC BYPASS] Salto la pulizia del ramo e disimballo direttamente l'oggetto...");

        // Svincoliamo Target Role e Target Subject direttamente dal rumore
        HDVector noisyTargetObject = noisyTargetBranch.bind(targetSubject).bind(targetRole.permute(1)).permute(-2);

        // Facciamo il Clean-Up solo alla fine, cercando nel vocabolario l'entità che risuona di più
        HDVector finalAnalogue = itemMemory.cleanUpRelative(noisyTargetObject);

        if (finalAnalogue != null) {
            System.out.println("\n[!] ANALOGIA RISOLTA CON SUCCESSO:");
            System.out.printf("    %-15s sta a  %-15s%n", armstrongEntity.entityLabel(), apolloEntity.entityLabel());
            System.out.println("    COME");

            String humanReadableResult = fetchLabelFromWikidata(itemMemory.getLastBestKey());
            System.out.printf("    %-15s sta a  %-15s%n", humanReadableResult, currentTargetLabel);
            System.out.println("\n    [Logica Applicata]: " + sourceRoleLabel + " ===> " + targetRoleLabel);
            System.out.println("    [Confidenza VSA]: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
        } else {
            System.out.println("\n[?] Il segnale è stato completamente inghiottito dal rumore del grafo (Z-Score troppo basso).");
        }
    }

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

        localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(s -> outgoingProps.add(s.getPredicate()));
        localGraph.listStatements(null, null, entity).forEachRemaining(s -> incomingProps.add(s.getPredicate()));

        System.out.println("   [INDAGINE ONTOLOGICA] Inventario delle proprietà del target:");

        System.out.println("   --- PROPRIETÀ IN USCITA (I Figli) ---");
        for (Property prop : outgoingProps) {
            String uri = prop.getURI();
            if (isMetadata(uri) || !uri.contains("/direct/")) continue;

            String label = fetchLabelFromWikidata(uri);
            labels.put(uri, label);
            System.out.println("      - [OUT] " + uri + "  --->  Label: '" + label + "'");
        }

        System.out.println("   --- PROPRIETÀ IN ENTRATA (I Genitori) ---");
        for (Property prop : incomingProps) {
            String uri = prop.getURI();
            if (isMetadata(uri) || !uri.contains("/direct/")) continue;

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

        String entityUri = uri.replace("/prop/direct-normalized/", "/entity/")
                .replace("/prop/direct/", "/entity/")
                .replace("/prop/statement/", "/entity/")
                .replace("/prop/", "/entity/");

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