package com.investigator.engine;

import org.apache.jena.rdf.model.*;
import com.investigator.vsa.*;
import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.jena.GraphManager;
import com.investigator.jena.TripleExtractor;
import java.util.*;
import java.util.stream.Stream;

public class InvestigationEngine {
    private final ItemMemory itemMemory;
    private final GraphManager graphManager;
    private final TopologicalVectorUpdater topologicalUpdater;

    private final List<HDVector> extractedNodeVectors = new ArrayList<>();
    private final List<HDVector> extractedTripleVectors = new ArrayList<>();

    public InvestigationEngine(HDVector contextTarget, GraphManager graphManager) {
        this.itemMemory = new ItemMemory(new RandomGenerationStrategy());
        this.graphManager = graphManager;
        this.topologicalUpdater = new TopologicalVectorUpdater();
    }

    public InvestigationEngine(HDVector contextTarget, GraphManager graphManager, ItemMemory itemMemory) {
        this.itemMemory = itemMemory;
        this.graphManager = graphManager;
        this.topologicalUpdater = new TopologicalVectorUpdater();
    }

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

        HDVector tripleVector = vS
                .bind(vP.permute(1))
                .bind(vO.permute(2));

        extractedNodeVectors.add(vS);

        if (o.isResource()) {
            extractedNodeVectors.add(vO);
        }

        extractedTripleVectors.add(tripleVector);
    }

    public void expandAndProcess(Resource node) {
        Set<Resource> updatedNodes = new HashSet<>();
        updatedNodes.add(node);

        graphManager.expandNodeOutgoing(node);
        graphManager.expandNodeIncoming(node);

        Model gm = graphManager.getLocalModel();
        StmtIterator iter = gm.listStatements();
        int count = 0;
        while (iter.hasNext()) { iter.next(); count++; }
        iter.close();
        System.out.println("[DEBUG] LocalGraph contains " + count + " statements after expansion of " + node.getURI());

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

    public ItemMemory getItemMemory() {
        return itemMemory;
    }

    public List<HDVector> getNodeVectors() {
        return new ArrayList<>(extractedNodeVectors);
    }

    public List<HDVector> getTripleVectors() {
        return new ArrayList<>(extractedTripleVectors);
    }

    public void clearExtractedVectors() {
        extractedNodeVectors.clear();
        extractedTripleVectors.clear();
    }
}