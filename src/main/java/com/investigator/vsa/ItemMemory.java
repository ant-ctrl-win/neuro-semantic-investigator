package com.investigator.vsa;

import com.investigator.vsa.strategy.VectorGenerationStrategy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

public class ItemMemory {
    // 1. IL VOCABOLARIO (Foglie) - Vettori atomici puri
    private final Map<String, HDVector> atomicMemory = new ConcurrentHashMap<>();

    // 2. LA MEMORIA DEI CHUNK (Nodi Intermedi) - Salva i Bucket Semantici puri
    private final Map<String, HDVector> chunkMemory = new ConcurrentHashMap<>();

    // 3. L'ENCICLOPEDIA (Radici) - Salva i Mega Vettori finali di Simpkin
    private final Map<String, HDVector> treeMemory = new ConcurrentHashMap<>();

    public static final String TREE_POSITION_URI = "vsa:internal:tree_position";
    private final VectorGenerationStrategy strategy;

    private double lastMean = 0.0;
    private double lastStdDev = 0.0;
    private double lastBestSigma = 0.0;
    private String lastBestKey = null;

    // LA NUOVA SOGLIA DINAMICA
    private final double dynamicThresholdSigma;

    public ItemMemory(VectorGenerationStrategy strategy) {
        this.strategy = strategy;

        // Calcolo della soglia dinamica basata sulle dimensioni dello spazio vettoriale.
        // A D=10.000 sarà ~4.0, a D=100.000 sarà ~5.0.
        this.dynamicThresholdSigma = Math.log10(HDVectorMapB.D);
        System.out.printf("   [ItemMemory] Inizializzata con D=%d. Soglia di clean-up calcolata: %.2f σ\n",
                HDVectorMapB.D, this.dynamicThresholdSigma);
    }

    public HDVector getOrGenerate(String uri) {
        return atomicMemory.computeIfAbsent(uri, k -> strategy.generate(uri));
    }

    public Map<String, HDVector> getAllVectors() {
        return atomicMemory;
    }

    // --- GESTIONE RADICI ---
    public void saveTreeVector(String uri, HDVector treeRoot) {
        treeMemory.put(uri, treeRoot);
    }

    public HDVector getTreeVector(String uri) {
        return treeMemory.getOrDefault(uri, getOrGenerate(uri));
    }

    // --- GESTIONE CHUNK (NOVITÀ) ---
    public void saveChunkVector(String chunkUri, HDVector chunk) {
        chunkMemory.put(chunkUri, chunk);
    }

    // Clean-up specifico per i livelli intermedi dell'albero
    public HDVector cleanUpChunk(HDVector noisyChunk) {
        return performStatisticalCleanUp(noisyChunk, chunkMemory, this.dynamicThresholdSigma);
    }

    // Clean-up per le foglie (vettori atomici)
    public HDVector cleanUpRelative(HDVector noisyVector) {
        return performStatisticalCleanUp(noisyVector, atomicMemory, this.dynamicThresholdSigma);
    }

    // Motore di Clean-Up Statistico Centralizzato
    private HDVector performStatisticalCleanUp(HDVector noisyVector, Map<String, HDVector> memoryBank, double thresholdSigma) {
        if (memoryBank.isEmpty()) return null;

        List<Double> similarities = new ArrayList<>();
        double bestSimilarity = -1.0;
        HDVector bestMatch = null;
        String bestKey = null;

        for (Map.Entry<String, HDVector> entry : memoryBank.entrySet()) {
            HDVector candidate = entry.getValue();
            double sim = noisyVector.similarity(candidate);
            similarities.add(sim);
            if (sim > bestSimilarity) {
                bestSimilarity = sim;
                bestMatch = candidate;
                bestKey = entry.getKey();
            }
        }

        double sum = 0.0;
        for (double s : similarities) sum += s;
        double mean = sum / similarities.size();

        double variance = 0.0;
        for (double s : similarities) variance += (s - mean) * (s - mean);
        variance /= similarities.size();
        double stdDev = Math.sqrt(variance);

        double sigmaFromMean = (stdDev > 0) ? (bestSimilarity - mean) / stdDev : 0;

        lastMean = mean;
        lastStdDev = stdDev;
        lastBestSigma = sigmaFromMean;
        lastBestKey = bestKey;

        // Se supera la soglia di emergenza statistica, restituisce il vettore puro
        if (stdDev > 0 && sigmaFromMean >= thresholdSigma) {
            return bestMatch;
        }
        return null;
    }

    // ... (metodi legacy e getter per le metriche) ...
    public double getLastBestSigma() { return lastBestSigma; }
    public String getLastBestKey() { return lastBestKey; }
}