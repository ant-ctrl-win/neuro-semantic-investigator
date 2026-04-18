package com.investigator.automaton;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;
import com.investigator.vsa.ItemMemory;
import java.util.List;

public class AssociationMatrix {

    private HDVector w;
    private final ItemMemory cleanUpMemory;
    private int tripleCount;

    public AssociationMatrix(ItemMemory cleanUpMemory) {
        this.cleanUpMemory = cleanUpMemory;
        this.w = new HDVectorMapB();
        this.tripleCount = 0;
    }

    public void accumulate(HDVector tripleVector) {
        if (tripleCount == 0) {
            w = tripleVector;
        } else {
            w = w.bundle(tripleVector);
        }
        tripleCount++;
    }

    public HDVector getW() {
        return w;
    }

    public int getCardinality() {
        return tripleCount;
    }

    public ItemMemory getCleanUpMemory() {
        return cleanUpMemory;
    }

    public void computeAnalogicalW(List<HDVector> sourceTriples, List<HDVector> targetTriples) {
        HDVector w_acc = null;
        int crossCount = 0;

        for (HDVector sTriple : sourceTriples) {
            for (HDVector tTriple : targetTriples) {
                HDVector crossProduct = sTriple.bind(tTriple);
                if (w_acc == null) {
                    w_acc = crossProduct;
                } else {
                    w_acc = w_acc.bundle(crossProduct);
                }
                crossCount++;
            }
        }

        if (w_acc != null) {
            this.w = w_acc;
            this.tripleCount = crossCount;
        }
    }
}