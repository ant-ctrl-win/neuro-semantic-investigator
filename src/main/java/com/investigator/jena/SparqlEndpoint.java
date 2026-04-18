package com.investigator.jena;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SparqlEndpoint {
    private final String endpointUrl;
    private static final String USER_AGENT = "NeuroSemanticInvestigator/1.0 (mailto:acrispino10@gmail.com; Java/Jena5)";

    public SparqlEndpoint(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public Model queryOutgoing(org.apache.jena.rdf.model.Resource node) {
        String queryStr = "CONSTRUCT { <" + node.getURI() + "> ?p ?o } " +
            "WHERE { <" + node.getURI() + "> ?p ?o . " +
            "FILTER(STRSTARTS(STR(?p), \"http://www.wikidata.org/prop/direct/\")) } " +
            "LIMIT 100";
        return executeConstructQuery(queryStr);
    }

    public Model queryIncoming(org.apache.jena.rdf.model.Resource node) {
        String queryStr = "CONSTRUCT { ?s ?p <" + node.getURI() + "> } " +
            "WHERE { ?s ?p <" + node.getURI() + "> . " +
            "FILTER(STRSTARTS(STR(?p), \"http://www.wikidata.org/prop/direct/\")) } " +
            "LIMIT 100";
        return executeConstructQuery(queryStr);
    }

    private Model executeConstructQuery(String queryStr) {
        try {
            String encodedQuery = URLEncoder.encode(queryStr, StandardCharsets.UTF_8);
            URL url = new URL(endpointUrl + "?query=" + encodedQuery);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/rdf+xml");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("HTTP " + responseCode + " from SPARQL endpoint");
            }

            Model model = ModelFactory.createDefaultModel();
            try (InputStream in = conn.getInputStream()) {
                model.read(in, null, "RDF/XML");
            }

            conn.disconnect();
            return model;
        } catch (IOException e) {
            throw new RuntimeException("SPARQL query failed: " + e.getMessage(), e);
        }
    }
}