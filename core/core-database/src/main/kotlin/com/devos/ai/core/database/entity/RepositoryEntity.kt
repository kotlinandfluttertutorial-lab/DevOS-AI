package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity persisting an imported source repository.
 *
 * String columns [provider] and [syncStatus] store enum names; mapping to
 * domain enums is done in the data layer (RepositoryRepositoryImpl).
 *
 * [localPath] is null until the background clone worker sets it.
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
    /** Stores [com.devos.ai.domain.repository.model.RepositoryProvider] name. */
    val provider: String,
    val healthScore: Float,
    /** Epoch-milliseconds; null until first sync completes. */
    val lastSyncAt: Long?,
    /** Stores [com.devos.ai.domain.repository.model.SyncStatus] name. */
    val syncStatus: String,
    /** Absolute path to the local clone directory; null before cloning. */
    val localPath: String?,
)
