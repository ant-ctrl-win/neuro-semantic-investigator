package com.investigator.automaton;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;
import com.investigator.vsa.ItemMemory;

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
}