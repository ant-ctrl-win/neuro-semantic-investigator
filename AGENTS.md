# Neuro-Semantic Investigator

## Build Commands
```bash
mvn clean compile          # Clean and compile
mvn test                   # Run all tests
mvn test -Dtest=ClassName   # Run single test class
mvn package                # Build JAR
mvn exec:java              # Run App.java (main entry point)
```

## Architecture
**Main entry point:** `src/main/java/com/investigator/App.java`

**Paradigm:** Pure VSA MAP-B (Role-Based Extraction). No cellular automaton, no external embeddings required in default mode.

**Packages:**
- `core/` - (reserved for future semantic evidence structures)
- `engine/` - InvestigationEngine (processes triples into HD vectors)
- `jena/` - SparqlEndpoint (Wikidata), GraphManager, TripleExtractor
- `llm/` - OntologyTranslator (LLM-based semantic bridge between domains)
- `vsa/` - HDVector interface, HDVectorMapB implementation (D=100000), ItemMemory
- `vsa/strategy/` - VectorGenerationStrategy, RandomGenerationStrategy, TopologicalVectorUpdater, SemanticEmbeddingStrategy

**Key implementation details:**
- HDVector dimension is fixed at `D = 100000` in `HDVectorMapB`
- Triple binding pattern: `vS.bind(vP.permute(1)).bind(vO.permute(2))` (all three components)
- Chunk capacity: `MAX_CHUNK_CAPACITY = 30`
- Statistical clean-up threshold: `5.0 σ` (Z-Score)
- Tie-breaking in `bundleSimultaneous`: random per element (no bias)
- App.java queries Wikidata SPARQL endpoint (https://query.wikidata.org/sparql)
- User-Agent: `NeuroSemanticInvestigator/1.0 (mailto:acrispino10@gmail.com; Java/Jena5)`

## Dependencies
- **Apache Jena 5.0.0** - RDF model and SPARQL
- **LangChain4j** (dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2) - LLM embedding for semantic strategy
- **JUnit 5.10.2** - Primary test framework (also has TestNG and JUnit 4 on classpath)

## Testing
Tests in `src/test/java/com/investigator/` are largely empty skeletons. AppTest.java only asserts `true`.
