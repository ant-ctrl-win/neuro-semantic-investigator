package com.investigator.vsa;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class HDVectorMapB implements HDVector {
    public static final int D = 10000;
    private final byte[] values;

    public HDVectorMapB() {
        this.values = new byte[D];
    }

    public HDVectorMapB(byte[] values) {
        this.values = Arrays.copyOf(values, values.length);
    }

    public static HDVectorMapB generateRandom() {
        HDVectorMapB v = new HDVectorMapB();
        Random rnd = new Random();
        for (int i = 0; i < D; i++) {
            v.values[i] = (byte) (rnd.nextBoolean() ? 1 : -1);
        }
        return v;
    }

    public static HDVectorMapB generateSeeded(long seed) {
        HDVectorMapB v = new HDVectorMapB();
        Random rnd = new Random(seed);
        for (int i = 0; i < D; i++) {
            v.values[i] = (byte) (rnd.nextBoolean() ? 1 : -1);
        }
        return v;
    }

    public static HDVector bundleSimultaneous(List<HDVector> vectors) {
        int[] sum = new int[D];
        long localSeed = 0;

        // Calcoliamo un seed deterministico unico per questo gruppo di vettori
        for (HDVector v : vectors) {
            HDVectorMapB bv = (HDVectorMapB) v;
            localSeed ^= Arrays.hashCode(bv.values);
            for (int i = 0; i < D; i++) {
                sum[i] += bv.values[i];
            }
        }

        byte[] result = new byte[D];
        // Il random è seedato deterministicamente, quindi è stateless e riproducibile!
        Random tieBreaker = new Random(localSeed != 0 ? localSeed : 42L);

        for (int i = 0; i < D; i++) {
            if (sum[i] > 0) result[i] = 1;
            else if (sum[i] < 0) result[i] = -1;
            else result[i] = (byte) (tieBreaker.nextBoolean() ? 1 : -1);
        }
        return new HDVectorMapB(result);
    }

    @Override
    public HDVector bind(HDVector other) {
        HDVectorMapB o = (HDVectorMapB) other;
        byte[] result = new byte[D];
        for (int i = 0; i < D; i++) {
            result[i] = (byte) (this.values[i] * o.values[i]); // Hadamard product
        }
        return new HDVectorMapB(result);
    }

    @Override
    public HDVector bundle(HDVector other) {
        HDVectorMapB o = (HDVectorMapB) other;
        byte[] result = new byte[D];

        // NESSUN RANDOM NECESSARIO
        for (int i = 0; i < D; i++) {
            int sum = this.values[i] + o.values[i];
            if (sum > 0) {
                result[i] = 1;
            } else if (sum < 0) {
                result[i] = -1;
            } else {
                // Tie-breaker posizionale puro e stateless
                result[i] = (byte) ((i % 2 == 0) ? 1 : -1);
            }
        }
        return new HDVectorMapB(result);
    }

    @Override
    public HDVector permute(int shift) {
        byte[] result = new byte[D];
        for (int i = 0; i < D; i++) {
            int newIndex = (i + shift) % D;
            if (newIndex < 0) newIndex += D;
            result[newIndex] = this.values[i];
        }
        return new HDVectorMapB(result);
    }

    @Override
    public double similarity(HDVector other) {
        HDVectorMapB o = (HDVectorMapB) other;
        int dotProduct = 0;
        for (int i = 0; i < D; i++) {
            dotProduct += this.values[i] * o.values[i];
        }
        return (double) dotProduct / D;
    }
}