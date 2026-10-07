# Kiro Prompt — DEVOS-046: Quality Intelligence (Security + Test Intelligence)

**Jira:** DEVOS-046 / DEVOS-047 / DEVOS-048 / DEVOS-049  
**Epic:** DEVOS-E07  
**Figma:** FIGMA-26 / FIGMA-27  
**Kiro Spec:** `.kiro/specs/security-intelligence/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the Quality Intelligence module for DevOS AI: security findings screen, security scanning service, test intelligence screen, and test coverage analysis service.

**Existing files to read first:**
- `.kiro/specs/security-intelligence/core.md`
- `docs/figma/screen-inventory.md` — Screens FIGMA-26, FIGMA-27
- `docs/figma/component-inventory.md` — DevOSStatusBadge, DevOSCard, DevOSAIMessage
- `core/core-database/` — existing Room setup for entities
- `domain/domain-ai/` — AIRepository for streaming suggestions

**Modules:**
- `feature/feature-security/` — SecurityFindingsScreen
- `feature/feature-testing/` — TestIntelligenceScreen
- `data/data-repository/` — SecurityScanningService, TestCoverageService (extend existing)
- `core/core-database/` — new entities: SecurityFindingEntity, FileCoverageEntity, TestRunEntity

**Package:** `com.devos.ai.feature`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- Security scanner runs ONLY in WorkManager background task — never on main thread.
- CVE database bundled as assets — never fetched at runtime without explicit user update action.
- Test coverage XML parsing uses SAX (not DOM) to avoid OOM on large reports.
- AI coverage suggestions stream via `Flow<StreamChunk>` — never await full response before updating UI.
- Security scanner results are data from the codebase — sanitize before injecting into AI prompts to prevent prompt injection.

**What to implement:**

### 1. Domain Models

```kotlin
data class SecurityFinding(
    val id: String,
    val repoId: String,
    val severity: Severity,
    val ruleId: String,
    val title: String,
    val description: String,
    val filePath: String,
    val lineNumber: Int,
    val codeSnippet: String?,
    val cveId: String?,             // for dependency findings
    val status: FindingStatus,
    val detectedAt: Instant,
)

enum class Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
enum class FindingStatus { OPEN, FIXED, DISMISSED }

data class FileCoverage(
    val filePath: String,
    val repoId: String,
    val lineCoverage: Float,        // 0.0–1.0
    val branchCoverage: Float,
    val coveredLines: Int,
    val totalLines: Int,
)

data class TestRun(
    val id: String,
    val repoId: String,
    val testName: String,
    val passed: Boolean,
    val durationMs: Long,
    val ranAt: Instant,
)
```

Repository interfaces in `domain-repository`:
```kotlin
interface SecurityRepository {
    fun observeFindings(repoId: String): Flow<List<SecurityFinding>>
    suspend fun dismissFinding(findingId: String): Result<Unit>
    suspend fun markFixed(findingId: String): Result<Unit>
    suspend fun scanRepository(repoId: String): Result<Unit>
}

interface TestIntelligenceRepository {
    fun observeCoverage(repoId: String): Flow<List<FileCoverage>>
    fun observeTestRuns(repoId: String): Flow<List<TestRun>>
    suspend fun analyzeRepository(repoId: String): Result<Unit>
    fun streamCoverageSuggestions(repoId: String): Flow<StreamChunk>
}
```

### 2. Room Entities (core-database)

```kotlin
@Entity(tableName = "security_findings",
    foreignKeys = [ForeignKey(entity = RepositoryEntity::class,
        parentColumns = ["id"], childColumns = ["repoId"], onDelete = ForeignKey.CASCADE)])
data class SecurityFindingEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    val severity: String,
    val ruleId: String,
    val title: String,
    val description: String,
    val filePath: String,
    val lineNumber: Int,
    val codeSnippet: String?,
    val cveId: String?,
    val status: String,
    val detectedAt: Long,
)

