package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an imported repository.
 *
 * [syncStatus]  — persisted as the enum name string (e.g. "SYNCING").
 * [provider]    — persisted as the enum name string (e.g. "GITHUB").
 * [lastSyncAt]  — epoch-milliseconds UTC, null if never synced.
 * [localPath]   — absolute path inside `filesDir/repos/<id>` after cloning, null until cloned.
 */
@Entity(tableName = "repositories")
data class RepositoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val owner: String,
    val description: String?,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val defaultBranch: String,
    val cloneUrl: String,
    /** Stored as RepositoryProvider.name() */
    val provider: String,
    val healthScore: Float,
    /** Epoch-millis UTC; null = never synced */
    val lastSyncAt: Long?,
    /** Stored as SyncStatus.name() */
    val syncStatus: String,
    /** Absolute path on device after clone; null until worker completes CLONE step */
    val localPath: String?,
)
