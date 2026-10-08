package com.devos.ai.domain.repository.model

import java.time.Instant

/**
 * Domain model representing an imported source repository.
 *
 * This is a pure Kotlin data class — zero Android imports.
 * UI and data layers map to/from this model; it is the single source of
 * truth for repository state throughout the domain layer.
 */
data class Repository(
    val id: String,
    val name: String,
    val owner: String,
    /** Combined "owner/name" slug, e.g. "google/iosched". */
    val fullName: String,
    val description: String?,
    val language: String?,
    val stars: Int = 0,
    val forks: Int = 0,
    val defaultBranch: String = "main",
    val cloneUrl: String,
    val provider: RepositoryProvider,
    /** 0–100 health score derived from recent commit activity, test coverage, etc. */
    val healthScore: Float = 0f,
    val lastSyncAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
)

enum class RepositoryProvider {
    GITHUB,
    GITLAB,
    LOCAL,
}

enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR,
}
