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

    private static final String USER_AGENT = "neuro-semantic-investigator/0.1 (research; project@example.com)";

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

        // Sostituisci la vecchia query complessa con questa:
        String sparqlQuery =
                "CONSTRUCT { " +
                        "  <" + entityUri + "> ?pOut ?o . " +
                        "} WHERE { " +
                        "  <" + entityUri + "> ?pOut ?o . " +
                        "}";

        try {
            Query query = QueryFactory.create(sparqlQuery);
            try (QueryExecution qexec = QueryExecution.service(endpointUrl)
                    .query(query)
                    .httpHeader("User-Agent", USER_AGENT)
                    .build()) {
                qexec.execConstruct(model);
            }
        } catch (Exception e) {
            System.err.println("   [ERRORE EXTR] Fallita estrazione per " + entityUri + ": " + e.getMessage());
        }

        return model;
    }
}