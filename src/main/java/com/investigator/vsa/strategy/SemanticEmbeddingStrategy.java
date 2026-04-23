package com.investigator.vsa.strategy;
import com.investigator.vsa.HDVectorMapB;
import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strategia Neuro-Simbolica Reale:
 * Usa LangChain4j (all-MiniLM-L6-v2) per capire il significato,
 * e LSH per binarizzarlo nell'algebra HDC MAP-B.
 */
public class SemanticEmbeddingStrategy implements VectorGenerationStrategy {

    private final int embeddingDim;
    private final float[][] projectionMatrix;
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();
    private final Map<String, String> labelCache = new ConcurrentHashMap<>(); // Solo la cache delle label
    private final EmbeddingModel embeddingModel;



    public SemanticEmbeddingStrategy() {
        // Inizializza il modello locale in-memory (pesa circa 22MB)
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.embeddingDim = 384; // Dimensione esatta prodotta da all-MiniLM-L6-v2

        // Creazione della Matrice di Proiezione Fissa LSH
        this.projectionMatrix = new float[embeddingDim][HDVectorMapB.D];
        initializeProjectionMatrix(42);
    }

    private void initializeProjectionMatrix(long seed) {
        Random random = new Random(seed);
        for (int i = 0; i < embeddingDim; i++) {
            for (int j = 0; j < HDVectorMapB.D; j++) {
                projectionMatrix[i][j] = (float) random.nextGaussian();
            }
        }
    }

    @Override
    public HDVector generate(String uri) {
        // ATTENZIONE: per un'app reale l'URI dovrebbe essere convertita
        // nella sua etichetta testuale (es. "crew member" invece di "P1029")
        // prima di essere passata qui. Per ora, embeddiamo la stringa passata.
        String textToEmbed = cleanUriForEmbedding(uri);

        // 1. Ottieni l'embedding denso reale (384-D)
        float[] denseEmbedding = fetchDenseEmbedding(textToEmbed);

        // 2. Proiezione LSH -> 100.000-D Bipolare
        return binarize(denseEmbedding);
    }

    private HDVector binarize(float[] dense) {
        byte[] bipolarValues = new byte[HDVectorMapB.D];

        // Moltiplicazione Vettore x Matrice
        for (int j = 0; j < HDVectorMapB.D; j++) {
            float sum = 0;
            for (int i = 0; i < embeddingDim; i++) {
                sum += dense[i] * projectionMatrix[i][j];
            }
            // Funzione Segno per costringere lo spazio in {-1, +1}
            bipolarValues[j] = (byte) (sum >= 0 ? 1 : -1);
        }

        // Ora che il costruttore è public, questo funzionerà perfettamente
        return new HDVectorMapB(bipolarValues);
    }

    private float[] fetchDenseEmbedding(String textToEmbed) {
        // Caching per non ricalcolare embedding di parole già viste
        if (embeddingCache.containsKey(textToEmbed)) {
            return embeddingCache.get(textToEmbed);
        }

        // CHIAMATA REALE A LANGCHAIN4J
        System.out.println("   [LLM] Calcolo embedding semantico per: '" + textToEmbed + "'");
        Embedding embedding = embeddingModel.embed(textToEmbed).content();
        float[] denseVector = embedding.vector();

        embeddingCache.put(textToEmbed, denseVector);
        return denseVector;
    }

    // Utility di base per pulire un po' l'URI se ci arriva grezza
    private String cleanUriForEmbedding(String uri) {
        if (!uri.startsWith("http://www.wikidata.org/")) {
            return uri.replace("_", " "); // Per cose come "Apollo_11"
        }

        // Se l'abbiamo già cercata, restituiscila
        if (labelCache.containsKey(uri)) {
            return labelCache.get(uri);
        }

        // Estrai l'ID (es. Q43653 o P1029)
        String[] parts = uri.split("/");
        String id = parts[parts.length - 1];

        // Se non è un Q-node o un P-node, prova un clean base
        if (!id.matches("^[QP]\\d+$")) {
            return id.replace("_", " ");
        }

        System.out.println("   [WIKIDATA] Risoluzione label per: " + id);

        // Costruisci una query SPARQL per ottenere l'etichetta inglese
        String sparqlQuery =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX wd: <http://www.wikidata.org/entity/> " +
                        "SELECT ?label WHERE { " +
                        "  <" + uri + "> rdfs:label ?label . " +
                        "  FILTER (lang(?label) = 'en') " +
                        "} LIMIT 1";
        try {
            org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);

            // CORREZIONE ANTI-BOT: Aggiungiamo l'header User-Agent personalizzato
            try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
                    .query(query)
                    .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
                    .build()) {

                org.apache.jena.query.ResultSet results = qexec.execSelect();
                if (results.hasNext()) {
                    org.apache.jena.query.QuerySolution soln = results.nextSolution();
                    org.apache.jena.rdf.model.Literal label = soln.getLiteral("label");
                    if (label != null) {
                        String cleanLabel = label.getString();
                        labelCache.put(uri, cleanLabel);
                        return cleanLabel;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("   [AVVISO] Impossibile risolvere la label per " + uri + ": " + e.getMessage());
        }

        // Fallback: usa l'ID se Wikidata non risponde o non ha l'etichetta in inglese
        labelCache.put(uri, id);
        return id;
    }
}