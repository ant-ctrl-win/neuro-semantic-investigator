package com.investigator;

import com.investigator.embedding.OntologyTranslator;
import com.investigator.vsa.*;
import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.jena.*;
import com.investigator.engine.InvestigationEngine;
import org.apache.jena.rdf.model.*;

import java.util.*;

public class App {
    private static final String USER_AGENT = "neuro-semantic-investigator/0.1 (research; project@example.com)";
    private static final String WIKIDATA_ENDPOINT = "https://query.wikidata.org/sparql";

    public static void main(String[] args) {
        System.out.println("=== NEURO-SEMANTIC INVESTIGATOR: LA VERA ANALOGIA ===");
        System.out.println("=== Flusso: Risoluzione -> Reranking -> Scoperta VSA -> Encoder-Embedding -> Proiezione Olografica ===\n");

        ItemMemory itemMemory = new ItemMemory(new RandomGenerationStrategy());
        OntologyTranslator translator = new OntologyTranslator();

        SparqlEndpoint wikidataEndpoint = new SparqlEndpoint(WIKIDATA_ENDPOINT);
        GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
        TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();

        EntityResolver resolver = new EntityResolver();

        System.out.println("[*] FASE 0: Risoluzione entità e profilazione ontologica...");

        Map<String, String> opts = parseArgs(args);
        String sourceName = opts.get("source");
        String knownName = opts.get("known");
        String targetName = opts.get("target");
        int targetCandidates = Integer.parseInt(opts.get("candidates"));
        int topK = Integer.parseInt(opts.get("top"));

        System.out.println("[*] Query: " + sourceName + " : " + knownName
                + " = " + targetName + " : ?");
        System.out.println("[*] Target candidates: " + targetCandidates
                + ", top-K: " + topK);

        List<ResolvedEntity> apolloResults = resolver.resolve(sourceName, 1);
        List<ResolvedEntity> armstrongResults = resolver.resolve(knownName, 1);
        List<ResolvedEntity> targetResults = resolver.resolve(targetName, targetCandidates);

        if (apolloResults.isEmpty() || armstrongResults.isEmpty() || targetResults.isEmpty()) {
            System.out.println("   [ERRORE CRITICO] Una o più entità non sono state trovate dal motore di ricerca.");
            return;
        }

        ResolvedEntity apolloEntity = apolloResults.get(0);
        ResolvedEntity armstrongEntity = armstrongResults.get(0);
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
                try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service(WIKIDATA_ENDPOINT)
                        .query(query)
                        .httpHeader("User-Agent", USER_AGENT)
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

        HDVector armstrongVector = itemMemory.getOrGenerate(armstrongUri);

        System.out.println("\n=======================================================");
        System.out.println("   STEP 1: DEDUZIONE DEL RUOLO SORGENTE (VSA)          ");
        System.out.println("=======================================================");

        String sourceRoleUri = null;
        double bestHypothesisScore = -1.0;

        HDVector subjectVector = itemMemory.getOrGenerate(apollo11Uri);
        Set<Property> apolloProps = new HashSet<>();
        localModel.listStatements(apolloResource, null, (RDFNode) null).forEachRemaining(s -> apolloProps.add(s.getPredicate()));

        // Prima passata: raccogli tutte le similarità (per calcolare μ e σ globali)
        record Hypothesis(String propUri, double rawSimilarity) {}
        List<Hypothesis> allHypotheses = new ArrayList<>();

        for (Property prop : apolloProps) {
            String propUri = prop.getURI();
            if (isMetadata(propUri)) continue;

            HDVector candidateRole = itemMemory.getOrGenerate(propUri);
            HDVector cleanBranch = topologicalUpdater.recoverBranch(itemMemory, apollo11Uri, propUri);

            if (cleanBranch != null) {
                // Simpkin, sez. III-A: l'API percorre la gerarchia dei chunk
                // invece di assumere che tutte le triple siano nella radice.
                for (HDVector triple : topologicalUpdater.recoverTriples(itemMemory, apollo11Uri, propUri)) {
                    HDVector noisyObject = triple.bind(subjectVector).bind(candidateRole.permute(1)).permute(-2);
                    double rawSimilarity = noisyObject.similarity(armstrongVector);
                    allHypotheses.add(new Hypothesis(propUri, rawSimilarity));
                }
            }
        }

        // Calcola μ e σ sulla distribuzione osservata
        double mean = 0.0;
        for (Hypothesis h : allHypotheses) mean += h.rawSimilarity();
        mean /= Math.max(1, allHypotheses.size());

        double variance = 0.0;
        for (Hypothesis h : allHypotheses) variance += (h.rawSimilarity() - mean) * (h.rawSimilarity() - mean);
        variance /= Math.max(1, allHypotheses.size());
        double stdDev = Math.sqrt(variance);

        // Soglia z-score adattiva (Bonferroni-style). Cresce col numero di ipotesi
        // testate: con N=250 ipotesi la soglia sale a ~4.2σ, eliminando i falsi
        // picchi da rumore statistico.
        final double SIGMA_THRESHOLD = 3.0 + 0.5 * Math.log10(Math.max(1, allHypotheses.size()));

        // Seconda passata: seleziona il candidato con z-score massimo sopra soglia
        double bestSigma = Double.NEGATIVE_INFINITY;
        for (Hypothesis h : allHypotheses) {
            if (stdDev == 0) continue;
            double z = (h.rawSimilarity() - mean) / stdDev;
            if (z >= SIGMA_THRESHOLD && z > bestSigma) {
                bestSigma = z;
                sourceRoleUri = h.propUri();
                bestHypothesisScore = h.rawSimilarity();
            }
        }

        if (sourceRoleUri == null) {
            System.out.println("   [ERRORE] La VSA non riesce a trovare l'Oggetto Noto nel vettore della Sorgente.");
            System.out.println("   [Diagnostica] μ=" + String.format("%.4f", mean) +
                    ", σ=" + String.format("%.4f", stdDev) +
                    ", soglia=" + SIGMA_THRESHOLD + "σ (" +
                    String.format("%.4f", mean + SIGMA_THRESHOLD * stdDev) + ")");
            return;
        }


        String sourceRoleLabel = fetchLabelFromWikidata(sourceRoleUri);
        System.out.printf("   -> DEDUZIONE COMPLETATA: L'oggetto è legato tramite '%s' (z=%.2fσ)\n", sourceRoleLabel, bestSigma);

        System.out.println("\n=======================================================");
        System.out.println("   STEP 2: IL PONTE ANALOGICO (Embedding + Filtro Strutturale) ");
        System.out.println("=======================================================");

        Map<String, String> allTargetProperties = extractPropertyLabels(targetResource, localModel);
        Map<String, String> targetProperties = new HashMap<>();

        System.out.println("   [FILTRO STRUTTURALE RDF] Scrematura delle proprietà incompatibili...");

        for (Map.Entry<String, String> entry : allTargetProperties.entrySet()) {
            String propUri = entry.getKey();
            String propLabel = entry.getValue();

            boolean pointsToEntity = false;
            org.apache.jena.rdf.model.StmtIterator iter = localModel.listStatements(targetResource, localModel.getProperty(propUri), (RDFNode) null);
            while (iter.hasNext()) {
                RDFNode obj = iter.next().getObject();
                if (obj.isResource() && obj.asResource().getURI() != null && obj.asResource().getURI().contains("/entity/Q")) {
                    pointsToEntity = true;
                    break;
                }
            }

            if (pointsToEntity) {
                targetProperties.put(propUri, propLabel);
            }
        }

        System.out.println("   -> Proprietà rimaste dopo il filtro strutturale: " + targetProperties.size() + " su " + allTargetProperties.size());
        System.out.println("   -> Chiedo all'Embedding di tradurre '" + sourceRoleLabel + "' tra i candidati rimasti...");

        // Soglia 0.60: sotto questa soglia il match semantico è inaffidabile
        // (evidenza empirica su casi come basin country vs place of publication,
        // score 0.55, non equivalenti).
        String targetRoleUri = translator.findSemanticEquivalent(
                sourceRoleLabel, null, targetProperties, 0.60);

        if (targetRoleUri == null) {
            System.out.println("   [ERRORE] Il traduttore non ha trovato un equivalente per '" + sourceRoleLabel + "'. Analogia fallita.");
            return;
        }
        String targetRoleLabel = fetchLabelFromWikidata(targetRoleUri);

        System.out.println("\n=======================================================");
        System.out.println("    STEP 3 & 4: PROIEZIONE E ESTRAZIONE TARGET (VSA)    ");
        System.out.println("=======================================================");

        HDVector targetRole = itemMemory.getOrGenerate(targetRoleUri);
        HDVector targetSubject = itemMemory.getOrGenerate(currentTargetUri);

        System.out.println("   -> Uso la chiave tradotta ('" + targetRoleLabel + "') per aprire il vettore bersaglio...");

        System.out.println("   -> [SIMPKIN CLEAN-UP] Recupero del ramo intermedio (Chunk)...");
        HDVector pureTargetBranch = topologicalUpdater.recoverBranch(itemMemory, currentTargetUri, targetRoleUri);

        if (pureTargetBranch == null) {
            System.out.println("\n[!] FALLIMENTO: Il ramo semantico si è perso nel rumore (Z-Score sotto soglia).");
            System.out.println("    [Logica Applicata]: " + sourceRoleLabel + " ===> " + targetRoleLabel);
            System.out.println("    [Confidenza VSA Ramo]: "
                    + String.format("%.2f", topologicalUpdater.getLastStructuralSigma()) + " σ");
            return;
        }

        System.out.println("   -> Ramo recuperato con successo! Z-Score Ramo: "
                + String.format("%.2f", topologicalUpdater.getLastStructuralSigma()) + " σ");
        System.out.println("   -> Svincolo l'oggetto dal ramo purificato...");

        // ==========================================
        // FASE 2: Estrazione dell'Oggetto dal Ramo Puro (posizione per posizione)
        // ==========================================
        Map<String, ItemMemory.ScoredMatch> candidateMap = new LinkedHashMap<>();

        // Simpkin, sez. III-A: la cardinalità e la profondità provengono
        // dall'albero costruito, non dal tentativo di leggere una radice piatta.
        for (HDVector triple : topologicalUpdater.recoverTriples(itemMemory, currentTargetUri, targetRoleUri)) {
            HDVector noisyTargetObject = triple.bind(targetSubject).bind(targetRole.permute(1)).permute(-2);
            List<ItemMemory.ScoredMatch> positionCandidates = itemMemory.cleanUpRelativeTopK(noisyTargetObject, 3);
            for (ItemMemory.ScoredMatch match : positionCandidates) {
                ItemMemory.ScoredMatch existing = candidateMap.get(match.key());
                if (existing == null || existing.sigma() < match.sigma()) {
                    candidateMap.put(match.key(), match);
                }
            }
        }

        List<ItemMemory.ScoredMatch> topCandidates = candidateMap.values().stream()
                .sorted((a, b) -> Double.compare(b.sigma(), a.sigma()))
                .limit(topK)
                .toList();

        if (!topCandidates.isEmpty()) {
            System.out.println("\n[!] ANALOGIA RISOLTA CON SUCCESSO:");
            System.out.printf("    %-15s sta a  %-15s%n", armstrongEntity.entityLabel(), apolloEntity.entityLabel());
            System.out.println("    COME");
            System.out.println();

            Set<String> candidateKeys = topCandidates.stream()
                    .map(ItemMemory.ScoredMatch::key)
                    .collect(java.util.stream.Collectors.toSet());
            Map<String, String> candidateLabels = batchFetchLabels(candidateKeys);

            int rank = 1;
            for (ItemMemory.ScoredMatch match : topCandidates) {
                String label = candidateLabels.getOrDefault(match.key(), match.key());
                System.out.printf("    #%d  %-25s sta a  %-15s    (σ = %.2f)%n",
                        rank, label, currentTargetLabel, match.sigma());
                rank++;
            }

            System.out.println("\n    [Logica Applicata]: " + sourceRoleLabel + " ===> " + targetRoleLabel);
        } else {
            System.out.println("\n[?] Fallimento nel recupero dell'oggetto finale dal Chunk.");
            System.out.println("    [Confidenza VSA Oggetto]: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
        }
    }

    private static Map<String, String> extractPropertyLabels(Resource entity, Model localGraph) {
        Set<Property> outgoingProps = new HashSet<>();
        Set<Property> incomingProps = new HashSet<>();

        localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(s -> outgoingProps.add(s.getPredicate()));
        localGraph.listStatements(null, null, entity).forEachRemaining(s -> incomingProps.add(s.getPredicate()));

        System.out.println("   [INDAGINE ONTOLOGICA] Inventario delle proprietà del target:");

        Set<String> allUris = new HashSet<>();
        for (Property prop : outgoingProps) {
            String uri = prop.getURI();
            if (isMetadata(uri) || !uri.contains("/direct/")) continue;
            allUris.add(uri);
        }
        for (Property prop : incomingProps) {
            String uri = prop.getURI();
            if (isMetadata(uri) || !uri.contains("/direct/")) continue;
            allUris.add(uri);
        }

        Map<String, String> labelCache = batchFetchLabels(allUris);
        Map<String, String> labels = new HashMap<>();

        System.out.println("   --- PROPRIETÀ IN USCITA (I Figli) ---");
        for (Property prop : outgoingProps) {
            String uri = prop.getURI();
            if (isMetadata(uri) || !uri.contains("/direct/")) continue;
            String label = labelCache.getOrDefault(uri, uri);
            labels.put(uri, label);
            System.out.println("      - [OUT] " + uri + "  --->  Label: '" + label + "'");
        }

        System.out.println("   --- PROPRIETÀ IN ENTRATA (I Genitori) ---");
        for (Property prop : incomingProps) {
            String uri = prop.getURI();
            if (isMetadata(uri) || !uri.contains("/direct/")) continue;
            if (!labels.containsKey(uri)) {
                String label = labelCache.getOrDefault(uri, uri);
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
            try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service(WIKIDATA_ENDPOINT)
                    .query(query)
                    .httpHeader("User-Agent", USER_AGENT)
                    .build()) {
                org.apache.jena.query.ResultSet results = qexec.execSelect();
                if (results.hasNext()) return results.nextSolution().getLiteral("label").getString();
            }
        } catch (Exception e) { /* Silenzioso */ }

        String[] parts = uri.split("/");
        return parts[parts.length - 1];
    }

    private static Map<String, String> batchFetchLabels(java.util.Collection<String> uris) {
        Map<String, String> result = new java.util.HashMap<>();
        if (uris == null || uris.isEmpty()) return result;

        java.util.List<String> wikidataUris = new java.util.ArrayList<>();
        for (String uri : uris) {
            if (uri != null && uri.startsWith("http://www.wikidata.org/")) {
                wikidataUris.add(uri);
            } else if (uri != null) {
                result.put(uri, uri.replace("_", " "));
            }
        }
        if (wikidataUris.isEmpty()) return result;

        java.util.List<String> normalized = new java.util.ArrayList<>();
        java.util.Map<String, String> normalizedToOriginal = new java.util.HashMap<>();
        for (String uri : wikidataUris) {
            String entityUri = uri.replace("/prop/direct-normalized/", "/entity/")
                    .replace("/prop/direct/", "/entity/")
                    .replace("/prop/statement/", "/entity/")
                    .replace("/prop/", "/entity/");
            normalized.add(entityUri);
            normalizedToOriginal.put(entityUri, uri);
        }

        String valuesClause = normalized.stream()
                .distinct()
                .map(u -> "<" + u + ">")
                .collect(java.util.stream.Collectors.joining(" "));

        String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "SELECT ?entity ?label WHERE { " +
                "  VALUES ?entity { " + valuesClause + " } " +
                "  ?entity rdfs:label ?label . " +
                "  FILTER (lang(?label) = 'en') " +
                "}";

        try {
            org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparql);
            try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service(WIKIDATA_ENDPOINT)
                    .query(query)
                    .httpHeader("User-Agent", USER_AGENT)
                    .build()) {
                org.apache.jena.query.ResultSet rs = qexec.execSelect();
                while (rs.hasNext()) {
                    org.apache.jena.query.QuerySolution soln = rs.next();
                    String entityUri = soln.getResource("entity").getURI();
                    String label = soln.getLiteral("label").getString();
                    String originalUri = normalizedToOriginal.getOrDefault(entityUri, entityUri);
                    result.put(originalUri, label);
                }
            }
        } catch (Exception e) {
            System.err.println("   [BATCH LABELS] Fallito fetch: " + e.getMessage());
        }

