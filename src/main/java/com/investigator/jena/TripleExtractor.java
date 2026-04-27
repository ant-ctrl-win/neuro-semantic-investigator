package com.investigator.jena;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;

public class TripleExtractor {

//    public Stream<Statement> extract(Model model) {
//        List<Statement> statements = new ArrayList<>();
//        StmtIterator iter = model.listStatements();
//
//        while (iter.hasNext()) {
//            statements.add(iter.next());
//        }
//        iter.close();
//
//        return statements.stream();
//    }
//
//    public Stream<Statement> extractFiltered(Model model, Resource startNode, Direction direction) {
//        List<Statement> statements = new ArrayList<>();
//        StmtIterator iter;
//
//        if (direction == Direction.OUTGOING) {
//            iter = model.listStatements(startNode, null, (RDFNode) null);
//        } else {
//            iter = model.listStatements(null, null, startNode);
//        }
//
//        while (iter.hasNext()) {
//            statements.add(iter.next());
//        }
//        iter.close();
//
//        return statements.stream();
//    }
//
//    public enum Direction {
//        OUTGOING,
//        INCOMING
//    }



    /**
     * Estrae le triple dal Knowledge Graph in modo BIDIREZIONALE (Multi-Hop di base).
     * Prende sia i Figli (frecce in uscita) che i Genitori (frecce in entrata).
     */

    // RIPRISTINIAMO L'ENUM PER MANTENERE LA COMPATIBILITÀ DEL CODICE
    public enum Direction {
        OUTGOING,
        INCOMING,
        BOTH
    }

    /**
     * Il nuovo estrattore che fa Multi-Hop bidirezionale in un colpo solo.
     */
    public Model extractBidirectional(String endpointUrl, String entityUri) {
        Model model = ModelFactory.createDefaultModel();

        String sparqlQuery =
                "CONSTRUCT { " +
                        "  <" + entityUri + "> ?pOut ?o . " +
                        "  ?s ?pIn <" + entityUri + "> . " +
                        "} WHERE { " +
                        "  { " +
                        "    SELECT ?pOut ?o WHERE { <" + entityUri + "> ?pOut ?o . } " +
                        "  } UNION { " +
                        "    SELECT ?s ?pIn WHERE { ?s ?pIn <" + entityUri + "> . } LIMIT 500 " +
                        "  } " +
                        "}";

        try {
            Query query = QueryFactory.create(sparqlQuery);
            try (QueryExecution qexec = QueryExecution.service(endpointUrl)
                    .query(query)
                    .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
                    .build()) {
                qexec.execConstruct(model);
            }
        } catch (Exception e) {
            System.err.println("   [ERRORE EXTR] Fallita estrazione per " + entityUri + ": " + e.getMessage());
        }

        return model;
    }
}