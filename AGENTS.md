# AGENTS.md

## Build & Run
- **Maven** — no wrapper (`mvnw`). Requires `mvn` on PATH.
- **Java target is 21** — aligned across `pom.xml` properties and `maven-compiler-plugin`.
- `mvn compile` / `mvn test` / `mvn package`
- No `exec-maven-plugin` in POM — run main classes via IDE (IntelliJ/Eclipse) or `java -cp`.
- Three test frameworks declared (JUnit 5 engine, TestNG, JUnit 4), but **no real tests exist** — only skeletons. `mvn test` passes trivially.

## Architecture
- **Neuro-Symbolic analogical reasoner** on Wikidata: finds `D` such that `A:B = C:D` via VSA algebra.
- Full technical description: `Description.md` (Italian).
- **Entry points**: `App.java` (basic pipeline) and `AppDis.java` (adds disambiguation/reranking). Both have **hardcoded entity names** — edit strings in `main()` to run different analogies.
- Requires **live network** — queries `query.wikidata.org/sparql` for entity resolution, graph ingestion, and label lookups.
- **Embedding model**: `bge-base-en-v1.5` (768-dim ONNX) loaded from local files — **no auto-download**. `OnnxEmbeddingModel` takes the `.onnx` file path; `tokenizer.json` is auto-resolved from the same directory. Requires `models/model.onnx` and `models/tokenizer.json` in the project root. Download these from [BAAI/bge-base-en-v1.5](https://huggingface.co/BAAI/bge-base-en-v1.5) before first run (see Setup below).

## Source Layout
| Package | Role |
|---|---|
| `com.investigator` | Entry points (`App`, `AppDis`) |
| `com.investigator.jena` | Wikidata access: `EntityResolver`, `GraphManager`, `TripleExtractor`, `SparqlEndpoint`, `ResolvedEntity` |
| `com.investigator.vsa` | VSA algebra: `HDVector` (interface), `HDVectorMapB` (D=10000, bipolar `byte[]`), `ItemMemory` (3-tier) |
| `com.investigator.vsa.strategy` | Vector generation: `RandomGenerationStrategy` (default, deterministic from URI), `SemanticEmbeddingStrategy` (not used by default) |
| `com.investigator.engine` | `InvestigationEngine` (orchestrator) |
| `com.investigator.embedding` | `OntologyTranslator` (embedding-based cross-ontology matching, bge-base-en-v1.5) |

## Setup
Download the embedding model files to `models/` in the project root:
```powershell
New-Item -ItemType Directory -Path models -Force
Invoke-WebRequest -Uri "https://huggingface.co/BAAI/bge-base-en-v1.5/resolve/main/onnx/model.onnx" -OutFile "models/model.onnx"
Invoke-WebRequest -Uri "https://huggingface.co/BAAI/bge-base-en-v1.5/resolve/main/tokenizer.json" -OutFile "models/tokenizer.json"
```
`models/` is git-ignored.

## Key Quirks
- All comments, docs, and console output are in **Italian**.
- `App.java` and `AppDis.java` are ~90% duplicated code. Edit both if changing pipeline logic.
- VSA vectors are deterministic: `URI → UUID.nameUUIDFromBytes → seed → Random LCG → 10000 bipolar bytes`. Same URI = same vector on any JVM.
- `fetchLabelFromWikidata()` normalizes `/prop/direct/` → `/entity/` before SPARQL label lookup.
- `isMetadata()` hardcodes a list of Wikidata property IDs to skip (P18, P373, P2002, P2013, P137).
- Eclipse project files (`.project`, `.classpath`, `.settings/`) coexist with IntelliJ (`.idea/`). Do not delete either set.