@Entity(tableName = "file_coverage")
data class FileCoverageEntity(
    @PrimaryKey val id: String,     // "$repoId:$filePath"
    val repoId: String,
    val filePath: String,
    val lineCoverage: Float,
    val branchCoverage: Float,
    val coveredLines: Int,
    val totalLines: Int,
    val updatedAt: Long,
)

@Entity(tableName = "test_runs")
data class TestRunEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    val testName: String,
    val passed: Boolean,
    val durationMs: Long,
    val ranAt: Long,
)
```

### 3. SecurityFindingsScreen (DEVOS-046, FIGMA-26)

Route: `SECURITY_FINDINGS/{repoId}`

UiState:
```kotlin
sealed interface SecurityUiState {
    data object Loading : SecurityUiState
    data class Success(
        val findings: List<SecurityFinding>,
        val summaryCounts: Map<Severity, Int>,
        val activeFilter: Severity?,
        val expandedFindingId: String?,
    ) : SecurityUiState
    data object Empty : SecurityUiState
    data class Error(val message: String, val retryable: Boolean) : SecurityUiState
}
```

Layout:
```kotlin
Scaffold(topBar = { DevOSTopBar(title = "Security", onBack = onBack) }) { padding ->
    when (val s = uiState) {
        is Loading -> DevOSLoadingState()
        is Empty   -> DevOSEmptyState(
            icon = Icons.Outlined.VerifiedUser,
            title = "No findings",
            description = "No security issues detected in this repository",
        )
        is Error   -> DevOSErrorState(description = s.message,
            onRetry = if (s.retryable) viewModel::retry else null)
        is Success -> LazyColumn(Modifier.padding(padding)) {
            // Summary counts row
            item {
                Row(Modifier.padding(MaterialTheme.spacing.base),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                    Severity.entries.forEach { sev ->
                        val count = s.summaryCounts[sev] ?: 0
                        SeverityCountChip(severity = sev, count = count,
                            isActive = s.activeFilter == sev,
                            onClick = { onFilterChange(sev) })
                    }
                }
            }
            // Findings list
            items(s.findings, key = { it.id }) { finding ->
                FindingCard(
                    finding = finding,
                    isExpanded = s.expandedFindingId == finding.id,
                    onToggle = { onToggleFinding(finding.id) },
                    onMarkFixed = { onMarkFixed(finding.id) },
                    onDismiss = { onDismiss(finding.id) },
                )
            }
        }
    }
}

