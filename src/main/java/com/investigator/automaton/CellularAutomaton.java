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

    public void initializeAnalogicalState(
            HDVector sourceTreeRoot,
            HDVector targetTreeRoot,
            List<HDVector> sourceBranches,
            List<HDVector> targetBranches) {

        this.x_t = sourceTreeRoot.bind(targetTreeRoot);
        associationMatrix.computeHierarchicalW(sourceBranches, targetBranches);
    }

    public void step() {
        // 1. Proiezione dell'evidenza strutturale
        HDVector pi_t = x_t.bind(associationMatrix.getW());

        // CORREZIONE 2: Usa l'intersezione olistica distribuita invece del bundle.
        // Questo filtra le "identità fantasma" tenendo solo i mapping supportati da W.
        x_t = gaylerIntersection.intersect(x_t, pi_t);
    }

    public void runInvestigationStep() {
        step();
    }

    public HDVector getState() {
        return x_t;
    }

    public AssociationMatrix getAssociationMatrix() {
        return associationMatrix;
    }
}