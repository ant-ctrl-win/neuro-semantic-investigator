# AGENTS.md

## Build & Run
- **Maven** — no wrapper (`mvnw`). Requires `mvn` on PATH.
- **Java target is 17**, not 21. `pom.xml` properties say 21 but `maven-compiler-plugin` overrides to 17.
- `mvn compile` / `mvn test` / `mvn package`
- No `exec-maven-plugin` in POM — run main classes via IDE (IntelliJ/Eclipse) or `java -cp`.
- Three test frameworks declared (JUnit 5 engine, TestNG, JUnit 4), but **no real tests exist** — only skeletons. `mvn test` passes trivially.

## Architecture
- **Neuro-Symbolic analogical reasoner** on Wikidata: finds `D` such that `A:B = C:D` via VSA algebra.
- Full technical description: `Description.md` (Italian).
- **Entry points**: `App.java` (basic pipeline) and `AppDis.java` (adds disambiguation/reranking). Both have **hardcoded entity names** — edit strings in `main()` to run different analogies.
- Requires **live network** — queries `query.wikidata.org/sparql` for entity resolution, graph ingestion, and label lookups.
- **First run downloads MiniLM embedding model** (~22 MB) automatically via Langchain4j.
- `OntologyTranslator` uses `AllMiniLmL6V2EmbeddingModel` (384-d embeddings), **not an LLM** — despite variable naming and doc mentions of "LLM".

## Source Layout
| Package | Role |
|---|---|
| `com.investigator` | Entry points (`App`, `AppDis`) |
| `com.investigator.jena` | Wikidata access: `EntityResolver`, `GraphManager`, `TripleExtractor`, `SparqlEndpoint`, `ResolvedEntity` |
| `com.investigator.vsa` | VSA algebra: `HDVector` (interface), `HDVectorMapB` (D=10000, bipolar `byte[]`), `ItemMemory` (3-tier) |
| `com.investigator.vsa.strategy` | Vector generation: `RandomGenerationStrategy` (default, deterministic from URI), `SemanticEmbeddingStrategy` (not used by default) |
| `com.investigator.engine` | `InvestigationEngine` (orchestrator) |
| `com.investigator.llm` | `OntologyTranslator` (MiniLM embedding-based cross-ontology matching) |

## Key Quirks
- All comments, docs, and console output are in **Italian**.
- `App.java` and `AppDis.java` are ~90% duplicated code. Edit both if changing pipeline logic.
- VSA vectors are deterministic: `URI → UUID.nameUUIDFromBytes → seed → Random LCG → 10000 bipolar bytes`. Same URI = same vector on any JVM.
- `fetchLabelFromWikidata()` normalizes `/prop/direct/` → `/entity/` before SPARQL label lookup.
- `isMetadata()` hardcodes a list of Wikidata property IDs to skip (P18, P373, P2002, P2013, P137).
- Eclipse project files (`.project`, `.classpath`, `.settings/`) coexist with IntelliJ (`.idea/`). Do not delete either set.

## Graphify
- Check `graphify-out/GRAPH_REPORT.md` for component overview and community structure before non-trivial changes.
- AST cache at `src/graphify-out/cache/ast/`.
- Run `/graphify --update` to regenerate after significant changes.
