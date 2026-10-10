package com.devos.ai.data.repository.security

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.devos.ai.core.database.dao.SecurityFindingDao
import com.devos.ai.domain.repository.SecurityRepository
import com.devos.ai.domain.repository.model.FindingStatus
import com.devos.ai.domain.repository.model.SecurityFinding
import com.devos.ai.domain.repository.model.Severity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [SecurityRepository].
 *
 * Responsibilities:
 * - Enqueues [SecurityScanWorker] via WorkManager for SAST scans.
 * - Maps [SecurityFindingEntity] ↔ [SecurityFinding] (domain model).
 * - Delegates status mutations to [SecurityFindingDao].
 *
 * Feature modules must **never** import this class — inject [SecurityRepository].
 *
 * DEVOS-047 / DA-58
 */
@Singleton
class SecurityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityFindingDao: SecurityFindingDao,
) : SecurityRepository {

    // ── Observe ───────────────────────────────────────────────────────────────

    override fun observeFindings(repoId: String): Flow<List<SecurityFinding>> =
        securityFindingDao.observeByRepo(repoId).map { entities ->
            entities.map { entity ->
                SecurityFinding(
                    id          = entity.id,
                    repoId      = entity.repoId,
                    severity    = runCatching { Severity.valueOf(entity.severity) }
                        .getOrDefault(Severity.LOW),
                    ruleId      = entity.ruleId,
                    title       = entity.title,
                    description = entity.description,
                    filePath    = entity.filePath,
                    lineNumber  = entity.lineNumber,
                    status      = runCatching { FindingStatus.valueOf(entity.status) }
                        .getOrDefault(FindingStatus.OPEN),
                )
            }
        }

    // ── Trigger scan ──────────────────────────────────────────────────────────

    override suspend fun triggerScan(repoId: String): Result<Unit> = runCatching {
        val request = OneTimeWorkRequestBuilder<SecurityScanWorker>()
            .setInputData(workDataOf(SecurityScanWorker.KEY_REPO_ID to repoId))
            .addTag("security_scan_$repoId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(repoId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
        Timber.d("Enqueued SecurityScanWorker for repo %s", repoId)
    }

    // ── Status mutations ──────────────────────────────────────────────────────

    override suspend fun markFixed(findingId: String): Result<Unit> = runCatching {
        securityFindingDao.updateStatus(findingId, FindingStatus.FIXED.name)
    }

    override suspend fun dismiss(findingId: String): Result<Unit> = runCatching {
        securityFindingDao.updateStatus(findingId, FindingStatus.DISMISSED.name)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun workName(repoId: String) = "security_scan_$repoId"
}
