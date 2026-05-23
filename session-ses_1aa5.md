# AGENTS.md for neuro-semantic-investigator

**Session ID:** ses_1aa59e9dbffewjtOpDXU5ACwd4
**Created:** 23/5/2026, 18:23:28
**Updated:** 23/5/2026, 18:24:51

---

## User

Create or update `AGENTS.md` for this repository.

The goal is a compact instruction file that helps future OpenCode sessions avoid mistakes and ramp up quickly. Every line should answer: "Would an agent likely miss this without help?" If not, leave it out.

User-provided focus or constraints (honor these):


## How to investigate

Read the highest-value sources first:
- `README*`, root manifests, workspace config, lockfiles
- build, test, lint, formatter, typecheck, and codegen config
- CI workflows and pre-commit / task runner config
- existing instruction files (`AGENTS.md`, `CLAUDE.md`, `.cursor/rules/`, `.cursorrules`, `.github/copilot-instructions.md`)
- repo-local OpenCode config such as `opencode.json`

If architecture is still unclear after reading config and docs, inspect a small number of representative code files to find the real entrypoints, package boundaries, and execution flow. Prefer reading the files that explain how the system is wired together over random leaf files.

Prefer executable sources of truth over prose. If docs conflict with config or scripts, trust the executable source and only keep what you can verify.

## What to extract

Look for the highest-signal facts for an agent working in this repo:
- exact developer commands, especially non-obvious ones
- how to run a single test, a single package, or a focused verification step
- required command order when it matters, such as `lint -> typecheck -> test`
- monorepo or multi-package boundaries, ownership of major directories, and the real app/library entrypoints
- framework or toolchain quirks: generated code, migrations, codegen, build artifacts, special env loading, dev servers, infra deploy flow
- repo-specific style or workflow conventions that differ from defaults
- testing quirks: fixtures, integration test prerequisites, snapshot workflows, required services, flaky or expensive suites
- important constraints from existing instruction files worth preserving

Good `AGENTS.md` content is usually hard-earned context that took reading multiple files to infer.

## Questions

Only ask the user questions if the repo cannot answer something important. Use the `question` tool for one short batch at most.

Good questions:
- undocumented team conventions
- branch / PR / release expectations
- missing setup or test prerequisites that are known but not written down

Do not ask about anything the repo already makes clear.

## Writing rules

Include only high-signal, repo-specific guidance such as:
- exact commands and shortcuts the agent would otherwise guess wrong
- architecture notes that are not obvious from filenames
- conventions that differ from language or framework defaults
- setup requirements, environment quirks, and operational gotchas
- references to existing instruction sources that matter

Exclude:
- generic software advice
- long tutorials or exhaustive file trees
- obvious language conventions
- speculative claims or anything you could not verify
- content better stored in another file referenced via `opencode.json` `instructions`

When in doubt, omit.

Prefer short sections and bullets. If the repo is simple, keep the file simple. If the repo is large, summarize the few structural facts that actually change how an agent should work.

If `AGENTS.md` already exists at `C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator`, improve it in place rather than rewriting blindly. Preserve verified useful guidance, delete fluff or stale claims, and reconcile it with the current codebase.

---

## Assistant (Build · DeepSeek V4 Pro · 6.6s)

_Thinking:_

Let me investigate this repository systematically to create/update the AGENTS.md file. I'll read the highest-value sources first as instructed.

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator</path>
<type>directory</type>
<entries>
.classpath
.git/
.gitignore
.idea/
.project
.settings/
AGENTS.md
Description.md
graphify-out/
pom.xml
src/
target/

