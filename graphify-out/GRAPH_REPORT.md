# Graph Report - .  (2026-05-22)

## Corpus Check
- Corpus is ~13,441 words - fits in a single context window. You may not need a graph.

## Summary
- 138 nodes · 233 edges · 14 communities (4 shown, 10 thin omitted)
- Extraction: 70% EXTRACTED · 30% INFERRED · 0% AMBIGUOUS · INFERRED: 71 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Core Application & Entry Points|Core Application & Entry Points]]
- [[_COMMUNITY_VSA Vector Strategies|VSA Vector Strategies]]
- [[_COMMUNITY_Wikidata Entity Resolution|Wikidata Entity Resolution]]
- [[_COMMUNITY_Investigation Engine Core|Investigation Engine Core]]
- [[_COMMUNITY_Graph Traversal & Expansion|Graph Traversal & Expansion]]
- [[_COMMUNITY_Topological Vector Encoding|Topological Vector Encoding]]
- [[_COMMUNITY_Ontology Translation & Disambiguation|Ontology Translation & Disambiguation]]
- [[_COMMUNITY_SPARQL Endpoint Operations|SPARQL Endpoint Operations]]
- [[_COMMUNITY_Semantic Embedding Generation|Semantic Embedding Generation]]
- [[_COMMUNITY_Application Tests|Application Tests]]
- [[_COMMUNITY_VSA Component Tests|VSA Component Tests]]
- [[_COMMUNITY_Application Test Concept|Application Test Concept]]
- [[_COMMUNITY_HDVectorMapB Test Concept|HDVectorMapB Test Concept]]

## God Nodes (most connected - your core abstractions)
1. `ItemMemory` - 12 edges
2. `HDVectorMapB (MAP-B Implementation D=10000)` - 11 edges
3. `HDVectorMapB` - 10 edges
4. `ItemMemory (3-Tier Memory + Z-Score Clean-Up)` - 10 edges
5. `InvestigationEngine` - 9 edges
6. `HDVector` - 9 edges
7. `App (Main Pipeline Entry Point)` - 9 edges
8. `GraphManager` - 8 edges
9. `SemanticEmbeddingStrategy` - 8 edges
10. `AppDis (Disambiguation Pipeline Entry Point)` - 8 edges

## Surprising Connections (you probably didn't know these)
- `Wikidata Knowledge Graph` --conceptually_related_to--> `EntityResolver`  [INFERRED]
  Description.md → src/main/java/com/investigator/jena/EntityResolver.java
- `VSA MAP-B Encoding Paradigm` --conceptually_related_to--> `HDVectorMapB (MAP-B Implementation D=10000)`  [INFERRED]
  Description.md → src/main/java/com/investigator/vsa/HDVectorMapB.java
- `Z-Score Statistical Clean-Up Mechanism` --conceptually_related_to--> `ItemMemory (3-Tier Memory + Z-Score Clean-Up)`  [INFERRED]
  Description.md → src/main/java/com/investigator/vsa/ItemMemory.java
- `AGENTS_0.md (Build & Architecture Guide)` --references--> `App (Main Pipeline Entry Point)`  [EXTRACTED]
  AGENTS_0.md → src/main/java/com/investigator/App.java
- `Structural Analogy Problem (A:B = C:D over Open-World KG)` --conceptually_related_to--> `App (Main Pipeline Entry Point)`  [INFERRED]
  Description.md → src/main/java/com/investigator/App.java

## Hyperedges (group relationships)
- **Simpkin Tree Encoding Pipeline** — topological_vector_updater, item_memory, hdvectormapb [INFERRED 0.90]
- **SPARQL Data Access Layer** — entity_resolver, triple_extractor, sparql_endpoint [INFERRED 0.85]
- **Semantic Bridge Subsystem (Embedding-Based Cross-Ontology Matching)** — ontology_translator, semantic_embedding_strategy, all_minilm_model [INFERRED 0.85]

## Communities (14 total, 10 thin omitted)

### Community 0 - "Core Application & Entry Points"
Cohesion: 0.19
Nodes (26): AGENTS_0.md (Build & Architecture Guide), all-MiniLM-L6-v2 Embedding Model (384-d), AppDis (Disambiguation Pipeline Entry Point), App (Main Pipeline Entry Point), Description.md (Technical Description), EntityResolver, GraphManager, HDVector (VSA Interface) (+18 more)

### Community 1 - "VSA Vector Strategies"
Cohesion: 0.10
Nodes (4): RandomGenerationStrategy, VectorGenerationStrategy, HDVector, HDVectorMapB

### Community 3 - "Investigation Engine Core"
Cohesion: 0.14
Nodes (3): InvestigationEngine, App, EntityResolver

## Knowledge Gaps
- **4 isolated node(s):** `HDVectorMapBTest`, `HDVector (VSA Interface)`, `AppTest (JUnit 4 Skeleton)`, `HDVectorMapBTest (Empty Skeleton)`
  These have ≤1 connection - possible missing edges or undocumented components.
- **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HDVectorMapB (MAP-B Implementation D=10000)` connect `Core Application & Entry Points` to `VSA Vector Strategies`?**
  _High betweenness centrality (0.267) - this node is a cross-community bridge._
- **Why does `HDVectorMapB` connect `VSA Vector Strategies` to `Topological Vector Encoding`?**
  _High betweenness centrality (0.243) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `HDVectorMapB (MAP-B Implementation D=10000)` (e.g. with `VSA MAP-B Encoding Paradigm` and `Simpkin Tree (Hierarchical Vector Encoding)`) actually correct?**
  _`HDVectorMapB (MAP-B Implementation D=10000)` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 5 inferred relationships involving `ItemMemory (3-Tier Memory + Z-Score Clean-Up)` (e.g. with `InvestigationEngine (Orchestrator)` and `TopologicalVectorUpdater (Simpkin Tree Builder)`) actually correct?**
  _`ItemMemory (3-Tier Memory + Z-Score Clean-Up)` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `HDVectorMapBTest`, `HDVector (VSA Interface)`, `AppTest (JUnit 4 Skeleton)` to the rest of the system?**
  _4 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `VSA Vector Strategies` be split into smaller, more focused modules?**
  _Cohesion score 0.09956709956709957 - nodes in this community are weakly interconnected._
- **Should `Investigation Engine Core` be split into smaller, more focused modules?**
  _Cohesion score 0.1437908496732026 - nodes in this community are weakly interconnected._