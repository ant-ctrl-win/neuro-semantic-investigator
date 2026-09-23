package com.investigator.jena;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import java.util.stream.Stream;

public class GraphManager {
    private final Model localModel;
    private final SparqlEndpoint sparqlEndpoint;
    private final TripleExtractor tripleExtractor;

    public GraphManager(SparqlEndpoint sparqlEndpoint, TripleExtractor tripleExtractor) {
        this.localModel = ModelFactory.createDefaultModel();
        this.sparqlEndpoint = sparqlEndpoint;
        this.tripleExtractor = tripleExtractor;
    }

    // Nota: estraiamo il 1-hop outgoing. L'espansione incoming è pianificata (vedi README - Roadmap).
    // Il parametro `direction` è attualmente ignorato; estrazione incoming in roadmap; l'estrazione attuale è 1-hop outgoing.
    public void expandNode(Resource node, TripleExtractor.Direction direction) {
        System.out.println("   [GraphManager] 1-hop expansion: " + node.getURI());

        // Passiamo direttamente la richiesta al nuovo motore di estrazione
        Model remoteModel = tripleExtractor.extractBidirectional("https://query.wikidata.org/sparql", node.getURI());

        localModel.add(remoteModel);
    }

    public void expandNodeOutgoing(Resource node) {
        expandNode(node, TripleExtractor.Direction.OUTGOING);
    }

    public void expandNodeIncoming(Resource node) {
        expandNode(node, TripleExtractor.Direction.INCOMING);
    }

    public Model fetchOutgoing(Resource node) {
        return tripleExtractor.extractBidirectional("https://query.wikidata.org/sparql", node.getURI());
    }

    public void addRemoteModel(Model remoteModel) {
        localModel.add(remoteModel);
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

    public Model getLocalModel() {
        return localModel;
    }
}