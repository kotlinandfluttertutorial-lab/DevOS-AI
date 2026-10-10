package com.devos.ai.data.ai.repository

import com.devos.ai.core.database.dao.ChunkDao
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.entity.CodeChunkEntity
import com.devos.ai.data.ai.rag.CodeChunker
import com.devos.ai.data.ai.rag.Tokenizer
import com.devos.ai.data.ai.rag.VectorStore
import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.repository.CodeChunk
import com.devos.ai.domain.ai.repository.RAGRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full implementation of [RAGRepository] using TF-IDF + cosine similarity.
 *
 * ## Architecture
 * ```
 * File on disk
 *   └─ CodeChunker.chunk()        → List<CodeChunkEntity> (no embeddings)
 *      └─ embed()                 → TF-IDF vectors stored in embeddingJson
 *         └─ ChunkDao.insertAll() → persisted in Room (code_chunks table)
 *
 * retrieve(query, context, topK)
 *   └─ ChunkDao.getEmbeddedByRepo() → load candidates
 *      └─ VectorStore.search()      → cosine-ranked results
 *         └─ List<CodeChunk>        → returned to AI layer
 * ```
 *
 * ## Vocabulary
 * TF-IDF requires a shared vocabulary built from the corpus. For simplicity
 * this implementation builds the vocabulary from the chunk tokens at index time
 * and stores it alongside the embeddings in memory. The vocabulary is rebuilt on
 * every full re-index and persisted as a JSON string in a single special
 * [CodeChunkEntity] row with id="vocab:$repoId".
 *
 * This keeps all RAG state inside Room with no external files.
 */
