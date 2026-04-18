package com.investigator.automaton;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.ItemMemory;
import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.engine.InvestigationEngine;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class CellularAutomaton {

    private HDVector x_t;
    private final AssociationMatrix associationMatrix;
    private final GaylerIntersection gaylerIntersection;
    private final InvestigationEngine engine;

    private static final double ACTIVATION_THRESHOLD = 2.5;

    public CellularAutomaton(
            HDVector initialState,
            InvestigationEngine engine,
            ItemMemory cleanUpMemory) {
        this.x_t = initialState;
        this.engine = engine;
        this.associationMatrix = new AssociationMatrix(cleanUpMemory);
        this.gaylerIntersection = new GaylerIntersection(cleanUpMemory);
    }

    public void step() {
        HDVector pi_t = x_t.bind(associationMatrix.getW());

        HDVector z_t = gaylerIntersection.intersect(x_t, pi_t);

        x_t = gaylerIntersection.cleanUp(z_t);
    }

    public void addTripleVector(HDVector tripleVector) {
        associationMatrix.accumulate(tripleVector);
    }

    public void addTriple(Statement stmt) {
        Resource s = stmt.getSubject();
        Resource p = stmt.getPredicate();
        org.apache.jena.rdf.model.RDFNode o = stmt.getObject();

        ItemMemory mem = associationMatrix.getCleanUpMemory();

        HDVector vS = mem.getOrGenerate(s.getURI());
        HDVector vP = mem.getOrGenerate(p.getURI());
        HDVector vO = o.isResource() ?
                mem.getOrGenerate(o.asResource().getURI()) :
                mem.getOrGenerate(o.asLiteral().getString());

        HDVector tripleVector = vS
                .bind(vP.permute(1))
                .bind(vO.permute(2));

        associationMatrix.accumulate(tripleVector);
    }

    public void runInvestigationStep() {
        Set<Resource> visited = new HashSet<>();
        for (var nodeEntry : engine.getGraphState().entrySet()) {
            visited.add(nodeEntry.getKey());
        }

        Set<Resource> unexplored = engine.getGraphManager().getUnexploredNodes(visited);

        for (Resource node : unexplored) {
            engine.expandAndProcess(node);
        }

        for (var nodeEntry : engine.getGraphState().entrySet()) {
            var node = nodeEntry.getValue();
            double resonance = node.getCurrentVector().similarity(x_t);

            if (resonance > 0) {
                node.addEnergy(resonance);
            } else {
                node.reduceEnergy(0.1);
            }

            if (node.getEnergy() > ACTIVATION_THRESHOLD) {
                engine.expandAndProcess(node.getJenaResource());
                node.resetEnergy();
            }
        }

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