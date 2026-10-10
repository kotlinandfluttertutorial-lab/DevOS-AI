package com.devos.ai.data.repository.security

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.devos.ai.core.database.dao.SecurityFindingDao
import com.devos.ai.core.database.entity.SecurityFindingEntity
import com.devos.ai.domain.repository.model.FindingStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID

/**
 * WorkManager worker that runs SAST rules over the source files of a cloned
 * repository and stores findings via [SecurityFindingDao].
 *
 * ## Security
 * Every resolved file path is checked against the repository root via
 * canonical path comparison to prevent path-traversal attacks.
 *
 * ## Input data
 * - [KEY_REPO_ID] — the repository identifier (required).
 *
 * ## Output data
 * - [KEY_ERROR] — error message on failure.
 *
 * DEVOS-047 / DA-58
 */
@HiltWorker
class SecurityScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val securityFindingDao: SecurityFindingDao,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_REPO_ID = "repo_id"
        const val KEY_ERROR   = "error_message"

        private val SOURCE_EXTENSIONS = setOf("kt", "java")
    }

    override suspend fun doWork(): Result {
        val repoId = inputData.getString(KEY_REPO_ID)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing repo_id"))

        return try {
            val findings = scanRepository(repoId)

            // Replace all existing findings for this repo atomically
            securityFindingDao.deleteByRepo(repoId)
            securityFindingDao.upsertAll(findings)

            Timber.d(
                "SecurityScanWorker completed for %s: %d finding(s)",
                repoId,
                findings.size,
            )
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SecurityScanWorker failed for repo %s", repoId)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun scanRepository(repoId: String): List<SecurityFindingEntity> =
        withContext(Dispatchers.IO) {
            val repoRoot = File(applicationContext.filesDir, "repos/$repoId")
            if (!repoRoot.exists()) {
                Timber.w("Repo root does not exist for %s, skipping scan", repoId)
                return@withContext emptyList()
            }

            val canonicalRoot = repoRoot.canonicalPath
            val now = System.currentTimeMillis()
            val allFindings = mutableListOf<SecurityFindingEntity>()

            repoRoot.walkTopDown()
                .filter { it.isFile && it.extension in SOURCE_EXTENSIONS }
                .forEach { file ->
                    ensureActive()

                    // PATH TRAVERSAL PROTECTION
                    val canonical = file.canonicalPath
                    if (!canonical.startsWith(canonicalRoot)) {
                        Timber.w("Path traversal rejected: %s escapes repo root", canonical)
                        return@forEach
                    }

                    val relativePath = canonical
                        .removePrefix(canonicalRoot)
                        .trimStart(File.separatorChar)

                    val lines = runCatching { file.readLines() }.getOrElse { emptyList() }
                    lines.forEachIndexed { index, line ->
                        SASTRules.ALL.forEach { rule ->
                            if (rule.pattern.containsMatchIn(line)) {
                                allFindings += SecurityFindingEntity(
                                    id          = UUID.randomUUID().toString(),
                                    repoId      = repoId,
                                    severity    = rule.severity.name,
                                    ruleId      = rule.ruleId,
                                    title       = rule.title,
                                    description = rule.description,
                                    filePath    = relativePath,
                                    lineNumber  = index + 1, // 1-based
                                    codeSnippet = line.trim().take(200),
                                    cveId       = null,
                                    status      = FindingStatus.OPEN.name,
                                    detectedAt  = now,
                                )
                            }
                        }
                    }
                }

            allFindings
        }
}
