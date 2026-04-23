package com.investigator.automaton;

import com.investigator.vsa.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AnalogyProjector {

    private final ItemMemory targetMemory;

    public AnalogyProjector(ItemMemory targetMemory) {
        this.targetMemory = targetMemory;
    }

    public HDVector project(HDVector sourceVector, HDVector stabilizedState) {
        // T_discovery = x_t ⊗ S_query
        return stabilizedState.bind(sourceVector);
    }

    /**
     * Esegue la proiezione forzando la ricerca solo tra i candidati del dominio Target.
     * Risolve il problema dell'identità sorgente che "inquina" il risultato.
     */
//    public ProjectionResult discoverInTarget(HDVector sourceVector, HDVector stabilizedState, Set<String> targetCandidates) {
//        HDVector projected = project(sourceVector, stabilizedState);
//
//        double bestSim = -1.0;
//        String bestKey = "UNKNOWN";
//        List<Double> similarities = new ArrayList<>();
//
//        // Scansioniamo solo i nodi che appartengono al grafo Target
//        for (String uri : targetCandidates) {
//            HDVector candidate = targetMemory.getOrGenerate(uri);
//            double sim = projected.similarity(candidate);
//            similarities.add(sim);
//
//            if (sim > bestSim) {
//                bestSim = sim;
//                bestKey = uri;
//            }o
//        }

    public ProjectionResult discoverInTarget(String sourceUri, HDVector stabilizedState, Set<String> targetCandidates) {
        HDVector sourceVector = targetMemory.getOrGenerate(sourceUri);
        HDVector projected = project(sourceVector, stabilizedState);

        double bestSim = -1.0;
        String bestKey = "UNKNOWN";
        List<Double> similarities = new ArrayList<>();

        for (String uri : targetCandidates) {
            // REGOLA D'ORO: Un'entità non può essere l'analogo di se stessa
            if (uri.equals(sourceUri)) continue;

            HDVector candidate = targetMemory.getOrGenerate(uri);
            double sim = projected.similarity(candidate);
            similarities.add(sim);

            if (sim > bestSim) {
                bestSim = sim;
                bestKey = uri;
            }
        }
        // Calcolo dello Z-Score relativo al solo set di candidati
        double mean = similarities.stream().mapToDouble(d -> d).average().orElse(0.0);
        double variance = similarities.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        double sigma = (stdDev > 0) ? (bestSim - mean) / stdDev : 0;

        return new ProjectionResult(bestKey, sigma, null);
    }

    // Metodo legacy per ricerca globale (opzionale)
    public ProjectionResult discover(HDVector sourceVector, HDVector stabilizedState) {
        HDVector projected = project(sourceVector, stabilizedState);
        HDVector bestMatch = targetMemory.cleanUpRelative(projected);
        return new ProjectionResult(targetMemory.getLastBestKey(), targetMemory.getLastBestSigma(), bestMatch);
    }

    public static class ProjectionResult {
        public final String uri;
        public final double confidenceSigma;
        public final HDVector vector;

        public ProjectionResult(String uri, double confidenceSigma, HDVector vector) {
            this.uri = uri;
            this.confidenceSigma = confidenceSigma;
            this.vector = vector;
        }

        @Override
        public String toString() {
            String label = uri.contains("/") ? uri.substring(uri.lastIndexOf("/") + 1) : uri;
            return String.format("Discovery: %s (Confidence: %.2f σ)", label, confidenceSigma);
        }
    }
}