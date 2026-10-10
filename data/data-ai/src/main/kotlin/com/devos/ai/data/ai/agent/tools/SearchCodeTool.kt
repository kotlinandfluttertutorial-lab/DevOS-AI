package com.devos.ai.data.ai.agent.tools

import com.devos.ai.core.database.dao.ChunkDao
import com.devos.ai.data.ai.agent.AgentToolExecutor
import com.devos.ai.data.ai.rag.Tokenizer
import com.devos.ai.data.ai.rag.VectorStore
import com.devos.ai.domain.ai.model.AgentTool
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

/**
 * Agent tool: semantic code search via the RAG vector index.
 *
 * Argument schema: `{ "query": "<natural language query>", "top_k": <int>? }`
 */
class SearchCodeTool @Inject constructor(
    private val chunkDao: ChunkDao,
) : AgentToolExecutor {

    companion object { private const val VOCAB_SIZE = 8_000 }

    override val tool = AgentTool(
        name             = "search_code",
        description      = "Semantically search the codebase for relevant code snippets. " +
            "Use natural language to describe what you're looking for. " +
            "Returns the most relevant code chunks with file paths and line ranges.",
        parametersSchema = """{"type":"object","properties":{"query":{"type":"string"},"top_k":{"type":"integer"}},"required":["query"]}""",
    )

    override suspend fun execute(argsJson: String, repoId: String?): String {
        if (repoId == null) return "Error: no repository context."

        return try {
            val args  = JSONObject(argsJson)
            val query = args.getString("query")
            val topK  = args.optInt("top_k", 5).coerceIn(1, 20)

            val chunks = chunkDao.getEmbeddedByRepo(repoId)
            if (chunks.isEmpty()) return "Code index is empty. The repository may still be indexing."

            // Load vocabulary
            val vocabRow = chunkDao.getById("vocab:$repoId")
                ?: return "Code index vocabulary not found. Try again after indexing completes."

            val vocab = runCatching {
                val obj = org.json.JSONObject(vocabRow.embeddingJson ?: return "Empty vocabulary.")
                buildMap { for (k in obj.keys()) this[k] = obj.getInt(k) }
            }.getOrElse { return "Failed to load vocabulary: ${it.message}" }

            val qTokens = Tokenizer.tokenize(query)
            val df      = vocab.keys.associateWith { 1 }
            val qPairs  = Tokenizer.tfidf(qTokens, vocab, df, chunks.size)
            if (qPairs.isEmpty()) return "No meaningful tokens found in query '$query'."

            val queryVec = FloatArray(VOCAB_SIZE)
            for ((idx, w) in qPairs) if (idx < VOCAB_SIZE) queryVec[idx] = w

            val store   = VectorStore.from(chunks, VOCAB_SIZE)
            val results = store.search(queryVec, topK, minRelevance = 0.1f)
            if (results.isEmpty()) return "No relevant code found for '$query'."

            buildString {
                appendLine("Found ${results.size} relevant code chunk(s):")
                results.forEach { (chunk, score) ->
                    appendLine("\n📄 ${chunk.filePath} (lines ${chunk.lineStart}–${chunk.lineEnd}, score=${"%.2f".format(score)})")
                    appendLine("```${chunk.language}")
                    appendLine(chunk.content.take(600))
                    appendLine("```")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "search_code failed")
            "Error searching code: ${e.message}"
        }
    }
}
