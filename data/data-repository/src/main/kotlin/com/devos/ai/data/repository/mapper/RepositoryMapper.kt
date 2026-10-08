package com.devos.ai.data.repository.mapper

import com.devos.ai.core.database.entity.RepositoryEntity
import com.devos.ai.domain.repository.model.Repository
import com.devos.ai.domain.repository.model.RepositoryProvider
import com.devos.ai.domain.repository.model.SyncStatus
import java.time.Instant

/**
 * Converts between [RepositoryEntity] (Room) and [Repository] (domain).
 *
 * Enum fields are persisted as their [Enum.name] strings and parsed back
 * with safe fallbacks to avoid crashes on unknown values.
 */
internal fun RepositoryEntity.toDomain(): Repository = Repository(
    id            = id,
    name          = name,
    owner         = owner,
    fullName      = "$owner/$name",
    description   = description,
    language      = language,
    stars         = stars,
    forks         = forks,
    defaultBranch = defaultBranch,
    cloneUrl      = cloneUrl,
    provider      = runCatching { RepositoryProvider.valueOf(provider) }
                        .getOrDefault(RepositoryProvider.GITHUB),
    healthScore   = healthScore,
    lastSyncAt    = lastSyncAt?.let { Instant.ofEpochMilli(it) },
    syncStatus    = runCatching { SyncStatus.valueOf(syncStatus) }
                        .getOrDefault(SyncStatus.IDLE),
)

internal fun Repository.toEntity(localPath: String? = null): RepositoryEntity = RepositoryEntity(
    id            = id,
    name          = name,
    owner         = owner,
    description   = description,
    language      = language,
    stars         = stars,
    forks         = forks,
    defaultBranch = defaultBranch,
    cloneUrl      = cloneUrl,
    provider      = provider.name,
    healthScore   = healthScore,
    lastSyncAt    = lastSyncAt?.toEpochMilli(),
    syncStatus    = syncStatus.name,
    localPath     = localPath,
)
