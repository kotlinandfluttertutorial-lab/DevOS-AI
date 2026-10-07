package com.devos.ai.data.ai.repository

import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.repository.CodeChunk
import com.devos.ai.domain.ai.repository.RAGRepository
import javax.inject.Inject

/**
 * Stub implementation of [RAGRepository].
 *
 * Full implementation will be provided in DEVOS-027 (RAG/indexing feature).
 * This stub allows the app to compile and Hilt graph to resolve.
 */
class RAGRepositoryImpl @Inject constructor() : RAGRepository {

    override suspend fun retrieve(
        query: String,
        context: AIContext,
        topK: Int,
        minRelevance: Float,
    ): List<CodeChunk> = emptyList()

    override suspend fun indexChunks(repoId: String, chunks: List<CodeChunk>) { /* stub */ }

    override suspend fun deleteChunks(repoId: String) { /* stub */ }

    override suspend fun updateChunks(repoId: String, changedFilePaths: List<String>) { /* stub */ }
}
