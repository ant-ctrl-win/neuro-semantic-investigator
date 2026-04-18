package com.investigator.vsa.strategy;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;
import com.investigator.vsa.ItemMemory;
import org.apache.jena.rdf.model.*;
import java.util.*;

public class TopologicalVectorUpdater {

    private static final String DEBUG_NODE = "http://www.wikidata.org/entity/Q23398";
    private static final String DEBUG_PROPERTY = "http://www.wikidata.org/prop/direct/P17";

    public void applyTopologicalUpdate(ItemMemory memory, Model localGraph, Set<Resource> updatedNodes) {
        for (Resource node : updatedNodes) {
            if (node.isURIResource()) {
                updateNodeVector(memory, node, localGraph);
            }
        }
    }

    private void updateNodeVector(ItemMemory memory, Resource node, Model model) {
        String uri = node.getURI();
        boolean isDebugNode = uri.equals(DEBUG_NODE);

        if (isDebugNode) {
            System.out.println("[DEBUG] ========== TOPOLOGICAL UPDATE ==========");
            System.out.println("[DEBUG] Nodo: " + uri);
        }

        HDVector currentVector = memory.getOrGenerate(uri);
        List<HDVector> outgoingConnections = new ArrayList<>();
        List<HDVector> incomingConnections = new ArrayList<>();

        StmtIterator outIter = model.listStatements(node, null, (RDFNode) null);
        while (outIter.hasNext()) {
            Statement stmt = outIter.next();
            String predUri = stmt.getPredicate().getURI();
            if (!predUri.contains("/prop/direct/")) {
                continue;
            }
            HDVector vP = memory.getOrGenerate(predUri);
            HDVector connection;
            if (stmt.getObject().isResource()) {
                HDVector vO = memory.getOrGenerate(stmt.getObject().asResource().getURI());
                connection = vP.permute(1).bind(vO.permute(2));
                outgoingConnections.add(connection);
                outgoingConnections.add(connection);
                outgoingConnections.add(connection);
            } else {
                HDVector vLit = memory.getOrGenerate(stmt.getObject().asLiteral().getString());
                connection = vP.permute(1).bind(vLit.permute(2));
                outgoingConnections.add(connection);
            }

            if (isDebugNode) {
                System.out.println("[DEBUG]   OUTGOING: " + stmt.getPredicate().getURI());
                if (stmt.getPredicate().getURI().equals(DEBUG_PROPERTY)) {
                    System.out.println("[DEBUG]   *** TROVATO P17 (country)! ***");
                }
            }
        }
        outIter.close();

        StmtIterator inIter = model.listStatements(null, null, node);
        while (inIter.hasNext()) {
            Statement stmt = inIter.next();
            if (stmt.getSubject().isURIResource()) {
                HDVector vS = memory.getOrGenerate(stmt.getSubject().getURI());
                HDVector vP = memory.getOrGenerate(stmt.getPredicate().getURI());
                HDVector connection = vS.bind(vP.permute(1));
                incomingConnections.add(connection);

                if (isDebugNode) {
                    System.out.println("[DEBUG]   INCOMING: " + stmt.getSubject().getURI() + " -> " + stmt.getPredicate().getURI());
                    System.out.println("[DEBUG]   Bundling Connection: P(perm1)=" + stmt.getPredicate().getURI() + " ⊗ O(perm2)=" + stmt.getSubject().getURI());
                }
            }
        }
        inIter.close();

        if (isDebugNode) {
            System.out.println("[DEBUG]   Outgoing connections: " + outgoingConnections.size());
            System.out.println("[DEBUG]   Incoming connections: " + incomingConnections.size());
            System.out.println("[DEBUG]   Vettori totali da bundlare: " + (1 + outgoingConnections.size() + incomingConnections.size()));
        }

        List<HDVector> allForBundle = new ArrayList<>();
        allForBundle.add(currentVector);
        allForBundle.addAll(outgoingConnections);
        allForBundle.addAll(incomingConnections);

        HDVector neighborhoodVector = HDVectorMapB.bundleSimultaneous(allForBundle);
        memory.updateVector(uri, neighborhoodVector);

        if (isDebugNode) {
            System.out.println("[DEBUG]   Updated vector stored for: " + uri);
            System.out.println("[DEBUG] ========== END TOPOLOGICAL UPDATE ==========");
        }
    }
}