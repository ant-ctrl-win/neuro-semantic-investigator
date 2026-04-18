package com.investigator.jena;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import java.util.stream.Stream;
import java.util.HashSet;
import java.util.Set;

public class GraphManager {
    private final Model localModel;
    private final SparqlEndpoint sparqlEndpoint;
    private final TripleExtractor tripleExtractor;

    public GraphManager(SparqlEndpoint sparqlEndpoint, TripleExtractor tripleExtractor) {
        this.localModel = ModelFactory.createDefaultModel();
        this.sparqlEndpoint = sparqlEndpoint;
        this.tripleExtractor = tripleExtractor;
    }

    public void expandNode(Resource node, TripleExtractor.Direction direction) {
        Model remoteModel;
        if (direction == TripleExtractor.Direction.OUTGOING) {
            remoteModel = sparqlEndpoint.queryOutgoing(node);
        } else {
            remoteModel = sparqlEndpoint.queryIncoming(node);
        }
        localModel.add(remoteModel);
    }

    public void expandNodeOutgoing(Resource node) {
        expandNode(node, TripleExtractor.Direction.OUTGOING);
    }

    public void expandNodeIncoming(Resource node) {
        expandNode(node, TripleExtractor.Direction.INCOMING);
    }

    public Stream<Statement> getNeighborhood(Resource node) {
        StmtIterator iter = localModel.listStatements(node, null, (RDFNode) null);
        Stream<Statement> outgoing = iter.toList().stream();
        iter.close();

        StmtIterator iter2 = localModel.listStatements(null, null, node);
        Stream<Statement> incoming = iter2.toList().stream();
        iter2.close();

        return Stream.concat(outgoing, incoming);
    }

    public Set<Resource> getUnexploredNodes(Set<Resource> visited) {
        Set<Resource> unexplored = new HashSet<>();
        StmtIterator iter = localModel.listStatements();

        while (iter.hasNext()) {
            Statement stmt = iter.next();
            Resource s = stmt.getSubject();
            Resource o = stmt.getObject().isResource() ? stmt.getObject().asResource() : null;

            if (!visited.contains(s)) {
                unexplored.add(s);
            }
            if (o != null && !visited.contains(o)) {
                unexplored.add(o);
            }
        }
        iter.close();

        return unexplored;
    }

    public Model getLocalModel() {
        return localModel;
    }
}