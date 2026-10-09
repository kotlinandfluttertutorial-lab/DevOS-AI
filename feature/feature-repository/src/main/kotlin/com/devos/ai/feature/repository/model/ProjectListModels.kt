package com.devos.ai.feature.repository.model

/**
 * Stub models for the Repository / Project List screen (DEVOS-017).
 *
 * Pure Kotlin — no Compose imports. DEVOS-015 will replace [stubRepositories]
 * with real data from domain use cases.
 */

/** Health status category derived from the health score. */
enum class HealthStatus {
    HEALTHY,   // score >= 80
    WARNING,   // score 50..79
    CRITICAL,  // score < 50
}

/** Synchronization state for a project. */
enum class SyncStatus {
    SYNCED,
    SYNCING,
    ERROR,
    IDLE,
}

/** Sort order for the project list. */
enum class SortOrder(val label: String) {
    NAME("Name"),
    LAST_SYNC("Last sync"),
    HEALTH("Health"),
}

/**
 * A project/repository shown in the list.
 *
 * [id] is the unique key for the list.
 * [healthScore] drives the [HealthStatus] badge — 0..100.
 * [warningCount] and [prCount] appear in the card footer row.
 */
data class ProjectSummary(
    val id: String,
    val name: String,
    val fullPath: String,
    val healthScore: Int,
    val healthStatus: HealthStatus,
    val syncStatus: SyncStatus,
    val lastSync: String,
    val languageTags: List<String>,
    val warningCount: Int,
    val prCount: Int,
)

/** Stub data matching the `#s-project-list` mockup. */
fun stubProjects(): List<ProjectSummary> = listOf(
    ProjectSummary(
        id = "devos-ai",
        name = "DevOS AI",
        fullPath = "github.com/dev/devos-ai",
        healthScore = 92,
        healthStatus = HealthStatus.HEALTHY,
        syncStatus = SyncStatus.SYNCED,
        lastSync = "2h ago",
        languageTags = listOf("Kotlin", "Android", "Compose"),
        warningCount = 2,
        prCount = 5,
    ),
    ProjectSummary(
        id = "compose-lib",
        name = "Compose Lib",
        fullPath = "github.com/dev/compose-lib",
        healthScore = 65,
        healthStatus = HealthStatus.WARNING,
        syncStatus = SyncStatus.SYNCED,
        lastSync = "1d ago",
        languageTags = listOf("Kotlin", "Compose"),
        warningCount = 2,
        prCount = 2,
    ),
    ProjectSummary(
        id = "sdk-tools",
        name = "SDK Tools",
        fullPath = "github.com/dev/sdk-tools",
        healthScore = 35,
        healthStatus = HealthStatus.CRITICAL,
        syncStatus = SyncStatus.ERROR,
        lastSync = "3d ago",
        languageTags = listOf("Kotlin MP"),
        warningCount = 1,
        prCount = 0,
    ),
)

/** All distinct language tags from [stubProjects], for the filter chip row. */
fun stubLanguageTags(): List<String> = stubProjects()
    .flatMap { it.languageTags }
    .distinct()
    .sorted()
