package com.devos.ai.data.repository.mapper

import com.devos.ai.domain.repository.model.Repository
import com.devos.ai.domain.repository.model.RepositoryProvider
import com.devos.ai.domain.repository.model.SyncStatus
import com.devos.ai.network.model.RepositoryResponse
import java.time.Instant

/**
 * Maps [RepositoryResponse] (network DTO from `core-network`) to the domain
 * [Repository] model.
 *
 * Used in [RepositoryRepositoryImpl] when the backend returns repository data
 * that is not yet persisted locally (e.g. listing repositories registered on
 * the backend).
 *
 * All enum fallbacks use safe defaults to avoid crashes on new/unknown server values.
 */
fun RepositoryResponse.toDomainModel(): Repository = Repository(
    id            = id,
    name          = name,
    owner         = owner,
    fullName      = fullName,
    description   = description,
    language      = language,
    stars         = stars,
    forks         = forks,
    defaultBranch = defaultBranch,
    cloneUrl      = cloneUrl,
    provider      = runCatching { RepositoryProvider.valueOf(provider.uppercase()) }
                        .getOrDefault(RepositoryProvider.GITHUB),
    healthScore   = healthScore,
    lastSyncAt    = lastSyncAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    syncStatus    = runCatching { SyncStatus.valueOf(syncStatus.uppercase()) }
                        .getOrDefault(SyncStatus.IDLE),
)
