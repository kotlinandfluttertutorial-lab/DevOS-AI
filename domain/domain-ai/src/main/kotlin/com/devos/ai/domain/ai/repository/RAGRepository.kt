package com.devos.ai.domain.ai.repository

import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.model.CodeChunk

/**
 * Repository interface for RAG (Retrieval-Augmented Generation) operations.
 *
 * Full spec: .kiro/specs/ai-platform/core.md
 * Evaluation strategy: docs/testing/ai-evaluation-strategy.md
 */
interface RAGRepository {

    /**
     * Retrieve the top-[topK] most relevant code chunks for [query] within [context].
     * Results are sorted by descending relevance score.
     */
    suspend fun retrieve(
        query: String,
        context: AIContext,
        topK: Int = 10,
        minRelevance: Float = 0.3f,
    ): List<CodeChunk>

    /** Index (or re-index) [chunks] for a repository. */
    suspend fun indexChunks(repoId: String, chunks: List<CodeChunk>)

    /** Remove all chunks for a repository (called before full re-index). */
    suspend fun deleteChunks(repoId: String)

    /** Incrementally update chunks for specific file paths. */
    suspend fun updateChunks(repoId: String, changedFilePaths: List<String>)
}

data class CodeChunk(
    val id: String,
    val repoId: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val content: String,
    val language: String,
    val relevance: Float = 0f,   // populated on retrieval
)