        for (String uri : uris) {
            if (!result.containsKey(uri)) {
                String[] parts = uri.split("/");
                result.put(uri, parts[parts.length - 1]);
            }
        }
        return result;
    }

    private static boolean isMetadata(String uri) {
        // Proprietà P-numeriche note come metadata (immagini, ID di autorità)
        if (uri.contains("P18") || uri.contains("P373") || uri.contains("P2002")
                || uri.contains("P2013") || uri.contains("P137")) {
            return true;
        }
        // Namespace non semantici
        if (uri.startsWith("http://www.w3.org/2000/01/rdf-schema#")) return true;
        if (uri.startsWith("http://schema.org/")) return true;
        if (uri.startsWith("http://wikiba.se/")) return true;
        if (uri.startsWith("http://www.w3.org/2004/02/skos/core#")) return true;
        // Forme non-dirette di Wikidata (statement form, qualificatori, normalizzate)
        if (uri.contains("/prop/direct-normalized/")) return true;
        if (uri.contains("/prop/statement/")) return true;
        if (uri.contains("/prop/P") && !uri.contains("/prop/direct/")) return true;
        return false;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new HashMap<>();
        opts.put("source", "Amleto");
        opts.put("known", "William Shakespeare");
        opts.put("target", "Lacrimosa");
        opts.put("candidates", "5");
        opts.put("top", "3");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--source" -> { if (i + 1 < args.length) opts.put("source", args[++i]); }
                case "--known" -> { if (i + 1 < args.length) opts.put("known", args[++i]); }
                case "--target" -> { if (i + 1 < args.length) opts.put("target", args[++i]); }
                case "--candidates" -> { if (i + 1 < args.length) opts.put("candidates", args[++i]); }
                case "--top" -> { if (i + 1 < args.length) opts.put("top", args[++i]); }
                case "--help", "-h" -> { printUsage(); System.exit(0); }
                default -> {
                    System.err.println("Argomento sconosciuto: " + args[i]);
                    printUsage();
                    System.exit(1);
                }
            }
        }
        return opts;
    }

    private static void printUsage() {
        System.out.println("Neuro-Semantic Investigator");
        System.out.println("Uso: App [--source <nome>] [--known <nome>] [--target <nome>]");
        System.out.println("         [--candidates <n>] [--top <n>] [--help]");
        System.out.println();
        System.out.println("  --source      Entità sorgente (default: Amleto)");
        System.out.println("  --known       Entità nota (default: William Shakespeare)");
        System.out.println("  --target      Entità target (default: Lacrimosa)");
        System.out.println("  --candidates  Candidati per disambiguazione target (default: 5)");
        System.out.println("  --top         Numero di risultati finali da mostrare (default: 3)");
        System.out.println("  --help, -h    Mostra questo messaggio");
    }
}
