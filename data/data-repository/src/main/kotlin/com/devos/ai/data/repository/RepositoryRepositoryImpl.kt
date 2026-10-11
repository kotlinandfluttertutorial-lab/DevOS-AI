package com.devos.ai.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.entity.RepositoryEntity
import com.devos.ai.data.repository.mapper.toDomain
import com.devos.ai.data.repository.worker.RepositoryIndexingWorker
import com.devos.ai.domain.repository.RepositoryRepository
import com.devos.ai.domain.repository.model.Repository
import com.devos.ai.domain.repository.model.RepositoryProvider
import com.devos.ai.domain.repository.model.SyncProgress
import com.devos.ai.domain.repository.model.SyncStatus
import com.devos.ai.domain.repository.model.SyncStep
import com.devos.ai.network.DevOSApiService
import com.devos.ai.network.NetworkException
import com.devos.ai.network.model.CreateRepositoryRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Production implementation of [RepositoryRepository].
 *
 * ## Responsibilities
 * - Maps between [RepositoryEntity] (Room) and [Repository] (domain).
 * - Delegates clone + indexing work to [RepositoryIndexingWorker] via WorkManager.
 * - Exposes live [SyncProgress] by observing WorkManager [WorkInfo].
 * - **DA-130**: Registers/removes repositories with the DevOS backend via
 *   [DevOSApiService] so the Python backend stays in sync with local state.
 *
 * ## Backend integration (DA-130)
 * - [importRepository] calls `POST /v1/repositories` to register the repo
 *   with the backend before enqueuing local WorkManager work.
 * - [deleteRepository] calls `DELETE /v1/repositories/{id}` to remove the
 *   backend record.  Failure is logged but does not block local deletion.
 * - [syncRepository] calls `POST /v1/repositories/{id}/index` to trigger
 *   server-side indexing in addition to local WorkManager indexing.
 *   Failure is logged but does not block local work.
 *
 * ## Threading
 * All `suspend` functions dispatch I/O on [Dispatchers.IO] inside the DAO
 * and WorkManager calls. This class is safe to call from any dispatcher.
 */
