package com.investigator.vsa.strategy;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;
import com.investigator.vsa.ItemMemory;
import org.apache.jena.rdf.model.*;
import java.util.*;

public class TopologicalVectorUpdater {
    private static final int DEFAULT_CHUNK_CAPACITY = 30;
    private static final String V_STOP_URI = "vsa:internal:stop";
    private static final String POSITION_URI = "vsa:internal:position:";
    private static final double MIN_STRUCTURAL_SIMILARITY = 3.0 / Math.sqrt(HDVectorMapB.D);

    private final int chunkCapacity;
    private final Map<String, BranchPath> branchPaths = new HashMap<>();
    private final Map<String, ValueNode> valueTrees = new HashMap<>();
    private double lastStructuralSigma;

    public TopologicalVectorUpdater() {
        this(DEFAULT_CHUNK_CAPACITY);
    }

    public TopologicalVectorUpdater(int chunkCapacity) {
        if (chunkCapacity < 3) throw new IllegalArgumentException("La capacità deve essere almeno 3");
        this.chunkCapacity = chunkCapacity;
    }

    // Simpkin et al. (2018), sez. III: limite di capacità BSC a 3 sigma.
    public static int capacityForSigma(double sigma) {
        if (sigma <= 0) throw new IllegalArgumentException("Sigma deve essere positivo");
        return Math.max(3, (int) Math.floor(89.0 * HDVectorMapB.D / 10_000 * 9 / (sigma * sigma)));
    }

    public void applyTopologicalUpdate(ItemMemory memory, Model localGraph, Set<Resource> updatedNodes) {
        for (Resource node : updatedNodes) {
            if (node.isURIResource()) updateNodeVectorHierarchical(memory, node, localGraph);
        }
    }

