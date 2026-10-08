package com.devos.ai.domain.repository

import com.devos.ai.domain.repository.model.Repository
import com.devos.ai.domain.repository.model.RepositoryProvider
import com.devos.ai.domain.repository.model.SyncProgress
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for the repository data layer.
 *
 * Implemented by `data-repository` — feature modules must never import the
 * implementation directly; inject this interface via Hilt.
 *
 * **Thread safety:** All `suspend` functions are safe to call from any dispatcher.
 * Flows are cold and cancel safely.
 */
interface RepositoryRepository {

    /**
     * Emits the full list of imported repositories whenever any row changes.
     * Backed by a Room `Flow` — always up to date.
     */
    fun observeRepositories(): Flow<List<Repository>>

    /**
     * Emits the repository with [repoId], or `null` if it was deleted.
     */
    fun observeRepository(repoId: String): Flow<Repository?>

    /**
     * Imports a new repository from [cloneUrl]:
     * 1. Persists a stub entity with [SyncStatus.IDLE].
     * 2. Enqueues a [RepositoryIndexingWorker] via WorkManager.
     *
     * @return `Result.success(repoId)` on success, or a wrapped exception on failure.
     */
    suspend fun importRepository(cloneUrl: String, provider: RepositoryProvider): Result<String>

    /**
     * Re-enqueues the indexing worker for an already-imported repository.
     * Cancels any in-flight job for [repoId] before re-enqueuing.
     */
    suspend fun syncRepository(repoId: String): Result<Unit>

    /**
     * Cancels the in-flight WorkManager job for [repoId] and resets its
     * sync status to [SyncStatus.IDLE].
     */
    suspend fun cancelSync(repoId: String): Result<Unit>

    /**
     * Removes the repository record and all associated files / symbols from
     * the local database. Does NOT delete the cloned directory — that is
     * handled by a separate cleanup job in DEVOS-023.
     */
    suspend fun deleteRepository(repoId: String): Result<Unit>

    /**
     * Emits live sync progress for [repoId], derived from WorkManager
     * `WorkInfo` progress data. Emits `null` when there is no active job.
     */
    fun observeSyncProgress(repoId: String): Flow<SyncProgress?>
}