@Singleton
open class RepositoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repositoryDao: RepositoryDao,
    private val fileDao: FileDao,
    @Named("ChunkingWorkerClass")
    private val chunkingWorkerClass: Class<out ListenableWorker>,
    private val apiService: DevOSApiService,
) : RepositoryRepository {

    /** Overrideable in tests â€” avoids WorkManager.getInstance() static call. */
    internal open fun getWorkManager(): WorkManager = WorkManager.getInstance(context)

    private val workManager: WorkManager
        get() = getWorkManager()

    // â”€â”€ Observe â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override fun observeRepositories(): Flow<List<Repository>> =
        repositoryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeRepository(repoId: String): Flow<Repository?> =
        repositoryDao.observeById(repoId).map { it?.toDomain() }

    // â”€â”€ Import â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Imports a new repository:
     * 1. Registers the repository with the DevOS backend (best-effort).
     * 2. Generates a stable UUID as [repoId].
     * 3. Derives [owner] and [name] from the clone URL.
     * 4. Inserts a stub [RepositoryEntity] with [SyncStatus.IDLE].
     * 5. Enqueues [RepositoryIndexingWorker].
     *
     * @return `Result.success(repoId)` or a wrapped exception.
     */
    override suspend fun importRepository(
        cloneUrl: String,
        provider: RepositoryProvider,
    ): Result<String> = runCatching {
        val repoId = UUID.randomUUID().toString()
        val (owner, name) = parseOwnerAndName(cloneUrl)

        // Register with backend â€” best effort; local import proceeds even if this fails
        registerWithBackend(cloneUrl, provider, owner, name)

        val entity = RepositoryEntity(
            id            = repoId,
            name          = name,
            owner         = owner,
            description   = null,
            language      = null,
            stars         = 0,
            forks         = 0,
            defaultBranch = "main",
            cloneUrl      = cloneUrl,
            provider      = provider.name,
            healthScore   = 0f,
            lastSyncAt    = null,
            syncStatus    = SyncStatus.IDLE.name,
            localPath     = null,
        )

        repositoryDao.upsert(entity)
        enqueueIndexingWork(repoId, cloneUrl, provider)

        Timber.d("Imported repository %s/%s (id=%s)", owner, name, repoId)
        repoId
    }

    // â”€â”€ Sync â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override suspend fun syncRepository(repoId: String): Result<Unit> = runCatching {
        val entity = repositoryDao.getById(repoId)
            ?: error("Repository $repoId not found")

        val provider = runCatching {
            RepositoryProvider.valueOf(entity.provider)
        }.getOrDefault(RepositoryProvider.GITHUB)

        // Trigger server-side indexing â€” best effort
        triggerBackendIndex(repoId)

        enqueueIndexingWork(repoId, entity.cloneUrl, provider)
        Timber.d("Re-enqueued sync for repository %s", repoId)
    }

    // â”€â”€ Cancel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override suspend fun cancelSync(repoId: String): Result<Unit> = runCatching {
        workManager.cancelUniqueWork(workName(repoId))
        repositoryDao.updateSyncStatus(repoId, SyncStatus.IDLE.name)
        Timber.d("Cancelled sync for repository %s", repoId)
    }

    // â”€â”€ Delete â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override suspend fun deleteRepository(repoId: String): Result<Unit> = runCatching {
        workManager.cancelUniqueWork(workName(repoId))
        repositoryDao.delete(repoId)
        // FileEntity and SymbolEntity rows are removed by Room CASCADE.

        // Remove from backend â€” best effort; local deletion already complete
        deleteFromBackend(repoId)

        Timber.d("Deleted repository %s", repoId)
    }

    // â”€â”€ Progress â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Maps the live [WorkInfo] for the indexing job to a [SyncProgress] domain model.
     *
     * Emits `null` when no work is enqueued or the job has reached a terminal
     * state (SUCCEEDED / CANCELLED / FAILED) â€” the caller should fall back to
     * [observeRepository] for the final [SyncStatus].
     */
    override fun observeSyncProgress(repoId: String): Flow<SyncProgress?> =
        workManager
            .getWorkInfosForUniqueWorkFlow(workName(repoId))
            .map { infos -> infos.firstOrNull()?.toSyncProgress(repoId) }

    // â”€â”€ Backend helpers (DA-130) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Registers the repository with the DevOS backend.
     * Failures are logged but never rethrow â€” the local import always proceeds.
     */
    private suspend fun registerWithBackend(
        cloneUrl: String,
        provider: RepositoryProvider,
        owner: String,
        name: String,
    ) {
        try {
            apiService.createRepository(
                CreateRepositoryRequest(
                    cloneUrl = cloneUrl,
                    provider = provider.name.lowercase(),
                    name     = name,
                    owner    = owner,
                ),
            )
            Timber.d("Registered repository %s/%s with backend", owner, name)
        } catch (e: HttpException) {
            Timber.w("Failed to register repository with backend: HTTP %d", e.code())
        } catch (e: IOException) {
            Timber.w("Failed to register repository with backend: network unavailable")
        }
    }

    /**
     * Triggers server-side re-indexing for an existing repository.
     * Failures are logged but never rethrow.
     */
    private suspend fun triggerBackendIndex(repoId: String) {
        try {
            apiService.triggerIndex(repoId)
            Timber.d("Triggered backend index for repository %s", repoId)
        } catch (e: HttpException) {
            Timber.w("Failed to trigger backend index: HTTP %d", e.code())
        } catch (e: IOException) {
            Timber.w("Failed to trigger backend index: network unavailable")
        }
    }

    /**
     * Removes the repository record from the backend.
     * Failures are logged but never rethrow â€” local deletion already completed.
     */
    private suspend fun deleteFromBackend(repoId: String) {
        try {
            apiService.deleteRepository(repoId)
            Timber.d("Deleted repository %s from backend", repoId)
        } catch (e: HttpException) {
            Timber.w("Failed to delete repository from backend: HTTP %d", e.code())
        } catch (e: IOException) {
            Timber.w("Failed to delete repository from backend: network unavailable")
        }
    }

    // â”€â”€ Private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Unique WorkManager job name for a given repository. */
    private fun workName(repoId: String) = "index_$repoId"

    private fun enqueueIndexingWork(
        repoId: String,
        cloneUrl: String,
        provider: RepositoryProvider,
    ) {
        val indexRequest = OneTimeWorkRequestBuilder<RepositoryIndexingWorker>()
            .setInputData(
                workDataOf(
                    RepositoryIndexingWorker.KEY_REPO_ID   to repoId,
                    RepositoryIndexingWorker.KEY_CLONE_URL to cloneUrl,
                    RepositoryIndexingWorker.KEY_PROVIDER  to provider.name,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .addTag("index_$repoId")
            .build()

        // Build ChunkingWorker request using the injected class reference so that
        // data-repository stays free of data-ai imports (Clean Architecture).
        val chunkRequest = androidx.work.OneTimeWorkRequest.Builder(chunkingWorkerClass)
            .setInputData(workDataOf("repo_id" to repoId))
            .addTag("chunk_$repoId")
            .build()

        // Chain: RepositoryIndexingWorker â†’ ChunkingWorker (RAG vectors)
        workManager
            .beginUniqueWork(workName(repoId), ExistingWorkPolicy.REPLACE, indexRequest)
            .then(chunkRequest)
            .enqueue()
    }

    /**
     * Parses "owner" and "name" from common clone URL formats:
     * - `https://github.com/owner/repo.git`
     * - `git@github.com:owner/repo.git`
     *
     * Falls back to ("unknown", last path segment) on unrecognised formats.
     */
    private fun parseOwnerAndName(cloneUrl: String): Pair<String, String> {
        // Strip trailing .git and split on the last two path segments
        val cleaned = cloneUrl.trimEnd('/').removeSuffix(".git")
        val segments = cleaned.split("/", ":")
        return if (segments.size >= 2) {
            val name  = segments.last()
            val owner = segments[segments.size - 2]
            owner to name
        } else {
            "unknown" to (segments.lastOrNull() ?: "repo")
        }
    }
}

// â”€â”€ WorkInfo â†’ SyncProgress mapper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun WorkInfo.toSyncProgress(repoId: String): SyncProgress? {
    // Terminal non-success states with no active progress data
    if (state == WorkInfo.State.CANCELLED) return null
    if (state == WorkInfo.State.SUCCEEDED) return SyncProgress(
        repoId        = repoId,
        currentStep   = SyncStep.DONE,
        completedSteps = SyncStep.entries.dropLast(1), // all steps except DONE
    )

    val stepName = progress.getString(RepositoryIndexingWorker.KEY_STEP)
        ?: outputData.getString(RepositoryIndexingWorker.KEY_STEP)

    val currentStep = stepName
        ?.let { runCatching { SyncStep.valueOf(it) }.getOrNull() }
        ?: if (state == WorkInfo.State.FAILED) SyncStep.CLONE else return null

    val errorMessage = if (state == WorkInfo.State.FAILED) {
        outputData.getString(RepositoryIndexingWorker.KEY_ERROR)
    } else null

    // Mark all steps that precede the current one as completed
    val allSteps = SyncStep.entries
    val currentIndex = allSteps.indexOf(currentStep)
    val completedSteps = if (currentIndex > 0) allSteps.subList(0, currentIndex) else emptyList()

    return SyncProgress(
        repoId         = repoId,
        currentStep    = currentStep,
        completedSteps = completedSteps,
        errorMessage   = errorMessage,
    )
}
