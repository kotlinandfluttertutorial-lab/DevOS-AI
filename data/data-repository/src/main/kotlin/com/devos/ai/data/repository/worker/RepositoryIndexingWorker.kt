package com.devos.ai.data.repository.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException

/**
 * WorkManager worker that clones and indexes a repository.
 *
 * ## Pipeline steps
 * 1. [SyncStep.CLONE]         — shallow git clone into `filesDir/repos/<repoId>`
 * 2. [SyncStep.PARSE]         — walk the file tree and upsert [FileEntity] rows
 * 3. [SyncStep.INDEX_SYMBOLS] — enqueue [SymbolIndexingWorker] as chained work
 * 4. [SyncStep.DONE]          — mark sync status SYNCED
 *
 * [SyncStep.BUILD_VECTORS] (RAG embeddings) is handled by DEVOS-031.
 *
 * ## Security
 * - Tokens are NEVER logged raw; only `token.take(4)+"****"` appears in logs.
 * - All file paths are canonicalised against the repo root to prevent
 *   path-traversal before any [FileEntity] is written.
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

        /** Emitted via [setProgress] so the UI can display the current pipeline step. */
        const val KEY_STEP      = "current_step"

        /** Emitted via [Result.failure] output data on error. */
        const val KEY_ERROR     = "error_message"

        /** Unique work name prefix for chaining with [SymbolIndexingWorker]. */
        internal fun symbolWorkName(repoId: String) = "symbol_index_$repoId"
    }

    override suspend fun doWork(): Result {
        val repoId   = inputData.getString(KEY_REPO_ID)   ?: return Result.failure()
        val cloneUrl = inputData.getString(KEY_CLONE_URL) ?: return Result.failure()
        val provider = runCatching {
            OAuthProvider.valueOf(inputData.getString(KEY_PROVIDER) ?: OAuthProvider.GITHUB.name)
        }.getOrDefault(OAuthProvider.GITHUB)

        return try {
            // ── CLONE ─────────────────────────────────────────────────────────
            repositoryDao.updateSyncStatus(repoId, SyncStatus.SYNCING.name)
            setProgress(workDataOf(KEY_STEP to SyncStep.CLONE.name))

            val localPath = cloneRepository(cloneUrl, repoId, provider)
            repositoryDao.updateLocalPath(repoId, localPath)

            // ── PARSE ─────────────────────────────────────────────────────────
            setProgress(workDataOf(KEY_STEP to SyncStep.PARSE.name))
            val files = parseFiles(localPath, repoId)
            fileDao.insertAll(files)

            // ── INDEX_SYMBOLS ─────────────────────────────────────────────────
            // Enqueue SymbolIndexingWorker as chained unique work.
            // It runs after this worker completes and shares the same work chain tag.
            setProgress(workDataOf(KEY_STEP to SyncStep.INDEX_SYMBOLS.name))
            enqueueSymbolIndexing(repoId)

            // ── DONE ──────────────────────────────────────────────────────────
            setProgress(workDataOf(KEY_STEP to SyncStep.DONE.name))
            repositoryDao.updateSyncStatus(repoId, SyncStatus.SYNCED.name)
            repositoryDao.updateLastSyncAt(repoId, System.currentTimeMillis())

            Result.success()
        } catch (e: CancellationException) {
            // Worker was cancelled — reset to IDLE so the user can retry
            repositoryDao.updateSyncStatus(repoId, SyncStatus.IDLE.name)
            Result.failure()
        } catch (e: Exception) {
            Timber.e(e, "Indexing failed for repo %s", repoId)
            repositoryDao.updateSyncStatus(repoId, SyncStatus.ERROR.name)
            Result.failure(
                workDataOf(KEY_ERROR to (e.message ?: "Unknown error")),
            )
        }
    }

    // ── Symbol worker chaining ────────────────────────────────────────────────

    /**
     * Enqueues [SymbolIndexingWorker] for [repoId] with REPLACE policy so that a
     * manual re-sync always gets a fresh symbol extraction pass.
     *
     * The chained worker runs independently — if symbol indexing fails, the file
     * index and sync status are not affected.
     */
    private fun enqueueSymbolIndexing(repoId: String) {
        val request = OneTimeWorkRequestBuilder<SymbolIndexingWorker>()
            .setInputData(workDataOf(SymbolIndexingWorker.KEY_REPO_ID to repoId))
            .addTag("symbol_index")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                symbolWorkName(repoId),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        Timber.d("Enqueued SymbolIndexingWorker for repo %s", repoId)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Performs a shallow (depth=1) clone of [url] into `filesDir/repos/[repoId]`.
     *
     * If a token is available it is passed as the password for "oauth2" HTTP auth.
     * The raw token is NEVER logged — only a masked form is printed.
     *
     * @return The absolute path of the cloned directory.
     */
    private suspend fun cloneRepository(
        url: String,
        repoId: String,
        provider: OAuthProvider,
    ): String = withContext(Dispatchers.IO) {
        val localDir = File(applicationContext.filesDir, "repos/$repoId")
        localDir.mkdirs()

        val token = tokenRepository.getToken(provider)
        if (token != null) {
            Timber.d("Cloning with token for %s: %s****", provider.displayName, token.take(4))
        }

        val cmd = Git.cloneRepository()
            .setURI(url)
            .setDirectory(localDir)
            .setDepth(1)

        if (token != null) {
            cmd.setCredentialsProvider(
                UsernamePasswordCredentialsProvider("oauth2", token),
            )
        }

        cmd.call().use { /* close JGit resources */ }
        localDir.absolutePath
    }

    /**
     * Walks the cloned directory tree and returns [FileEntity] rows for files
     * that are new or whose content hash has changed since the last sync.
     *
     * ## Incremental behaviour
     * - Existing rows with matching hashes are skipped (no DB write).
     * - Rows for files deleted from disk are removed from the DB.
     *
     * ## Path-traversal protection
     * Every resolved canonical path is checked against the repo root canonical
     * path before creating a [FileEntity]. Paths that escape the root throw
     * [IllegalArgumentException] and abort the parse step.
     */
    private suspend fun parseFiles(
        localPath: String,
        repoId: String,
    ): List<FileEntity> = withContext(Dispatchers.IO) {
        val root = File(localPath)
        val canonicalRoot = root.canonicalPath

        val skipDirs = setOf(".git", "build", ".gradle", ".idea", "node_modules")

        // Load existing file hashes for incremental comparison
        val existing = fileDao.getByRepo(repoId).associateBy { it.path }
        val onDisk = mutableSetOf<String>()

        val changed = root.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                // Skip internal/build directories
                skipDirs.none { skip ->
                    file.absolutePath.contains(File.separator + skip + File.separator) ||
                        file.absolutePath.endsWith(File.separator + skip)
                }
            }
            .mapNotNull { file ->
                // ── PATH TRAVERSAL PROTECTION ──────────────────────────────
                val resolved = file.canonicalPath
                require(resolved.startsWith(canonicalRoot)) {
                    "Path traversal rejected: $resolved escapes repo root $canonicalRoot"
                }

                val rel = resolved
                    .removePrefix(canonicalRoot)
                    .trimStart(File.separatorChar)

                onDisk += rel

                // Check coroutine cancellation during long walks
                ensureActive()

                val hash = computeHash(file)
                if (existing[rel]?.contentHash == hash) {
                    null // unchanged — skip
                } else {
                    FileEntity(
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
            }
            .toList()

        // Remove DB rows for files that no longer exist on disk
        val removed = existing.keys - onDisk
        if (removed.isNotEmpty()) {
            Timber.d("Removing %d deleted files from index for repo %s", removed.size, repoId)
            removed.forEach { path -> fileDao.deleteByPath(repoId, path) }
        }

        changed
    }

    /**
     * Computes a SHA-256 hex digest for [file].
     * Reads in 8 KB chunks to avoid large heap allocations for big files.
     */
    private fun computeHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(8192)
            var n: Int
            while (stream.read(buf).also { n = it } != -1) {
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
