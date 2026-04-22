package com.investigator.vsa;

import com.investigator.vsa.strategy.VectorGenerationStrategy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

public class ItemMemory {
    // 1. IL VOCABOLARIO (Memoria Atomica Pura) - MAI SOVRASCRITTA
    private final Map<String, HDVector> atomicMemory = new ConcurrentHashMap<>();

    // 2. L'ENCICLOPEDIA (Memoria Topologica) - Salva i Mega Vettori di Simpkin
    private final Map<String, HDVector> treeMemory = new ConcurrentHashMap<>();

    public static final String TREE_POSITION_URI = "vsa:internal:tree_position";
    private final VectorGenerationStrategy strategy;

    private double lastMean = 0.0;
    private double lastStdDev = 0.0;
    private double lastBestSigma = 0.0;
    private String lastBestKey = null;

    public ItemMemory(VectorGenerationStrategy strategy) {
        this.strategy = strategy;
    }

    // Recupera dal Vocabolario (Genera se non esiste)
    public HDVector getOrGenerate(String uri) {
        return atomicMemory.computeIfAbsent(uri, k -> strategy.generate(uri));
    }

    public Map<String, HDVector> getAllVectors() {
        return atomicMemory;
    }

    // --- GESTIONE ALBERO DI SIMPKIN ---
    public void saveTreeVector(String uri, HDVector treeRoot) {
        treeMemory.put(uri, treeRoot);
    }

    // Recupera l'Albero di un'entità. Se non ha un albero, restituisce il vettore atomico.
    public HDVector getTreeVector(String uri) {
        return treeMemory.getOrDefault(uri, getOrGenerate(uri));
    }

    // Metodo legacy richiesto da GaylerIntersection.java per l'automa cellulare
    public HDVector cleanUp(HDVector noisyVector, double threshold) {
        double bestSimilarity = -1.0;
        HDVector bestMatch = noisyVector;

        for (Map.Entry<String, HDVector> entry : atomicMemory.entrySet()) {
            HDVector candidate = entry.getValue();
            double sim = noisyVector.similarity(candidate);
            if (sim > bestSimilarity && sim > threshold) {
                bestSimilarity = sim;
                bestMatch = candidate;
            }
        }
        return bestMatch;
    }


    // --- CLEAN-UP STATISTICO (Scansiona SOLO il Vocabolario puro) ---
    public HDVector cleanUpRelative(HDVector noisyVector) {
        List<Double> similarities = new ArrayList<>();
        double bestSimilarity = -1.0;
        HDVector bestMatch = noisyVector;
        String bestKey = null;

        for (Map.Entry<String, HDVector> entry : atomicMemory.entrySet()) {
            HDVector candidate = entry.getValue();
            double sim = noisyVector.similarity(candidate);
            similarities.add(sim);
            if (sim > bestSimilarity) {
                bestSimilarity = sim;
                bestMatch = candidate;
                bestKey = entry.getKey();
            }
        }

        int n = similarities.size();
        double sum = 0.0;
        for (double s : similarities) { sum += s; }
        double mean = sum / n;

        double variance = 0.0;
        for (double s : similarities) { variance += (s - mean) * (s - mean); }
        variance /= n;
        double stdDev = Math.sqrt(variance);

        double sigmaFromMean = (stdDev > 0) ? (bestSimilarity - mean) / stdDev : 0;

        lastMean = mean;
        lastStdDev = stdDev;
        lastBestSigma = sigmaFromMean;
        lastBestKey = bestKey;

        // Se l'outlier è forte (> 5 sigma), restituisce la "parola" pura!
        if (stdDev > 0 && sigmaFromMean > 5.0) {
            return bestMatch;
        }
        return noisyVector;
    }

    public double getLastMean() { return lastMean; }
    public double getLastStdDev() { return lastStdDev; }
    public double getLastBestSigma() { return lastBestSigma; }
    public String getLastBestKey() { return lastBestKey; }
}