# DevOS AI — AI Evaluation Strategy

**Version:** 1.0  
**Date:** 2026-10-07

---

## 1. Overview

AI features in DevOS AI require evaluation beyond standard unit tests. Every AI capability must have a quantitative baseline established before shipping, and regressions must be detectable automatically.

**Evaluated capabilities:**

| Capability | Primary Metric | Baseline Target |
|-----------|---------------|-----------------|
| RAG Retrieval | Precision@5 | ≥ 0.70 |
| RAG Retrieval | Recall@10 | ≥ 0.65 |
| Answer Grounding | Citation accuracy | ≥ 0.80 |
| Answer Grounding | Hallucination rate | ≤ 0.10 |
| Agent Execution | Task success rate | ≥ 0.75 |
| Agent Execution | Step efficiency | ≤ 8 steps avg |
| PR Review | Comment precision | ≥ 0.65 |
| Security Scanner | True positive rate | ≥ 0.85 |
| Security Scanner | False positive rate | ≤ 0.15 |
| Learning Recommendations | Relevance to repo | ≥ 0.70 |

---

## 2. RAG Evaluation

### 2.1 Test Dataset

Maintain a curated test repository (`test-fixtures/eval-repo/`) with known content:
- 50 Kotlin files covering: auth, networking, database, UI, domain
- 200 pre-labeled `(query, expected_file_paths)` pairs
- Organized by topic: auth, navigation, data, ui, testing

### 2.2 Metrics

**Precision@K:** Of the top-K retrieved chunks, what fraction are relevant?
```kotlin
fun precisionAtK(retrieved: List<CodeChunk>, relevant: Set<String>, k: Int): Float {
    val topK = retrieved.take(k)
    val relevantRetrieved = topK.count { it.filePath in relevant }
    return relevantRetrieved.toFloat() / k
}
```

**Recall@K:** Of all relevant chunks, what fraction appear in top-K?
```kotlin
fun recallAtK(retrieved: List<CodeChunk>, relevant: Set<String>, k: Int): Float {
    val topK = retrieved.take(k).map { it.filePath }.toSet()
    val found = relevant.count { it in topK }
    return found.toFloat() / relevant.size
}
```

**Mean Reciprocal Rank (MRR):** Rank of first relevant result:
```kotlin
fun mrr(results: List<List<CodeChunk>>, relevantSets: List<Set<String>>): Float {
    return results.zip(relevantSets).map { (retrieved, relevant) ->
        val rank = retrieved.indexOfFirst { it.filePath in relevant } + 1
        if (rank > 0) 1f / rank else 0f
    }.average().toFloat()
}
```

### 2.3 Eval Runner

```kotlin
class RAGEvaluationRunner @Inject constructor(
    private val ragRepository: RAGRepository,
    private val evalDataset: EvalDataset,
) {
    suspend fun evaluate(): RAGEvalReport {
        val results = evalDataset.queries.map { (query, context, relevant) ->
            val retrieved = ragRepository.retrieve(query, context, topK = 10)
            QueryResult(query, retrieved, relevant)
        }
        return RAGEvalReport(
            precisionAt5  = results.map { precisionAtK(it.retrieved, it.relevant, 5) }.average().toFloat(),
            precisionAt10 = results.map { precisionAtK(it.retrieved, it.relevant, 10) }.average().toFloat(),
            recallAt10    = results.map { recallAtK(it.retrieved, it.relevant, 10) }.average().toFloat(),
            mrr           = mrr(results.map { it.retrieved }, results.map { it.relevant }),
            totalQueries  = results.size,
        )
    }
}
```

### 2.4 Regression Gate

```kotlin
@Test fun `rag precision@5 meets baseline`() = runTest {
    val report = ragEvaluationRunner.evaluate()
    assertThat(report.precisionAt5).isGreaterThan(0.70f)
}

@Test fun `rag recall@10 meets baseline`() = runTest {
    val report = ragEvaluationRunner.evaluate()
    assertThat(report.recallAt10).isGreaterThan(0.65f)
}
```

---

## 3. Answer Grounding Evaluation

### 3.1 What is Grounding?

A grounded answer cites actual code that exists in the repository and is relevant to the answer content.

### 3.2 Citation Accuracy

For a set of reference (question, answer, expected_sources) triples:

```kotlin
data class GroundingEvalCase(
    val question: String,
    val context: AIContext,
    val expectedSourceFiles: Set<String>,  // files that SHOULD be cited
)

suspend fun evaluateGrounding(cases: List<GroundingEvalCase>): GroundingReport {
    var citationAccuracy = 0f
    var hallucinations = 0
    var totalSources = 0

    cases.forEach { case ->
        val answer = sendMessageUseCase(case.question, case.context).last()
        answer.sources.forEach { source ->
            totalSources++
            // Check 1: cited file exists in repository
            val fileExists = fileRepository.exists(source.filePath)
            if (!fileExists) hallucinations++

            // Check 2: cited file is in expected sources
            if (source.filePath in case.expectedSourceFiles) citationAccuracy++
        }
    }
    return GroundingReport(
        citationAccuracy  = citationAccuracy / totalSources,
        hallucinationRate = hallucinations.toFloat() / totalSources,
    )
}
```

### 3.3 Regression Gate

```kotlin
@Test fun `grounding citation accuracy meets baseline`() = runTest {
    val report = evaluateGrounding(groundingTestCases)
    assertThat(report.citationAccuracy).isGreaterThan(0.80f)
}

@Test fun `hallucination rate below threshold`() = runTest {
    val report = evaluateGrounding(groundingTestCases)
    assertThat(report.hallucinationRate).isLessThan(0.10f)
}
```

