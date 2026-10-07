# Kiro Prompt — DEVOS-067: Platform & AI-SDLC (CI/CD, Observability, AI Evaluation, Performance)

**Jira:** DEVOS-067 / DEVOS-068 / DEVOS-069 / DEVOS-070  
**Epic:** DEVOS-E12  
**Figma:** — (no UI screens)  
**Kiro Spec:** `.kiro/specs/platform/core.md`  
**AI-SDLC Phase:** DEPLOY / OBSERVE / IMPROVE

---

## Prompt

You are implementing the platform and AI-SDLC infrastructure for DevOS AI: CI/CD pipeline, structured observability, AI evaluation framework, and performance optimization.

**Existing files to read first:**
- `.kiro/specs/platform/core.md`
- `build.gradle.kts` — root Gradle configuration
- `app/build.gradle.kts` — app module configuration
- `.github/` — existing GitHub Actions workflows (if any)
- `docs/testing/test-strategy.md` — test strategy
- `docs/testing/ai-evaluation-strategy.md` — AI evaluation requirements

**Modules:**
- `.github/workflows/` — CI/CD pipeline YAML
- `core/core-common/` — logging and analytics infrastructure
- `scripts/` — evaluation scripts

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- No PII in logs — user IDs logged only as hashed values; no email, name, or content.
- CI/CD secrets (signing keys, API keys for tests) stored as GitHub Actions secrets — never committed to the repository.
- Performance budget: target 60fps (16ms per frame); no ANR; memory stable on 4GB device.
- AI evaluation runs on a fixed test set — never on live user data.

**What to implement:**

### 1. CI/CD Pipeline (DEVOS-067)

File: `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - uses: gradle/actions/setup-gradle@v3
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: '**/build/test-results/**/*.xml'

  build:
    needs: unit-tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - uses: gradle/actions/setup-gradle@v3
      - name: Assemble debug APK
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk

  ui-tests:
    needs: build
    # Run on merge to main only (expensive)
    if: github.ref == 'refs/heads/main'
    runs-on: macos-latest   # macOS for hardware-accelerated emulator
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run UI tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          script: ./gradlew connectedDebugAndroidTest

  release:
    needs: [unit-tests, build]
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build release AAB
        env:
          KEYSTORE_FILE: ${{ secrets.KEYSTORE_FILE }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
        run: |
          echo "$KEYSTORE_FILE" | base64 --decode > keystore.jks
          ./gradlew bundleRelease \
            -Pandroid.injected.signing.store.file=keystore.jks \
            -Pandroid.injected.signing.key.alias=$KEY_ALIAS \
            -Pandroid.injected.signing.key.password=$KEY_PASSWORD \
            -Pandroid.injected.signing.store.password=$STORE_PASSWORD
```

Signing keys stored as GitHub Actions secrets — never committed.

### 2. Observability — Logging and Analytics (DEVOS-068)

File: `core/core-common/src/main/kotlin/com/devos/ai/common/logging/DevOSLogger.kt`

```kotlin
/**
 * Structured logging wrapper. All logs go through here — never call Timber directly from features.
 */
object DevOSLogger {

    fun event(category: LogCategory, action: String, properties: Map<String, Any> = emptyMap()) {
        // Sanitize: ensure no PII in properties
        val sanitized = properties
            .filterKeys { !it.lowercase().contains("email") && !it.lowercase().contains("name") }
            .mapValues { (_, v) -> if (v is String && v.length > 200) v.take(200) + "…" else v }

        Timber.tag(category.tag).d("$action ${sanitized.entries.joinToString(" ") { "${it.key}=${it.value}" }}")
        // Send to analytics backend (Firebase Analytics / custom) in non-debug builds
        if (!BuildConfig.DEBUG) {
            analyticsClient.logEvent(action, sanitized)
        }
    }

    fun error(category: LogCategory, message: String, throwable: Throwable? = null) {
        Timber.tag(category.tag).e(throwable, message)
        if (!BuildConfig.DEBUG) {
            crashlyticsClient.recordException(throwable ?: Exception(message))
        }
    }

    fun performance(name: String, durationMs: Long, metadata: Map<String, Any> = emptyMap()) {
        Timber.tag("PERF").d("$name ${durationMs}ms $metadata")
    }

    // Hash user ID before logging — never log raw user ID
    fun hashUserId(userId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray())
            .take(8)
            .joinToString("") { "%02x".format(it) }
}

enum class LogCategory(val tag: String) {
    AUTH("AUTH"),
    REPOSITORY("REPO"),
    AI("AI"),
    AGENTS("AGENTS"),
    NAVIGATION("NAV"),
    PERFORMANCE("PERF"),
    SECURITY("SEC"),
}
```