@Singleton
class RAGRepositoryImpl @Inject constructor(
    private val chunkDao: ChunkDao,
    private val fileDao: FileDao,
) : RAGRepository {

    companion object {
        private const val VOCAB_CHUNK_ID_PREFIX = "vocab:"
        private const val VOCAB_SIZE = 8_000
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    override suspend fun retrieve(
        query: String,
        context: AIContext,
        topK: Int,
        minRelevance: Float,
    ): List<CodeChunk> = withContext(Dispatchers.Default) {
        val repoIds = resolveRepoIds(context)
        if (repoIds.isEmpty()) return@withContext emptyList()

        repoIds.flatMap { repoId ->
            retrieveFromRepo(query, repoId, topK, minRelevance)
        }
            .sortedByDescending { it.relevance }
            .take(topK)
    }

    private suspend fun retrieveFromRepo(
        query: String,
        repoId: String,
        topK: Int,
        minRelevance: Float,
    ): List<CodeChunk> {
        val chunks = chunkDao.getEmbeddedByRepo(repoId)
        if (chunks.isEmpty()) return emptyList()

        // Load vocabulary stored in the special vocab row
        val vocab = loadVocabulary(repoId) ?: return emptyList()

        // Tokenise and embed the query using the same vocabulary
        val qTokens = Tokenizer.tokenize(query)
        val df      = buildDfFromVocab(vocab)
        val n       = chunks.size
        val qPairs  = Tokenizer.tfidf(qTokens, vocab, df, n)
        if (qPairs.isEmpty()) return emptyList()

        val queryVec = FloatArray(VOCAB_SIZE)
        for ((idx, w) in qPairs) queryVec[idx] = w

        val store   = VectorStore.from(chunks, VOCAB_SIZE)
        val results = store.search(queryVec, topK, minRelevance)

        return results.map { (entity, score) ->
            entity.toDomain(score)
        }
    }

    // ── Indexing ──────────────────────────────────────────────────────────────

    override suspend fun indexChunks(
        repoId: String,
        chunks: List<CodeChunk>,
    ): Unit = withContext(Dispatchers.Default) {
        Timber.d("RAG: indexing %d chunks for repo %s", chunks.size, repoId)

        // Convert domain chunks → entity stubs (no embeddings yet)
        val entities = chunks.map { it.toEntity() }

        // Build TF-IDF vocabulary from the whole corpus
        val corpus = entities.map { Tokenizer.tokenize(it.content) }
        val vocab  = Tokenizer.buildVocabulary(corpus, VOCAB_SIZE)
        val df     = buildDf(corpus)
        val n      = corpus.size

        // Embed each chunk and mark indexedAt
        val now = System.currentTimeMillis()
        val embedded = entities.mapIndexed { i, entity ->
            val tfidfPairs = Tokenizer.tfidf(corpus[i], vocab, df, n)
            entity.copy(
                embeddingJson = VectorStore.toJson(tfidfPairs),
                indexedAt     = now,
            )
        }

        // Persist — vocabulary row first so retrieval finds it
        val vocabRow = buildVocabRow(repoId, vocab)

        withContext(Dispatchers.IO) {
            chunkDao.insertAll(embedded + vocabRow)
        }

        Timber.d("RAG: indexed %d chunks (vocab size %d) for repo %s",
            embedded.size, vocab.size, repoId)
    }

    override suspend fun deleteChunks(repoId: String): Unit = withContext(Dispatchers.IO) {
        chunkDao.deleteByRepo(repoId)
        Timber.d("RAG: deleted all chunks for repo %s", repoId)
    }

    override suspend fun updateChunks(
        repoId: String,
        changedFilePaths: List<String>,
    ): Unit = withContext(Dispatchers.Default) {
        if (changedFilePaths.isEmpty()) return@withContext
        Timber.d("RAG: incremental update for %d files in repo %s",
            changedFilePaths.size, repoId)

        // Load the repo's local path to read file content
        val localPath = fileDao.getByRepo(repoId)
            .firstOrNull()
            ?.let { File(it.path).parent }
            ?: return@withContext

        val newChunks = mutableListOf<CodeChunk>()
        for (filePath in changedFilePaths) {
            withContext(Dispatchers.IO) {
                chunkDao.deleteByFile(repoId, filePath)
            }
            val file = File(localPath, filePath)
            if (!file.exists()) continue   // deleted file — chunks already removed

            val source = withContext(Dispatchers.IO) { file.readText() }
            val entities = CodeChunker.chunk(repoId, filePath, source)
            newChunks.addAll(entities.map { it.toDomain() })
        }

        if (newChunks.isNotEmpty()) {
            indexChunks(repoId, newChunks)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves which repository IDs to search based on [AIContext].
     * Global context searches all indexed repos (returned as a special marker
     * handled by the caller).
     */
    private fun resolveRepoIds(context: AIContext): List<String> = when (context) {
        is AIContext.Repository -> listOf(context.repoId)
        is AIContext.File       -> listOf(context.repoId)
        is AIContext.Symbol     -> listOf(context.repoId)
        is AIContext.Project    -> listOf(context.projectId)  // projectId used as repoId scope
        is AIContext.Global     -> emptyList() // not supported in MVP — caller handles
    }

    /** Builds document-frequency map from a tokenised corpus. */
    private fun buildDf(corpus: List<List<String>>): Map<String, Int> {
        val df = mutableMapOf<String, Int>()
        for (doc in corpus) {
            for (term in doc.toSet()) df[term] = (df[term] ?: 0) + 1
        }
        return df
    }

    /**
     * Reconstructs a minimal DF map from the vocabulary (all terms have DF=1
     * for the purpose of query embedding against the stored vocabulary).
     */
    private fun buildDfFromVocab(vocab: Map<String, Int>): Map<String, Int> =
        vocab.keys.associateWith { 1 }

    /**
     * Stores the vocabulary as a special [CodeChunkEntity] row so it survives
     * app restarts without requiring a separate table.
     *
     * The vocabulary is encoded in [CodeChunkEntity.embeddingJson] as a
     * JSON object `{"term": index, ...}`.
     */
    private fun buildVocabRow(repoId: String, vocab: Map<String, Int>): CodeChunkEntity {
        val json = org.json.JSONObject(vocab as Map<*, *>).toString()
        return CodeChunkEntity(
            id            = "$VOCAB_CHUNK_ID_PREFIX$repoId",
            repoId        = repoId,
            filePath      = "__vocab__",
            lineStart     = 0,
            lineEnd       = 0,
            content       = "",
            language      = "json",
            embeddingJson = json,
            indexedAt     = System.currentTimeMillis(),
        )
    }

    private suspend fun loadVocabulary(repoId: String): Map<String, Int>? {
        val row = withContext(Dispatchers.IO) {
            chunkDao.getById("$VOCAB_CHUNK_ID_PREFIX$repoId")
        } ?: return null

        return try {
            val obj   = org.json.JSONObject(row.embeddingJson ?: return null)
            val vocab = mutableMapOf<String, Int>()
            for (key in obj.keys()) vocab[key] = obj.getInt(key)
            vocab
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse vocabulary for repo %s", repoId)
            null
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun CodeChunkEntity.toDomain(relevance: Float = 0f): CodeChunk = CodeChunk(
        id        = id,
        repoId    = repoId,
        filePath  = filePath,
        lineStart = lineStart,
        lineEnd   = lineEnd,
        content   = content,
        language  = language,
        relevance = relevance,
    )

    private fun CodeChunkEntity.toDomain(): CodeChunk = toDomain(0f)

    private fun CodeChunk.toEntity(): CodeChunkEntity = CodeChunkEntity(
        id            = id,
        repoId        = repoId,
        filePath      = filePath,
        lineStart     = lineStart,
        lineEnd       = lineEnd,
        content       = content,
        language      = language,
        embeddingJson = null,
        indexedAt     = 0L,
    )
}
