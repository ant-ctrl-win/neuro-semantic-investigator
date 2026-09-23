package com.investigator.engine;

import org.apache.jena.rdf.model.*;
import com.investigator.vsa.*;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.jena.GraphManager;
import com.investigator.jena.TripleExtractor;
import java.util.*;
import java.util.stream.Stream;

public class InvestigationEngine {
    private final ItemMemory itemMemory;
    private final GraphManager graphManager;
    private final TopologicalVectorUpdater topologicalUpdater;

    public InvestigationEngine(HDVector contextTarget, GraphManager graphManager,
                              ItemMemory itemMemory, TopologicalVectorUpdater topologicalUpdater) {
        this.itemMemory = itemMemory;
        this.graphManager = graphManager;
        this.topologicalUpdater = topologicalUpdater;
    }

    public void processTriple(Statement stmt) {
        Resource s = stmt.getSubject();
        Property p = stmt.getPredicate();
        RDFNode o = stmt.getObject();

        HDVector vS = itemMemory.getOrGenerate(s.getURI());
        HDVector vP = itemMemory.getOrGenerate(p.getURI());
        HDVector vO = o.isResource() ?
                itemMemory.getOrGenerate(o.asResource().getURI()) :
                itemMemory.getOrGenerate(o.asLiteral().getString());
    }

    public void expandAndProcess(Resource node) {
        Set<Resource> updatedNodes = new HashSet<>();
        updatedNodes.add(node);

        graphManager.expandNodeOutgoing(node);

        Stream<Statement> neighborhood = graphManager.getNeighborhood(node);
        neighborhood.forEach(stmt -> {
            processTriple(stmt);
            updatedNodes.add(stmt.getSubject());
            if (stmt.getObject().isResource()) {
                updatedNodes.add(stmt.getObject().asResource());
            }
        });

        topologicalUpdater.applyTopologicalUpdate(itemMemory, graphManager.getLocalModel(), updatedNodes);
    }

    public GraphManager getGraphManager() {
        return graphManager;
    }
}