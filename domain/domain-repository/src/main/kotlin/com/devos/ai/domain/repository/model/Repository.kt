package com.devos.ai.domain.repository.model

import java.time.Instant

/**
 * Domain model for a source-code repository managed by DevOS AI.
 *
 * This is a pure Kotlin data class — no Android imports allowed.
 */
data class Repository(
    /** Stable UUID assigned at import time. */
    val id: String,

    /** Short repository name (e.g. "MyApp"). */
    val name: String,

    /** Owner login / organisation (e.g. "acme-corp"). */
    val owner: String,

    /** Composite "owner/name" used in API calls and display. */
    val fullName: String,

    val description: String?,
    val language: String?,
    val stars: Int = 0,
    val forks: Int = 0,
    val defaultBranch: String = "main",

    /** Remote URL used to clone the repository. */
    val cloneUrl: String,

    val provider: RepositoryProvider,

    /**
     * Composite health score [0.0, 1.0] derived from security findings,
     * test coverage, and sync freshness. Populated by DEVOS-023+.
     */
    val healthScore: Float = 0f,

    /** UTC timestamp of the last successful sync, or null if never synced. */
    val lastSyncAt: Instant? = null,

    val syncStatus: SyncStatus = SyncStatus.IDLE,
)
