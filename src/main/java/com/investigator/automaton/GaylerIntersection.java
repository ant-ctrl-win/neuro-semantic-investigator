package com.investigator.automaton;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;
import com.investigator.vsa.ItemMemory;
import java.util.Map;

public class GaylerIntersection {

    private static final int N_CHANNELS = 50;
    private static final double CLEANUP_THRESHOLD = 0.7;

    private final HDVector[] intersectionMemories;
    private final int[][] channelShifts;
    private final ItemMemory cleanUpMemory;

    public GaylerIntersection(ItemMemory cleanUpMemory) {
        this.cleanUpMemory = cleanUpMemory;
        this.intersectionMemories = new HDVector[N_CHANNELS];
        this.channelShifts = new int[N_CHANNELS][2];
        initializeIntersectionMemories();
    }

    private void initializeIntersectionMemories() {
        Map<String, HDVector> allVectors = cleanUpMemory.getAllVectors();

        for (int i = 0; i < N_CHANNELS; i++) {
            int[] shifts = generateChannelShifts(i);
            int p1Shift = shifts[0];
            int p2Shift = shifts[1];

            channelShifts[i] = shifts;
            HDVector C_wedge_i = null;

            for (Map.Entry<String, HDVector> entry : allVectors.entrySet()) {
                HDVector V_k = entry.getValue();

                HDVector perm1_Vk = V_k.permute(p1Shift);
                HDVector perm2_Vk = V_k.permute(p2Shift);

                HDVector bound = V_k.bind(perm1_Vk).bind(perm2_Vk);

                if (C_wedge_i == null) {
                    C_wedge_i = bound;
                } else {
                    C_wedge_i = C_wedge_i.bundle(bound);
                }
            }

            intersectionMemories[i] = C_wedge_i;
        }
    }

    private int[] generateChannelShifts(int i) {
        int base1 = 137;
        int base2 = 251;
        int mod = HDVectorMapB.D;
        int shift1 = (base1 * (i + 1)) % mod;
        int shift2 = (base2 * (i + 1)) % mod;
        if (shift1 == 0) shift1 = 1;
        if (shift2 == 0) shift2 = 2;
        if (shift1 == shift2) shift2 = (shift2 + 1) % mod;
        if (shift2 == 0) shift2 = 1;
        return new int[] { shift1, shift2 };
    }

    public HDVector intersect(HDVector X, HDVector Y) {
        HDVector result = null;

        for (int i = 0; i < N_CHANNELS; i++) {
            HDVector Z_i = sigmaPiChannel(X, Y, i);

            if (result == null) {
                result = Z_i;
            } else {
                result = result.bundle(Z_i);
            }
        }

        return cleanUpMemory.cleanUp(result, 0.1);
    }

    private HDVector sigmaPiChannel(HDVector X, HDVector Y, int channelIndex) {
        int[] shifts = channelShifts[channelIndex];
        int p1Shift = shifts[0];
        int p2Shift = shifts[1];

        HDVector T_i = X.permute(p1Shift).bind(Y.permute(p2Shift));

        HDVector Z_i = T_i.bind(intersectionMemories[channelIndex]);

        return Z_i;
    }

    public HDVector cleanUp(HDVector z) {
        Map<String, HDVector> allVectors = cleanUpMemory.getAllVectors();

        double bestSimilarity = -1.0;
        HDVector bestMatch = z;

        for (Map.Entry<String, HDVector> entry : allVectors.entrySet()) {
            HDVector candidate = entry.getValue();
            double sim = z.similarity(candidate);
            if (sim > bestSimilarity && sim > CLEANUP_THRESHOLD) {
                bestSimilarity = sim;
                bestMatch = candidate;
            }
        }

        return bestMatch;
    }

    public int[][] getChannelShifts() {
        return channelShifts;
    }

    public HDVector[] getIntersectionMemories() {
        return intersectionMemories;
    }

    public ItemMemory getCleanUpMemory() {
        return cleanUpMemory;
    }
}