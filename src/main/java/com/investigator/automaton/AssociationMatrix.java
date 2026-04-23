package com.investigator.automaton;

import com.investigator.vsa.*;
import java.util.ArrayList;
import java.util.List;

public class AssociationMatrix {

    private HDVector w;
    private final ItemMemory cleanUpMemory;
    private int crossProductCount;

    public AssociationMatrix(ItemMemory cleanUpMemory) {
        this.cleanUpMemory = cleanUpMemory;
        this.w = new HDVectorMapB();
    }

    /**
     * Calcola la matrice W gerarchica.
     * Invece di incrociare triple atomiche, incrociamo i macro-rami (Contextual Branches).
     */
    public void computeHierarchicalW(List<HDVector> sourceBranches, List<HDVector> targetBranches) {
        List<HDVector> crossProducts = new ArrayList<>();

        // 1. PRODOTTO CARTESIANO DEI RAMI (Simpkin L1)
        // Questo codifica la compatibilità tra i "temi" semantici delle due entità
        for (HDVector sBranch : sourceBranches) {
            for (HDVector tBranch : targetBranches) {
                // Binding dei rami: se i rami sono simili, il prodotto sarà vicino all'identità
                // in certi spazi, permettendo la risonanza di Gayler.
                crossProducts.add(sBranch.bind(tBranch));
            }
        }

        this.crossProductCount = crossProducts.size();

        // 2. PARITY FIX & BUNDLING
        // Fondamentale in MAP-B per evitare che la funzione segno (signum) collassi su zero
        if (crossProducts.isEmpty()) {
            this.w = new HDVectorMapB(); // Matrice vuota (nessuna associazione)
            return;
        }

        if (crossProducts.size() % 2 == 0) {
            // Aggiungiamo un vettore random per rompere la simmetria dei pareggi
            crossProducts.add(HDVectorMapB.generateRandom());
        }

        // Il bundle simultaneo crea la superposizione di tutte le "ipotesi di mapping"
        this.w = HDVectorMapB.bundleSimultaneous(crossProducts);
    }

    public HDVector getW() {
        return w;
    }

    public int getCrossProductCount() {
        return crossProductCount;
    }
}