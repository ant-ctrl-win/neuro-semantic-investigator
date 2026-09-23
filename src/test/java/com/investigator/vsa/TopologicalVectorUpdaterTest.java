package com.investigator.vsa;

import com.investigator.vsa.strategy.RandomGenerationStrategy;
import com.investigator.vsa.strategy.TopologicalVectorUpdater;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

import java.util.List;
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

    @Test
    void branchesBelowChunkCapacityAreRecoveredWithoutDoubleBinding() {
        for (int propertyCount : new int[]{1, 5, 29}) {
            Model model = modelWithProperties(propertyCount);
            Resource node = model.getResource(SUBJECT);
            TopologicalVectorUpdater updater = new TopologicalVectorUpdater();
            ItemMemory memory = new ItemMemory(new RandomGenerationStrategy());

            updater.applyTopologicalUpdate(memory, model, Set.of(node));

            for (int index : new int[]{0, propertyCount - 1}) {
                String predicate = "urn:test:property:" + String.format("%03d", index);
                assertNotNull(updater.recoverBranch(memory, SUBJECT, predicate),
                        "Ramo diretto non recuperato con " + propertyCount + " predicati: " + predicate);
            }
        }
    }

    @Test
    void hundredObjectsAreRecoveredThroughTheRecursiveValueTree() {
        String predicate = "urn:test:property:many";
        Model model = ModelFactory.createDefaultModel();
        Resource node = model.createResource(SUBJECT);
        for (int i = 99; i >= 0; i--) {
            node.addProperty(model.createProperty(predicate), model.createResource("urn:test:many-object:" + i));
        }
        // Simpkin, sez. III-A: capacità 30 crea due livelli; capacità 5
        // forza quattro livelli e verifica che la discesa sia realmente ricorsiva.
        for (int capacity : new int[]{30, 5}) {
            TopologicalVectorUpdater updater = new TopologicalVectorUpdater(capacity);
            ItemMemory memory = new ItemMemory(new RandomGenerationStrategy());
            updater.applyTopologicalUpdate(memory, model, Set.of(node));

            for (int index : new int[]{0, 28, 29, 99}) {
                HDVector triple = updater.recoverTriple(memory, SUBJECT, predicate, index);
                assertNotNull(triple, "Tripla non recuperata alla posizione " + index
                        + " con capacità " + capacity);
                HDVector object = triple.bind(memory.getOrGenerate(SUBJECT))
                        .bind(memory.getOrGenerate(predicate).permute(1)).permute(-2);
                String expectedUri = sortedManyObjectUri(index);
                assertTrue(object.similarity(memory.getOrGenerate(expectedUri)) > 0.25,
                        "Oggetto perso alla posizione " + index + ": " + expectedUri);
            }
        }
    }

    @Test
    void recoverCombinedFatNodeFatPredicate() {
        final String fatPredicate = "urn:test:property:000";
        final int objectCount = 100;
        final int propertyCount = 30;

        Model model = ModelFactory.createDefaultModel();
        Resource node = model.createResource(SUBJECT);
        for (int i = objectCount - 1; i >= 0; i--) {
            node.addProperty(model.createProperty(fatPredicate),
                    model.createResource(sortedManyObjectUri(i)));
        }
        for (int p = 1; p < propertyCount; p++) {
            node.addProperty(model.createProperty("urn:test:property:" + String.format("%03d", p)),
                    model.createResource("urn:test:object:" + p));
        }

        // Simpkin, sez. III e fig. 1: il nodo fat costringe il raggruppamento
        // dei rami, mentre il predicato fat costringe la discesa nel value tree.
        for (int capacity : new int[]{30, 5}) {
            TopologicalVectorUpdater updater = new TopologicalVectorUpdater(capacity);
            ItemMemory memory = new ItemMemory(new RandomGenerationStrategy());
            updater.applyTopologicalUpdate(memory, model, Set.of(node));

            HDVector branch = updater.recoverBranch(memory, SUBJECT, fatPredicate);
            assertNotNull(branch, "Ramo del predicato fat non recuperato (capacità " + capacity + ")");

            for (int index : new int[]{1, 15, propertyCount - 1}) {
                String predicate = "urn:test:property:" + String.format("%03d", index);
                assertNotNull(updater.recoverBranch(memory, SUBJECT, predicate),
                        "Ramo non recuperato nel nodo fat (capacità " + capacity + "): " + predicate);
            }

            List<HDVector> triples = updater.recoverTriples(memory, SUBJECT, fatPredicate);
            assertEquals(objectCount, triples.size(),
                    "Il recupero combinato ha perso foglie (capacità " + capacity + "): ottenute "
                            + triples.size());

            for (int index : new int[]{0, objectCount / 2, objectCount - 1}) {
                HDVector object = triples.get(index).bind(memory.getOrGenerate(SUBJECT))
                        .bind(memory.getOrGenerate(fatPredicate).permute(1)).permute(-2);
                assertTrue(object.similarity(memory.getOrGenerate(sortedManyObjectUri(index))) > 0.25,
                        "Oggetto perso alla posizione " + index + " (capacità " + capacity + ")");
            }
        }
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

    private String sortedManyObjectUri(int index) {
        return java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> "urn:test:many-object:" + i)
                .sorted()
                .toList()
                .get(index);
    }
}
