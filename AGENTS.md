# Neuro-Semantic Investigator

## Build Commands
```bash
mvn clean compile          # Clean and compile
mvn test                   # Run all tests
mvn test -Dtest=ClassName  # Run single test class
mvn package                # Build JAR
mvn exec:java -Dexec.mainClass=com.investigator.App    # Run App.java (main pipeline)
mvn exec:java -Dexec.mainClass=com.investigator.AppDis # Run AppDis.java (disambiguation variant)
```
**Warning:** `mvn exec:java` requires the exec-maven-plugin, which is **not in pom.xml**. You must add it first:
```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <version>3.1.0</version>
</plugin>
```

## Architecture

**Two entry points** (duplicate ~90% of logic, not yet consolidated):
- `src/main/java/com/investigator/App.java` — Pipeline with SPARQL-based ontological alignment (hardcoded entities)
- `src/main/java/com/investigator/AppDis.java` — Pipeline with reranker, embedding-based compatibility, and structural RDF filter

**Paradigm:** VSA MAP-B (Role-Based Extraction) backed by Wikidata SPARQL. No cellular automaton, no external LLM API — the "LLM" is local cosine similarity on MiniLM-L6-v2 embeddings (384-d).

**Packages:**
- `core/` — Does **not exist yet** (reserved in AGENTS.md but no source directory)
- `engine/` — InvestigationEngine (orchestrates ingestion, triple encoding, topological update)
- `jena/` — SparqlEndpoint (HTTP to Wikidata), GraphManager (local RDF accumulator), TripleExtractor (bidirectional CONSTRUCT), EntityResolver (wikibase:mwapi search), ResolvedEntity (record)
- `llm/` — OntologyTranslator (cosine similarity on local MiniLM embeddings; no actual LLM API)
- `vsa/` — HDVector (interface), HDVectorMapB (D=10000, byte[] bipolar), ItemMemory (3-tier: atomic/chunk/tree)
- `vsa/strategy/` — VectorGenerationStrategy, RandomGenerationStrategy (URI→seed deterministic), SemanticEmbeddingStrategy (unused by default, LSH projection), TopologicalVectorUpdater (Simpkin tree builder)

## Key Implementation Details

- **D = 10000** (not 100000). Defined in `HDVectorMapB.java:8`.
- **Triple encoding:** `vS.bind(vP.permute(1)).bind(vO.permute(2))` — `TopologicalVectorUpdater.java:97`
- **Simpkin Tree:** macro-branches permuted by +100 positions; root = bundleSimultaneous(branches + identity + optional parity)
- **Chunk capacity:** `MAX_CHUNK_CAPACITY = 30` — `TopologicalVectorUpdater.java:10`
- **Clean-up threshold:** `Math.log10(D) = 4.0 σ` (dynamic, not hardcoded 5.0) — `ItemMemory.java:35`
- **Tie-breaking in `bundleSimultaneous`:** deterministic, seeded from XOR of vector hash codes — `HDVectorMapB.java:52`
- **Tie-breaking in `bundle` (two vectors):** positional parity (`i % 2`), no randomness — `HDVectorMapB.java:86`
- **Vector generation:** `URI → UTF-8 bytes → UUID.nameUUIDFromBytes → 64-bit seed → LCG → 10000 bipolar bits` — fully deterministic across JVMs — `RandomGenerationStrategy.java:12-13`
- **GraphManager** and **TripleExtractor** both have large commented-out legacy code blocks at the top of the file — do not delete them, they document the evolution of the code
- **Parity fix non-determinism:** `buildSimpleChunk` and `buildRecursiveSubTree` use `HDVectorMapB.generateRandom()` (no seed), so parity-fix vectors differ every run; the tree-level parity uses `memory.getOrGenerate("vsa:internal:parity_fix")` which IS deterministic

## Java Version Gotcha

pom.xml properties declare Java 21, but the maven-compiler-plugin config overrides to **Java 17**. The Eclipse `.classpath` also targets JavaSE-17. The effective target is **17**.

## Dependencies & Runtime

- **Apache Jena 5.0.0** — RDF model + SPARQL query/construct
- **LangChain4j 0.27.1** (all-MiniLM-L6-v2) — Local embedding model, downloads ~22MB on first use
- **SLF4J + Logback** — Logging (config at `src/main/resources/logback.xml`)
- **Tests:** JUnit 5.10.2, TestNG, AND JUnit 4.13.1 all on classpath. **AppTest.java uses JUnit 4** (`org.junit.Test`) not JUnit 5 — use `@Test` from `org.junit.jupiter.api.Test` for new tests
- **Network required:** The app queries `https://query.wikidata.org/sparql` at runtime. Tests are empty skeletons that don't need network.
- **OntologyTranslator is NOT an LLM API call** — it uses in-process `AllMiniLmL6V2EmbeddingModel` with `CosineSimilarity`. Misleading naming in the code and comments.

## User-Agent Note

Multiple User-Agent strings exist in the codebase — some use `ifts-project@example.com`, others `acrispino10@gmail.com`. If Wikidata starts rejecting requests, update all of them in one pass (grep for `User-Agent`).

## Testing

Tests in `src/test/java/com/investigator/` are largely empty: `AppTest.java` asserts `true`, and `HDVectorMapBTest.java` exists but may also be minimal. No integration tests exist. All tests can run offline (no Wikidata calls).