Performance tracing:
```kotlin
// Usage in ViewModels
suspend fun loadData() {
    val start = SystemClock.elapsedRealtime()
    val result = useCase()
    DevOSLogger.performance("load_repository_list",
        durationMs = SystemClock.elapsedRealtime() - start,
        metadata = mapOf("count" to result.size))
}
```

**No PII rule enforcement:**
- Never log: email, display name, repository content, AI responses, code snippets
- User ID logged only as 8-char hex hash
- API keys masked as `key.take(4) + "****"`

### 3. AI Evaluation Framework (DEVOS-069)

File: `scripts/eval/` + `core/core-testing/`

**Evaluation test suite structure:**

```
scripts/eval/
├── datasets/
│   ├── rag_queries.json          # 50 test queries with expected top-k sources
│   ├── grounding_pairs.json      # 20 (question, answer, expected_citations) triples
│   └── agent_tasks.json          # 10 multi-step agent tasks with success criteria
├── eval_rag.py                   # retrieval precision/recall metrics
├── eval_grounding.py             # does answer cite real code from the repo?
├── eval_agents.py                # agent task success rate
└── eval_report.py                # aggregate report + regression detection
```

**RAG evaluation (`eval_rag.py`):**
```python
def evaluate_rag(query, expected_sources, retrieved_sources):
    """
    Precision@k: fraction of retrieved sources that are in expected
    Recall@k: fraction of expected sources that were retrieved
    """
    retrieved_set = set(retrieved_sources[:k])
    expected_set = set(expected_sources)
    precision = len(retrieved_set & expected_set) / len(retrieved_set) if retrieved_set else 0
    recall = len(retrieved_set & expected_set) / len(expected_set) if expected_set else 0
    return {"precision": precision, "recall": recall, "f1": 2*precision*recall/(precision+recall+1e-9)}
```

**Grounding accuracy:** for each (question, answer) pair, extract all code citations from the answer and verify they reference actual lines from the indexed repository. Score = cited_real_lines / total_cited_lines.

**Agent success rate:** run each agent task in `agent_tasks.json` against the test repository; record pass/fail based on defined success criteria (e.g., "file X exists and contains Y").

**Baseline establishment:**
- Run evaluation suite on initial implementation to establish baseline scores
- Store baseline in `scripts/eval/baseline.json`
- Regression detected when any metric drops >5% below baseline

**CI integration:** add eval job to `.github/workflows/ci.yml` that runs on merge to main against the test repository. Fails if regression detected.

**Android-side evaluation hook (`core/core-testing`):**
```kotlin
// Instrumented test — not shipped in production
class RAGEvaluationTest {
    @Test fun retrievalPrecision_isAboveBaseline() {
        val queries = loadTestDataset("rag_queries.json")
        queries.forEach { testCase ->
            val retrieved = ragRepository.retrieve(testCase.query, k = 5)
            val precision = computePrecision(retrieved, testCase.expectedSources)
            assertThat(precision).isGreaterThan(PRECISION_BASELINE)
        }
    }
}
```

### 4. Performance Optimization (DEVOS-070)

This ticket is a verification and optimization pass. Key rules to enforce across all feature modules:

**Rule 1 — Lazy lists everywhere:**
```kotlin
// CORRECT
LazyColumn { items(list) { item -> ItemCard(item) } }

// WRONG — creates all items upfront
Column { list.forEach { item -> ItemCard(item) } }
```

**Rule 2 — Pagination with Pager:**
```kotlin
// For repository list, issue list, PR list, commit history
val pager = Pager(PagingConfig(pageSize = 30)) {
    RepositoryPagingSource(repositoryDao)
}
val pagingItems = pager.flow.collectAsLazyPagingItems()

LazyColumn {
    items(pagingItems) { item -> if (item != null) RepositoryCard(item) }
    pagingItems.loadState.append.let { state ->
        if (state is LoadState.Loading) item { DevOSLoadingState() }
        if (state is LoadState.Error) item { DevOSErrorState(...) }
    }
}
```

