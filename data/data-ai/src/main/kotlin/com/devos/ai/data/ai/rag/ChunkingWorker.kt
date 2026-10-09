package com.devos.ai.data.ai.rag

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.data.ai.repository.RAGRepositoryImpl
import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.repository.CodeChunk
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * WorkManager background job that chunks source files and builds the TF-IDF
 * vector index for RAG retrieval (DEVOS-031).
 *
 * ## Pipeline position
 * Enqueued after [com.devos.ai.data.repository.worker.SymbolIndexingWorker]
 * completes, making it the [SyncStep.BUILD_VECTORS] phase of the full
 * repository sync pipeline.
 *
 * ## What it does
 * 1. Loads all [com.devos.ai.core.database.entity.FileEntity] rows for the repo.
 * 2. Filters to source files (Kotlin, Java, and common text-based types).
 * 3. For each file reads its content, calls [CodeChunker.chunk], and collects
 *    all [CodeChunk] domain objects.
 * 4. Calls [RAGRepositoryImpl.indexChunks] which builds the TF-IDF vocabulary,
 *    embeds every chunk, and persists them in Room.
 *
 * A single file error is logged and skipped — it does not abort the whole job.
 */
@HiltWorker
class ChunkingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repositoryDao: RepositoryDao,
    private val fileDao: FileDao,
    private val ragRepository: RAGRepositoryImpl,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_REPO_ID      = "repo_id"
        const val KEY_ERROR        = "error_message"
        const val KEY_FILES_DONE   = "files_done"
        const val KEY_FILES_TOTAL  = "files_total"

        /** Extensions that carry meaningful semantic content for RAG. */
        private val INDEXABLE_EXTENSIONS = setOf(
            "kt", "java", "swift", "py", "ts", "tsx", "js", "jsx",
            "cpp", "c", "cs", "go", "rs", "rb",
            "md", "txt", "gradle", "kts",
        )
    }

    override suspend fun doWork(): Result {
        val repoId = inputData.getString(KEY_REPO_ID) ?: return Result.failure(
            workDataOf(KEY_ERROR to "Missing repo_id"),
        )

        return try {
            Timber.d("ChunkingWorker: starting for repo %s", repoId)

            val entity = withContext(Dispatchers.IO) {
                repositoryDao.observeById(repoId).let {
                    kotlinx.coroutines.flow.first(it)
                }
            }

            val localPath = entity?.localPath ?: run {
                Timber.w("ChunkingWorker: no local path for repo %s — skipping", repoId)
                return Result.success()
            }

            val root      = File(localPath)
            val allFiles  = withContext(Dispatchers.IO) { fileDao.getByRepo(repoId) }
            val srcFiles  = allFiles.filter {
                it.extension.lowercase() in INDEXABLE_EXTENSIONS
            }

            Timber.d("ChunkingWorker: chunking %d files in repo %s", srcFiles.size, repoId)

            // Delete stale chunks before full re-index
            ragRepository.deleteChunks(repoId)

            val allChunks = mutableListOf<CodeChunk>()
            var done      = 0
            val total     = srcFiles.size

            for (fileEntity in srcFiles) {
                ensureActive()
                try {
                    val file = File(root, fileEntity.path)
                    if (!file.exists()) continue

                    val source = withContext(Dispatchers.IO) { file.readText() }
                    val chunks = CodeChunker.chunk(repoId, fileEntity.path, source)
                    allChunks.addAll(chunks.map { entity ->
                        CodeChunk(
                            id        = entity.id,
                            repoId    = entity.repoId,
                            filePath  = entity.filePath,
                            lineStart = entity.lineStart,
                            lineEnd   = entity.lineEnd,
                            content   = entity.content,
                            language  = entity.language,
                        )
                    })
                } catch (e: Exception) {
                    Timber.w(e, "ChunkingWorker: skipping %s due to error", fileEntity.path)
                }

                done++
                if (done % 20 == 0 || done == total) {
                    setProgress(workDataOf(
                        KEY_FILES_DONE  to done,
                        KEY_FILES_TOTAL to total,
                    ))
                }
            }

            // Build TF-IDF index for all collected chunks in one pass
            if (allChunks.isNotEmpty()) {
                ragRepository.indexChunks(repoId, allChunks)
            }

            Timber.d("ChunkingWorker: done — %d chunks from %d files in repo %s",
                allChunks.size, done, repoId)

            Result.success()
        } catch (e: CancellationException) {
            Timber.d("ChunkingWorker: cancelled for repo %s", repoId)
            Result.failure()
        } catch (e: Exception) {
            Timber.e(e, "ChunkingWorker: fatal error for repo %s", repoId)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Chunking error")))
        }
    }
}
