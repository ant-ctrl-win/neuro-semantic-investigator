package com.investigator.vsa.strategy;

import com.investigator.vsa.*;
import org.apache.jena.rdf.model.*;
import java.util.*;
import java.util.stream.Collectors;

public class TopologicalVectorUpdater {

    private static final int MAX_CHUNK_CAPACITY = 30;
    private static final String V_STOP_URI = "vsa:internal:stop";

    public void applyTopologicalUpdate(ItemMemory memory, Model localGraph, Set<Resource> updatedNodes) {
        for (Resource node : updatedNodes) {
            if (node.isURIResource()) {
                updateNodeVectorHierarchical(memory, node, localGraph);
            }
        }
    }

    private void updateNodeVectorHierarchical(ItemMemory memory, Resource node, Model model) {
        String entityUri = node.getURI();
        HDVector pureIdentity = memory.getOrGenerate(entityUri);

        List<Statement> allStatements = model.listStatements(node, null, (RDFNode) null).toList();
        Map<Property, List<Statement>> buckets = allStatements.stream()
                .collect(Collectors.groupingBy(Statement::getPredicate));

        List<HDVector> macroBranches = new ArrayList<>();

        for (Map.Entry<Property, List<Statement>> entry : buckets.entrySet()) {
            Property predicate = entry.getKey();
            List<Statement> triples = entry.getValue();
            HDVector predicateVector = memory.getOrGenerate(predicate.getURI());

            // Costruiamo il Chunk (NON contiene più l'identità pura per evitare l'overpower)
            HDVector branchContent = (triples.size() <= MAX_CHUNK_CAPACITY) ?
                    buildSimpleChunk(triples, memory) :
                    buildRecursiveSubTree(triples, memory);

            // 1. SALVIAMO IL CHUNK PURO NELLA MEMORIA
            // Usiamo una chiave univoca: URI_Entità + URI_Predicato
            String chunkKey = entityUri + ":chunk:" + predicate.getURI();
            memory.saveChunkVector(chunkKey, branchContent);

            // 2. COSTRUIAMO L'ALBERO (Simpkin)
            // Bindiamo il Chunk al Ruolo e lo shiftiamo per impacchettarlo nel Mega-Vettore
            macroBranches.add(branchContent.bind(predicateVector).permute(100));
        }

        // AGGIUNGIAMO L'IDENTITÀ PURA *SOLO* AL LIVELLO RADICE!
        // Così il Mega-Vettore risuona con l'identità dell'entità, ma i suoi rami interni restano puliti.
        macroBranches.add(pureIdentity);

        if (macroBranches.size() % 2 == 0) {
            macroBranches.add(HDVectorMapB.generateRandom());
        }

        HDVector rootVector = HDVectorMapB.bundleSimultaneous(macroBranches);
        memory.saveTreeVector(entityUri, rootVector);
    }

    private HDVector buildSimpleChunk(List<Statement> triples, ItemMemory memory) {
        List<HDVector> vectors = new ArrayList<>();
        for (Statement s : triples) {
            vectors.add(encodeTriple(s, memory));
        }

        // Lo StopVec di Simpkin delimita semanticamente la fine del Chunk
        vectors.add(memory.getOrGenerate(V_STOP_URI));

        if (vectors.size() % 2 == 0) vectors.add(HDVectorMapB.generateRandom());
        return HDVectorMapB.bundleSimultaneous(vectors);
    }

    private HDVector buildRecursiveSubTree(List<Statement> triples, ItemMemory memory) {
        List<HDVector> subChunks = new ArrayList<>();
        for (int i = 0; i < triples.size(); i += MAX_CHUNK_CAPACITY) {
            int end = Math.min(i + MAX_CHUNK_CAPACITY, triples.size());
            List<Statement> subList = triples.subList(i, end);

            HDVector chunk = buildSimpleChunk(subList, memory);
            subChunks.add(chunk.permute(i / MAX_CHUNK_CAPACITY + 1));
        }
        if (subChunks.size() % 2 == 0) subChunks.add(HDVectorMapB.generateRandom());
        return HDVectorMapB.bundleSimultaneous(subChunks);
    }

    private HDVector encodeTriple(Statement stmt, ItemMemory memory) {
        HDVector vP = memory.getOrGenerate(stmt.getPredicate().getURI());
        HDVector vO = stmt.getObject().isResource() ?
                memory.getOrGenerate(stmt.getObject().asResource().getURI()) :
                memory.getOrGenerate(stmt.getObject().asLiteral().getString());

        // Pattern: P(1) ⊗ O(2)
        return vP.permute(1).bind(vO.permute(2));
    }
}