---

## 4. Agent Evaluation

### 4.1 Task Suite

Maintain a set of test tasks that agents must complete:

| Task | Success Criterion | Max Steps |
|------|------------------|-----------|
| "Find where authentication is implemented" | Returns AuthViewModel.kt + AuthRepository.kt | 5 |
| "Explain the login flow" | Answer references correct files | 8 |
| "Find all callers of login()" | Returns ≥ 80% of actual callers | 6 |
| "What architecture pattern does this project use?" | Identifies MVVM or Clean Architecture | 4 |
| "Summarize recent git changes" | Returns non-empty summary | 3 |

### 4.2 Success Metrics

```kotlin
data class AgentTaskResult(
    val task: AgentTask,
    val completed: Boolean,
    val stepsUsed: Int,
    val finalAnswer: String?,
    val correctnessScore: Float,   // 0.0–1.0, evaluated by judge LLM or rules
)

fun evaluateAgentTasks(tasks: List<AgentTask>): AgentEvalReport {
    val results = tasks.map { task ->
        val run = agentEngine.runAgent(task.goal, task.context).last()
        AgentTaskResult(
            task = task,
            completed = run is AgentProgress.Complete,
            stepsUsed = run.steps.size,
            finalAnswer = (run as? AgentProgress.Complete)?.answer,
            correctnessScore = task.evaluator(run),
        )
    }
    return AgentEvalReport(
        successRate     = results.count { it.completed }.toFloat() / results.size,
        avgSteps        = results.filter { it.completed }.map { it.stepsUsed }.average().toFloat(),
        avgCorrectness  = results.map { it.correctnessScore }.average().toFloat(),
    )
}
```

### 4.3 Regression Gate

```kotlin
@Test fun `agent task success rate meets baseline`() = runTest {
    val report = evaluateAgentTasks(agentTestSuite)
    assertThat(report.successRate).isGreaterThan(0.75f)
}

@Test fun `agent average steps below efficiency target`() = runTest {
    val report = evaluateAgentTasks(agentTestSuite)
    assertThat(report.avgSteps).isLessThan(8f)
}
```

---

## 5. Security Scanner Evaluation

### 5.1 Test Fixtures

Maintain `test-fixtures/security-samples/` with:
- 20 files with **known vulnerabilities** (true positives)
- 20 clean files (true negatives)
- Labels: `{filePath, lineRange, category, severity}`

### 5.2 Metrics

```kotlin
fun evaluateScanner(knownVulns: List<KnownVuln>, findings: List<SecurityFinding>): ScannerReport {
    val truePositives  = knownVulns.count { known -> findings.any { it.matches(known) } }
    val falsePositives = findings.count { finding -> knownVulns.none { it.matches(finding) } }
    val falseNegatives = knownVulns.count { known -> findings.none { it.matches(known) } }

    return ScannerReport(
        truePositiveRate  = truePositives.toFloat() / knownVulns.size,
        falsePositiveRate = falsePositives.toFloat() / (falsePositives + truePositives),
        recall = truePositives.toFloat() / (truePositives + falseNegatives),
    )
}
```

---

## 6. Learning Recommendation Evaluation

### 6.1 Test Cases

Repositories with known technology usage → expected recommended courses:

| Repository Pattern | Expected Recommendation |
|-------------------|------------------------|
| Uses Retrofit + Coroutines | "Kotlin Coroutines" course |
| Uses Room + Flow | "Android Data Persistence" |
| Uses Hilt | "Dependency Injection" |
| Uses RAG patterns | "RAG Architecture" |
| No tests | "Android Testing" |

### 6.2 Precision

```kotlin
@Test fun `rag-based repo recommends rag course`() = runTest {
    val connections = recommendationEngine.analyzeRepository("rag-sample-repo")
    assertThat(connections.map { it.topic }).contains("RAG")
}
```

---

## 7. Evaluation CI Integration

```yaml
# .github/workflows/ai-eval.yml
# Runs on merge to main only (not every PR — expensive)
on:
  push:
    branches: [main]

jobs:
  ai-evaluation:
    runs-on: ubuntu-latest
    steps:
      - name: RAG Evaluation
        run: ./gradlew :data-ai:test --tests "*.RAGEvaluationTest"

      - name: Grounding Evaluation
        run: ./gradlew :data-ai:test --tests "*.GroundingEvaluationTest"

      - name: Agent Evaluation
        run: ./gradlew :data-ai:test --tests "*.AgentEvaluationTest"

      - name: Upload eval report
        uses: actions/upload-artifact@v4
        with:
          name: ai-eval-report
          path: build/reports/ai-eval/
```

---

## 8. Baseline Management

- Baselines stored in `test-fixtures/eval-baselines.json`
- On first run: write baselines
- On subsequent runs: compare against baselines, fail if regression >5%
- Baseline updates require explicit `./gradlew updateEvalBaselines` + PR review

```json
{
  "rag": { "precisionAt5": 0.74, "recallAt10": 0.68, "mrr": 0.81 },
  "grounding": { "citationAccuracy": 0.83, "hallucinationRate": 0.07 },
  "agent": { "successRate": 0.80, "avgSteps": 6.2, "avgCorrectness": 0.77 },
  "scanner": { "truePositiveRate": 0.88, "falsePositiveRate": 0.12 }
}
```
