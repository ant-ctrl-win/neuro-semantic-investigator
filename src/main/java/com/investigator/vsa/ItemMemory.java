package com.investigator.vsa;

import com.investigator.vsa.strategy.VectorGenerationStrategy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

public class ItemMemory {
    private final Map<String, HDVector> memory = new ConcurrentHashMap<>();
    private final VectorGenerationStrategy strategy;

    private double lastMean = 0.0;
    private double lastStdDev = 0.0;
    private double lastBestSigma = 0.0;
    private String lastBestKey = null;

    public ItemMemory(VectorGenerationStrategy strategy) {
        this.strategy = strategy;
    }

    public HDVector getOrGenerate(String uri) {
        return memory.computeIfAbsent(uri, k -> strategy.generate(uri));
    }

    public Map<String, HDVector> getAllVectors() {
        return memory;
    }

    public void updateVector(String uri, HDVector newVector) {
        if (memory.containsKey(uri)) {
            memory.put(uri, newVector);
        } else {
            throw new IllegalArgumentException("URI not found in memory: " + uri);
        }
    }

    public HDVector cleanUp(HDVector noisyVector, double threshold) {
        double bestSimilarity = -1.0;
        HDVector bestMatch = noisyVector;

        for (Map.Entry<String, HDVector> entry : memory.entrySet()) {
            HDVector candidate = entry.getValue();
            double sim = noisyVector.similarity(candidate);
            if (sim > bestSimilarity && sim > threshold) {
                bestSimilarity = sim;
                bestMatch = candidate;
            }
        }
        return bestMatch;
    }

    public HDVector cleanUpRelative(HDVector noisyVector) {
        List<Double> similarities = new ArrayList<>();
        double bestSimilarity = -1.0;
        HDVector bestMatch = noisyVector;
        String bestKey = null;

        for (Map.Entry<String, HDVector> entry : memory.entrySet()) {
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
        for (double s : similarities) {
            sum += s;
        }
        double mean = sum / n;

        double variance = 0.0;
        for (double s : similarities) {
            variance += (s - mean) * (s - mean);
        }
        variance /= n;
        double stdDev = Math.sqrt(variance);

        double sigmaFromMean = (stdDev > 0) ? (bestSimilarity - mean) / stdDev : 0;

        lastMean = mean;
        lastStdDev = stdDev;
        lastBestSigma = sigmaFromMean;
        lastBestKey = bestKey;

        if (stdDev > 0 && bestSimilarity > mean + (6 * stdDev)) {
            return bestMatch;
        }
        return noisyVector;
    }

    public double getLastMean() {
        return lastMean;
    }

    public double getLastStdDev() {
        return lastStdDev;
    }

    public double getLastBestSigma() {
        return lastBestSigma;
    }

    public String getLastBestKey() {
        return lastBestKey;
    }
}