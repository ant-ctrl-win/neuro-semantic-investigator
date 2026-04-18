package com.investigator.jena;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;

public class TripleExtractor {

    public Stream<Statement> extract(Model model) {
        List<Statement> statements = new ArrayList<>();
        StmtIterator iter = model.listStatements();

        while (iter.hasNext()) {
            statements.add(iter.next());
        }
        iter.close();

        return statements.stream();
    }

    public Stream<Statement> extractFiltered(Model model, Resource startNode, Direction direction) {
        List<Statement> statements = new ArrayList<>();
        StmtIterator iter;

        if (direction == Direction.OUTGOING) {
            iter = model.listStatements(startNode, null, (RDFNode) null);
        } else {
            iter = model.listStatements(null, null, startNode);
        }

        while (iter.hasNext()) {
            statements.add(iter.next());
        }
        iter.close();

        return statements.stream();
    }

    public enum Direction {
        OUTGOING,
        INCOMING
    }
}