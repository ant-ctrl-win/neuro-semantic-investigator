package com.investigator.engine;

import org.apache.jena.rdf.model.*;
import com.investigator.vsa.*;
import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import com.investigator.automaton.SemanticNode;
import com.investigator.jena.GraphManager;
import com.investigator.jena.TripleExtractor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class InvestigationEngine {
    private final ItemMemory itemMemory;
    private final Map<Resource, SemanticNode> graphState;
    private final HDVector globalContextVector;
    private final GraphManager graphManager;
    private final TopologicalVectorUpdater topologicalUpdater;
    private final double ACTIVATION_THRESHOLD = 2.5;

    private final List<HDVector> extractedNodeVectors = new ArrayList<>();
    private final List<HDVector> extractedTripleVectors = new ArrayList<>();

    public InvestigationEngine(HDVector contextTarget, GraphManager graphManager) {
        this.globalContextVector = contextTarget;
        this.itemMemory = new ItemMemory(new RandomGenerationStrategy());
        this.graphState = new ConcurrentHashMap<>();
        this.graphManager = graphManager;
        this.topologicalUpdater = new TopologicalVectorUpdater();
    }

    public InvestigationEngine(HDVector contextTarget, GraphManager graphManager, ItemMemory itemMemory) {
        this.globalContextVector = contextTarget;
        this.itemMemory = itemMemory;
        this.graphState = new ConcurrentHashMap<>();
        this.graphManager = graphManager;
        this.topologicalUpdater = new TopologicalVectorUpdater();
    }

    public InvestigationEngine(HDVector contextTarget, GraphManager graphManager,
                              ItemMemory itemMemory, TopologicalVectorUpdater topologicalUpdater) {
        this.globalContextVector = contextTarget;
        this.itemMemory = itemMemory;
        this.graphState = new ConcurrentHashMap<>();
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

        SemanticNode nodeS = graphState.putIfAbsent(s, new SemanticNode(s, vS));
        if (nodeS == null) {
            extractedNodeVectors.add(vS);
        }

        if (o.isResource()) {
            SemanticNode nodeO = graphState.putIfAbsent(o.asResource(), new SemanticNode(o.asResource(), vO));
            if (nodeO == null) {
                extractedNodeVectors.add(vO);
            }
        }

        extractedTripleVectors.add(tripleVector);
    }

    public void runCellularAutomatonStep() {
        for (SemanticNode node : graphState.values()) {
            double resonance = node.getCurrentVector().similarity(globalContextVector);

            if (resonance > 0) {
                node.addEnergy(resonance);
            } else {
                node.reduceEnergy(0.1);
            }

            if (node.getEnergy() > ACTIVATION_THRESHOLD) {
                System.out.println("Nodo attivato! Innesco esplorazione per: " + node.getJenaResource().getURI());
                expandAndProcess(node.getJenaResource());
                node.resetEnergy();
            }
        }
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

    public Map<Resource, SemanticNode> getGraphState() {
        return graphState;
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