@Composable
fun FindingCard(finding: SecurityFinding, isExpanded: Boolean,
                onToggle: () -> Unit, onMarkFixed: () -> Unit, onDismiss: () -> Unit) {
    DevOSCard(onClick = onToggle) {
        Column(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DevOSStatusBadge(
                    finding.severity.name,
                    when (finding.severity) {
                        Severity.CRITICAL -> Color(0xFFFF4444)
                        Severity.HIGH     -> Color(0xFFFF8800)
                        Severity.MEDIUM   -> Color(0xFFFFCC00)
                        Severity.LOW      -> Color(0xFF44BB44)
                        Severity.INFO     -> MaterialTheme.colorScheme.outline
                    }
                )
                Spacer(Modifier.width(MaterialTheme.spacing.sm))
                Text(finding.title, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
            }
            Text("${finding.filePath}:${finding.lineNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(Modifier.height(MaterialTheme.spacing.sm))
                    Text(finding.description, style = MaterialTheme.typography.bodySmall)
                    finding.codeSnippet?.let { snippet ->
                        Spacer(Modifier.height(MaterialTheme.spacing.xs))
                        DevOSCodeBlock(code = snippet, language = "kotlin", showLineNumbers = false)
                    }
                    finding.cveId?.let { cve ->
                        Text("CVE: $cve", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(MaterialTheme.spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                        DevOSButton("Mark Fixed", onClick = onMarkFixed,
                            style = DevOSButtonStyle.Secondary)
                        DevOSButton("Dismiss", onClick = onDismiss,
                            style = DevOSButtonStyle.Tertiary)
                    }
                }
            }
        }
    }
}
```

### 4. Security Scanning Service (DEVOS-047)

File: `data/data-repository/src/main/kotlin/com/devos/ai/data/repository/worker/SecurityScanWorker.kt`

Runs as WorkManager task triggered after indexing completes.

SAST rules (applied via regex per file):
```kotlin
object SASTRules {
    val rules = listOf(
        SASTRule("HARDCODED_SECRET",   Severity.CRITICAL,
            Regex("""(password|api_key|secret|token)\s*=\s*["'][^"']{8,}["']""", IGNORE_CASE)),
        SASTRule("SQL_INJECTION",      Severity.HIGH,
            Regex("""rawQuery\s*\(\s*"[^"]*\$""")),
        SASTRule("INSECURE_RANDOM",    Severity.MEDIUM,
            Regex("""new\s+Random\(\)|Math\.random\(\)""")),
        SASTRule("CLEARTEXT_HTTP",     Severity.HIGH,
            Regex("""http://(?!localhost)""")),
        SASTRule("WORLD_READABLE",     Regex("""MODE_WORLD_READABLE|MODE_WORLD_WRITEABLE"""),
            Severity.HIGH),
    )
}
```

Dependency CVE check: load bundled `assets/cve-database.json` (format: `[{groupId, artifactId, version, cveId, severity, description}]`). Parse `build.gradle.kts` dependencies and cross-reference against CVE list.

Incremental: only re-scan files changed since last scan (compare file `contentHash`).

### 5. TestIntelligenceScreen (DEVOS-048, FIGMA-27)

Route: `TEST_INTELLIGENCE/{repoId}`

UiState:
```kotlin
sealed interface TestIntelligenceUiState {
    data object Loading : TestIntelligenceUiState
    data class Success(
        val overallCoverage: Float,
        val branchCoverage: Float,
        val coverageTrend: List<Float>,     // last 7 data points
        val uncoveredFiles: List<FileCoverage>,
        val flakyTests: List<FlakyTest>,
        val aiSuggestions: String,
        val isStreamingSuggestions: Boolean,
        val recentRuns: List<TestRunSummary>,
    ) : TestIntelligenceUiState
    data object Empty : TestIntelligenceUiState
    data class Error(val message: String, val retryable: Boolean) : TestIntelligenceUiState
}

data class FlakyTest(
    val testName: String,
    val failureRate: Float,     // 0.0–1.0
    val lastFailedAt: Instant,
)
```

Layout:
```kotlin
LazyColumn(Modifier.padding(MaterialTheme.spacing.base)) {
    // Coverage summary card
    item {
        DevOSCard {
            Column(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Line Coverage", style = MaterialTheme.typography.labelSmall)
                        Text("${(overallCoverage * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = coverageColor(overallCoverage))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Branch Coverage", style = MaterialTheme.typography.labelSmall)
                        Text("${(branchCoverage * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = coverageColor(branchCoverage))
                    }
                }
                // Sparkline trend
                CoverageTrendSparkline(values = coverageTrend,
                    modifier = Modifier.fillMaxWidth().height(40.dp))
            }
        }
    }
    // AI suggestions
    item {
        SectionHeader("AI Suggestions")
        DevOSAIMessage(content = aiSuggestions, isStreaming = isStreamingSuggestions)
    }
    // Uncovered files (sorted by file size — highest impact first)
    item { SectionHeader("Uncovered Files (${uncoveredFiles.size})") }
    items(uncoveredFiles) { file ->
        UncoveredFileItem(file, onClick = { onNavigateToFile(file.filePath) })
    }
    // Flaky tests
    item { SectionHeader("Flaky Tests") }
    items(flakyTests) { test ->
        FlakyTestItem(test)
    }
    // Recent runs
    item { SectionHeader("Recent Test Runs") }
    items(recentRuns) { run -> TestRunItem(run) }
}
```

### 6. Test Coverage Analysis Service (DEVOS-049)

File: `data/data-repository/src/main/kotlin/com/devos/ai/data/repository/coverage/TestCoverageAnalysisService.kt`

JaCoCo/Kover XML parsing using SAX (not DOM):
```kotlin
class JaCoCoParser : DefaultHandler() {
    private val coverageMap = mutableMapOf<String, FileCoverage>()