**Rule 3 — Stable data classes for recomposition:**
```kotlin
// CORRECT — stable, Compose skips recomposition when data unchanged
@Stable
data class RepositoryUiModel(
    val id: String,
    val name: String,
    val healthScore: Float,
)

// WRONG — List<Any> is unstable, triggers full recomposition
@Composable
fun RepositoryList(items: List<Any>) { ... }
```

**Rule 4 — derivedStateOf for computed values:**
```kotlin
val filteredItems by remember(items, filter) {
    derivedStateOf { items.filter { it.matches(filter) } }
}
```

**Rule 5 — AI responses streamed incrementally:**
```kotlin
// CORRECT — UI updates as each chunk arrives
viewModelScope.launch {
    aiRepository.chat(messages, context).collect { chunk ->
        _uiState.update { it.copy(
            streamingResponse = it.streamingResponse + chunk.delta,
            isStreaming = !chunk.isComplete,
        )}
    }
}

// WRONG — blocks until full response
val fullResponse = aiRepository.chatBlocking(messages, context)
```

**Performance audit checklist:**
- [ ] All list screens use `LazyColumn`/`LazyRow` — no `forEach` in Column
- [ ] All paginated data uses `Pager` + `PagingSource`
- [ ] All background indexing via `WorkManager`
- [ ] `AsyncImage` (Coil) for all remote images — no manual bitmap loading
- [ ] `remember`, `derivedStateOf`, stable data classes on all hot composables
- [ ] AI streaming updates incrementally — no full-response buffering
- [ ] Frame budget: run Android Studio Profiler on Home Dashboard, AI Chat, and Code Viewer
  - Target: <16ms per frame (60fps)
  - Memory: stable over 30-minute session on 4GB device
  - No ANR: all network/disk I/O on IO dispatcher

**Tests:**
- `CIPipelineTest`: `./gradlew assembleDebug` succeeds; `./gradlew testDebugUnitTest` passes all tests
- `LoggingTest`: user ID logged as hash; no email or name in log output; API keys masked
- `RAGEvaluationTest`: retrieval precision ≥ baseline on test dataset
- `AgentEvaluationTest`: agent success rate ≥ baseline on test task set
- `PerformanceBenchmark` (Macrobenchmark): startup time; AI Chat first frame; Repository List scroll FPS

**Acceptance Criteria:**
- AC1 (DEVOS-067): GitHub Actions workflow runs unit tests on every PR
- AC2 (DEVOS-067): UI tests run on merge to main
- AC3 (DEVOS-067): Release AAB built and signed from GitHub Actions secrets on version tags
- AC4 (DEVOS-067): CI fails on test failure — no green build with failing tests
- AC5 (DEVOS-068): Structured log events emitted for key user actions (repo import, AI chat sent, agent run)
- AC6 (DEVOS-068): Error reporting sends crashes to monitoring dashboard
- AC7 (DEVOS-068): No PII (email, name, content) in any log output
- AC8 (DEVOS-068): Performance traces emitted for slow operations (>500ms)
- AC9 (DEVOS-069): Retrieval precision/recall metrics computed on test dataset
- AC10 (DEVOS-069): Grounding accuracy metric computed (cited code is real)
- AC11 (DEVOS-069): Agent success rate metric computed on task set
- AC12 (DEVOS-069): Baseline scores established and stored; regression detection in CI
- AC13 (DEVOS-070): All list screens use `LazyColumn` — no `forEach` in Column confirmed
- AC14 (DEVOS-070): All paginated lists use `Pager` + `PagingSource`
- AC15 (DEVOS-070): Background indexing confirmed in WorkManager (not foreground coroutines)
- AC16 (DEVOS-070): AI responses stream incrementally — UI updates character by character
- AC17 (DEVOS-070): App runs at 60fps (no jank) on Home Dashboard and AI Chat screens
- AC18 (DEVOS-070): No ANR under normal usage on 4GB device
- AC19 (DEVOS-070): Memory stable over 30-minute session (no leak detected by LeakCanary)
