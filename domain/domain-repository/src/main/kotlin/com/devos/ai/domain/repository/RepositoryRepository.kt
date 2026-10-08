package com.devos.ai.domain.repository

import com.devos.ai.domain.repository.model.Repository
import com.devos.ai.domain.repository.model.RepositoryProvider
import com.devos.ai.domain.repository.model.SyncProgress
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for repository storage and synchronisation.
 *
 * Bound by Hilt in [com.devos.ai.data.repository.di.RepositoryModule].
 *
 * **These method signatures are frozen** — Track A ViewModels inject this
 * interface directly.  Do not change signatures without coordinating with the
 * Track A developer.
 */
interface RepositoryRepository {

    /** Emits the current list of all imported repositories and every subsequent change. */
    fun observeRepositories(): Flow<List<Repository>>

    /** Emits the repository with [repoId], or null if it has been deleted. */
    fun observeRepository(repoId: String): Flow<Repository?>

    /**
     * Persists a new repository record and enqueues the background indexing job.
     *
     * @param cloneUrl HTTPS clone URL, e.g. "https://github.com/owner/repo.git"
     * @param provider [RepositoryProvider] that owns this URL
     * @return [Result.success] with the new repository ID on success,
     *         [Result.failure] if the repo could not be queued.
     */
    suspend fun importRepository(
        cloneUrl: String,
        provider: RepositoryProvider,
    ): Result<String>

    /**
     * Re-enqueues the indexing job for an already-imported repository.
     *
     * Used by the "Refresh" action on the repository overview screen.
     */
    suspend fun syncRepository(repoId: String): Result<Unit>

    /**
     * Cancels a running or pending sync job for [repoId].
     *
     * Leaves the repository record intact; sync status is reset to [com.devos.ai.domain.repository.model.SyncStatus.IDLE].
     */
    suspend fun cancelSync(repoId: String): Result<Unit>

    /**
     * Removes the repository record and all associated files/symbols from the
     * local database.  Does NOT delete files from disk — that is intentional so
     * the user can re-import without re-downloading.
     */
    suspend fun deleteRepository(repoId: String): Result<Unit>

    /**
     * Emits live sync progress for [repoId], or null when no job is running.
     *
     * Backed by WorkManager's [androidx.work.WorkInfo] progress data.
     */
    fun observeSyncProgress(repoId: String): Flow<SyncProgress?>
}
