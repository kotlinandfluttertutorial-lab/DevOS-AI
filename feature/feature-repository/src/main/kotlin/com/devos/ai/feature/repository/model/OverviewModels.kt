package com.devos.ai.feature.repository.model

/**
 * Stub overview models for the Repository Overview screen (DEVOS-016).
 *
 * Pure Kotlin — no Compose imports. The color role for a language-bar segment
 * is modeled as an enum ([LanguageBarColor]) and resolved to a Compose color
 * in the screen, keeping these types testable and Compose-free.
 *
 * DEVOS-015 will replace [stubRepoOverview] with a real domain use case.
 */

/** Color role for a language-bar segment, resolved to a Compose color in the screen. */
enum class LanguageBarColor { PRIMARY, TERTIARY, WARNING }

/** One segment of the repository language-breakdown bar. [fraction] is the flex weight (e.g. 7f/2f/1f). */
data class LanguageSegment(
    val name: String,
    val fraction: Float,
    val color: LanguageBarColor,
)

/** A single recent commit shown on the Overview tab. */
data class CommitSummary(
    val sha: String,
    val message: String,
    val authorInitial: String,
    val relativeTime: String,
    val additions: Int,
    val deletions: Int,
)

/** Aggregate repository overview data for DEVOS-016 (stub). */
data class RepoOverview(
    val name: String,
    val branch: String,
    val lastSync: String,
    val isIndexed: Boolean,
    val languageBreakdown: List<LanguageSegment>,
    val stars: Int,
    val fileCount: Int,
    val openIssues: Int,
    val openPRs: Int,
    val ciStatus: Int,
    val aiInsight: String,
    val aiInsightHighlight: String,
    val recentCommits: List<CommitSummary>,
)

/**
 * The 7 in-page tabs shown on the overview screen.
 *
 * The mockup (`#s-repo-overview`) is authoritative: 7 tabs, not 8.
 */
enum class OverviewTab(val label: String) {
    OVERVIEW("Overview"),
    FILES("Files"),
    SEARCH("Search"),
    SYMBOLS("Symbols"),
    GRAPH("Graph"),
    GIT("Git"),
    AI("AI"),
}

/** Stub data matching the `#s-repo-overview` mockup. Replaced by real data in a later ticket. */
fun stubRepoOverview(): RepoOverview = RepoOverview(
    name = "devos-ai",
    branch = "main",
    lastSync = "2h ago",
    isIndexed = true,
    languageBreakdown = listOf(
        LanguageSegment("Kotlin", 7f, LanguageBarColor.PRIMARY),
        LanguageSegment("XML", 2f, LanguageBarColor.TERTIARY),
        LanguageSegment("Gradle", 1f, LanguageBarColor.WARNING),
    ),
    stars = 2400,
    fileCount = 847,
    openIssues = 12,
    openPRs = 5,
    ciStatus = 87,
    aiInsight = "MVVM + Clean Architecture detected. Well-separated layers with good dependency injection via Hilt.",
    aiInsightHighlight = "MVVM + Clean Architecture",
    recentCommits = listOf(
        CommitSummary("a4f2c3d", "feat: AI chat context selector", "D", "2 hours ago", 127, 23),
        CommitSummary("e8c1f9a", "fix: navigation memory leak", "D", "yesterday", 12, 45),
    ),
)
