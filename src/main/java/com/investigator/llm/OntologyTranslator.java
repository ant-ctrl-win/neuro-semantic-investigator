package com.investigator.llm;

import com.investigator.jena.ResolvedEntity;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OntologyTranslator {

    private final EmbeddingModel llm;
    private final Map<String, String> expectedTypeCache = new ConcurrentHashMap<>();

    public OntologyTranslator() {
        this.llm = new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Arricchisce una mappa URI→label con il tipo atteso della proprietà (range constraint),
     * per migliorare il matching embedding. Es. "composer [expects: human]" vs "author [expects: human]".
     */
    public Map<String, String> enrichLabelsWithExpectedTypes(Map<String, String> uriToLabel) {
        List<String> uris = new ArrayList<>(uriToLabel.keySet());
        Map<String, String> fetched = batchFetchExpectedTypes(uris);
        expectedTypeCache.putAll(fetched);

        Map<String, String> enriched = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : uriToLabel.entrySet()) {
            String uri = entry.getKey();
            String label = entry.getValue();
            String expected = expectedTypeCache.getOrDefault(uri, "");
            if (!expected.isEmpty()) {
                enriched.put(uri, label + " [expects: " + expected + "]");
            } else {
                enriched.put(uri, label);
            }
        }
        return enriched;
    }

    private Map<String, String> batchFetchExpectedTypes(List<String> propertyUris) {
        if (propertyUris.isEmpty()) return Collections.emptyMap();

        String valuesClause = propertyUris.stream()
                .map(uri -> "<" + uri + ">")
                .collect(Collectors.joining(" "));

        String sparql =
                "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
                "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
                "PREFIX p: <http://www.wikidata.org/prop/>\n" +
                "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n" +
                "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT ?prop ?expectedTypeLabel WHERE {\n" +
                "  VALUES ?prop { " + valuesClause + " }\n" +
                "  OPTIONAL {\n" +
                "    ?prop p:P2302 ?stmt.\n" +
                "    ?stmt ps:P2302 wd:Q21510865.\n" +
                "    ?stmt pq:P2308 ?expectedType.\n" +
                "    ?expectedType rdfs:label ?expectedTypeLabel. FILTER(lang(?expectedTypeLabel)='en')\n" +
                "  }\n" +
                "}";

        Map<String, String> result = new LinkedHashMap<>();
        try {
            org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparql);
            try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
                    .query(query)
                    .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
                    .build()) {
                org.apache.jena.query.ResultSet rs = qexec.execSelect();
                while (rs.hasNext()) {
                    org.apache.jena.query.QuerySolution soln = rs.next();
                    String propUri = soln.getResource("prop").getURI();
                    String typeLabel = soln.contains("expectedTypeLabel")
                            ? soln.getLiteral("expectedTypeLabel").getString() : "";
                    result.put(propUri, typeLabel);
                }
            }
        } catch (Exception e) {
            System.err.println("   [BATCH TYPE] Fallito fetch: " + e.getMessage());
        }
        return result;
    }

    public String findSemanticEquivalent(String sourceConcept, Map<String, String> targetProperties, double threshold) {
        return findSemanticEquivalent(sourceConcept, null, targetProperties, threshold);
    }

    /**
     * Trova la proprietà target semanticamente più simile al concetto sorgente.
     * Se sourcePropertyUri è fornito, arricchisce il concetto con il suo tipo atteso.
     */
    public String findSemanticEquivalent(String sourceConcept, String sourcePropertyUri,
                                         Map<String, String> targetProperties, double threshold) {
        String enrichedSource = sourceConcept;
        if (sourcePropertyUri != null && expectedTypeCache.containsKey(sourcePropertyUri)) {
            String srcType = expectedTypeCache.get(sourcePropertyUri);
            if (!srcType.isEmpty()) {
                enrichedSource = sourceConcept + " [expects: " + srcType + "]";
            }
        }

        System.out.println("\n   [LLM] Analisi semantica per: '" + enrichedSource + "'...");
        Embedding sourceEmbedding = llm.embed(enrichedSource).content();

        double bestScore = -1.0;
        String bestUriMatch = null;
        String bestLabelMatch = null;

        for (Map.Entry<String, String> entry : targetProperties.entrySet()) {
            String targetUri = entry.getKey();
            String targetLabel = entry.getValue();

            if (targetLabel == null || targetLabel.isEmpty()) continue;

            Embedding targetEmbedding = llm.embed(targetLabel).content();
            double similarity = CosineSimilarity.between(sourceEmbedding, targetEmbedding);

            if (similarity > 0.3) {
                System.out.printf("      -> Confronto con '%s': Similarità %.3f\n", targetLabel, similarity);
            }

            if (similarity > bestScore) {
                bestScore = similarity;
                bestUriMatch = targetUri;
                bestLabelMatch = targetLabel;
            }
        }

        if (bestScore >= threshold) {
            System.out.printf("\n   [LLM] -> MATCH VINCENTE! '%s' è l'equivalente di '%s' (Score: %.2f)\n",
                    bestLabelMatch, enrichedSource, bestScore);
            return bestUriMatch;
        } else {
            System.out.printf("\n   [LLM] -> NESSUN MATCH SUFFICIENTE. Miglior candidato era '%s' (Score: %.2f, Soglia: %.2f)\n",
                    bestLabelMatch, bestScore, threshold);
            return null;
        }
    }

    /**
     * Esegue un Semantic Reranking su una lista di candidati, basato sulla classe ontologica desiderata.
     */
    /**
     * Confronta due classi ontologiche usando embedding semantico invece di uguaglianza stringale.
     * Es. "tragedy" vs "musical composition" sono entrambe creative works e avranno similarità > soglia.
     */
    public boolean areClassesCompatible(String classLabelA, String classLabelB, double threshold) {
        if (classLabelA == null || classLabelB == null) return true;
        Embedding embA = llm.embed(classLabelA).content();
        Embedding embB = llm.embed(classLabelB).content();
        double sim = CosineSimilarity.between(embA, embB);
        System.out.printf("   [COMPAT CHECK] '%s' vs '%s' -> Similarità: %.3f (Soglia: %.2f)%n",
                classLabelA, classLabelB, sim, threshold);
        return sim >= threshold;
    }

    public ResolvedEntity disambiguateCandidates(String targetClassLabel, List<ResolvedEntity> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (targetClassLabel == null) return candidates.get(0);

        System.out.println("   [RERANKER] Disambiguazione di " + candidates.size()
                + " candidati (class similarity 60% + sitelinks 40%):");

        int maxSitelinks = candidates.stream().mapToInt(ResolvedEntity::sitelinks).max().orElse(1);
        Embedding targetEmbedding = llm.embed(targetClassLabel).content();

        ResolvedEntity bestCandidate = candidates.get(0);
        double bestScore = -1.0;

        for (ResolvedEntity candidate : candidates) {
            String candidateClass = candidate.classLabel();
            if (candidateClass == null) {
                System.out.printf("      -> Salto [%s]: Classe non definita\n", candidate.entityLabel());
                continue;
            }

            Embedding candidateEmbedding = llm.embed(candidateClass).content();
            double classSim = CosineSimilarity.between(targetEmbedding, candidateEmbedding);
            double sitelinksScore = maxSitelinks > 0 ? (double) candidate.sitelinks() / maxSitelinks : 0;
            double combined = classSim * 0.60 + sitelinksScore * 0.40;

            System.out.printf("      -> [%s] Classe='%s' | ClassSim=%.3f | Sitelinks=%d (%.2f) | COMBINATO=%.3f\n",
                    candidate.entityLabel(), candidateClass, classSim,
                    candidate.sitelinks(), sitelinksScore, combined);

            if (combined > bestScore) {
                bestScore = combined;
                bestCandidate = candidate;
            }
        }

        System.out.println("   [RERANKER] -> Scelto: " + bestCandidate.entityLabel()
                + " (Combinato: " + String.format("%.3f", bestScore) + ")");
        return bestCandidate;
    }
}