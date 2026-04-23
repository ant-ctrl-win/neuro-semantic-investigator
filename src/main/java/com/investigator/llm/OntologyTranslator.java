package com.investigator.llm;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;

import java.util.Map;

public class OntologyTranslator {

    private final EmbeddingModel llm;

    public OntologyTranslator() {
        // Usiamo il modello leggero in RAM da 384 dimensioni
        this.llm = new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Trova la proprietà target semanticamente più simile al concetto sorgente.
     * * @param sourceConcept Il significato che stiamo cercando (es. "crew member")
     * @param targetProperties Mappa delle proprietà del target <URI, Etichetta>
     * @param threshold Soglia minima di similarità del coseno (es. 0.6)
     * @return L'URI della proprietà target corrispondente, o null se non c'è match.
     */
//    public String findSemanticEquivalent(String sourceConcept, Map<String, String> targetProperties, double threshold) {
//        System.out.println("   [LLM] Analisi semantica per il concetto: '" + sourceConcept + "'...");
//        Embedding sourceEmbedding = llm.embed(sourceConcept).content();
//
//        double bestScore = -1.0;
//        String bestUriMatch = null;
//        String bestLabelMatch = null;
//
//        for (Map.Entry<String, String> entry : targetProperties.entrySet()) {
//            String targetUri = entry.getKey();
//            String targetLabel = entry.getValue();
//
//            // Ignoriamo le etichette vuote o non valide
//            if (targetLabel == null || targetLabel.isEmpty() || targetLabel.startsWith("P")) continue;
//
//            Embedding targetEmbedding = llm.embed(targetLabel).content();
//            double similarity = CosineSimilarity.between(sourceEmbedding, targetEmbedding);
//
//            if (similarity > bestScore) {
//                bestScore = similarity;
//                bestUriMatch = targetUri;
//                bestLabelMatch = targetLabel;
//            }
//        }
//
//        if (bestScore >= threshold) {
//            System.out.printf("   [LLM] Match Trovato! '%s' è semanticamente equivalente a '%s' (Similarità: %.2f)\n",
//                    sourceConcept, bestLabelMatch, bestScore);
//            return bestUriMatch;
//        } else {
//            System.out.printf("   [LLM] Nessun equivalente trovato per '%s' (Miglior match: '%s' a %.2f)\n",
//                    sourceConcept, bestLabelMatch, bestScore);
//            return null;
//        }
//    }

    public String findSemanticEquivalent(String sourceConcept, Map<String, String> targetProperties, double threshold) {
        System.out.println("\n   [LLM] Analisi semantica per il concetto: '" + sourceConcept + "'...");
        Embedding sourceEmbedding = llm.embed(sourceConcept).content();

        double bestScore = -1.0;
        String bestUriMatch = null;
        String bestLabelMatch = null;

        for (Map.Entry<String, String> entry : targetProperties.entrySet()) {
            String targetUri = entry.getKey();
            String targetLabel = entry.getValue();

            if (targetLabel == null || targetLabel.isEmpty()) continue;

            Embedding targetEmbedding = llm.embed(targetLabel).content();
            double similarity = CosineSimilarity.between(sourceEmbedding, targetEmbedding);

            // STAMPA DEI "PENSIERI" DELL'LLM (mostra solo le somiglianze interessanti per non intasare lo schermo)
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
                    bestLabelMatch, sourceConcept, bestScore);
            return bestUriMatch;
        } else {
            System.out.printf("\n   [LLM] -> NESSUN MATCH SUFFICIENTE. Miglior candidato era '%s' (Score: %.2f, Soglia: %.2f)\n",
                    bestLabelMatch, bestScore, threshold);
            return null;
        }
    }
}