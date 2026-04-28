package com.investigator.jena;

import org.apache.jena.query.*;
import java.util.ArrayList;
import java.util.List;

public class EntityResolver {

    private static final String SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";
    private static final String USER_AGENT = "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)";

    private static final String SEARCH_QUERY =
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
            "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
            "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
            "PREFIX mwapi: <https://www.mediawiki.org/ontology#API/>\n" +
            "\n" +
            "SELECT ?entity ?entityLabel ?class ?classLabel ?sitelinks WHERE {\n" +
            "  SERVICE wikibase:mwapi {\n" +
            "      bd:serviceParam wikibase:endpoint \"www.wikidata.org\";\n" +
            "                      wikibase:api \"EntitySearch\";\n" +
            "                      mwapi:search \"%s\";\n" +
            "                      mwapi:language \"en\".\n" +
            "      ?entity wikibase:apiOutputItem mwapi:item.\n" +
            "  }\n" +
            "  OPTIONAL { ?entity wdt:P31 ?class . }\n" +
            "  OPTIONAL { ?entity wikibase:sitelinks ?sitelinks . }\n" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". }\n" +
            "}\n" +
            "ORDER BY DESC(?sitelinks)\n" +
            "LIMIT %d";

    public List<ResolvedEntity> resolve(String keyword, int limit) {
        List<ResolvedEntity> results = new ArrayList<>();

        String queryStr = String.format(SEARCH_QUERY, keyword, limit);
        Query query = QueryFactory.create(queryStr);

        try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT)
                .query(query)
                .httpHeader("User-Agent", USER_AGENT)
                .build()) {

            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution soln = rs.next();

                String entityUri = soln.getResource("entity").getURI();
                String entityLabel = soln.getLiteral("entityLabel").getString();

                String classUri = null;
                String classLabel = null;
                if (soln.getResource("class") != null) {
                    classUri = soln.getResource("class").getURI();
                }
                if (soln.getLiteral("classLabel") != null) {
                    classLabel = soln.getLiteral("classLabel").getString();
                }

                int sitelinks = 0;
                if (soln.getLiteral("sitelinks") != null) {
                    sitelinks = soln.getLiteral("sitelinks").getInt();
                }

                results.add(new ResolvedEntity(keyword, entityUri, entityLabel,
                        classUri, classLabel, sitelinks));
            }
        } catch (Exception e) {
            System.err.println("   [EntityResolver] Search failed for '" + keyword + "': " + e.getMessage());
        }

        return results;
    }
}
