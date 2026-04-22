package com.investigator.automaton;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.ItemMemory;
import com.investigator.engine.InvestigationEngine;
import java.util.List;

public class CellularAutomaton {

    private HDVector x_t;
    private final AssociationMatrix associationMatrix;
    private final GaylerIntersection gaylerIntersection;
    private final InvestigationEngine engine;

    public CellularAutomaton(
            HDVector initialState,
            InvestigationEngine engine,
            ItemMemory cleanUpMemory) {
        this.x_t = initialState;
        this.engine = engine;
        this.associationMatrix = new AssociationMatrix(cleanUpMemory);
        this.gaylerIntersection = new GaylerIntersection(cleanUpMemory);
    }

    // INIZIALIZZAZIONE: Usa direttamente gli Alberi di Simpkin e i loro Rami pronti
    public void initializeAnalogicalState(
            HDVector sourceTreeRoot,
            HDVector targetTreeRoot,
            List<HDVector> sourceBranches,
            List<HDVector> targetBranches) {

        // Lo stato iniziale dell'automa è l'ipotesi di mapping globale tra le due Radici
        this.x_t = sourceTreeRoot.bind(targetTreeRoot);

        // La matrice di associazione W viene costruita incrociando i rami strutturali (macro-chunk)
        associationMatrix.computeAnalogicalW(sourceBranches, targetBranches);
    }

//    // IL MOTORE MATEMATICO DI GAYLER
//    public void step() {
//        // 1. π_t = x_t ⊗ W (Calcola l'evoluzione prevista moltiplicando per la matrice W)
//        HDVector pi_t = x_t.bind(associationMatrix.getW());
//
//        // 2. Intersezione Sigma-Pi tra lo stato attuale e quello previsto
//        HDVector z_t = gaylerIntersection.intersect(x_t, pi_t);
//
//        // 3. Clean-up per forzare il vettore emergente nel dominio concettuale noto
//        x_t = gaylerIntersection.cleanUp(z_t);
//    }

    // IL MOTORE MATEMATICO DI RELAXATION
    public void step() {
        // 1. Calcola l'evoluzione prevista moltiplicando per la matrice strutturale W
        HDVector pi_t = x_t.bind(associationMatrix.getW());

        // 2. Aggiorna lo stato. In MAP-B usiamo il bundle per sommare l'ipotesi precedente
        // con la nuova evoluzione strutturale, stabilizzando la rete.
        // NON facciamo il cleanUp atomico, altrimenti distruggiamo la natura "relazionale" dello stato!
        x_t = x_t.bundle(pi_t);
    }


    public void runInvestigationStep() {
        // Esegue semplicemente lo step matematico (niente più esplorazioni di grafi qui)
        step();
    }

    public HDVector getState() {
        return x_t;
    }

    public AssociationMatrix getAssociationMatrix() {
        return associationMatrix;
    }

    public GaylerIntersection getGaylerIntersection() {
        return gaylerIntersection;
    }
}