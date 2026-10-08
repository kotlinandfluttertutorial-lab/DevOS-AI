package com.devos.ai.data.repository.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.entity.FileEntity
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.domain.repository.model.SyncStatus
import com.devos.ai.domain.repository.model.SyncStep
import com.devos.ai.feature.auth.model.OAuthProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

/**
 * WorkManager background job that clones a remote repository and indexes its
 * files into the local Room database.
 *
 * ## Security rules enforced here
 * - OAuth tokens are fetched from [SecureTokenRepository] — never from
 *   BuildConfig or hardcoded strings.
 * - Token values are **never logged** — only the masked form `XXXX****`.
 * - All file paths are validated against the repo root to prevent path traversal.
 *
 * ## Progress reporting
 * Each step calls [setProgress] with [KEY_STEP] so the UI can show a live
 * progress indicator via WorkManager's `WorkInfo.progress`.
 *
 * Enqueued and observed by [com.devos.ai.data.repository.RepositoryRepositoryImpl].
 */
@HiltWorker
class RepositoryIndexingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repositoryDao: RepositoryDao,
    private val fileDao: FileDao,
    private val tokenRepository: SecureTokenRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_REPO_ID   = "repo_id"
        const val KEY_CLONE_URL = "clone_url"
        const val KEY_PROVIDER  = "provider"
        const val KEY_STEP      = "current_step"
        const val KEY_ERROR     = "error_message"
    }

    override suspend fun doWork(): Result {
        val repoId   = inputData.getString(KEY_REPO_ID)   ?: return Result.failure()
        val cloneUrl = inputData.getString(KEY_CLONE_URL) ?: return Result.failure()
        val provider = try {
            OAuthProvider.valueOf(
                inputData.getString(KEY_PROVIDER) ?: OAuthProvider.GITHUB.name,
            )
        } catch (e: IllegalArgumentException) {
            Timber.w("Unknown provider in worker input, defaulting to GITHUB")
            OAuthProvider.GITHUB
        }

        return try {
            repositoryDao.updateSyncStatus(repoId, SyncStatus.SYNCING.name)
            setProgress(workDataOf(KEY_STEP to SyncStep.CLONE.name))

            val localPath = cloneRepository(cloneUrl, repoId, provider)
            repositoryDao.updateLocalPath(repoId, localPath)

            setProgress(workDataOf(KEY_STEP to SyncStep.PARSE.name))
            val files = parseFiles(localPath, repoId)
            fileDao.insertAll(files)

            // INDEX_SYMBOLS step is handled by DEVOS-023 (SymbolIndexingWorker).
            // BUILD_VECTORS step is handled by DEVOS-031 (RAG pipeline).
            setProgress(workDataOf(KEY_STEP to SyncStep.DONE.name))
            repositoryDao.updateSyncStatus(repoId, SyncStatus.SYNCED.name)

            Result.success()
        } catch (e: CancellationException) {
            // Worker was cancelled (cancelSync called) — reset to IDLE so the
            // UI can offer a retry.
            repositoryDao.updateSyncStatus(repoId, SyncStatus.IDLE.name)
            Result.failure()
        } catch (e: Exception) {
            Timber.e(e, "Indexing failed for repo $repoId")
            repositoryDao.updateSyncStatus(repoId, SyncStatus.ERROR.name)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    // -------------------------------------------------------------------------
    // Clone
    // -------------------------------------------------------------------------

    private suspend fun cloneRepository(
        url: String,
        repoId: String,
        provider: OAuthProvider,
    ): String = withContext(Dispatchers.IO) {
        val localDir = File(applicationContext.filesDir, "repos/$repoId")
        localDir.mkdirs()

        val token = tokenRepository.getToken(provider)

        // Shallow clone (depth=1) keeps disk footprint minimal.
        val cmd = Git.cloneRepository()
            .setURI(url)
            .setDirectory(localDir)
            .setDepth(1)
            .setCloneAllBranches(false)

        if (token != null) {
            // SECURITY: never log the raw token value.
            Timber.d(
                "Authenticating clone for %s with token %s****",
                provider.displayName,
                token.take(4),
            )
            cmd.setCredentialsProvider(
                UsernamePasswordCredentialsProvider("oauth2", token),
            )
        }

        cmd.call().use { /* close the Git handle */ }
        localDir.absolutePath
    }

    // -------------------------------------------------------------------------
    // Parse
    // -------------------------------------------------------------------------

    private suspend fun parseFiles(
        localPath: String,
        repoId: String,
    ): List<FileEntity> = withContext(Dispatchers.IO) {
        val root = File(localPath)
        val canonicalRoot = root.canonicalPath

        val skipDirs = setOf(".git", "build", ".gradle", ".idea", "node_modules")

        // Fetch existing file hashes for incremental update.
        val existing = fileDao.getByRepo(repoId).associateBy { it.path }
        val onDisk   = mutableSetOf<String>()

        val changed = root.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                skipDirs.none { skip ->
                    file.absolutePath.contains(File.separator + skip + File.separator) ||
                        file.absolutePath.endsWith(File.separator + skip)
                }
            }
            .mapNotNull { file ->
                ensureActive() // respect coroutine cancellation during large repos

                // PATH TRAVERSAL PROTECTION — reject any path that escapes the root.
                val resolved = file.canonicalPath
                require(resolved.startsWith(canonicalRoot)) {
                    "Path traversal rejected: $resolved escapes $canonicalRoot"
                }

                val rel = resolved
                    .removePrefix(canonicalRoot)
                    .trimStart(File.separatorChar)
                onDisk += rel

                val hash = computeHash(file)
                // Skip unchanged files — avoids unnecessary DB writes.
                if (existing[rel]?.contentHash == hash) null
                else FileEntity(
                    id           = "$repoId:$rel",
                    repoId       = repoId,
                    path         = rel,
                    name         = file.name,
                    extension    = file.extension,
                    sizeBytes    = file.length(),
                    lastModified = file.lastModified(),
                    contentHash  = hash,
                )
            }
            .toList()

        // Delete DB rows for files that no longer exist on disk.
        val removed = existing.keys - onDisk
        if (removed.isNotEmpty()) {
            Timber.d("Removing %d stale file records for repo %s", removed.size, repoId)
            removed.forEach { path ->
                fileDao.deleteByPath(repoId, path)
            }
        }

        changed
    }

    // -------------------------------------------------------------------------
    // Hashing
    // -------------------------------------------------------------------------

    private fun computeHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buf = ByteArray(8_192)
            var n: Int
            while (stream.read(buf).also { n = it } != -1) {
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