    private void updateNodeVectorHierarchical(ItemMemory memory, Resource node, Model model) {
        String entityUri = node.getURI();
        // Simpkin, fig. 1: i percorsi seguono l'albero corrente dopo ogni aggiornamento.
        branchPaths.keySet().removeIf(key -> key.startsWith(entityUri + "\u0000"));
        valueTrees.keySet().removeIf(key -> key.startsWith(entityUri + "\u0000"));
        List<Statement> statements = model.listStatements(node, null, (RDFNode) null).toList();
        Map<Property, List<Statement>> buckets = new LinkedHashMap<>();
        statements.stream().sorted(Comparator.comparing((Statement s) -> s.getPredicate().getURI())
                        .thenComparing(s -> s.getObject().toString()))
                .forEach(s -> buckets.computeIfAbsent(s.getPredicate(), key -> new ArrayList<>()).add(s));

        List<BranchNode> nodes = new ArrayList<>();
        for (Map.Entry<Property, List<Statement>> entry : buckets.entrySet()) {
            String predicateUri = entry.getKey().getURI();
            ValueNode valueTree = buildTripleTree(entry.getValue(), memory,
                    entityUri + ":value-tree:" + predicateUri);
            HDVector branch = valueTree.vector;
            valueTrees.put(treeKey(entityUri, predicateUri), valueTree);
            memory.saveChunkVector(entityUri + ":chunk:" + predicateUri, branch);
            nodes.add(new BranchNode(branch.bind(memory.getOrGenerate(predicateUri)),
                    Map.of(predicateUri, List.of())));
        }

        boolean grouped = nodes.size() + 1 > chunkCapacity;
        int level = 0;
        // Simpkin, sez. III e fig. 1: si ricombinano i rami finché ogni padre rientra nella capacità.
        while (nodes.size() + 1 > chunkCapacity) {
            List<BranchNode> parents = new ArrayList<>();
            for (int start = 0; start < nodes.size(); start += chunkCapacity - 1) {
                List<BranchNode> children = nodes.subList(start, Math.min(start + chunkCapacity - 1, nodes.size()));
                HDVector group = encodeChunk(children.stream().map(child -> child.vector).toList(), memory);
                memory.saveChunkVector(entityUri + ":branch-level:" + level + ":" + parents.size(), group);
                Map<String, List<BranchStep>> paths = new HashMap<>();
                for (int i = 0; i < children.size(); i++) {
                    for (Map.Entry<String, List<BranchStep>> path : children.get(i).paths.entrySet()) {
                        List<BranchStep> steps = new ArrayList<>();
                        steps.add(new BranchStep(i, children.get(i).vector));
                        steps.addAll(path.getValue());
                        paths.put(path.getKey(), List.copyOf(steps));
                    }
                }
                parents.add(new BranchNode(group, paths));
            }
            nodes = parents;
            level++;
        }

        List<HDVector> macroBranches = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            BranchNode branch = nodes.get(i);
            String roleUri = grouped ? entityUri + ":branch-root:" + i : branch.paths.keySet().iterator().next();
            HDVector rootTerm = grouped
                    ? branch.vector.bind(memory.getOrGenerate(roleUri))
                    : branch.vector;
            // Simpkin, sez. III-A: il ramo foglia contiene già il ruolo-predicato;
            // un secondo binding lo annullerebbe nei binary spatter codes.
            macroBranches.add(rootTerm.permute(100));
            if (grouped) {
                for (Map.Entry<String, List<BranchStep>> path : branch.paths.entrySet()) {
                    branchPaths.put(treeKey(entityUri, path.getKey()),
                            new BranchPath(roleUri, branch.vector, path.getValue()));
                }
            }
        }
        macroBranches.add(memory.getOrGenerate(entityUri));
        if (macroBranches.size() % 2 == 0) macroBranches.add(HDVectorMapB.generateRandom(macroBranches));
        memory.saveTreeVector(entityUri, HDVectorMapB.bundleSimultaneous(macroBranches));
    }

    private ValueNode buildTripleTree(List<Statement> triples, ItemMemory memory, String chunkPrefix) {
        List<ValueNode> level = new ArrayList<>();
        for (Statement triple : triples)
            level.add(new ValueNode(encodeTriple(triple, memory), List.of(), 1));
        int treeLevel = 0;
        while (level.size() > chunkCapacity - 1) {
            List<ValueNode> parents = new ArrayList<>();
            for (int start = 0; start < level.size(); start += chunkCapacity - 1) {
                List<ValueNode> children = List.copyOf(
                        level.subList(start, Math.min(start + chunkCapacity - 1, level.size())));
                HDVector vector = encodeChunk(children.stream().map(child -> child.vector).toList(), memory);
                memory.saveChunkVector(chunkPrefix + ":level:" + treeLevel + ":" + parents.size(), vector);
                parents.add(new ValueNode(vector, children,
                        children.stream().mapToInt(child -> child.leafCount).sum()));
            }
            level = parents;
            treeLevel++;
        }
        HDVector root = encodeChunk(level.stream().map(child -> child.vector).toList(), memory);
        // Simpkin, sez. III-A e fig. 1: la radice conserva i figli per rendere
        // percorribili tutti i livelli creati dalla partizione ricorsiva.
        return new ValueNode(root, List.copyOf(level), triples.size());
    }

    private HDVector encodeChunk(List<HDVector> content, ItemMemory memory) {
        List<HDVector> terms = new ArrayList<>();
        HDVector cumulativeRole = null;
        for (int i = 0; i < content.size(); i++) {
            HDVector role = memory.getOrGenerate(POSITION_URI + i);
            cumulativeRole = cumulativeRole == null ? role : cumulativeRole.bind(role);
            // Simpkin, eq. 5: Z_i^i moltiplicato per il prodotto cumulativo dei ruoli p_j.
            terms.add(content.get(i).permute(i + 1).bind(cumulativeRole));
        }
        HDVector stopRole = memory.getOrGenerate(POSITION_URI + content.size());
        cumulativeRole = cumulativeRole == null ? stopRole : cumulativeRole.bind(stopRole);
        terms.add(memory.getOrGenerate(V_STOP_URI).bind(cumulativeRole));
        if (terms.size() % 2 == 0) terms.add(HDVectorMapB.generateRandom(terms));
        return HDVectorMapB.bundleSimultaneous(terms);
    }

    public HDVector decodeChunkElement(HDVector chunk, int zeroBasedIndex, ItemMemory memory) {
        HDVector cumulativeRole = memory.getOrGenerate(POSITION_URI + 0);
        for (int i = 1; i <= zeroBasedIndex; i++)
            cumulativeRole = cumulativeRole.bind(memory.getOrGenerate(POSITION_URI + i));
        // Simpkin, eq. 5 e sez. III-A: invertire il ruolo e lo shift prima del clean-up.
        return chunk.bind(cumulativeRole).permute(-(zeroBasedIndex + 1));
    }

    public HDVector recoverBranch(ItemMemory memory, String entityUri, String predicateUri) {
        BranchPath path = branchPaths.get(treeKey(entityUri, predicateUri));
        ValueNode expectedTree = valueTrees.get(treeKey(entityUri, predicateUri));
        if (expectedTree == null) return null;
        HDVector root = memory.getTreeVector(entityUri);
        if (path == null) {
            HDVector noisyBranch = root.permute(-100).bind(memory.getOrGenerate(predicateUri));
            return cleanUpExpected(noisyBranch, expectedTree.vector);
        }

        HDVector branch = cleanUpExpected(
                root.permute(-100).bind(memory.getOrGenerate(path.rootRoleUri)), path.rootVector);
        if (branch == null) return null;
        for (int index = 0; index < path.steps.size(); index++) {
            BranchStep step = path.steps.get(index);
            branch = decodeChunkElement(branch, step.index, memory);
            if (index == path.steps.size() - 1) {
                branch = branch.bind(memory.getOrGenerate(predicateUri));
                branch = cleanUpExpected(branch, expectedTree.vector);
            } else {
                branch = cleanUpExpected(branch, step.expectedVector);
            }
            if (branch == null) return null;
        }
        return branch;
    }

    public HDVector recoverTriple(ItemMemory memory, String entityUri, String predicateUri, int zeroBasedIndex) {
        ValueNode tree = valueTrees.get(treeKey(entityUri, predicateUri));
        if (tree == null || zeroBasedIndex < 0 || zeroBasedIndex >= tree.leafCount) return null;
        HDVector branch = recoverBranch(memory, entityUri, predicateUri);
        if (branch == null) return null;
        return recoverTripleFromTree(branch, tree, zeroBasedIndex, memory);
    }

    public List<HDVector> recoverTriples(ItemMemory memory, String entityUri, String predicateUri) {
        ValueNode tree = valueTrees.get(treeKey(entityUri, predicateUri));
        if (tree == null) return List.of();
        HDVector branch = recoverBranch(memory, entityUri, predicateUri);
        if (branch == null) return List.of();
        List<HDVector> triples = new ArrayList<>(tree.leafCount);
        for (int index = 0; index < tree.leafCount; index++) {
            HDVector triple = recoverTripleFromTree(branch, tree, index, memory);
            if (triple != null) triples.add(triple);
        }
        return List.copyOf(triples);
    }

    private HDVector recoverTripleFromTree(
            HDVector currentVector, ValueNode currentNode, int zeroBasedIndex, ItemMemory memory) {
        int remaining = zeroBasedIndex;
        while (!currentNode.children.isEmpty()) {
            int childIndex = 0;
            while (remaining >= currentNode.children.get(childIndex).leafCount) {
                remaining -= currentNode.children.get(childIndex).leafCount;
                childIndex++;
            }
            ValueNode child = currentNode.children.get(childIndex);
            HDVector decoded = decodeChunkElement(currentVector, childIndex, memory);
            if (child.children.isEmpty()) return cleanUpExpected(decoded, child.vector);
            // Simpkin, sez. III-A: dopo ogni discesa il clean-up strutturale
            // ripristina il sotto-chunk puro prima di decodificare il livello seguente.
            currentVector = cleanUpExpected(decoded, child.vector);
            if (currentVector == null) return null;
            currentNode = child;
        }
        return null;
    }

    private HDVector cleanUpExpected(HDVector noisyVector, HDVector expectedVector) {
        double similarity = noisyVector.similarity(expectedVector);
        // Simpkin, sez. III-A: per BSC casuali σ(similarità)=1/√D;
        // questo rende confrontabile il clean-up strutturale con la soglia a 3σ.
        lastStructuralSigma = similarity * Math.sqrt(HDVectorMapB.D);
        return similarity >= MIN_STRUCTURAL_SIMILARITY ? expectedVector : null;
    }

    public double getLastStructuralSigma() {
        return lastStructuralSigma;
    }

    private String treeKey(String entityUri, String predicateUri) {
        return entityUri + "\u0000" + predicateUri;
    }

    private HDVector encodeTriple(Statement stmt, ItemMemory memory) {
        HDVector subject = memory.getOrGenerate(stmt.getSubject().getURI());
        HDVector predicate = memory.getOrGenerate(stmt.getPredicate().getURI());
        HDVector object = stmt.getObject().isResource()
                ? memory.getOrGenerate(stmt.getObject().asResource().getURI())
                : memory.getOrGenerate(stmt.getObject().asLiteral().getString());
        return subject.bind(predicate.permute(1)).bind(object.permute(2));
    }

    private record ValueNode(HDVector vector, List<ValueNode> children, int leafCount) { }
    private record BranchStep(int index, HDVector expectedVector) { }
    private record BranchNode(HDVector vector, Map<String, List<BranchStep>> paths) { }
    private record BranchPath(String rootRoleUri, HDVector rootVector, List<BranchStep> steps) { }
}
