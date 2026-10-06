# Spec: Testing Intelligence — Core

**Jira:** DEVOS-048 / DEVOS-049  
**Epic:** DEVOS-E07  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement test coverage analysis and the test intelligence dashboard.

## Domain Models

```kotlin
data class TestCoverage(
    val repoId: String,
    val overallPercent: Float,
    val linePercent: Float,
    val branchPercent: Float,
    val trend: List<CoveragePoint>,       // last 30 days
    val uncoveredFiles: List<UncoveredFile>,
    val worstFiles: List<FileCoverage>,
    val lastAnalyzedAt: Long,
)

data class Filecoverage(
    val filePath: String,
    val linePercent: Float,
    val coveredLines: Int,
    val totalLines: Int,
    val uncoveredRanges: List<LineRange>,
)

data class FlakyTest(
    val testName: String,
    val filePath: String,
    val failureRate: Float,       // 0.0–1.0
    val lastFlake: Long,
    val details: String,
)

data class TestRun(
    val id: String,
    val repoId: String,
    val branch: String,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val duration: Long,
    val timestamp: Long,
    val failures: List<TestFailure>,
)
```

## Coverage Parsing

Support:
- **JaCoCo XML** (`jacocoTestReport.xml`)
- **Kover XML** (`kover/report.xml`)
- Parse to `FilesCoverage` model, store in Room

## Screen: TestIntelligenceScreen (FIGMA-27)

**States:** Loading | Success | Empty (no coverage data) | Error

**Sections:**
1. **Coverage Summary Card**
   - Overall % with color ring (green >80%, yellow 50-80%, red <50%)
   - Line % + Branch % secondary stats
   - Trend sparkline (last 30 days)

2. **AI Suggestions** (prominent section)
   - "Add tests for AuthViewModel — 0% coverage"
   - "Fix flaky LoginTest — 40% failure rate"
   - Each tappable → opens AI chat with suggestion context

3. **Uncovered Files** (LazyColumn, sorted by priority)
   - File path + % covered + uncovered line count
   - Tap → CodeViewerScreen with uncovered lines highlighted

4. **Flaky Tests** (LazyColumn)
   - Test name + failure rate + last flake date
   - Tap → details + AI fix suggestion

5. **Recent Test Runs** (LazyColumn)
   - Run result badge + pass/fail counts + duration + branch

## AI Gap Analysis
```
Input: uncoveredFiles (sorted by importance score)
       importanceScore = function_count × (1 - coverage_percent)
Output: prioritized list of "Add test for X" suggestions
        with code context injected from RAG
```

## Acceptance Criteria
- [ ] JaCoCo XML parsed correctly for test project
- [ ] Kover XML parsed correctly for test project
- [ ] Coverage percentage matches direct file analysis
- [ ] AI suggestions appear for files with <50% coverage
- [ ] Uncovered files list sorted by importance
- [ ] Flaky test detection based on multiple run history
- [ ] Tap on uncovered file navigates to correct CodeViewerScreen
- [ ] Empty state when no coverage data available
- [ ] Trend chart shows last 30 data points
