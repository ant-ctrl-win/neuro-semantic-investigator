package com.investigator.automaton;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;
import com.investigator.vsa.ItemMemory;
import java.util.ArrayList;
import java.util.List;

public class AssociationMatrix {

    private HDVector w;
    private final ItemMemory cleanUpMemory;
    private int branchCrossCount;

    public AssociationMatrix(ItemMemory cleanUpMemory) {
        this.cleanUpMemory = cleanUpMemory;
        this.w = new HDVectorMapB();
        this.branchCrossCount = 0;
    }

    public HDVector getW() {
        return w;
    }

    public ItemMemory getCleanUpMemory() {
        return cleanUpMemory;
    }

    // NUOVO METODO: Crea la matrice W incrociando i rami (Chunk) invece delle triple!
    public void computeAnalogicalW(List<HDVector> sourceBranches, List<HDVector> targetBranches) {
        List<HDVector> crossProducts = new ArrayList<>();

        // Prodotto cartesiano macroscopico tra i rami delle due entità
        for (HDVector sBranch : sourceBranches) {
            for (HDVector tBranch : targetBranches) {
                // Leghiamo l'intera topologia del ramo Source con il ramo Target
                HDVector crossProduct = sBranch.bind(tBranch);
                crossProducts.add(crossProduct);
            }
        }

        this.branchCrossCount = crossProducts.size();

        // TRUCCO MATEMATICO: Prevenzione del Tie-Breaking Noise (Parità)
        // Se il numero di rami incrociati è pari, aggiungiamo un vettore identità neutro per rendere la somma dispari
        if (crossProducts.size() % 2 == 0) {
            // Un vettore casuale puro funge da rumore neutro che non altera le similarità, ma rompe i pareggi
            crossProducts.add(HDVectorMapB.generateRandom());
        }

        // Creiamo la matrice di associazione W con un singolo bundle simultaneo e pulito
        this.w = HDVectorMapB.bundleSimultaneous(crossProducts);
    }

    public int getCardinality() {
        return branchCrossCount;
    }
}