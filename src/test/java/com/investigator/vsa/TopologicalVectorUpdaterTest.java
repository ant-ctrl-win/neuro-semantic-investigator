package com.investigator.vsa;

import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TopologicalVectorUpdaterTest {
    private static final String SUBJECT = "urn:test:node";

    @Test
    void sameNodeProducesSameVectorOnConsecutiveBuilds() {
        Model model = modelWithProperties(100);
        Resource node = model.getResource(SUBJECT);
        TopologicalVectorUpdater updater = new TopologicalVectorUpdater();
        ItemMemory memory = new ItemMemory(new RandomGenerationStrategy());

        updater.applyTopologicalUpdate(memory, model, Set.of(node));
        HDVector first = memory.getTreeVector(SUBJECT);
        updater.applyTopologicalUpdate(memory, model, Set.of(node));
        HDVector second = memory.getTreeVector(SUBJECT);

        // Simpkin, eq. 5: ruoli, ordine e StopVec devono dare lo stesso albero.
        assertEquals(1.0, first.similarity(second));
    }

    @Test
    void hundredPropertiesAreChunkedAndKnownBranchesRecovered() {
        Model model = modelWithProperties(100);
        Resource node = model.getResource(SUBJECT);
        // Simpkin, sez. III e fig. 1: capacità 5 forza più livelli ricorsivi.
        for (int capacity : new int[]{30, 5}) {
            TopologicalVectorUpdater updater = new TopologicalVectorUpdater(capacity);
            ItemMemory memory = new ItemMemory(new RandomGenerationStrategy());
            updater.applyTopologicalUpdate(memory, model, Set.of(node));
            for (int index : new int[]{0, 49, 99}) {
                String predicate = "urn:test:property:" + String.format("%03d", index);
                HDVector branch = updater.recoverBranch(memory, SUBJECT, predicate);
                assertNotNull(branch, "Ramo non recuperato: " + predicate + ", capacità " + capacity);
                HDVector triple = updater.decodeChunkElement(branch, 0, memory);
                HDVector object = triple.bind(memory.getOrGenerate(SUBJECT))
                        .bind(memory.getOrGenerate(predicate).permute(1)).permute(-2);
                assertTrue(object.similarity(memory.getOrGenerate("urn:test:object:" + index)) > 0.25,
                        "Oggetto perso nel ramo " + predicate);
            }
        }
        assertEquals(89, TopologicalVectorUpdater.capacityForSigma(3));
    }

    private Model modelWithProperties(int count) {
        Model model = ModelFactory.createDefaultModel();
        Resource node = model.createResource(SUBJECT);
        for (int i = count - 1; i >= 0; i--) {
            node.addProperty(model.createProperty("urn:test:property:" + String.format("%03d", i)),
                    model.createResource("urn:test:object:" + i));
        }
        return model;
    }
}