(12 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\AGENTS.md"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\AGENTS.md</path>
<type>file</type>
<content>
1: # AGENTS.md
2: 
3: ## Graphify Integration
4: Prima di eseguire qualsiasi compito, leggi l'indice e le comunità del knowledge graph.
5: - Controlla `graphify-out/GRAPH_REPORT.md` per una panoramica dei componenti principali.
6: - Se una richiesta non è banale, usa il grafo per identificare i file esatti da leggere, invece di fare ricerche alla cieca.
7: 
8: ## Istruzioni per il contesto
9: - Quando modifichi un file, usa Graphify per capire l'impatto su altri moduli.
10: - Per domande architetturali, fornisci spiegazioni basate sulla struttura del grafo.
11: - Se trovi sezioni non mappate o relazioni mancanti, usa il comando `/graphify --update` per rigenerare il grafo.

(End of file - total 11 lines)
</content>
```

**Tool: glob**

**Input:**
```json
{
  "pattern": "README*"
}
```

**Output:**
```
No files found
```

**Tool: glob**

**Input:**
```json
{
  "pattern": "package.json"
}
```

**Output:**
```
No files found
```

**Tool: glob**

**Input:**
```json
{
  "pattern": "**/package.json"
}
```

**Output:**
```
No files found
```

---

## Assistant (Build · DeepSeek V4 Pro · 6.6s)

_Thinking:_

This is a Java project (Maven) with a `pom.xml`. Let me read the key files: pom.xml, Description.md, .gitignore, and explore the source structure. There's also a Graphify output directory.

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\pom.xml"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\pom.xml</path>
<type>file</type>
<content>
1: <?xml version="1.0" encoding="UTF-8"?>
2: <project xmlns="http://maven.apache.org/POM/4.0.0"
3:          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
4:          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
5:     <modelVersion>4.0.0</modelVersion>
6: 
7:     <groupId>com.investigator</groupId>
8:     <artifactId>neuro-semantic-investigator</artifactId>
9:     <version>1.0</version>
10: 
11:     <properties>
12:         <maven.compiler.source>21</maven.compiler.source>
13:         <maven.compiler.target>21</maven.compiler.target>
14:         <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
15: 
16:         <jena.version>5.0.0</jena.version>
17:         <slf4j.version>2.0.12</slf4j.version>
18:         <logback.version>1.5.3</logback.version>
19:         <junit.version>5.10.2</junit.version>
20:     </properties>
21: 
22:     <dependencies>
23:         <dependency>
24:             <groupId>org.apache.jena</groupId>
25:             <artifactId>jena-core</artifactId>
26:             <version>${jena.version}</version>
27:         </dependency>
28:         <dependency>
29:             <groupId>org.apache.jena</groupId>
30:             <artifactId>jena-arq</artifactId>
31:             <version>${jena.version}</version>
32:         </dependency>
33:         <dependency>
34:             <groupId>dev.langchain4j</groupId>
35:             <artifactId>langchain4j-embeddings-all-minilm-l6-v2</artifactId>
36:             <version>0.27.1</version> </dependency>
37:         <dependency>
38:             <groupId>org.slf4j</groupId>
39:             <artifactId>slf4j-api</artifactId>
40:             <version>${slf4j.version}</version>
41:         </dependency>
42:         <dependency>
43:             <groupId>ch.qos.logback</groupId>
44:             <artifactId>logback-classic</artifactId>
45:             <version>${logback.version}</version>
46:         </dependency>
47: 
48:         <dependency>
49:             <groupId>org.junit.jupiter</groupId>
50:             <artifactId>junit-jupiter-engine</artifactId>
51:             <version>${junit.version}</version>
52:             <scope>test</scope>
53:         </dependency>
54:         <dependency>
55:             <groupId>org.testng</groupId>
56:             <artifactId>testng</artifactId>
57:             <version>RELEASE</version>
58:             <scope>test</scope>
59:         </dependency>
60:         <dependency>
61:             <groupId>junit</groupId>
62:             <artifactId>junit</artifactId>
63:             <version>4.13.1</version>
64:             <scope>test</scope>
65:         </dependency>
66:     </dependencies>
67: 
68:     <build>
69:         <plugins>
70:             <plugin>
71:                 <groupId>org.apache.maven.plugins</groupId>
72:                 <artifactId>maven-compiler-plugin</artifactId>
73:                 <version>3.13.0</version>
74:                 <configuration>
75:                     <source>17</source>
76:                     <target>17</target>
77:                 </configuration>
78:             </plugin>
79:         </plugins>
80:     </build>
81: </project>

(End of file - total 81 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\Description.md"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\Description.md</path>
<type>file</type>
<content>
1: # Neuro-Semantic Investigator: Technical Description
2: 
3: ## 1. Panoramica dell'Architettura
4: 
5: Il Neuro-Semantic Investigator risolve il problema dell'**Analogia Strutturale** su Knowledge Graph Open-World: date due entità sorgente `A : B` (es. *"Amleto sta a William Shakespeare"*) e un'entità target `C` (es. *"Lacrimosa"*), il sistema deve inferire l'entità incognita `D` tale che `A : B = C : D` (es. *"Mozart"*), percorrendo path non esplicitamente materializzati nel grafo.
6: 
7: ### Pipeline di Esecuzione (AppDis.java)
8: 
9: ```
10: ┌─────────────────────────────────────────────────────────────────┐
11: │ FASE 0: Entity Resolution                                        │
12: │   Input testuale → EntityResolver (wikibase:mwapi) → ResolvedEntity │
13: ├─────────────────────────────────────────────────────────────────┤
14: │ FASE 0.5: Reranking & Compatibilità Ontologica                  │
15: │   OntologyTranslator.disambiguateCandidates()                   │
16: │   OntologyTranslator.areClassesCompatible()  (cosine ≥ 0.35)    │
17: │   → Allineamento SPARQL se incompatibili                        │
18: ├─────────────────────────────────────────────────────────────────┤
19: │ Graph Ingestion                                                  │
20: │   GraphManager + TripleExtractor → CONSTRUCT SPARQL su Wikidata  │
21: │   → Model RDF locale (bidirezionale: outgoing + incoming)       │
22: ├─────────────────────────────────────────────────────────────────┤
23: │ VSA Encoding (Simpkin Tree)                                      │
24: │   InvestigationEngine.expandAndProcess()                        │
25: │   → TopologicalVectorUpdater: tripla → vettore → chunk → albero │
26: │   → ItemMemory: 3-tier memory (atomic/chunk/tree)               │
27: ├─────────────────────────────────────────────────────────────────┤
28: │ STEP 1: Role Deduction (Dominio Sorgente)                        │
29: │   Per ogni proprietà P di A:                                    │
30: │     noisyBranch = Tree(A) ⊗ ρ⁻¹⁰⁰(vP)                          │
31: │     cleanBranch = cleanUpChunk(noisyBranch)                     │
32: │     noisyObject = cleanBranch ⊗ vA ⊗ ρ¹(vP) ⊗ ρ⁻²              │
33: │     sim = cosine(noisyObject, vB)                               │
34: │   → Scopre QUALE proprietà lega A a B                           │
35: ├─────────────────────────────────────────────────────────────────┤
36: │ STEP 2: Semantic Bridge (Cambio di Dominio)                      │
37: │   OntologyTranslator.findSemanticEquivalent()                   │
38: │   → Embedding MiniLM del ruolo sorgente vs. proprietà target    │
39: │   → Traduce il ruolo dal dominio A al dominio C                  │
40: ├─────────────────────────────────────────────────────────────────┤
41: │ STEP 3 & 4: Projection + Extraction (Dominio Target)            │
42: │   branch = Tree(C) ⊗ ρ⁻¹⁰⁰(targetRole)                         │
43: │   chunk = cleanUpChunk(branch)                                  │
44: │   object = chunk ⊗ vC ⊗ ρ¹(targetRole) ⊗ ρ⁻²                   │
45: │   finalAnswer = cleanUpRelative(object)                         │
46: └─────────────────────────────────────────────────────────────────┘
47: ```
48: 
49: ### Mappa dei Moduli
50: 
51: | Modulo | Classe | Ruolo |
52: |--------|--------|-------|
53: | **Entity Resolution** | `EntityResolver.java` | Converte testo → URI Wikidata via `wikibase:mwapi` |
54: | **Graph Ingestion** | `GraphManager.java`, `TripleExtractor.java`, `SparqlEndpoint.java` | Scarica e accumula triple RDF da Wikidata |
55: | **VSA Core** | `HDVectorMapB.java`, `HDVector.java` | Operazioni algebriche su vettori bipolari D=10000 |
56: | **VSA Memory** | `ItemMemory.java` | 3-tier memory + clean-up statistico (Z-Score) |
57: | **VSA Encoding** | `TopologicalVectorUpdater.java` | Costruzione Simpkin Tree da modello RDF |
58: | **VSA Generation** | `RandomGenerationStrategy.java`, `SemanticEmbeddingStrategy.java` | Generazione deterministica di vettori atomici |
59: | **Semantic Bridge** | `OntologyTranslator.java` | Embedding MiniLM per matching cross-ontologia |
60: | **Orchestrator** | `InvestigationEngine.java` | Coordina ingestion, encoding, aggiornamento topologico |
61: | **Entry Points** | `App.java`, `AppDis.java` | Pipeline complete (duplicate al ~90%) |
62: 
63: ---
64: 
65: ## 2. Rappresentazione della Conoscenza (VSA Encoding)
66: 
67: ### 2.1 Spazio Vettoriale e Dimensionalità
68: 
69: Il sistema opera in uno spazio vettoriale ipercubo di **D = 10000 dimensioni**, con vettori **bipolari** i cui elementi appartengono all'insieme `{-1, +1}`. La scelta di vettori bipolari (anziché binari `{0,1}`) garantisce che l'operatore di binding sia auto-inverso: `bind(a, bind(a, b)) = b`.
70: 
71: L'implementazione utilizza array di `byte[]` (Java):
72: 
73: ```java
74: // HDVectorMapB.java:8-9
75: public static final int D = 10000;
76: private final byte[] values;
77: ```
78: 
79: Ogni elemento occupa 1 byte (anche se il dominio è {-1, +1}), per un footprint di **10 KB per vettore**. Nessuna libreria esterna per algebra lineare: tutte le operazioni sono implementate direttamente con cicli `for` su array primitivi.
80: 
81: ### 2.2 Codifica delle Triple RDF (Struttura MAP-B)
82: 
83: Il sistema utilizza lo schema **MAP-B** (Multiply-Add-Permute, variante Bipolare) per codificare triple RDF. Il pattern fondamentale è:
84: 
85: **`encode(S, P, O) = vS ⊗ ρ¹(vP) ⊗ ρ²(vO)`**
86: 
87: ```java
88: // TopologicalVectorUpdater.java:97
89: return vS.bind(vP.permute(1)).bind(vO.permute(2));
90: ```
91: 
92: Dove:
93: - `⊗` (bind) è il **prodotto di Hadamard** (moltiplicazione elemento per elemento)
94: - `ρⁿ` (permute) è lo **shift ciclico** di `n` posizioni
95: 
96: La permutazione differenziata per ruolo (+1 per il predicato, +2 per l'oggetto) garantisce che i tre componenti occupino posizioni distinte e non commutino: `encode(S, P, O) ≠ encode(P, S, O)`. Questo è essenziale per preservare la direzionalità del grafo RDF.
97: 
98: #### Operazioni Algebriche nel Dettaglio
99: 
100: **Bind (⊗) — Prodotto di Hadamard:**
101: ```java
102: // HDVectorMapB.java:63-70
103: public HDVector bind(HDVector other) {
104:     HDVectorMapB o = (HDVectorMapB) other;
105:     byte[] result = new byte[D];
106:     for (int i = 0; i < D; i++) {
107:         result[i] = (byte) (this.values[i] * o.values[i]);
108:     }
109:     return new HDVectorMapB(result);
110: }
111: ```
112: Proprietà chiave: `(a ⊗ b) ⊗ b = a` (auto-inversione), poiché nel dominio {-1,+1} la moltiplicazione per due volte per lo stesso elemento restituisce l'identità.
113: 
114: **Bundle (+) — Somma con Segno (Majority Vote):**
115: ```java
116: // HDVectorMapB.java:73-89
117: public HDVector bundle(HDVector other) {
118:     HDVectorMapB o = (HDVectorMapB) other;
119:     byte[] result = new byte[D];
120:     for (int i = 0; i < D; i++) {
121:         int sum = this.values[i] + o.values[i];
122:         if (sum > 0)        result[i] = 1;
123:         else if (sum < 0)   result[i] = -1;
124:         else                result[i] = (byte) ((i % 2 == 0) ? 1 : -1);
125:     }
126:     return new HDVectorMapB(result);
127: }
128: ```
129: Il bundle di due vettori usa un tie-breaker **posizionale deterministico** (parità dell'indice `i % 2`), eliminando ogni fonte di non-determinismo.
130: 
131: **Bundle Simultaneo (N vettori):**
132: ```java
133: // HDVectorMapB.java:37-60
134: public static HDVector bundleSimultaneous(List<HDVector> vectors) {
135:     int[] sum = new int[D];
136:     long localSeed = 0;
137:     for (HDVector v : vectors) {
138:         HDVectorMapB bv = (HDVectorMapB) v;
139:         localSeed ^= Arrays.hashCode(bv.values);  // seed da XOR degli hash
140:         for (int i = 0; i < D; i++) sum[i] += bv.values[i];
141:     }
142:     byte[] result = new byte[D];
143:     Random tieBreaker = new Random(localSeed != 0 ? localSeed : 42L);
144:     for (int i = 0; i < D; i++) {
145:         if (sum[i] > 0)       result[i] = 1;
146:         else if (sum[i] < 0)  result[i] = -1;
147:         else                  result[i] = (byte) (tieBreaker.nextBoolean() ? 1 : -1);
148:     }
149:     return new HDVectorMapB(result);
150: }
151: ```
152: Il seed deterministico è lo XOR degli hash code di tutti i vettori partecipanti: **stesso insieme di vettori → stesso risultato**, garantendo riproducibilità assoluta.
153: 
154: **Permute (ρⁿ) — Shift Ciclico:**
155: ```java
156: // HDVectorMapB.java:93-101
157: public HDVector permute(int shift) {
158:     byte[] result = new byte[D];
159:     for (int i = 0; i < D; i++) {
160:         int newIndex = (i + shift) % D;
161:         if (newIndex < 0) newIndex += D;
162:         result[newIndex] = this.values[i];
163:     }
164:     return new HDVectorMapB(result);
165: }
166: ```
167: Lo shift di +100 posizioni per i macro-branch differenzia ciascun ramo nell'albero. Poiché `D = 10000`, lo shift "+100" preserva la quasi-ortogonalità: `cosine(v, ρ¹⁰⁰(v)) ≈ 0`.
168: 
169: **Similarity — Cosine su Vettori Bipolari:**
170: ```java
171: // HDVectorMapB.java:104-111
172: public double similarity(HDVector other) {
173:     HDVectorMapB o = (HDVectorMapB) other;
174:     int dotProduct = 0;
175:     for (int i = 0; i < D; i++) {
176:         dotProduct += this.values[i] * o.values[i];
177:     }
178:     return (double) dotProduct / D;
179: }
180: ```
181: Valori: `1.0` = identici, `0.0` = ortogonali (rumore), `-1.0` = opposti. La similarità attesa tra due vettori generati indipendentemente è `0.0` con deviazione standard `1/√D ≈ 0.01`.
182: 
183: ### 2.3 Generazione dei Vettori Atomici
184: 
185: I vettori di base per entità e proprietà sono generati in modo completamente **deterministico e riproducibile** tramite hashing crittografico:
186: 
187: ```java
188: // RandomGenerationStrategy.java:12-13
189: long seed = UUID.nameUUIDFromBytes(uri.getBytes(StandardCharsets.UTF_8))
190:     .getMostSignificantBits();
191: return HDVectorMapB.generateSeeded(seed);
192: ```
193: 
194: Pipeline: `URI → UTF-8 bytes → UUID.nameUUIDFromBytes (MD5) → 64-bit seed → java.util.Random(seed) LCG → 10000 bit bipolari`
195: 
196: Questo garantisce tre proprietà fondamentali:
197: 1. **Stesso URI → stesso vettore** su qualsiasi JVM, OS, o esecuzione
198: 2. **Nessun database**: i vettori sono rigenerati on-the-fly, non persistiti
199: 3. **URI diversi → vettori quasi-ortogonali** con probabilità `1 - 2⁻⁶⁴` di collisione
200: 
201: ### 2.4 Codifica Gerarchica: Il Simpkin Tree
202: 
203: Il sistema organizza le triple di un'entità in una struttura ad albero ispirata al **Simpkin Tree** dell'HDC:
204: 
205: ```
206: Entità E (es. "Amleto")
207: ├── Ramo: proprietà P1 (es. "author") ⊗ ρ¹⁰⁰
208: │   └── Chunk P1 = bundle(triple₁, triple₂, ..., tripleₙ, stopVec, [parity])
209: ├── Ramo: proprietà P2 (es. "genre") ⊗ ρ¹⁰⁰
210: │   └── Chunk P2 = bundle(triple₁, ..., stopVec, [parity])
211: ├── ...
212: ├── Identità pura: vE
213: └── Parity fix (se necessario)
214:          ↓ bundle simultaneo
215:     TreeVector(E) = radice dell'albero
216: ```
217: 
218: ```java
219: // TopologicalVectorUpdater.java:21-61
220: private void updateNodeVectorHierarchical(ItemMemory memory, Resource node, Model model) {
221:     String entityUri = node.getURI();
222:     HDVector pureIdentity = memory.getOrGenerate(entityUri);
223: 
224:     // Raggruppa triple per predicato
225:     Map<Property, List<Statement>> buckets = allStatements.stream()
226:             .collect(Collectors.groupingBy(Statement::getPredicate));
227: 
228:     List<HDVector> macroBranches = new ArrayList<>();
229:     for (Map.Entry<Property, List<Statement>> entry : buckets.entrySet()) {
230:         Property predicate = entry.getKey();
231:         List<Statement> triples = entry.getValue();
232:         HDVector predicateVector = memory.getOrGenerate(predicate.getURI());
233: 
234:         // Chunk: bundle delle triple per questo predicato
235:         HDVector branchContent = (triples.size() <= MAX_CHUNK_CAPACITY) ?
236:                 buildSimpleChunk(triples, memory) :
237:                 buildRecursiveSubTree(triples, memory);
238: 
239:         // Salva il chunk nella memoria per clean-up futuro
240:         String chunkKey = entityUri + ":chunk:" + predicate.getURI();
241:         memory.saveChunkVector(chunkKey, branchContent);
242: 
243:         // Ramo = Chunk ⊗ Predicato ⊗ ρ¹⁰⁰
244:         macroBranches.add(branchContent.bind(predicateVector).permute(100));
245:     }
246: 
247:     // Aggiunge identità pura e parity fix
248:     macroBranches.add(pureIdentity);
249:     if (macroBranches.size() % 2 == 0) {
250:         macroBranches.add(memory.getOrGenerate("vsa:internal:parity_fix"));
251:     }
252: 
253:     // Bundle simultaneo di tutti i rami = TreeVector(E)
254:     HDVector rootVector = HDVectorMapB.bundleSimultaneous(macroBranches);
255:     memory.saveTreeVector(entityUri, rootVector);
256: }
257: ```
258: 
259: Caratteristiche architetturali del Simpkin Tree:
260: - **Capacità massima per chunk**: 30 triple (`MAX_CHUNK_CAPACITY`). Oltre questa soglia, il chunk viene ricorsivamente suddiviso in sotto-chunk.
261: - **Stop vector**: Un delimitatore semantico (`vsa:internal:stop`) separa i chunk, migliorando il recupero in fase di clean-up.
262: - **Parity fix**: Se il numero di componenti nel bundle (o di macro-branch nell'albero) è pari, viene aggiunto un vettore di parità per garantire che la somma non collassi a zero su nessuna dimensione.
263: - **Permutazione +100**: Ogni ramo è shiftato di 100 posizioni per occupare una regione distinta dello spazio vettoriale, prevenendo interferenza tra rami diversi.
264: 
265: ### 2.5 Memoria a Tre Livelli (ItemMemory)
266: 
267: ```java
268: // ItemMemory.java
269: // 1. Foglie — Vettori atomici puri (entità, proprietà)
270: private final Map<String, HDVector> atomicMemory = new ConcurrentHashMap<>();
271: 
272: // 2. Chunk — Bundle di triple per singolo predicato
273: private final Map<String, HDVector> chunkMemory = new ConcurrentHashMap<>();
274: 
275: // 3. Radici — Simpkin Tree completi (TreeVector)
276: private final Map<String, HDVector> treeMemory = new ConcurrentHashMap<>();
277: ```
278: 
279: La separazione in tre livelli è critica per il clean-up: quando si tenta di estrarre un chunk da un TreeVector rumoroso, si cerca nella `chunkMemory` (non nella `atomicMemory`), perché il segnale del chunk (bundle di triple) ha una firma statistica diversa da quella di un vettore atomico.
280: 
281: ### 2.6 Integrazione Semantica
282: 
283: Il sistema dispone di due strategie di generazione vettoriale:
284: 
285: | Strategia | Classe | Meccanismo | Usata di default? |
286: |-----------|--------|------------|-------------------|
287: | **Random** | `RandomGenerationStrategy` | URI → UUID → seed → LCG → bipolare | **Sì** (entrambe le App) |
288: | **Semantic** | `SemanticEmbeddingStrategy` | URI → MiniLM embedding (384-d) → LSH → bipolare 10000-d | No |
289: 
290: La `SemanticEmbeddingStrategy` proietta embedding semantici densi nello spazio bipolare tramite una matrice di proiezione LSH fissa (seed=42), preservando le relazioni di similarità semantica: entità simili (es. "Apollo 11" e "Vostok 1") ricevono vettori con similarità > 0, anziché essere quasi-ortogonali come nella strategia random. Questa strategia non è abilitata di default, ma il suo utilizzo migliorerebbe la risonanza analogica tra domini affini.
291: 
292: ---
293: 
294: ## 3. Meccanismo di Ragionamento (Reasoning)
295: 
296: ### 3.1 Principio dell'Unbinding
297: 
298: Il ragionamento avviene per **algebra lineare inversa**: grazie all'auto-inversione del binding bipolare e all'inversione della permutazione ciclica, è possibile estrarre selettivamente componenti da un vettore composito.
299: 
300: **Teorema di Estrazione dal Simpkin Tree:**
301: 
302: Dato `Tree(E) = bundle(..., Chunk_P ⊗ vP ⊗ ρ¹⁰⁰, ...)` dove `Chunk_P = bundle(encode(S₁,P,O₁), ..., encode(Sₙ,P,Oₙ))`:
303: 
304: ```
305: Tree(E) ⊗ ρ⁻¹⁰⁰(vP) ≈ Chunk_P + rumore
306: ```
307: 
308: Il risultato è una versione rumorosa del chunk, dove il rumore proviene dagli altri rami dell'albero che, essendo quasi-ortogonali, contribuiscono con similarità attesa ≈ 0.
309: 
310: ### 3.2 Clean-Up Statistico (Z-Score)
311: 
312: Il clean-up è il meccanismo che separa il segnale dal rumore. Opera in due fasi:
313: 
314: **Fase 1: Ricerca del miglior candidato.**
315: Per ogni vettore `candidate` nel memory bank, calcola `sim = cosine(noisyVector, candidate)`.
316: 
317: **Fase 2: Test di significatività statistica.**
318: Calcola media e deviazione standard di tutte le similarità, poi verifica:
319: 
320: ```
321: Z-Score = (bestSimilarity - mean) / stdDev ≥ threshold
322: ```
323: 
324: ```java
325: // ItemMemory.java:73-113
326: private HDVector performStatisticalCleanUp(HDVector noisyVector,
327:         Map<String, HDVector> memoryBank, double thresholdSigma) {
328:     if (memoryBank.isEmpty()) return null;
329: 
330:     List<Double> similarities = new ArrayList<>();
331:     double bestSimilarity = -1.0;
332:     HDVector bestMatch = null;
333:     String bestKey = null;
334: 
335:     for (Map.Entry<String, HDVector> entry : memoryBank.entrySet()) {
336:         double sim = noisyVector.similarity(entry.getValue());
337:         similarities.add(sim);
338:         if (sim > bestSimilarity) {
339:             bestSimilarity = sim; bestMatch = entry.getValue();
340:             bestKey = entry.getKey();
341:         }
342:     }
343: 
344:     // Calcolo Z-Score
345:     double mean = similarities.stream().mapToDouble(d -> d).average().orElse(0);
346:     double variance = similarities.stream()
347:         .mapToDouble(s -> (s - mean) * (s - mean)).average().orElse(0);
348:     double stdDev = Math.sqrt(variance);
349:     double sigmaFromMean = (stdDev > 0) ? (bestSimilarity - mean) / stdDev : 0;
350: 
351:     if (stdDev > 0 && sigmaFromMean >= thresholdSigma) return bestMatch;
352:     return null;
353: }
354: ```
355: 
356: La soglia è dinamica: `threshold = log₁₀(D) = 4.0 σ` (per D=10000). Questo è un design intenzionale: a D=100000 la soglia salirebbe automaticamente a 5.0 σ, adattandosi alla maggiore capacità dello spazio.
357: 
358: ### 3.3 Pipeline di Inferenza Completa
359: 
360: **STEP 1 — Scoperta del Ruolo nel Dominio Sorgente:**
361: 
362: ```
363: Input: A (Apollo 11), B (Neil Armstrong), Tree(A)
364: 
365: Per ogni proprietà P ∈ proprietà(A):
366:   noisyBranch  = Tree(A) ⊗ ρ⁻¹⁰⁰(vP)      // 1. Estrai il ramo
367:   cleanBranch  = cleanUpChunk(noisyBranch)  // 2. Purifica il chunk
368:   if (cleanBranch == null) continue;        //    Ramo perso nel rumore
369:   
370:   noisyObject  = cleanBranch ⊗ vA ⊗ ρ¹(vP) ⊗ ρ⁻²  // 3. Unbind soggetto e predicato
371:   sim          = cosine(noisyObject, vB)            // 4. Confronta con B
372: 
373: Output: sourceRole = argmax_P sim  (es. "crew members" P1029)
374: ```
375: 
376: Questo step determina *quale relazione* connette A a B, senza che questa informazione sia esplicitamente nota al sistema.
377: 
378: **STEP 2 — Ponte Semantico (Traduzione Cross-Ontologia):**
379: 
380: ```
381: Input:  sourceRoleLabel ("crew members"), proprietà(targetEntity C)
382: 
383: Per ogni proprietà Q ∈ proprietà(C):
384:   sim = cosine(embed(sourceRoleLabel), embed(Q.label))
385:   
386: Output: targetRole = argmax_Q sim  (es. "crew members" in Vostok 1)
387: ```
388: 
389: Il matching è puramente basato su similarità coseno tra embedding MiniLM (384-d). Se la similarità supera la soglia configurata (tipicamente 0.30-0.40), la proprietà viene selezionata. Opzionalmente, le label vengono arricchite con i type constraint di Wikidata (es. "crew members (value type: human)") per migliorare la precisione del matching.
390: 
391: **STEP 3 & 4 — Proiezione ed Estrazione nel Dominio Target:**
392: 
393: ```
394: Input: C (Vostok 1), targetRole (P1029), Tree(C)
395: 
396: noisyBranch   = Tree(C) ⊗ ρ⁻¹⁰⁰(targetRole)     // 1. Estrai ramo del target
397: cleanBranch   = cleanUpChunk(noisyBranch)         // 2. Purifica chunk
398: noisyObject   = cleanBranch ⊗ vC ⊗ ρ¹(targetRole) ⊗ ρ⁻²  // 3. Unbind
399: finalAnalogue = cleanUpRelative(noisyObject)      // 4. Clean-up finale
400: 
401: Output: D = finalAnalogue → label(D)  (es. "Yuri Gagarin")
402: ```
403: 
404: ### 3.4 Gestione dei Path Multi-Hop
405: 
406: Il sistema non esegue esplicitamente path multi-hop. Tuttavia, il meccanismo di unbinding dal Simpkin Tree realizza implicitamente un reasoning a due hop:
407: 
408: ```
409: Hop 1: E → (Tree(E) ⊗ ρ⁻¹⁰⁰(vP)) → Chunk_P
410: Hop 2: Chunk_P → (Chunk_P ⊗ vE ⊗ ρ¹(vP) ⊗ ρ⁻²) → Oggetto O
411: ```
412: 
413: Dove il passaggio intermedio (Chunk_P) è un bundle di *tutte* le triple con predicato P, e l'estrazione dell'oggetto specifico avviene proprio grazie all'unbinding del soggetto: `Chunk_P ⊗ vA` sopprime tutti gli oggetti che non appartengono ad A, facendo emergere solo l'oggetto desiderato.
414: 
415: ---
416: 
417: ## 4. Tolleranza all'Incompletezza (Open-World Assumption)
418: 
419: ### 4.1 Principio Olografico della VSA
420: 
421: La proprietà fondamentale che abilita il reasoning open-world è la **natura olografica** dei vettori VSA. In un vettore olografico:
422: 
423: 1. **L'informazione è distribuita** su tutte le D dimensioni. Non esiste una corrispondenza localizzata "dimensione i = entità X". Ogni dimensione contribuisce a rappresentare ogni entità.
424: 
425: 2. **La sovrapposizione è non-distruttiva**. Il bundle di N vettori produce un vettore che risuona con *ciascuno* dei componenti originali, anche se il rapporto segnale-rumore (SNR) si degrada come `1/√N`.
426: 
427: 3. **Le operazioni sono associative e commutative dove appropriato**, permettendo di riordinare le query senza cambiare il risultato.
428: 
429: ### 4.2 Implicazioni per l'Open-World QA
430: 
431: **Entità non viste nel training:**
432: Il sistema non ha una fase di training. Ogni URI viene convertita in un vettore deterministico al volo. Un'entità mai incontrata prima (es. un nuovo film su Wikidata) riceve immediatamente un vettore, e il suo Simpkin Tree viene costruito scaricando il grafo locale in tempo reale. Non esiste alcuna distinzione tra "entità viste" ed "entità nuove".
433: 
434: **Relazioni incomplete:**
435: La similarità tra vettori atomici nello spazio a 10000 dimensioni permette di inferire relazioni non esplicitamente presenti. Se due entità hanno vettori simili (es. entrambe sono "missioni spaziali"), il sistema può trasferire pattern relazionali tra di esse anche in assenza di una connessione diretta nel grafo.
436: 
437: **Path non materializzati:**
438: Il clean-up statistico è il meccanismo chiave. Anche se il segnale di unbinding è parzialmente degradato dal rumore (SNR basso), il test Z-Score permette di distinguere un match genuino dal rumore di fondo. La soglia `log₁₀(D)` fornisce un intervallo di confidenza formale: con D=10000, un match a 4.0 σ ha probabilità di falso positivo inferiore a `3.2 × 10⁻⁵`.
439: 
440: ### 4.3 Limiti dell'Approccio Corrente
441: 
442: L'analisi del codice rivela una criticità documentata nel `PROJECT_REPORT.md`: il **clean-up intermedio dei chunk fallisce frequentemente** quando il numero di proprietà per entità è elevato (30-50), perché il bundle di molti rami degrada l'SNR di ciascun ramo. Il workaround ("Holographic Bypass") salta il clean-up intermedio, ma questo propaga il rumore allo step successivo. Possibili soluzioni esplorate nel report:
443: - Aumentare `D` a 100000 (10× capacità)
444: - Soglia di clean-up adattiva inversamente proporzionale al numero di candidati
445: - Utilizzare `SemanticEmbeddingStrategy` per ridurre l'ortogonalità tra entità correlate
446: 
447: ---
448: 
449: ## 5. Ruolo dell'LLM
450: 
451: ### 5.1 Natura del "LLM" nel Sistema
452: 
453: **Il sistema non effettua alcuna chiamata API a un Large Language Model esterno.** Il package `llm/` e il nome della classe `OntologyTranslator` sono fuorvianti: ciò che il codice chiama "LLM" è in realtà:
454: 
455: ```java
456: // OntologyTranslator.java:18-19
457: private final EmbeddingModel llm;
458: this.llm = new AllMiniLmL6V2EmbeddingModel();
459: ```
460: 
461: Il modello `all-MiniLM-L6-v2` è un **encoder embedding** locale (non generativo) di 384 dimensioni, eseguito interamente in-process tramite LangChain4j. Il download del modello avviene automaticamente al primo utilizzo (~22 MB).
462: 
463: ### 5.2 Ruoli Specifici dell'Embedding Model
464: 
465: L'embedding model (non un LLM generativo) viene utilizzato per tre compiti esclusivamente basati su **cosine similarity**:
466: 
467: 1. **Disambiguazione entità (Reranker):**
468: ```java
469: // OntologyTranslator.java:178-209
470: public ResolvedEntity disambiguateCandidates(String targetClassLabel,
471:         List<ResolvedEntity> candidates) {
472:     // combinedScore = classSimilarity × 0.60 + sitelinksNorm × 0.40
473:     // classSimilarity = cosine(embed(targetClass), embed(candidate.classLabel))
474: }
475: ```
476: Date più entità candidate con lo stesso nome (es. "Nile" può essere il fiume, un film, una band), il reranker seleziona quella la cui classe ontologica (P31) è semanticamente più vicina alla classe dell'entità sorgente.
477: 
478: 2. **Verifica compatibilità ontologica:**
479: ```java
480: // OntologyTranslator.java:168-176
481: public boolean areClassesCompatible(String classLabelA, String classLabelB,
482:         double threshold) {
483:     double sim = CosineSimilarity.between(
484:         llm.embed(classLabelA).content(), llm.embed(classLabelB).content());
485:     return sim >= threshold;  // default: 0.35
486: }
487: ```
488: Esempio: `cosine(embed("human spaceflight"), embed("space mission")) = 0.73 ≥ 0.35 → compatibili`.
489: 
490: 3. **Traduzione cross-ontologica (Semantic Bridge):**
491: ```java
492: // OntologyTranslator.java:114-166
493: public String findSemanticEquivalent(String sourceLabel, String sourcePropertyUri,
494:         Map<String, String> targetProperties, double threshold) {
495:     Embedding sourceEmbedding = llm.embed(enrichedSource).content();
496:     for (Map.Entry<String, String> entry : targetProperties.entrySet()) {
497:         Embedding targetEmbedding = llm.embed(entry.getValue()).content();
498:         double similarity = CosineSimilarity.between(sourceEmbedding, targetEmbedding);
499:         if (similarity > bestScore) { bestScore = similarity; bestUriMatch = entry.getKey(); }
500:     }
501:     return (bestScore >= threshold) ? bestUriMatch : null;
502: }
503: ```
504: 
505: ### 5.3 Numero di Chiamate di Embedding per Query
506: 
507: Per una singola query analogica, il numero di embedding calcolati è:
508: 
509: | Fase | Chiamate di embedding | Note |
510: |------|----------------------|------|
511: | Reranking | `N_candidates` embedding (class label) | Tipicamente 1-5 |
512: | Compatibilità | 2 embedding (class sorgente + class target) | |
513: | Ponte semantico | 1 embedding (ruolo sorgente) + `M_proprietà` (proprietà target) | M ≈ 30-50 per entità Wikidata |
514: | **Totale** | **~35-60 embedding** | Tutti in-process, nessuna API esterna |
515: 
516: Tutti gli embedding sono calcolati localmente e cachati in `ConcurrentHashMap` per evitare ricalcoli.
517: 
518: ### 5.4 Arricchimento con Type Constraints
519: 
520: Opzionalmente, le label delle proprietà vengono arricchite con il tipo atteso (value-type constraint di Wikidata) prima dell'embedding:
521: 
522: ```java
523: // OntologyTranslator.java:26-44
524: // Esempio: "composer" → "composer (value type: human)"
525: enriched.put(uri, label + " (value type: " + expectedType + ")");
526: ```
527: 
528: Questo migliora la precisione del matching cross-ontologia, specialmente quando due domini usano nomi di proprietà diversi per lo stesso concetto astratto.
529: 
530: ---
531: 
532: ## 6. Efficienza e Complessità Computazionale
533: 
534: ### 6.1 Complessità Temporale (Big-O)
535: 
536: | Operazione | Complessità | Note |
537: |-----------|-------------|-------|
538: | **Generazione vettore atomico** | `O(D)` | Hashing + LCG, one-shot per URI |
539: | **Bind (⊗)** | `O(D)` | Moltiplicazione element-wise |
540: | **Bundle (2 vettori)** | `O(D)` | Somma + signum |
541: | **Bundle (K vettori)** | `O(K·D)` | Somma + signum + tie-break |
542: | **Permute (ρⁿ)** | `O(D)` | Copia con shift ciclico |
543: | **Similarity** | `O(D)` | Dot product normalizzato |
544: | **Clean-up (M candidati)** | `O(M·D)` | M similarità + media/varianza |
545: | **Costruzione Simpkin Tree** | `O(P·C·D)` | P proprietà, C chunk build, D operazioni |
546: | **Role Deduction (STEP 1)** | `O(P·(M_chunk·D + M_atom·D))` | Per ogni proprietà P: unbind + 2 clean-up |
547: | **Semantic Bridge (STEP 2)** | `O(Q·E)` | Q proprietà target, E = dim embedding (384) |
548: | **Extraction (STEP 3-4)** | `O(M_chunk·D + M_atom·D)` | Unbind + 2 clean-up |
549: 
550: Dove:
551: - `D = 10000` (dimensionalità VSA)
552: - `P =` numero di proprietà dell'entità sorgente (~30-50)
553: - `C =` dimensione media di un chunk (≤ 30 triple)
554: - `M_chunk ≈ P` (numero di chunk nel memory bank)
555: - `M_atom =` numero di vettori atomici generati (~100-200)
556: - `E = 384` (dimensionalità embedding MiniLM)
557: 
558: ### 6.2 Complessità Spaziale (Big-O)
559: 
560: | Struttura | Footprint | Note |
561: |-----------|-----------|------|
562: | **Vettore HD (1×)** | `D × 1 byte = 10 KB` | byte[] di 10000 elementi |
563: | **atomicMemory** | `O(N_atom · D)` | ~100-200 vettori → 1-2 MB |
564: | **chunkMemory** | `O(E·P · D)` | E entità, P proprietà → ~5-15 MB |
565: | **treeMemory** | `O(E · D)` | ~10 KB per entità |
566: | **MiniLM Model** | 22 MB | Caricato una volta in RAM |
567: | **projectionMatrix** (LSH) | `E × D × 4 bytes = 384 × 10000 × 4 = 15.36 MB` | Solo se SemanticEmbeddingStrategy è attiva |
568: | **Totale stimato** | **~40-60 MB** | Per un'esecuzione tipica |
569: 
570: ### 6.3 Assenza di Training
571: 
572: Il sistema opera interamente in **forward-pass algebrico**:
573: 
574: - **Nessuna backpropagation**: non ci sono pesi addestrabili. Tutti i vettori sono generati deterministicamente o calcolati tramite operazioni algebriche.
575: - **Nessuna discesa del gradiente**: l'ottimizzazione è sostituita dal clean-up statistico (Z-Score).
576: - **Nessun dataset di training**: la conoscenza proviene interamente da Wikidata in tempo reale.
577: - **Nessun fine-tuning**: il modello MiniLM è usato off-the-shelf, solo per embedding.
578: 
579: Questo rende il sistema adatto a scenari dove i dati cambiano frequentemente e il retraining non è praticabile.
580: 
581: ### 6.4 Profilo di Esecuzione
582: 
583: Per una tipica query analogica (es. Amleto:Shakespeare = Lacrimosa:?), il costo dominante è:
584: 
585: 1. **3 query SPARQL a Wikidata** (entity resolution + 2× graph ingestion): latenza di rete, ~500ms-2s ciascuna
586: 2. **~50 embedding MiniLM**: computazione locale, ~10-50ms totali
587: 3. **Costruzione Simpkin Tree (2 entità)**: O(P·D) ~ 30×10000 = 300K operazioni, < 1ms
588: 4. **Role Deduction (30 proprietà)**: O(30·(30×10000 + 100×10000)) = O(39M) operazioni, ~20-50ms
589: 5. **Extraction finale**: O(30×10000 + 100×10000) = O(1.3M) operazioni, ~1-5ms
590: 
591: **Latenza totale stimata**: 2-6 secondi, dominata dalle chiamate HTTP a Wikidata.
592: 
593: ---
594: 
595: ## 7. Riepilogo delle Innovazioni Architetturali
596: 
597: 1. **Ragionamento puramente algebrico**: L'intera pipeline di inferenza è composta da moltiplicazioni, somme, shift e dot product — operazioni lineari senza training.
598: 
599: 2. **Memoria olografica senza database**: I vettori non sono memorizzati ma rigenerati deterministicamente da URI. Il sistema scala orizzontalmente senza overhead di sincronizzazione.
600: 
601: 3. **Clean-up statistico adattivo**: La soglia Z-Score `log₁₀(D)` si adatta automaticamente alla dimensionalità, fornendo un framework formale per il controllo dei falsi positivi.
602: 
603: 4. **Ponte semantico neuro-simbolico**: La fusione di VSA (struttura) e embedding (semantica) permette il reasoning cross-ontologia senza richiedere un allineamento manuale degli schemi.
604: 
605: 5. **Open-world by design**: Ogni entità è istanziabile al volo. Non esiste un insieme chiuso di entità o relazioni. Il sistema può ragionare su qualsiasi nodo Wikidata senza pre-processing.

(End of file - total 605 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\.gitignore"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\.gitignore</path>
<type>file</type>
<content>
1: /target/
2: /.idea/
3: /*.iml
4: .DS_Store
5: *.log

(End of file - total 5 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\graphify-out\\GRAPH_REPORT.md"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\graphify-out\GRAPH_REPORT.md</path>
<type>file</type>
<content>
1: # Graph Report - .  (2026-05-22)
2: 
3: ## Corpus Check
4: - Corpus is ~13,441 words - fits in a single context window. You may not need a graph.
5: 
6: ## Summary
7: - 138 nodes · 233 edges · 14 communities (4 shown, 10 thin omitted)
8: - Extraction: 70% EXTRACTED · 30% INFERRED · 0% AMBIGUOUS · INFERRED: 71 edges (avg confidence: 0.82)
9: - Token cost: 0 input · 0 output
10: 
11: ## Community Hubs (Navigation)
12: - [[_COMMUNITY_Core Application & Entry Points|Core Application & Entry Points]]
13: - [[_COMMUNITY_VSA Vector Strategies|VSA Vector Strategies]]
14: - [[_COMMUNITY_Wikidata Entity Resolution|Wikidata Entity Resolution]]
15: - [[_COMMUNITY_Investigation Engine Core|Investigation Engine Core]]
16: - [[_COMMUNITY_Graph Traversal & Expansion|Graph Traversal & Expansion]]
17: - [[_COMMUNITY_Topological Vector Encoding|Topological Vector Encoding]]
18: - [[_COMMUNITY_Ontology Translation & Disambiguation|Ontology Translation & Disambiguation]]
19: - [[_COMMUNITY_SPARQL Endpoint Operations|SPARQL Endpoint Operations]]
20: - [[_COMMUNITY_Semantic Embedding Generation|Semantic Embedding Generation]]
21: - [[_COMMUNITY_Application Tests|Application Tests]]
22: - [[_COMMUNITY_VSA Component Tests|VSA Component Tests]]
23: - [[_COMMUNITY_Application Test Concept|Application Test Concept]]
24: - [[_COMMUNITY_HDVectorMapB Test Concept|HDVectorMapB Test Concept]]
25: 
26: ## God Nodes (most connected - your core abstractions)
27: 1. `ItemMemory` - 12 edges
28: 2. `HDVectorMapB (MAP-B Implementation D=10000)` - 11 edges
29: 3. `HDVectorMapB` - 10 edges
30: 4. `ItemMemory (3-Tier Memory + Z-Score Clean-Up)` - 10 edges
31: 5. `InvestigationEngine` - 9 edges
32: 6. `HDVector` - 9 edges
33: 7. `App (Main Pipeline Entry Point)` - 9 edges
34: 8. `GraphManager` - 8 edges
35: 9. `SemanticEmbeddingStrategy` - 8 edges
36: 10. `AppDis (Disambiguation Pipeline Entry Point)` - 8 edges
37: 
38: ## Surprising Connections (you probably didn't know these)
39: - `Wikidata Knowledge Graph` --conceptually_related_to--> `EntityResolver`  [INFERRED]
40:   Description.md → src/main/java/com/investigator/jena/EntityResolver.java
41: - `VSA MAP-B Encoding Paradigm` --conceptually_related_to--> `HDVectorMapB (MAP-B Implementation D=10000)`  [INFERRED]
42:   Description.md → src/main/java/com/investigator/vsa/HDVectorMapB.java
43: - `Z-Score Statistical Clean-Up Mechanism` --conceptually_related_to--> `ItemMemory (3-Tier Memory + Z-Score Clean-Up)`  [INFERRED]
44:   Description.md → src/main/java/com/investigator/vsa/ItemMemory.java
45: - `AGENTS_0.md (Build & Architecture Guide)` --references--> `App (Main Pipeline Entry Point)`  [EXTRACTED]
46:   AGENTS_0.md → src/main/java/com/investigator/App.java
47: - `Structural Analogy Problem (A:B = C:D over Open-World KG)` --conceptually_related_to--> `App (Main Pipeline Entry Point)`  [INFERRED]
48:   Description.md → src/main/java/com/investigator/App.java
49: 
50: ## Hyperedges (group relationships)
51: - **Simpkin Tree Encoding Pipeline** — topological_vector_updater, item_memory, hdvectormapb [INFERRED 0.90]
52: - **SPARQL Data Access Layer** — entity_resolver, triple_extractor, sparql_endpoint [INFERRED 0.85]
53: - **Semantic Bridge Subsystem (Embedding-Based Cross-Ontology Matching)** — ontology_translator, semantic_embedding_strategy, all_minilm_model [INFERRED 0.85]
54: 
55: ## Communities (14 total, 10 thin omitted)
56: 
57: ### Community 0 - "Core Application & Entry Points"
58: Cohesion: 0.19
59: Nodes (26): AGENTS_0.md (Build & Architecture Guide), all-MiniLM-L6-v2 Embedding Model (384-d), AppDis (Disambiguation Pipeline Entry Point), App (Main Pipeline Entry Point), Description.md (Technical Description), EntityResolver, GraphManager, HDVector (VSA Interface) (+18 more)
60: 
61: ### Community 1 - "VSA Vector Strategies"
62: Cohesion: 0.10
63: Nodes (4): RandomGenerationStrategy, VectorGenerationStrategy, HDVector, HDVectorMapB
64: 
65: ### Community 3 - "Investigation Engine Core"
66: Cohesion: 0.14
67: Nodes (3): InvestigationEngine, App, EntityResolver
68: 
69: ## Knowledge Gaps
70: - **4 isolated node(s):** `HDVectorMapBTest`, `HDVector (VSA Interface)`, `AppTest (JUnit 4 Skeleton)`, `HDVectorMapBTest (Empty Skeleton)`
71:   These have ≤1 connection - possible missing edges or undocumented components.
72: - **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.
73: 
74: ## Suggested Questions
75: _Questions this graph is uniquely positioned to answer:_
76: 
77: - **Why does `HDVectorMapB (MAP-B Implementation D=10000)` connect `Core Application & Entry Points` to `VSA Vector Strategies`?**
78:   _High betweenness centrality (0.267) - this node is a cross-community bridge._
79: - **Why does `HDVectorMapB` connect `VSA Vector Strategies` to `Topological Vector Encoding`?**
80:   _High betweenness centrality (0.243) - this node is a cross-community bridge._
81: - **Are the 2 inferred relationships involving `HDVectorMapB (MAP-B Implementation D=10000)` (e.g. with `VSA MAP-B Encoding Paradigm` and `Simpkin Tree (Hierarchical Vector Encoding)`) actually correct?**
82:   _`HDVectorMapB (MAP-B Implementation D=10000)` has 2 INFERRED edges - model-reasoned connections that need verification._
83: - **Are the 5 inferred relationships involving `ItemMemory (3-Tier Memory + Z-Score Clean-Up)` (e.g. with `InvestigationEngine (Orchestrator)` and `TopologicalVectorUpdater (Simpkin Tree Builder)`) actually correct?**
84:   _`ItemMemory (3-Tier Memory + Z-Score Clean-Up)` has 5 INFERRED edges - model-reasoned connections that need verification._
85: - **What connects `HDVectorMapBTest`, `HDVector (VSA Interface)`, `AppTest (JUnit 4 Skeleton)` to the rest of the system?**
86:   _4 weakly-connected nodes found - possible documentation gaps or missing edges._
87: - **Should `VSA Vector Strategies` be split into smaller, more focused modules?**
88:   _Cohesion score 0.09956709956709957 - nodes in this community are weakly interconnected._
89: - **Should `Investigation Engine Core` be split into smaller, more focused modules?**
90:   _Cohesion score 0.1437908496732026 - nodes in this community are weakly interconnected._

(End of file - total 90 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\src"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src</path>
<type>directory</type>
<entries>
graphify-out/
main/
test/

(3 entries)
</entries>
```

---

## Assistant (Build · DeepSeek V4 Pro · 5.3s)

_Thinking:_

Now let me explore the source code structure to understand the actual entrypoints, package layout, tests, and any build/test commands.

**Tool: glob**

**Input:**
```json
{
  "pattern": "src/main/java/**/*.java"
}
```

**Output:**
```
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\AppDis.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\llm\OntologyTranslator.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\jena\TripleExtractor.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\vsa\strategy\TopologicalVectorUpdater.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\vsa\HDVectorMapB.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\vsa\strategy\RandomGenerationStrategy.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\jena\EntityResolver.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\App.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\vsa\ItemMemory.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\jena\ResolvedEntity.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\engine\InvestigationEngine.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\jena\GraphManager.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\vsa\strategy\SemanticEmbeddingStrategy.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\vsa\HDVector.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\jena\SparqlEndpoint.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\vsa\strategy\VectorGenerationStrategy.java
```

**Tool: glob**

**Input:**
```json
{
  "pattern": "src/test/java/**/*.java"
}
```

**Output:**
```
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\test\java\com\investigator\vsa\HDVectorMapBTest.java
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\test\java\com\investigator\AppTest.java
```

**Tool: glob**

**Input:**
```json
{
  "pattern": "src/graphify-out/**/*"
}
```

**Output:**
```
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\stat-index.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\440ee0de462f043c03d508f868559e159e25e54672cfbe8860218bd9e088cf41.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\35c301807c4bfa834c10c5f75926b64d306c5098a6ecab3a3cfc81d332708dd7.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\226bfede7cee69de2e2c4f865f48d70e1c29c2abcef17e05118b2deb5c780879.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\b28f7e6a9af16ae8d4807b94287e46dcaba94a507a6e7b66e9a8209a2d287e62.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\92ccceb5f0f17f8aa84a6a28794ff88037b2908121c8cd87c516dc842b5c955a.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\8b9f50201128c9b267c2e477cc7b3ecff91856aea6469d0800d7eab7df1fbdf3.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\6c1c78d32e2ac66dbbc36c6edbb67e5bc0709b0cd8798c23488e3bd6627d1813.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\b2cc8e0d0f9b688e3d103874647eae6bc4d91c37719405b2f4749ceb973a9dc7.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\4e6009a46d0d37a944e6a78a89a059527753d263a0ca866653ca98eed89373bb.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\25248c1698f11a2dca00fc97d7fee3571ee819b5a471cc7545f7396c882398f6.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\e48c4fd1178b10578ad3879350ad169f4a9bf57eae8eb94f5885f813a907eb87.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\116ca11c6ee2ee8f66b720be0ecd18bca35ade8d4741a82b770e38901dc1764b.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\8c8b124009af6e329bb7bc3b962187809aeebf6842a0c187e93390e2eef31f87.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\f56a55b023c3fff8bc31b6a39d8f6f7c6789eccb7549f53b55d31ae54a005f0e.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\76f30db0064d66257737a6454583cc17618fed250a269151af26726dc36aac81.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\4c7f62de65f4f19be618a92108dbb49831189839043324c936e32c140459f026.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\03f7d0db28ff42ffb4a6b4efaafa1fc813bd73bdf529c44aa8b25134f832da7b.json
C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\graphify-out\cache\ast\99823ddbdab777557f9d864b3b2eb3ec7f0eb8bf3b39414c473db088a176ba2a.json
```

**Tool: glob**

**Input:**
```json
{
  "pattern": "opencode.json"
}
```

**Output:**
```
No files found
```

---

## Assistant (Build · DeepSeek V4 Pro · 6.4s)

_Thinking:_

Now let me read the test files and the entry points to verify the build/test commands and any configuration quirks.

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\src\\test\\java\\com\\investigator\\AppTest.java"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\test\java\com\investigator\AppTest.java</path>
<type>file</type>
<content>
1: package com.investigator;
2: 
3: import static org.junit.Assert.assertTrue;
4: 
5: import org.junit.Test;
6: 
7: /**
8:  * Unit test for simple App.
9:  */
10: public class AppTest 
11: {
12:     /**
13:      * Rigorous Test :-)
14:      */
15:     @Test
16:     public void shouldAnswerWithTrue()
17:     {
18:         assertTrue( true );
19:     }
20: }

(End of file - total 20 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\src\\test\\java\\com\\investigator\\vsa\\HDVectorMapBTest.java"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\test\java\com\investigator\vsa\HDVectorMapBTest.java</path>
<type>file</type>
<content>
1: package com.investigator.vsa;
2: 
3: public class HDVectorMapBTest {
4: }

(End of file - total 4 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\src\\main\\java\\com\\investigator\\App.java"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\App.java</path>
<type>file</type>
<content>
1: package com.investigator;
2: 
3: import com.investigator.llm.OntologyTranslator;
4: import com.investigator.vsa.*;
5: import com.investigator.vsa.strategy.RandomGenerationStrategy;
6: import com.investigator.vsa.strategy.TopologicalVectorUpdater;
7: import com.investigator.jena.*;
8: import com.investigator.engine.InvestigationEngine;
9: import org.apache.jena.rdf.model.*;
10: 
11: import java.util.*;
12: 
13: public class App {
14: public static void main(String[] args) {
15:     System.out.println("=== NEURO-SEMANTIC INVESTIGATOR: LA VERA ANALOGIA ===");
16:     System.out.println("=== Flusso: Risoluzione -> Allineamento Ontologico -> Scoperta VSA -> LLM -> Proiezione ===\n");
17: 
18:     ItemMemory itemMemory = new ItemMemory(new RandomGenerationStrategy());
19:     OntologyTranslator translator = new OntologyTranslator();
20: 
21:     SparqlEndpoint wikidataEndpoint = new SparqlEndpoint("https://query.wikidata.org/sparql");
22:     GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
23:     TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();
24: 
25:     EntityResolver resolver = new EntityResolver();
26: 
27:     System.out.println("[*] FASE 0: Risoluzione entità e profilazione ontologica...");
28: 
29: 
30:     List<ResolvedEntity> apolloResults = resolver.resolve("Amazon", 1);
31:     List<ResolvedEntity> armstrongResults = resolver.resolve("Brazil", 1);
32: 
33:     // CHIEDIAMO AL RESOLVER DI TROVARE L'APOLLO 13!
34:     // USS Enterprise
35: //    List<ResolvedEntity> targetResults = resolver.resolve("Apollo 13", 1);
36:     List<ResolvedEntity> targetResults = resolver.resolve("Nile", 1);
37: 
38:     if (apolloResults.isEmpty() || armstrongResults.isEmpty() || targetResults.isEmpty()) {
39:         System.out.println("   [ERRORE CRITICO] Una o più entità non sono state trovate dal motore di ricerca.");
40:         return;
41:     }
42: 
43:     ResolvedEntity apolloEntity = apolloResults.get(0);
44:     ResolvedEntity armstrongEntity = armstrongResults.get(0);
45:     ResolvedEntity triesteEntity = targetResults.get(0); // Manteniamo il nome variabile per non rompere il resto
46: 
47:     // Rinominiamo la variabile locale per mantenere la coerenza col resto del codice
48:     ResolvedEntity targetEntity = triesteEntity;
49: 
50: 
51:     System.out.println("\n=======================================================");
52:     System.out.println("   REPORT ONTOLOGICO PRELIMINARE");
53:     System.out.println("=======================================================");
54:     System.out.printf("   SORGENTE  : %-25s  Tipo: %s%n", apolloEntity.entityLabel(), apolloEntity.classLabel());
55:     System.out.printf("   OGG. NOTO : %-25s  Tipo: %s%n", armstrongEntity.entityLabel(), armstrongEntity.classLabel());
56:     System.out.printf("   TARGET 1  : %-25s  Tipo: %s%n", targetEntity.entityLabel(), targetEntity.classLabel());
57: 
58:     String apollo11Uri = apolloEntity.entityUri();
59:     String armstrongUri = armstrongEntity.entityUri();
60:     String currentTargetUri = targetEntity.entityUri();
61:     String currentTargetLabel = targetEntity.entityLabel();
62: 
63:     // ------------------------------------------------------------------------------------------------
64:     // LA MAGIA: ALLINEAMENTO ONTOLOGICO AUTOMATICO (Graph + LLM)
65:     // ------------------------------------------------------------------------------------------------
66:     if (!apolloEntity.classLabel().equals(targetEntity.classLabel())) {
67:         System.out.println("\n   [ALLARME ASIMMETRIA] Le ontologie non coincidono ('" + apolloEntity.classLabel() + "' vs '" + targetEntity.classLabel() + "').");
68:         System.out.println("   L'Investigatore interroga l'inconscio collettivo (Wikidata) per trovare una missione affine...");
69: 
70:         // Chiediamo a Wikidata tutti i nodi collegati al Trieste tramite le relazioni tipiche degli eventi.
71:         // P793 = significant event (Out), P2283 = uses (In), P516 = equipment (In)
72:         String sparqlQuery =
73:                 "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " +
74:                         "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
75:                         "SELECT DISTINCT ?event ?eventLabel ?classLabel WHERE { " +
76:                         "  { <" + currentTargetUri + "> wdt:P793 ?event . } " +
77:                         "  UNION { ?event wdt:P2283 <" + currentTargetUri + "> . } " +
78:                         "  UNION { ?event wdt:P516 <" + currentTargetUri + "> . } " +
79:                         "  OPTIONAL { ?event wdt:P31 ?class . ?class rdfs:label ?classLabel . FILTER(lang(?classLabel)='en') } " +
80:                         "  ?event rdfs:label ?eventLabel . FILTER(lang(?eventLabel)='en') " +
81:                         "} LIMIT 20";
82: 
83:         Map<String, String> candidateOntologies = new HashMap<>();
84:         Map<String, String> candidateLabels = new HashMap<>();
85: 
86:         try {
87:             org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
88:             try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
89:                     .query(query)
90:                     .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
91:                     .build()) {
92:                 org.apache.jena.query.ResultSet results = qexec.execSelect();
93:                 while (results.hasNext()) {
94:                     org.apache.jena.query.QuerySolution soln = results.next();
95:                     String evUri = soln.getResource("event").getURI();
96:                     String evLabel = soln.getLiteral("eventLabel").getString();
97:                     String clLabel = soln.contains("classLabel") ? soln.getLiteral("classLabel").getString() : "event";
98: 
99:                     // Creiamo una chiave composita per aiutare l'LLM: "Classe: Nome Entità"
100:                     candidateOntologies.put(evUri, clLabel + ": " + evLabel);
101:                     candidateLabels.put(evUri, evLabel);
102:                 }
103:             }
104:         } catch (Exception e) {
105:             System.out.println("   [ERRORE SPARQL] " + e.getMessage());
106:         }
107: 
108:         if (!candidateOntologies.isEmpty()) {
109:             System.out.println("   [LLM] Valuto " + candidateOntologies.size() + " candidati topologici rispetto a '" + apolloEntity.classLabel() + "'...");
110: 
111:             // L'LLM confronterà "human spaceflight" con cose come "expedition: Project Nekton" e "key event (ships): ship launching"
112:             String bestEventUri = translator.findSemanticEquivalent(apolloEntity.classLabel(), candidateOntologies, 0.20);
113: 
114:             if (bestEventUri != null) {
115:                 currentTargetUri = bestEventUri;
116:                 currentTargetLabel = candidateLabels.get(bestEventUri);
117:                 System.out.println("   [COMPENSAZIONE RIUSCITA] L'LLM ha isolato l'evento topologicamente corretto!");
118:                 System.out.printf("   NUOVO TARGET: %-25s%n", currentTargetLabel);
119:             } else {
120:                 System.out.println("   [FALLIMENTO] L'LLM non ha trovato nessun evento candidato affine a " + apolloEntity.classLabel());
121:                 return;
122:             }
123:         } else {
124:             System.out.println("   [FALLIMENTO] Il grafo locale non contiene collegamenti a eventi o missioni noti.");
125:             return;
126:         }
127:     }
128:     System.out.println("=======================================================\n");
129: 
130:     InvestigationEngine engine = new InvestigationEngine(new HDVectorMapB(), graphManager, itemMemory, topologicalUpdater);
131: 
132:     System.out.println("[*] Ingestione e vettorizzazione grafi in corso...");
133:     Resource apolloResource = ModelFactory.createDefaultModel().createResource(apollo11Uri);
134:     Resource targetResource = ModelFactory.createDefaultModel().createResource(currentTargetUri); // Usiamo il Target Allineato!
135: 
136:     engine.expandAndProcess(apolloResource);
137:     engine.expandAndProcess(targetResource);
138:     Model localModel = engine.getGraphManager().getLocalModel();
139: 
140:     HDVector apolloRoot = itemMemory.getTreeVector(apollo11Uri);
141:     HDVector armstrongVector = itemMemory.getOrGenerate(armstrongUri);
142: 
143:     System.out.println("\n=======================================================");
144:     System.out.println("   STEP 1: DEDUZIONE DEL RUOLO SORGENTE (VSA)          ");
145:     System.out.println("=======================================================");
146: 
147:     String sourceRoleUri = null;
148:     double bestHypothesisScore = -1.0;
149: 
150:     HDVector subjectVector = itemMemory.getOrGenerate(apollo11Uri);
151:     Set<Property> apolloProps = new HashSet<>();
152:     localModel.listStatements(apolloResource, null, (RDFNode) null).forEachRemaining(s -> apolloProps.add(s.getPredicate()));
153: 
154:     for (Property prop : apolloProps) {
155:         String propUri = prop.getURI();
156:         if (isMetadata(propUri)) continue;
157: 
158:         HDVector candidateRole = itemMemory.getOrGenerate(propUri);
159:         HDVector noisyBranch = apolloRoot.permute(-100).bind(candidateRole);
160:         HDVector cleanBranch = itemMemory.cleanUpChunk(noisyBranch);
161: 
162:         if (cleanBranch != null) {
163:             HDVector noisyObject = cleanBranch.bind(subjectVector).bind(candidateRole.permute(1)).permute(-2);
164:             double rawSimilarity = noisyObject.similarity(armstrongVector);
165: 
166:             if (rawSimilarity > 0.05 && rawSimilarity > bestHypothesisScore) {
167:                 bestHypothesisScore = rawSimilarity;
168:                 sourceRoleUri = propUri;
169:             }
170:         }
171:     }
172: 
173:     if (sourceRoleUri == null || bestHypothesisScore < 0.05) {
174:         System.out.println("   [ERRORE] La VSA non riesce a trovare Armstrong nel vettore dell'Apollo 11.");
175:         return;
176:     }
177: 
178:     String sourceRoleLabel = fetchLabelFromWikidata(sourceRoleUri);
179:     System.out.printf("   -> DEDUZIONE COMPLETATA: Armstrong è legato tramite '%s' (Sim: %.2f)\n", sourceRoleLabel, bestHypothesisScore);
180: 
181:     System.out.println("\n=======================================================");
182:     System.out.println("   STEP 2: IL PONTE ANALOGICO (LLM)                    ");
183:     System.out.println("=======================================================");
184: 
185:     // Attenzione qui: estraiamo le proprietà dal Target Allineato!
186:     Map<String, String> targetProperties = extractPropertyLabels(targetResource, localModel);
187:     System.out.println("   -> Chiedo all'LLM di tradurre '" + sourceRoleLabel + "' nel dominio di " + currentTargetLabel + "...");
188: 
189:     String targetRoleUri = translator.findSemanticEquivalent(sourceRoleLabel, targetProperties, 0.40);
190: 
191:     if (targetRoleUri == null) {
192:         System.out.println("   [ERRORE] Il traduttore non ha trovato un equivalente per '" + sourceRoleLabel + "'. Analogia fallita.");
193:         return;
194:     }
195:     String targetRoleLabel = fetchLabelFromWikidata(targetRoleUri);
196: 
197:     System.out.println("\n=======================================================");
198:     System.out.println("   STEP 3 & 4: PROIEZIONE E ESTRAZIONE TARGET (VSA)    ");
199:     System.out.println("=======================================================");
200: 
201:     HDVector targetRoot = itemMemory.getTreeVector(currentTargetUri);
202:     HDVector targetRole = itemMemory.getOrGenerate(targetRoleUri);
203:     HDVector targetSubject = itemMemory.getOrGenerate(currentTargetUri);
204: 
205:     System.out.println("   -> Uso la chiave tradotta ('" + targetRoleLabel + "') per aprire il vettore bersaglio...");
206:     HDVector noisyTargetBranch = targetRoot.permute(-100).bind(targetRole);
207:     HDVector cleanTargetBranch = itemMemory.cleanUpChunk(noisyTargetBranch);
208: 
209:     if (cleanTargetBranch == null) {
210:         System.out.println("   [ERRORE] Il ramo estratto si è disintegrato nel rumore.");
211:         return;
212:     }
213: 
214:     HDVector noisyTargetObject = cleanTargetBranch.bind(targetSubject).bind(targetRole.permute(1)).permute(-2);
215:     HDVector finalAnalogue = itemMemory.cleanUpRelative(noisyTargetObject);
216: 
217:     if (finalAnalogue != null) {
218:         System.out.println("\n[!] ANALOGIA RISOLTA CON SUCCESSO:");
219:         System.out.println("    Neil Armstrong  sta a  Apollo 11");
220:         System.out.println("    COME");
221: 
222:         String humanReadableResult = fetchLabelFromWikidata(itemMemory.getLastBestKey());
223:         System.out.println("    " + humanReadableResult + "  sta a  " + currentTargetLabel);
224:         System.out.println("\n    [Logica Applicata]: " + sourceRoleLabel + " ===> " + targetRoleLabel);
225:         System.out.println("    [Confidenza VSA]: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
226:     } else {
227:         System.out.println("\n[?] Ramo trovato, ma nessun individuo chiaro all'interno.");
228:     }
229: }
230: 
231:     private static String fetchDescriptionFromWikidata(String entityUri) {
232:         String sparqlQuery =
233:                 "PREFIX schema: <http://schema.org/> " +
234:                         "SELECT ?desc WHERE { " +
235:                         "  <" + entityUri + "> schema:description ?desc . " +
236:                         "  FILTER (lang(?desc) = 'en') " +
237:                         "} LIMIT 1";
238: 
239:         try {
240:             org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
241:             try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
242:                     .query(query)
243:                     .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
244:                     .build()) {
245: 
246:                 org.apache.jena.query.ResultSet results = qexec.execSelect();
247:                 if (results.hasNext()) {
248:                     return results.nextSolution().getLiteral("desc").getString();
249:                 }
250:             }
251:         } catch (Exception e) {
252:             System.err.println("   [AVVISO] Errore nell'estrazione della descrizione: " + e.getMessage());
253:         }
254:         return "";
255:     }
256: 
257:     private static Map<String, String> extractPropertyLabels(Resource entity, Model localGraph) {
258:         Map<String, String> labels = new HashMap<>();
259:         Set<Property> outgoingProps = new HashSet<>();
260:         Set<Property> incomingProps = new HashSet<>();
261: 
262:         // 1. FIGLI (Outgoing edges: Entità -> Predicato -> Oggetto)
263:         localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(s -> outgoingProps.add(s.getPredicate()));
264: 
265:         // 2. GENITORI (Incoming edges: Soggetto -> Predicato -> Entità)
266:         localGraph.listStatements(null, null, entity).forEachRemaining(s -> incomingProps.add(s.getPredicate()));
267: 
268:         System.out.println("   [INDAGINE ONTOLOGICA] Inventario delle proprietà del target:");
269: 
270:         System.out.println("   --- PROPRIETÀ IN USCITA (I Figli) ---");
271:         for (Property prop : outgoingProps) {
272:             String uri = prop.getURI();
273:             // IL FIX: Ignoriamo i metadati E ignoriamo i nodi intermedi (teniamo solo le proprietà "direct")
274:             if (isMetadata(uri) || !uri.contains("/direct/")) continue;
275: 
276:             String label = fetchLabelFromWikidata(uri);
277:             labels.put(uri, label);
278:             System.out.println("      - [OUT] " + uri + "  --->  Label: '" + label + "'");
279:         }
280: 
281:         System.out.println("   --- PROPRIETÀ IN ENTRATA (I Genitori) ---");
282:         for (Property prop : incomingProps) {
283:             String uri = prop.getURI();
284:             // IL FIX ANCHE QUI
285:             if (isMetadata(uri) || !uri.contains("/direct/")) continue;
286: 
287:             if (!labels.containsKey(uri)) {
288:                 String label = fetchLabelFromWikidata(uri);
289:                 labels.put(uri, label);
290:             }
291:             System.out.println("      - [IN]  " + uri + "  --->  Label: '" + labels.get(uri) + "'");
292:         }
293: 
294:         return labels;
295:     }
296: 
297:     private static String fetchLabelFromWikidata(String uri) {
298:         if (!uri.startsWith("http://www.wikidata.org/")) {
299:             return uri.replace("_", " ");
300:         }
301: 
302:         // Pulizia aggressiva di tutti i possibili namespace di Wikidata
303:         String entityUri = uri.replace("/prop/direct-normalized/", "/entity/")
304:                 .replace("/prop/direct/", "/entity/")
305:                 .replace("/prop/statement/", "/entity/")
306:                 .replace("/prop/", "/entity/");
307: 
308:         String sparqlQuery =
309:                 "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
310:                         "SELECT ?label WHERE { " +
311:                         "  <" + entityUri + "> rdfs:label ?label . " +
312:                         "  FILTER (lang(?label) = 'en') " +
313:                         "} LIMIT 1";
314: 
315:         try {
316:             org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
317:             try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
318:                     .query(query)
319:                     .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
320:                     .build()) {
321:                 org.apache.jena.query.ResultSet results = qexec.execSelect();
322:                 if (results.hasNext()) return results.nextSolution().getLiteral("label").getString();
323:             }
324:         } catch (Exception e) { /* Silenzioso */ }
325: 
326:         // Fallback: se fallisce, restituiamo almeno il codice
327:         String[] parts = uri.split("/");
328:         return parts[parts.length - 1];
329:     }
330: 
331:     private static boolean isMetadata(String uri) {
332:         return uri.contains("P18") || uri.contains("P373") || uri.contains("P2002") || uri.contains("P2013") || uri.contains("P137");
333:     }
334: }

(End of file - total 334 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\src\\main\\java\\com\\investigator\\AppDis.java"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\src\main\java\com\investigator\AppDis.java</path>
<type>file</type>
<content>
1: package com.investigator;
2: 
3: import com.investigator.llm.OntologyTranslator;
4: import com.investigator.vsa.*;
5: import com.investigator.vsa.strategy.RandomGenerationStrategy;
6: import com.investigator.vsa.strategy.TopologicalVectorUpdater;
7: import com.investigator.jena.*;
8: import com.investigator.engine.InvestigationEngine;
9: import org.apache.jena.rdf.model.*;
10: 
11: import java.util.*;
12: 
13: public class AppDis {
14:     public static void main(String[] args) {
15:         System.out.println("=== NEURO-SEMANTIC INVESTIGATOR: LA VERA ANALOGIA ===");
16:         System.out.println("=== Flusso: Risoluzione -> Reranking -> Scoperta VSA -> Encoder-Embedding -> Proiezione Olografica ===\n");
17: 
18:         ItemMemory itemMemory = new ItemMemory(new RandomGenerationStrategy());
19:         OntologyTranslator translator = new OntologyTranslator();
20: 
21:         SparqlEndpoint wikidataEndpoint = new SparqlEndpoint("https://query.wikidata.org/sparql");
22:         GraphManager graphManager = new GraphManager(wikidataEndpoint, new TripleExtractor());
23:         TopologicalVectorUpdater topologicalUpdater = new TopologicalVectorUpdater();
24: 
25:         EntityResolver resolver = new EntityResolver();
26: 
27:         System.out.println("[*] FASE 0: Risoluzione entità e profilazione ontologica...");
28: 
29:         // SORGENTE (entità da esplorare) e OGGETTO NOTO (da trovare dentro la sorgente)
30:         List<ResolvedEntity> apolloResults = resolver.resolve("Amleto", 1);
31:         List<ResolvedEntity> armstrongResults = resolver.resolve("William Shakespeare", 1);
32: 
33:         // IL TARGET: Tiriamo giù i TOP 5 per il Nilo per evitare il Popularity Bias!
34:         List<ResolvedEntity> targetResults = resolver.resolve("Lacrimosa", 5);
35: 
36:         if (apolloResults.isEmpty() || armstrongResults.isEmpty() || targetResults.isEmpty()) {
37:             System.out.println("   [ERRORE CRITICO] Una o più entità non sono state trovate dal motore di ricerca.");
38:             return;
39:         }
40: 
41:         ResolvedEntity apolloEntity = apolloResults.get(0);
42:         ResolvedEntity armstrongEntity = armstrongResults.get(0);
43: 
44:         // LA MAGIA DEL RERANKER VETTORIALE:
45:         // Chiediamo all'encoder di scegliere il Nilo che più assomiglia all'ontologia della sorgente (es. "river")
46:         ResolvedEntity targetEntity = translator.disambiguateCandidates(apolloEntity.classLabel(), targetResults);
47: 
48:         System.out.println("\n=======================================================");
49:         System.out.println("   REPORT ONTOLOGICO PRELIMINARE");
50:         System.out.println("=======================================================");
51:         System.out.printf("   SORGENTE  : %-25s  Tipo: %s%n", apolloEntity.entityLabel(), apolloEntity.classLabel());
52:         System.out.printf("   OGG. NOTO : %-25s  Tipo: %s%n", armstrongEntity.entityLabel(), armstrongEntity.classLabel());
53:         System.out.printf("   TARGET 1  : %-25s  Tipo: %s%n", targetEntity.entityLabel(), targetEntity.classLabel());
54:         System.out.println("=======================================================\n");
55: 
56:         String apollo11Uri = apolloEntity.entityUri();
57:         String armstrongUri = armstrongEntity.entityUri();
58:         String currentTargetUri = targetEntity.entityUri();
59:         String currentTargetLabel = targetEntity.entityLabel();
60: 
61:         // ------------------------------------------------------------------------------------------------
62:         // ALLINEAMENTO ONTOLOGICO AUTOMATICO (Graph + Encoder-Embedding)
63:         // ------------------------------------------------------------------------------------------------
64:         if (!translator.areClassesCompatible(apolloEntity.classLabel(), targetEntity.classLabel(), 0.35)) {
65:             System.out.println("\n   [ALLARME ASIMMETRIA] Le ontologie sono semanticamente distanti ('" + apolloEntity.classLabel() + "' vs '" + targetEntity.classLabel() + "').");
66:             System.out.println("   L'Investigatore interroga l'inconscio collettivo (Wikidata) per trovare un nodo affine...");
67: 
68:             String sparqlQuery =
69:                     "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " +
70:                             "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
71:                             "SELECT DISTINCT ?event ?eventLabel ?classLabel WHERE { " +
72:                             "  { <" + currentTargetUri + "> wdt:P793 ?event . } " +
73:                             "  UNION { ?event wdt:P2283 <" + currentTargetUri + "> . } " +
74:                             "  UNION { ?event wdt:P516 <" + currentTargetUri + "> . } " +
75:                             "  OPTIONAL { ?event wdt:P31 ?class . ?class rdfs:label ?classLabel . FILTER(lang(?classLabel)='en') } " +
76:                             "  ?event rdfs:label ?eventLabel . FILTER(lang(?eventLabel)='en') " +
77:                             "} LIMIT 20";
78: 
79:             Map<String, String> candidateOntologies = new HashMap<>();
80:             Map<String, String> candidateLabels = new HashMap<>();
81: 
82:             try {
83:                 org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
84:                 try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
85:                         .query(query)
86:                         .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
87:                         .build()) {
88:                     org.apache.jena.query.ResultSet results = qexec.execSelect();
89:                     while (results.hasNext()) {
90:                         org.apache.jena.query.QuerySolution soln = results.next();
91:                         String evUri = soln.getResource("event").getURI();
92:                         String evLabel = soln.getLiteral("eventLabel").getString();
93:                         String clLabel = soln.contains("classLabel") ? soln.getLiteral("classLabel").getString() : "event";
94: 
95:                         candidateOntologies.put(evUri, clLabel + ": " + evLabel);
96:                         candidateLabels.put(evUri, evLabel);
97:                     }
98:                 }
99:             } catch (Exception e) {
100:                 System.out.println("   [ERRORE SPARQL] " + e.getMessage());
101:             }
102: 
103:             if (!candidateOntologies.isEmpty()) {
104:                 System.out.println("   [Embedding] Valuto " + candidateOntologies.size() + " candidati topologici rispetto a '" + apolloEntity.classLabel() + "'...");
105: 
106:                 String bestEventUri = translator.findSemanticEquivalent(apolloEntity.classLabel(), candidateOntologies, 0.20);
107: 
108:                 if (bestEventUri != null) {
109:                     currentTargetUri = bestEventUri;
110:                     currentTargetLabel = candidateLabels.get(bestEventUri);
111:                     System.out.println("   [COMPENSAZIONE RIUSCITA] L'Embedding ha isolato l'evento topologicamente corretto!");
112:                     System.out.printf("   NUOVO TARGET: %-25s%n", currentTargetLabel);
113:                 } else {
114:                     System.out.println("   [FALLIMENTO] L'Embedding non ha trovato nessun evento candidato affine a " + apolloEntity.classLabel());
115:                     return;
116:                 }
117:             } else {
118:                 System.out.println("   [FALLIMENTO] Il grafo locale non contiene collegamenti a eventi o missioni noti.");
119:                 return;
120:             }
121:         }
122: 
123:         InvestigationEngine engine = new InvestigationEngine(new HDVectorMapB(), graphManager, itemMemory, topologicalUpdater);
124: 
125:         System.out.println("[*] Ingestione e vettorizzazione grafi in corso...");
126:         Resource apolloResource = ModelFactory.createDefaultModel().createResource(apollo11Uri);
127:         Resource targetResource = ModelFactory.createDefaultModel().createResource(currentTargetUri);
128: 
129:         engine.expandAndProcess(apolloResource);
130:         engine.expandAndProcess(targetResource);
131:         Model localModel = engine.getGraphManager().getLocalModel();
132: 
133:         HDVector apolloRoot = itemMemory.getTreeVector(apollo11Uri);
134:         HDVector armstrongVector = itemMemory.getOrGenerate(armstrongUri);
135: 
136:         System.out.println("\n=======================================================");
137:         System.out.println("   STEP 1: DEDUZIONE DEL RUOLO SORGENTE (VSA)          ");
138:         System.out.println("=======================================================");
139: 
140:         String sourceRoleUri = null;
141:         double bestHypothesisScore = -1.0;
142: 
143:         HDVector subjectVector = itemMemory.getOrGenerate(apollo11Uri);
144:         Set<Property> apolloProps = new HashSet<>();
145:         localModel.listStatements(apolloResource, null, (RDFNode) null).forEachRemaining(s -> apolloProps.add(s.getPredicate()));
146: 
147:         for (Property prop : apolloProps) {
148:             String propUri = prop.getURI();
149:             if (isMetadata(propUri)) continue;
150: 
151:             HDVector candidateRole = itemMemory.getOrGenerate(propUri);
152:             HDVector noisyBranch = apolloRoot.permute(-100).bind(candidateRole);
153:             HDVector cleanBranch = itemMemory.cleanUpChunk(noisyBranch);
154: 
155:             if (cleanBranch != null) {
156:                 HDVector noisyObject = cleanBranch.bind(subjectVector).bind(candidateRole.permute(1)).permute(-2);
157:                 double rawSimilarity = noisyObject.similarity(armstrongVector);
158: 
159:                 if (rawSimilarity > 0.05 && rawSimilarity > bestHypothesisScore) {
160:                     bestHypothesisScore = rawSimilarity;
161:                     sourceRoleUri = propUri;
162:                 }
163:             }
164:         }
165: 
166:         if (sourceRoleUri == null || bestHypothesisScore < 0.05) {
167:             System.out.println("   [ERRORE] La VSA non riesce a trovare l'Oggetto Noto nel vettore della Sorgente.");
168:             return;
169:         }
170: 
171:         String sourceRoleLabel = fetchLabelFromWikidata(sourceRoleUri);
172:         System.out.printf("   -> DEDUZIONE COMPLETATA: L'oggetto è legato tramite '%s' (Sim: %.2f)\n", sourceRoleLabel, bestHypothesisScore);
173: 
174:         System.out.println("\n=======================================================");
175:         System.out.println("   STEP 2: IL PONTE ANALOGICO (Embedding + Filtro Strutturale) ");
176:         System.out.println("=======================================================");
177: 
178:         Map<String, String> allTargetProperties = extractPropertyLabels(targetResource, localModel);
179:         Map<String, String> targetProperties = new HashMap<>();
180: 
181:         System.out.println("   [FILTRO STRUTTURALE RDF] Scrematura delle proprietà incompatibili...");
182:         // Sappiamo che l'Oggetto Noto (William Shakespeare) è un'Entità (Resource/URI).
183:         // Scartiamo tutte le proprietà del Target (Requiem) che puntano a stringhe, ID o date (Literal).
184: 
185:         for (Map.Entry<String, String> entry : allTargetProperties.entrySet()) {
186:             String propUri = entry.getKey();
187:             String propLabel = entry.getValue();
188: 
189:             boolean pointsToEntity = false;
190:             // Interroghiamo il grafo locale
191:             org.apache.jena.rdf.model.StmtIterator iter = localModel.listStatements(targetResource, localModel.getProperty(propUri), (RDFNode) null);
192:             while (iter.hasNext()) {
193:                 RDFNode obj = iter.next().getObject();
194:                 // Verifichiamo che l'oggetto sia una Risorsa e punti a un Q-node di Wikidata (un'entità)
195:                 if (obj.isResource() && obj.asResource().getURI() != null && obj.asResource().getURI().contains("/entity/Q")) {
196:                     pointsToEntity = true;
197:                     break; // Basta che uno degli oggetti sia un'entità per validare la proprietà
198:                 }
199:             }
200: 
201:             if (pointsToEntity) {
202:                 targetProperties.put(propUri, propLabel);
203:             }
204:         }
205: 
206:         System.out.println("   -> Proprietà rimaste dopo il filtro strutturale: " + targetProperties.size() + " su " + allTargetProperties.size());
207:         System.out.println("   -> Chiedo all'Embedding di tradurre '" + sourceRoleLabel + "' tra i candidati rimasti...");
208: 
209:         // Ora passiamo all'LLM SOLO le proprietà che puntano a entità.
210:         // Niente più suffissi finti, passiamo le label pulite!
211:         String targetRoleUri = translator.findSemanticEquivalent(
212:                 sourceRoleLabel, null, targetProperties, 0.30); // Soglia abbassata leggermente perché le label sono nude
213: 
214:         if (targetRoleUri == null) {
215:             System.out.println("   [ERRORE] Il traduttore non ha trovato un equivalente per '" + sourceRoleLabel + "'. Analogia fallita.");
216:             return;
217:         }
218:         String targetRoleLabel = fetchLabelFromWikidata(targetRoleUri);
219: 
220:         System.out.println("\n=======================================================");
221:         System.out.println("    STEP 3 & 4: PROIEZIONE E ESTRAZIONE TARGET (VSA)    ");
222:         System.out.println("=======================================================");
223: 
224:         HDVector targetRoot = itemMemory.getTreeVector(currentTargetUri);
225:         HDVector targetRole = itemMemory.getOrGenerate(targetRoleUri);
226:         HDVector targetSubject = itemMemory.getOrGenerate(currentTargetUri);
227: 
228:         System.out.println("   -> Uso la chiave tradotta ('" + targetRoleLabel + "') per aprire il vettore bersaglio...");
229: 
230:         // ==========================================
231:         // FASE 1: Estrazione e Purificazione del Ramo
232:         // ==========================================
233:         HDVector noisyTargetBranch = targetRoot.permute(-100).bind(targetRole);
234: 
235:         System.out.println("   -> [SIMPKIN CLEAN-UP] Recupero del ramo intermedio (Chunk)...");
236:         HDVector pureTargetBranch = itemMemory.cleanUpChunk(noisyTargetBranch);
237: 
238:         if (pureTargetBranch == null) {
239:             System.out.println("\n[!] FALLIMENTO: Il ramo semantico si è perso nel rumore (Z-Score sotto soglia).");
240:             System.out.println("    [Logica Applicata]: " + sourceRoleLabel + " ===> " + targetRoleLabel);
241:             System.out.println("    [Confidenza VSA Ramo]: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
242:             return; // Interrompe pulitamente
243:         }
244: 
245:         System.out.println("   -> Ramo recuperato con successo! Z-Score Ramo: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
246: 
247:         // ==========================================
248:         // FASE 2: Estrazione dell'Oggetto dal Ramo Puro
249:         // ==========================================
250:         System.out.println("   -> Svincolo l'oggetto dal ramo purificato...");
251:         HDVector noisyTargetObject = pureTargetBranch.bind(targetSubject).bind(targetRole.permute(1)).permute(-2);
252: 
253:         // Clean-up Finale sulla memoria atomica (Oggetti)
254:         HDVector finalAnalogue = itemMemory.cleanUpRelative(noisyTargetObject);
255: 
256:         if (finalAnalogue != null) {
257:             System.out.println("\n[!] ANALOGIA RISOLTA CON SUCCESSO:");
258:             System.out.printf("    %-15s sta a  %-15s%n", armstrongEntity.entityLabel(), apolloEntity.entityLabel());
259:             System.out.println("    COME");
260: 
261:             String humanReadableResult = fetchLabelFromWikidata(itemMemory.getLastBestKey());
262:             System.out.printf("    %-15s sta a  %-15s%n", humanReadableResult, currentTargetLabel);
263:             System.out.println("\n    [Logica Applicata]: " + sourceRoleLabel + " ===> " + targetRoleLabel);
264:             System.out.println("    [Confidenza VSA Oggetto]: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
265:         } else {
266:             System.out.println("\n[?] Fallimento nel recupero dell'oggetto finale dal Chunk.");
267:             System.out.println("    [Confidenza VSA Oggetto]: " + String.format("%.2f", itemMemory.getLastBestSigma()) + " σ");
268:         }
269:     }
270: 
271:     private static String fetchDescriptionFromWikidata(String entityUri) {
272:         String sparqlQuery =
273:                 "PREFIX schema: <http://schema.org/> " +
274:                         "SELECT ?desc WHERE { " +
275:                         "  <" + entityUri + "> schema:description ?desc . " +
276:                         "  FILTER (lang(?desc) = 'en') " +
277:                         "} LIMIT 1";
278: 
279:         try {
280:             org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
281:             try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
282:                     .query(query)
283:                     .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
284:                     .build()) {
285: 
286:                 org.apache.jena.query.ResultSet results = qexec.execSelect();
287:                 if (results.hasNext()) {
288:                     return results.nextSolution().getLiteral("desc").getString();
289:                 }
290:             }
291:         } catch (Exception e) {
292:             System.err.println("   [AVVISO] Errore nell'estrazione della descrizione: " + e.getMessage());
293:         }
294:         return "";
295:     }
296: 
297:     private static Map<String, String> extractPropertyLabels(Resource entity, Model localGraph) {
298:         Map<String, String> labels = new HashMap<>();
299:         Set<Property> outgoingProps = new HashSet<>();
300:         Set<Property> incomingProps = new HashSet<>();
301: 
302:         localGraph.listStatements(entity, null, (RDFNode) null).forEachRemaining(s -> outgoingProps.add(s.getPredicate()));
303:         localGraph.listStatements(null, null, entity).forEachRemaining(s -> incomingProps.add(s.getPredicate()));
304: 
305:         System.out.println("   [INDAGINE ONTOLOGICA] Inventario delle proprietà del target:");
306: 
307:         System.out.println("   --- PROPRIETÀ IN USCITA (I Figli) ---");
308:         for (Property prop : outgoingProps) {
309:             String uri = prop.getURI();
310:             if (isMetadata(uri) || !uri.contains("/direct/")) continue;
311: 
312:             String label = fetchLabelFromWikidata(uri);
313:             labels.put(uri, label);
314:             System.out.println("      - [OUT] " + uri + "  --->  Label: '" + label + "'");
315:         }
316: 
317:         System.out.println("   --- PROPRIETÀ IN ENTRATA (I Genitori) ---");
318:         for (Property prop : incomingProps) {
319:             String uri = prop.getURI();
320:             if (isMetadata(uri) || !uri.contains("/direct/")) continue;
321: 
322:             if (!labels.containsKey(uri)) {
323:                 String label = fetchLabelFromWikidata(uri);
324:                 labels.put(uri, label);
325:             }
326:             System.out.println("      - [IN]  " + uri + "  --->  Label: '" + labels.get(uri) + "'");
327:         }
328: 
329:         return labels;
330:     }
331: 
332:     private static String fetchLabelFromWikidata(String uri) {
333:         if (!uri.startsWith("http://www.wikidata.org/")) {
334:             return uri.replace("_", " ");
335:         }
336: 
337:         String entityUri = uri.replace("/prop/direct-normalized/", "/entity/")
338:                 .replace("/prop/direct/", "/entity/")
339:                 .replace("/prop/statement/", "/entity/")
340:                 .replace("/prop/", "/entity/");
341: 
342:         String sparqlQuery =
343:                 "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
344:                         "SELECT ?label WHERE { " +
345:                         "  <" + entityUri + "> rdfs:label ?label . " +
346:                         "  FILTER (lang(?label) = 'en') " +
347:                         "} LIMIT 1";
348: 
349:         try {
350:             org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(sparqlQuery);
351:             try (org.apache.jena.query.QueryExecution qexec = org.apache.jena.query.QueryExecution.service("https://query.wikidata.org/sparql")
352:                     .query(query)
353:                     .httpHeader("User-Agent", "NeuroSemanticInvestigator/1.0 (ifts-project@example.com)")
354:                     .build()) {
355:                 org.apache.jena.query.ResultSet results = qexec.execSelect();
356:                 if (results.hasNext()) return results.nextSolution().getLiteral("label").getString();
357:             }
358:         } catch (Exception e) { /* Silenzioso */ }
359: 
360:         String[] parts = uri.split("/");
361:         return parts[parts.length - 1];
362:     }
363: 
364:     private static boolean isMetadata(String uri) {
365:         return uri.contains("P18") || uri.contains("P373") || uri.contains("P2002") || uri.contains("P2013") || uri.contains("P137");
366:     }
367: }

(End of file - total 367 lines)
</content>
```

---

## Assistant (Build · DeepSeek V4 Pro · 5.8s)

_Thinking:_

Now I have a very clear picture. Let me also check if there are any CI workflows, .mvn config, or other important files.

**Tool: glob**

**Input:**
```json
{
  "pattern": "**/.mvn/**"
}
```

**Output:**
```
No files found
```

**Tool: glob**

**Input:**
```json
{
  "pattern": "**/mvnw*"
}
```

**Output:**
```
No files found
```

**Tool: glob**

**Input:**
```json
{
  "pattern": ".github/**/*"
}
```

**Output:**
```
No files found
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\.project"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\.project</path>
<type>file</type>
<content>
1: <?xml version="1.0" encoding="UTF-8"?>
2: <projectDescription>
3: 	<name>neuro-semantic-investigator</name>
4: 	<comment></comment>
5: 	<projects>
6: 	</projects>
7: 	<buildSpec>
8: 		<buildCommand>
9: 			<name>org.eclipse.jdt.core.javabuilder</name>
10: 			<arguments>
11: 			</arguments>
12: 		</buildCommand>
13: 		<buildCommand>
14: 			<name>org.eclipse.m2e.core.maven2Builder</name>
15: 			<arguments>
16: 			</arguments>
17: 		</buildCommand>
18: 	</buildSpec>
19: 	<natures>
20: 		<nature>org.eclipse.jdt.core.javanature</nature>
21: 		<nature>org.eclipse.m2e.core.maven2Nature</nature>
22: 	</natures>
23: 	<filteredResources>
24: 		<filter>
25: 			<id>1776440710843</id>
26: 			<name></name>
27: 			<type>30</type>
28: 			<matcher>
29: 				<id>org.eclipse.core.resources.regexFilterMatcher</id>
30: 				<arguments>node_modules|\.git|__CREATED_BY_JAVA_LANGUAGE_SERVER__</arguments>
31: 			</matcher>
32: 		</filter>
33: 	</filteredResources>
34: </projectDescription>

(End of file - total 34 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\.classpath"
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\.classpath</path>
<type>file</type>
<content>
1: <?xml version="1.0" encoding="UTF-8"?>
2: <classpath>
3: 	<classpathentry kind="src" output="target/classes" path="src/main/java">
4: 		<attributes>
5: 			<attribute name="optional" value="true"/>
6: 			<attribute name="maven.pomderived" value="true"/>
7: 		</attributes>
8: 	</classpathentry>
9: 	<classpathentry excluding="**" kind="src" output="target/classes" path="src/main/resources">
10: 		<attributes>
11: 			<attribute name="maven.pomderived" value="true"/>
12: 			<attribute name="optional" value="true"/>
13: 		</attributes>
14: 	</classpathentry>
15: 	<classpathentry kind="src" output="target/test-classes" path="src/test/java">
16: 		<attributes>
17: 			<attribute name="optional" value="true"/>
18: 			<attribute name="maven.pomderived" value="true"/>
19: 			<attribute name="test" value="true"/>
20: 		</attributes>
21: 	</classpathentry>
22: 	<classpathentry excluding="**" kind="src" output="target/test-classes" path="src/test/resources">
23: 		<attributes>
24: 			<attribute name="maven.pomderived" value="true"/>
25: 			<attribute name="test" value="true"/>
26: 			<attribute name="optional" value="true"/>
27: 		</attributes>
28: 	</classpathentry>
29: 	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-17">
30: 		<attributes>
31: 			<attribute name="maven.pomderived" value="true"/>
32: 		</attributes>
33: 	</classpathentry>
34: 	<classpathentry kind="con" path="org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER">
35: 		<attributes>
36: 			<attribute name="maven.pomderived" value="true"/>
37: 		</attributes>
38: 	</classpathentry>
39: 	<classpathentry kind="src" path="target/generated-sources/annotations">
40: 		<attributes>
41: 			<attribute name="optional" value="true"/>
42: 			<attribute name="maven.pomderived" value="true"/>
43: 			<attribute name="ignore_optional_problems" value="true"/>
44: 			<attribute name="m2e-apt" value="true"/>
45: 		</attributes>
46: 	</classpathentry>
47: 	<classpathentry kind="src" output="target/test-classes" path="target/generated-test-sources/test-annotations">
48: 		<attributes>
49: 			<attribute name="optional" value="true"/>
50: 			<attribute name="maven.pomderived" value="true"/>
51: 			<attribute name="ignore_optional_problems" value="true"/>
52: 			<attribute name="m2e-apt" value="true"/>
53: 			<attribute name="test" value="true"/>
54: 		</attributes>
55: 	</classpathentry>
56: 	<classpathentry kind="output" path="target/classes"/>
57: </classpath>

(End of file - total 57 lines)
</content>
```

---

## Assistant (Build · DeepSeek V4 Pro · 22.4s)

_Thinking:_

Now I have a comprehensive picture of the repo. Let me also quickly check if there's logback config and the InvestigationEngine to understand entry points better.

Key findings so far:

1. **Java Maven project** - groupId: com.investigator, artifactId: neuro-semantic-investigator, version 1.0
2. **Java 21** source/target in properties, but **maven-compiler-plugin** overrides with **Java 17** source/target (quirk!)
3. **Eclipse project** (`.project`, `.classpath`, `.settings/`) - also Eclipse IDE configured
4. **No CI** (no .github directory)
5. **No mvnw** (Maven wrapper not present)
6. **Dependencies**: Apache Jena 5.0.0, Langchain4j embeddings (all-MiniLM-L6-v2), SLF4J/Logback, mixed test frameworks (JUnit 5 engine, TestNG, JUnit 4)
7. **Two entry points**: `App.java` (basic pipeline, hardcoded entities) and `AppDis.java` (with disambiguation/reranking)
8. **No tests** - AppTest is a JUnit 4 skeleton with `assertTrue(true)`, HDVectorMapBTest is an empty class
9. **Graphify** folders in multiple locations: root's `graphify-out/` (main graph), `src/graphify-out/` (AST cache)
10. **Description.md** is a very detailed technical document in Italian describing the architecture
11. **Existing AGENTS.md** is minimal - just tells to check Graphify

Let me also check if there's logback config.

Now let me build the AGENTS.md. The user wants a compact, high-signal instruction file. Let me derive what an agent would actually need:

**Critical facts:**
- Maven build, but NO maven wrapper - user must have `mvn` installed
- pom.xml declares Java 21 in properties but compiler plugin forces Java 17 - the REAL target is 17
- Three test frameworks in pom (JUnit 5 engine, TestNG, JUnit 4) but no real tests exist (only skeletons)
- Two App entry points with hardcoded entities - you change the strings in main() to run different analogies
- Requires network (Wikidata SPARQL endpoint) - won't work offline
- MiniLM embedding model downloads ~22MB on first run (auto, via Langchain4j)
- `OntologyTranslator` uses ALL MINILM L6 V2, not an LLM - despite class name mentioning "LLM"
- Graphify skill is available and has pre-generated graph at `graphify-out/GRAPH_REPORT.md`
- All comments and docs are in Italian
- Eclipse IDE config exists alongside the project
- source/target directories: `src/main/java`, `src/test/java`

**Build commands:**
- `mvn compile` 
- `mvn test` (runs only trivial test)
- `mvn exec:java -Dexec.mainClass="com.investigator.App"` (or AppDis)
- `mvn package` to get JAR

Wait, there's no exec-maven-plugin in the pom. So `mvn exec:java` won't work unless user adds it. Let me check...

Looking at the pom again - no, there's no exec plugin. So the way to run would be through IDE (Eclipse or IntelliJ) by executing the main class. Or manually building and running with `java -cp target/classes:...` (but dependency classpath would need to be constructed).

Actually the user has IntelliJ based on the path, but the project also has Eclipse config. The authentic way to run is likely through the IDE.

Let me write a concise AGENTS.md. The existing one can be preserved in its essence (Graphify guidance is reasonable) but needs to be expanded with actual build/run facts and gotchas.

Now I have a thorough understanding. Let me update the AGENTS.md with verified, high-signal guidance.

**Tool: read**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\graphify-out\\GRAPH_REPORT.md",
  "offset": 1,
  "limit": 5
}
```

**Output:**
```
<path>C:\Users\Ion\IdeaProjects\Vaimee\VSA\neuro-semantic-investigator\graphify-out\GRAPH_REPORT.md</path>
<type>file</type>
<content>
1: # Graph Report - .  (2026-05-22)
2: 
3: ## Corpus Check
4: - Corpus is ~13,441 words - fits in a single context window. You may not need a graph.
5: 

(Showing lines 1-5 of 90. Use offset=6 to continue.)
</content>
```

---

## Assistant (Build · DeepSeek V4 Pro · 19.2s)

_Thinking:_

Good. Now let me write the AGENTS.md file. I'll preserve the Graphify guidance (which is still useful) and add build/arch/test quirks.

**Tool: write**

**Input:**
```json
{
  "filePath": "C:\\Users\\Ion\\IdeaProjects\\Vaimee\\VSA\\neuro-semantic-investigator\\AGENTS.md",
  "content": "# AGENTS.md\n\n## Build & Run\n- **Maven** — no wrapper (`mvnw`). Requires `mvn` on PATH.\n- **Java target is 17**, not 21. `pom.xml` properties say 21 but `maven-compiler-plugin` overrides to 17.\n- `mvn compile` / `mvn test` / `mvn package`\n- No `exec-maven-plugin` in POM — run main classes via IDE (IntelliJ/Eclipse) or `java -cp`.\n- Three test frameworks declared (JUnit 5 engine, TestNG, JUnit 4), but **no real tests exist** — only skeletons. `mvn test` passes trivially.\n\n## Architecture\n- **Neuro-Symbolic analogical reasoner** on Wikidata: finds `D` such that `A:B = C:D` via VSA algebra.\n- Full technical description: `Description.md` (Italian).\n- **Entry points**: `App.java` (basic pipeline) and `AppDis.java` (adds disambiguation/reranking). Both have **hardcoded entity names** — edit strings in `main()` to run different analogies.\n- Requires **live network** — queries `query.wikidata.org/sparql` for entity resolution, graph ingestion, and label lookups.\n- **First run downloads MiniLM embedding model** (~22 MB) automatically via Langchain4j.\n- `OntologyTranslator` uses `AllMiniLmL6V2EmbeddingModel` (384-d embeddings), **not an LLM** — despite variable naming and doc mentions of \"LLM\".\n\n## Source Layout\n| Package | Role |\n|---|---|\n| `com.investigator` | Entry points (`App`, `AppDis`) |\n| `com.investigator.jena` | Wikidata access: `EntityResolver`, `GraphManager`, `TripleExtractor`, `SparqlEndpoint`, `ResolvedEntity` |\n| `com.investigator.vsa` | VSA algebra: `HDVector` (interface), `HDVectorMapB` (D=10000, bipolar `byte[]`), `ItemMemory` (3-tier) |\n| `com.investigator.vsa.strategy` | Vector generation: `RandomGenerationStrategy` (default, deterministic from URI), `SemanticEmbeddingStrategy` (not used by default) |\n| `com.investigator.engine` | `InvestigationEngine` (orchestrator) |\n| `com.investigator.llm` | `OntologyTranslator` (MiniLM embedding-based cross-ontology matching) |\n\n## Key Quirks\n- All comments, docs, and console output are in **Italian**.\n- `App.java` and `AppDis.java` are ~90% duplicated code. Edit both if changing pipeline logic.\n- VSA vectors are deterministic: `URI → UUID.nameUUIDFromBytes → seed → Random LCG → 10000 bipolar bytes`. Same URI = same vector on any JVM.\n- `fetchLabelFromWikidata()` normalizes `/prop/direct/` → `/entity/` before SPARQL label lookup.\n- `isMetadata()` hardcodes a list of Wikidata property IDs to skip (P18, P373, P2002, P2013, P137).\n- Eclipse project files (`.project`, `.classpath`, `.settings/`) coexist with IntelliJ (`.idea/`). Do not delete either set.\n\n## Graphify\n- Check `graphify-out/GRAPH_REPORT.md` for component overview and community structure before non-trivial changes.\n- AST cache at `src/graphify-out/cache/ast/`.\n- Run `/graphify --update` to regenerate after significant changes.\n"
}
```

**Output:**
```
Wrote file successfully.
```

---

## Assistant (Build · DeepSeek V4 Pro · 10.3s)

_Thinking:_

The file is written. Let me verify it looks correct.

Updated `AGENTS.md` with verified, high-signal facts covering:

- **Build quirks**: no Maven wrapper, real Java target is 17 despite pom properties claiming 21, no exec plugin
- **No real tests**: three frameworks declared but only trivial skeletons exist
- **Architecture**: VSA analogical reasoner on Wikidata, with two 90%-duplicated entry points using hardcoded entity names in `main()`
- **Source layout**: concise package-to-role mapping
- **Key gotchas**: Italian-only docs/output, deterministic VSA vectors, property URI normalization, hardcoded metadata filter, Eclipse+IntelliJ IDE config coexistence
- **Graphify**: pointer to pre-generated graph for codebase navigation, retained from the prior version

---

