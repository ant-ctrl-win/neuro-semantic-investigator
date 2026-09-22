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

    // LA SOGLIA DINAMICA
    private final double dynamicThresholdSigma;

    public ItemMemory(VectorGenerationStrategy strategy) {
        this.strategy = strategy;

        // Calcolo della soglia dinamica basata sulle dimensioni dello spazio vettoriale.
        // A D=10.000 sarà ~4.0, a D=100.000 sarà ~5.0.
        this.dynamicThresholdSigma = Math.log10(HDVectorMapB.D);
        System.out.printf("   [ItemMemory] Inizializzata con D=%d. Soglia di clean-up calcolata: %.2f σ\n",
                HDVectorMapB.D, this.dynamicThresholdSigma);
    }

    // ================================================================
    // Accesso alla memoria atomica
    // ================================================================

    public HDVector getOrGenerate(String uri) {
        return atomicMemory.computeIfAbsent(uri, k -> strategy.generate(uri));
    }

    public Map<String, HDVector> getAllVectors() {
        return atomicMemory;
    }

    // ================================================================
    // Gestione radici (treeMemory)
    // ================================================================

    public void saveTreeVector(String uri, HDVector treeRoot) {
        treeMemory.put(uri, treeRoot);
    }

    public HDVector getTreeVector(String uri) {
        return treeMemory.getOrDefault(uri, getOrGenerate(uri));
    }

    // ================================================================
    // Gestione chunk
    // ================================================================

    public void saveChunkVector(String chunkUri, HDVector chunk) {
        chunkMemory.put(chunkUri, chunk);
    }

    // ================================================================
    // Clean-Up Statistico — record e motore
    // ================================================================

    /**
     * Un candidato emerso dal clean-up statistico.
     * @param key        URI dell'entità in memoria
     * @param vector     vettore puro corrispondente
     * @param similarity similarità coseno col vettore rumoroso
     * @param sigma      z-score della similarità rispetto alla distribuzione
     */
    public record ScoredMatch(String key, HDVector vector, double similarity, double sigma) {}

    /**
     * Motore di clean-up statistico. Restituisce TUTTI i candidati con
     * sigma >= thresholdSigma, ordinati per sigma decrescente.
     */
    private List<ScoredMatch> performStatisticalCleanUpAll(
            HDVector noisyVector, Map<String, HDVector> memoryBank, double thresholdSigma) {

        if (memoryBank.isEmpty()) {
            lastMean = 0.0;
            lastStdDev = 0.0;
            lastBestSigma = 0.0;
            lastBestKey = null;
            return List.of();
        }

        // 1. Calcola similarità per ogni candidato
        List<ScoredMatch> raw = new ArrayList<>(memoryBank.size());
        double sum = 0.0;
        for (Map.Entry<String, HDVector> entry : memoryBank.entrySet()) {
            double sim = noisyVector.similarity(entry.getValue());
            raw.add(new ScoredMatch(entry.getKey(), entry.getValue(), sim, 0.0));
            sum += sim;
        }

        // 2. Media e deviazione standard
        double mean = sum / raw.size();
        double variance = 0.0;
        for (ScoredMatch c : raw) {
            variance += (c.similarity() - mean) * (c.similarity() - mean);
        }
        variance /= raw.size();
        double stdDev = Math.sqrt(variance);

        // 3. Sigma per ciascun candidato
        List<ScoredMatch> scored = new ArrayList<>(raw.size());
        for (ScoredMatch c : raw) {
            double sigma = (stdDev > 0) ? (c.similarity() - mean) / stdDev : 0.0;
            scored.add(new ScoredMatch(c.key(), c.vector(), c.similarity(), sigma));
        }

        // 4. Ordina per sigma decrescente
        scored.sort((a, b) -> Double.compare(b.sigma(), a.sigma()));

        // 5. Aggiorna statistiche "lastBest" per retrocompatibilità
        lastMean = mean;
        lastStdDev = stdDev;
        if (!scored.isEmpty()) {
            ScoredMatch best = scored.get(0);
            lastBestKey = best.key();
            lastBestSigma = best.sigma();
        } else {
            lastBestKey = null;
            lastBestSigma = 0.0;
        }

        // 6. Filtra per soglia
        List<ScoredMatch> aboveThreshold = new ArrayList<>();
        for (ScoredMatch c : scored) {
            if (c.sigma() >= thresholdSigma) aboveThreshold.add(c);
        }
        return aboveThreshold;
    }

    /**
     * Wrapper retrocompatibile: restituisce solo il miglior match (top-1) sopra soglia,
     * oppure null se nessuno supera la soglia.
     */
    private HDVector performStatisticalCleanUp(
            HDVector noisyVector, Map<String, HDVector> memoryBank, double thresholdSigma) {
        List<ScoredMatch> all = performStatisticalCleanUpAll(noisyVector, memoryBank, thresholdSigma);
        return all.isEmpty() ? null : all.get(0).vector();
    }

    // ================================================================
    // API pubblica — Top-K
    // ================================================================

    /** Clean-up chunk: restituisce fino a k candidati ordinati per sigma. */
    public List<ScoredMatch> cleanUpChunkTopK(HDVector noisyChunk, int k) {
        List<ScoredMatch> all = performStatisticalCleanUpAll(noisyChunk, chunkMemory, this.dynamicThresholdSigma);
        return all.size() <= k ? all : new ArrayList<>(all.subList(0, k));
    }

    /** Clean-up atomico: restituisce fino a k candidati ordinati per sigma. */
    public List<ScoredMatch> cleanUpRelativeTopK(HDVector noisyVector, int k) {
        List<ScoredMatch> all = performStatisticalCleanUpAll(noisyVector, atomicMemory, this.dynamicThresholdSigma);
        return all.size() <= k ? all : new ArrayList<>(all.subList(0, k));
    }

    // ================================================================
    // API pubblica — legacy (top-1)
    // ================================================================

    public HDVector cleanUpChunk(HDVector noisyChunk) {
        return performStatisticalCleanUp(noisyChunk, chunkMemory, this.dynamicThresholdSigma);
    }

    public HDVector cleanUpRelative(HDVector noisyVector) {
        return performStatisticalCleanUp(noisyVector, atomicMemory, this.dynamicThresholdSigma);
    }

    // ================================================================
    // Getter per metriche
    // ================================================================

    public double getLastBestSigma() { return lastBestSigma; }
    public String getLastBestKey()   { return lastBestKey; }
    public double getLastMean()      { return lastMean; }
    public double getLastStdDev()    { return lastStdDev; }
}