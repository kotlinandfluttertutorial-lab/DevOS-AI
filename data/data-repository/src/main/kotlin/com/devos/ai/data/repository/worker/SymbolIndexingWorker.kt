package com.devos.ai.data.repository.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.dao.SymbolDao
import com.devos.ai.data.repository.extractor.SymbolExtractor
import com.devos.ai.domain.repository.model.SyncStatus
import com.devos.ai.domain.repository.model.SyncStep
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import timber.log.Timber
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * WorkManager worker that extracts code symbols from an already-cloned repository
 * and stores them in the Room `symbols` table.
 *
 * This worker is enqueued by [RepositoryIndexingWorker] as the
 * [SyncStep.INDEX_SYMBOLS] phase after file indexing completes.
 *
 * ## Algorithm
 * 1. Load all [com.devos.ai.core.database.entity.FileEntity] rows for the repo.
 * 2. Filter to `.kt` and `.java` files.
 * 3. For each file that exists on disk, read its content and pass it to
 *    [SymbolExtractor.extract].
 * 4. Bulk-insert the resulting [com.devos.ai.core.database.entity.SymbolEntity]
 *    rows (REPLACE strategy handles re-indexing).
 * 5. Delete stale symbol rows for files that are no longer in the file index.
 *
 * ## Cancellation
 * The coroutine checks [ensureActive] after every file so the worker can be
 * cancelled promptly without processing hundreds of queued files.
 *
 * ## Error handling
 * - A single file parse error is logged and skipped; it does NOT abort the job.
 * - A fatal error sets the sync status to ERROR and returns [Result.failure].
 */
@HiltWorker
class SymbolIndexingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repositoryDao: RepositoryDao,
    private val fileDao: FileDao,
    private val symbolDao: SymbolDao,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_REPO_ID = "repo_id"
        const val KEY_STEP    = "current_step"
        const val KEY_ERROR   = "error_message"
        const val KEY_PROGRESS_FILES   = "files_processed"
        const val KEY_PROGRESS_TOTAL   = "total_files"
    }

    /** Source file extensions handled by [SymbolExtractor]. */
    private val SUPPORTED_EXTENSIONS = setOf("kt", "java")

    override suspend fun doWork(): Result {
        val repoId = inputData.getString(KEY_REPO_ID) ?: return Result.failure(
            workDataOf(KEY_ERROR to "Missing repo_id input"),
        )

        return try {
            setProgress(workDataOf(KEY_STEP to SyncStep.INDEX_SYMBOLS.name))
            Timber.d("SymbolIndexingWorker starting for repo %s", repoId)

            val entity = withContext(Dispatchers.IO) {
                repositoryDao.observeById(repoId).first()
            }

            val localPath = entity?.localPath
            if (localPath == null) {
                Timber.w("No local path for repo %s — skipping symbol indexing", repoId)
                return Result.success()
            }

            // Load all file rows for this repo; filter to supported languages
            val allFiles = withContext(Dispatchers.IO) {
                fileDao.getByRepo(repoId)
            }
            val sourceFiles = allFiles.filter {
                it.extension.lowercase() in SUPPORTED_EXTENSIONS
            }

            Timber.d("Extracting symbols from %d source files in repo %s",
                sourceFiles.size, repoId)

            val root  = File(localPath)
            var done  = 0
            val total = sourceFiles.size

            // Delete existing symbols for this repo before re-indexing
            // (handles removed/renamed files automatically)
            withContext(Dispatchers.IO) { symbolDao.deleteByRepo(repoId) }

            for (fileEntity in sourceFiles) {
                coroutineContext.ensureActive()  // Honour cancellation between files

                try {
                    val file = File(root, fileEntity.path)
                    if (!file.exists() || !file.isFile) continue

                    val source  = withContext(Dispatchers.IO) { file.readText() }
                    val symbols = SymbolExtractor.extract(repoId, fileEntity.path, source)

                    if (symbols.isNotEmpty()) {
                        withContext(Dispatchers.IO) { symbolDao.insertAll(symbols) }
                        Timber.v("  %s → %d symbols", fileEntity.path, symbols.size)
                    }
                } catch (e: Exception) {
                    // Log and skip — one bad file should not abort the entire job
                    Timber.w(e, "Symbol extraction failed for %s", fileEntity.path)
                }

                done++
                // Report progress every 10 files to avoid too many WorkManager DB writes
                if (done % 10 == 0 || done == total) {
                    setProgress(workDataOf(
                        KEY_STEP           to SyncStep.INDEX_SYMBOLS.name,
                        KEY_PROGRESS_FILES to done,
                        KEY_PROGRESS_TOTAL to total,
                    ))
                }
            }

            Timber.d("Symbol indexing complete for repo %s (%d files processed)", repoId, done)
            Result.success()
        } catch (e: CancellationException) {
            Timber.d("Symbol indexing cancelled for repo %s", repoId)
            // Do NOT change sync status here — the parent worker manages it
            Result.failure()
        } catch (e: Exception) {
            Timber.e(e, "Symbol indexing failed fatally for repo %s", repoId)
            repositoryDao.updateSyncStatus(repoId, SyncStatus.ERROR.name)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Symbol indexing error")))
        }
    }
}
