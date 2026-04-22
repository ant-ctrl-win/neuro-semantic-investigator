package com.investigator.vsa.strategy;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;
import com.investigator.vsa.ItemMemory;
import org.apache.jena.rdf.model.*;
import java.util.*;

public class TopologicalVectorUpdater {

    private static final int CHUNK_SIZE = 50; // Dimensione massima del chunk per evitare il rumore

    public void applyTopologicalUpdate(ItemMemory memory, Model localGraph, Set<Resource> updatedNodes) {
        for (Resource node : updatedNodes) {
            if (node.isURIResource()) {
                updateNodeVector(memory, node, localGraph);
            }
        }
    }

    private void updateNodeVector(ItemMemory memory, Resource node, Model model) {
        String uri = node.getURI();

        // Prendiamo l'identità pura dal Vocabolario (non la alteriamo)
        HDVector pureIdentityVector = memory.getOrGenerate(uri);

        List<HDVector> allTriples = new ArrayList<>();

        // 1. Raccogliamo tutte le connessioni in uscita (Outgoing)
        StmtIterator outIter = model.listStatements(node, null, (RDFNode) null);
        while (outIter.hasNext()) {
            Statement stmt = outIter.next();
            String predUri = stmt.getPredicate().getURI();
            if (!predUri.contains("/prop/direct/")) continue;

            HDVector vP = memory.getOrGenerate(predUri);
            HDVector connection;
            if (stmt.getObject().isResource()) {
                HDVector vO = memory.getOrGenerate(stmt.getObject().asResource().getURI());
                connection = vP.permute(1).bind(vO.permute(2));
            } else {
                HDVector vLit = memory.getOrGenerate(stmt.getObject().asLiteral().getString());
                connection = vP.permute(1).bind(vLit.permute(2));
            }
            allTriples.add(connection);
        }
        outIter.close();

        // 2. Raccogliamo tutte le connessioni in entrata (Incoming)
        StmtIterator inIter = model.listStatements(null, null, node);
        while (inIter.hasNext()) {
            Statement stmt = inIter.next();
            if (stmt.getSubject().isURIResource()) {
                HDVector vS = memory.getOrGenerate(stmt.getSubject().getURI());
                HDVector vP = memory.getOrGenerate(stmt.getPredicate().getURI());
                HDVector connection = vS.bind(vP.permute(1));
                allTriples.add(connection);
            }
        }
        inIter.close();

// 3. VECTOR CHUNKING (Dividiamo allTriples in pacchetti da max 50)
        List<HDVector> entityChunks = new ArrayList<>();

        if (allTriples.isEmpty()) {
            entityChunks.add(pureIdentityVector);
        } else {
            for (int i = 0; i < allTriples.size(); i += CHUNK_SIZE) {
                int end = Math.min(i + CHUNK_SIZE, allTriples.size());

                List<HDVector> chunkTriples = new ArrayList<>(allTriples.subList(i, end));
                chunkTriples.add(pureIdentityVector);

                // TRUCCO MATEMATICO 1: Risoluzione della Parità nei Chunk
                // Se i vettori sono in numero pari, il bundle genera 0 (pareggi) distruttivi.
                // Aggiungiamo una seconda volta l'identità per renderli dispari.
                if (chunkTriples.size() % 2 == 0) {
                    chunkTriples.add(pureIdentityVector);
                }

                HDVector chunkVector = HDVectorMapB.bundleSimultaneous(chunkTriples);
                entityChunks.add(chunkVector);
            }
        }

        // 4. SIMPKIN TREE COMPOSITION (Combiniamo i chunk nell'Albero)
        HDVector vTree = memory.getOrGenerate(ItemMemory.TREE_POSITION_URI);
        List<HDVector> treeBranches = new ArrayList<>();

        for (int i = 0; i < entityChunks.size(); i++) {
            HDVector boundChunk = entityChunks.get(i).bind(vTree.permute(i + 1));
            treeBranches.add(boundChunk);
        }

        // TRUCCO MATEMATICO 2: Risoluzione della Parità nei Rami dell'Albero
        // Anche l'albero deve avere rami dispari per non collassare il Root Vector.
        if (treeBranches.size() % 2 == 0) {
            treeBranches.add(pureIdentityVector);
        }

        HDVector rootVector = HDVectorMapB.bundleSimultaneous(treeBranches);
        // SALVIAMO L'ALBERO NELLA MEMORIA TOPOLOGICA (Salvaguardando il vettore atomico)
        memory.saveTreeVector(uri, rootVector);
    }
}