    override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
        when (qName) {
            "sourcefile" -> currentFile = attrs.getValue("name")
            "counter" -> if (attrs.getValue("type") == "LINE") {
                val covered = attrs.getValue("covered").toInt()
                val missed  = attrs.getValue("missed").toInt()
                coverageMap[currentFile] = FileCoverage(
                    filePath = currentFile,
                    lineCoverage = covered.toFloat() / (covered + missed).coerceAtLeast(1),
                    coveredLines = covered,
                    totalLines = covered + missed,
                    // branch coverage parsed similarly
                )
            }
        }
    }
}
```

Flaky test detection: a test is marked flaky if its pass/fail history in `TestRunEntity` shows failure rate > 20% over the last 10 runs.

AI gap analysis (`GetAICoverageSuggestionsUseCase`): sends list of uncovered file names + their sizes to `AIRepository.chat()` with prompt "Suggest which of these files should be prioritized for test coverage and why". Streams response.

**Tests:**
- `SecurityScanWorkerTest`: HARDCODED_SECRET rule fires on test file with plaintext key; CVE check flags known-bad dependency; incremental scan skips unchanged files
- `SecurityRepositoryImplTest`: findings stored in Room; dismiss updates status; mark fixed updates status
- `SecurityFindingsViewModelTest`: severity filter changes finding list; expand/collapse inline; mark fixed optimistic update
- `JaCoCoParserTest`: XML parsed correctly; line coverage percentages correct; SAX (not DOM) — no OOM on 10MB report
- `TestIntelligenceViewModelTest`: coverage data loads; flaky test detection correct; AI suggestions stream
- `FlakyTestDetectorTest`: test with 3/10 failures (30%) flagged as flaky; test with 1/10 failures (10%) not flagged

**Acceptance Criteria:**
- AC1 (DEVOS-046): Summary counts by severity shown as count chips (Critical/High/Medium/Low)
- AC2 (DEVOS-046): Finding list renders with severity badge, rule ID, file path, and line number
- AC3 (DEVOS-046): Tap finding expands inline with AI explanation, code snippet, CVE ID if present
- AC4 (DEVOS-046): "Mark Fixed" and "Dismiss" actions update finding status
- AC5 (DEVOS-046): Severity filter chips narrow the list
- AC6 (DEVOS-047): SAST rules applied — hardcoded secrets, SQL injection, cleartext HTTP detected
- AC7 (DEVOS-047): Dependency CVE check flags known-vulnerable versions using bundled CVE database
- AC8 (DEVOS-047): Findings stored in Room and incremental scan re-uses existing results for unchanged files
- AC9 (DEVOS-047): Scanner runs as WorkManager task — not on main thread
- AC10 (DEVOS-048): Overall line coverage percentage and branch coverage displayed with trend sparkline
- AC11 (DEVOS-048): Uncovered files list sorted by size (highest impact first) and navigable
- AC12 (DEVOS-048): Flaky test list shows test name, failure rate, and last failure date
- AC13 (DEVOS-048): AI suggestions streamed and rendered via `DevOSAIMessage`
- AC14 (DEVOS-048): Recent test runs history displayed
- AC15 (DEVOS-049): JaCoCo XML report parsed correctly using SAX — no OOM on large reports
- AC16 (DEVOS-049): Per-file coverage stored in Room and reflected in screen
- AC17 (DEVOS-049): Flaky tests detected when failure rate > 20% over last 10 runs
- AC18 (DEVOS-049): AI gap analysis streamed to `TestIntelligenceScreen`
