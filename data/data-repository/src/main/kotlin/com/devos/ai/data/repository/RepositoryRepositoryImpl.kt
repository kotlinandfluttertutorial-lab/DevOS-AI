package com.devos.ai.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.entity.RepositoryEntity
import kotlinx.coroutines.flow.first
import com.devos.ai.data.repository.worker.RepositoryIndexingWorker
import com.devos.ai.domain.repository.RepositoryRepository
import com.devos.ai.domain.repository.model.Repository
import com.devos.ai.domain.repository.model.RepositoryProvider
import com.devos.ai.domain.repository.model.SyncProgress
import com.devos.ai.domain.repository.model.SyncStatus
import com.devos.ai.domain.repository.model.SyncStep
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [RepositoryRepository].
 *
 * ## Responsibilities
 * - Owns the mapping between [RepositoryEntity] ↔ [Repository] domain model.
 * - Delegates all background work to [RepositoryIndexingWorker] via WorkManager.
 * - Exposes live [SyncProgress] by observing WorkManager's [WorkInfo].
 *
 * ## Architecture note
 * Feature modules must **never** import this class directly — they depend on
 * the [RepositoryRepository] interface bound in [di.RepositoryModule].
 */
@Singleton
class RepositoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repositoryDao: RepositoryDao,
) : RepositoryRepository {

    // -------------------------------------------------------------------------
    // Observe
    // -------------------------------------------------------------------------

    override fun observeRepositories(): Flow<List<Repository>> =
        repositoryDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeRepository(repoId: String): Flow<Repository?> =
        repositoryDao.observeById(repoId).map { it?.toDomain() }

    // -------------------------------------------------------------------------
    // Mutate
    // -------------------------------------------------------------------------

    override suspend fun importRepository(
        cloneUrl: String,
        provider: RepositoryProvider,
    ): Result<String> = runCatching {
        val repoId = UUID.randomUUID().toString()

        // Derive a best-effort name/owner from the clone URL.
        val (owner, name) = parseOwnerAndName(cloneUrl)

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
        repoId
    }

    override suspend fun syncRepository(repoId: String): Result<Unit> = runCatching {
        val entity = repositoryDao.observeById(repoId).first()
            ?: error("Repository $repoId not found")
        enqueueIndexingWork(repoId, entity.cloneUrl, RepositoryProvider.valueOf(entity.provider))
    }

    override suspend fun cancelSync(repoId: String): Result<Unit> = runCatching {
        WorkManager.getInstance(context).cancelUniqueWork(workName(repoId))
        repositoryDao.updateSyncStatus(repoId, SyncStatus.IDLE.name)
    }

    override suspend fun deleteRepository(repoId: String): Result<Unit> = runCatching {
        // Cancel any running job first.
        WorkManager.getInstance(context).cancelUniqueWork(workName(repoId))
        // Room CASCADE handles repository_files and symbols deletion.
        repositoryDao.delete(repoId)
    }

    // -------------------------------------------------------------------------
    // Progress
    // -------------------------------------------------------------------------

    override fun observeSyncProgress(repoId: String): Flow<SyncProgress?> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(workName(repoId))
            .map { infos ->
                val info = infos.firstOrNull() ?: return@map null
                when (info.state) {
                    WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                        val stepName = info.progress.getString(RepositoryIndexingWorker.KEY_STEP)
                        val currentStep = stepName?.let { runCatching { SyncStep.valueOf(it) }.getOrNull() }
                            ?: SyncStep.CLONE
                        val completed = SyncStep.entries
                            .takeWhile { it != currentStep }
                        SyncProgress(
                            repoId         = repoId,
                            currentStep    = currentStep,
                            completedSteps = completed,
                            errorMessage   = null,
                        )
                    }
                    WorkInfo.State.SUCCEEDED ->
                        SyncProgress(
                            repoId         = repoId,
                            currentStep    = SyncStep.DONE,
                            completedSteps = SyncStep.entries.dropLast(1),
                            errorMessage   = null,
                        )
                    WorkInfo.State.FAILED -> {
                        val error = info.outputData.getString(RepositoryIndexingWorker.KEY_ERROR)
                        SyncProgress(
                            repoId         = repoId,
                            currentStep    = SyncStep.CLONE,
                            completedSteps = emptyList(),
                            errorMessage   = error ?: "Sync failed",
                        )
                    }
                    WorkInfo.State.CANCELLED,
                    WorkInfo.State.BLOCKED -> null
                }
            }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun enqueueIndexingWork(
        repoId: String,
        cloneUrl: String,
        provider: RepositoryProvider,
    ) {
        val request = OneTimeWorkRequestBuilder<RepositoryIndexingWorker>()
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
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(repoId), ExistingWorkPolicy.REPLACE, request)
    }

    /** Stable unique work tag derived from the repo ID. */
    private fun workName(repoId: String) = "index_$repoId"

    /**
     * Best-effort extraction of owner and repo name from a clone URL.
     *
     * Handles HTTPS URLs like "https://github.com/owner/repo.git" and
     * "https://gitlab.com/group/sub/repo.git" (uses last two path segments).
     * Falls back to ("unknown", url) for unrecognised formats.
     */
    private fun parseOwnerAndName(cloneUrl: String): Pair<String, String> {
        val cleaned = cloneUrl.trimEnd('/').removeSuffix(".git")
        val segments = cleaned.split("/").filter { it.isNotBlank() }
        return if (segments.size >= 2) {
            segments[segments.size - 2] to segments.last()
        } else {
            "unknown" to (segments.lastOrNull() ?: cloneUrl)
        }
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private fun RepositoryEntity.toDomain(): Repository = Repository(
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
}
