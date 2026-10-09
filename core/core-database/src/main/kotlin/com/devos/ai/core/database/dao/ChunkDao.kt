package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devos.ai.core.database.entity.CodeChunkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [CodeChunkEntity].
 *
 * Retrieval for RAG is done in-memory after loading all chunks for a repo
 * (cosine similarity over TF-IDF vectors stored as JSON). This keeps the
 * query surface minimal and avoids adding SQLite FTS or custom SQLite functions.
 */
@Dao
interface ChunkDao {

    // ── Writes ────────────────────────────────────────────────────────────────

    /** Bulk-insert or replace chunks. REPLACE handles re-indexing. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<CodeChunkEntity>)

    /** Delete all chunks for [repoId] — called before full re-index. */
    @Query("DELETE FROM code_chunks WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: String)

    /** Delete all chunks for a single file — used on incremental updates. */
    @Query("DELETE FROM code_chunks WHERE repoId = :repoId AND filePath = :filePath")
    suspend fun deleteByFile(repoId: String, filePath: String)

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Returns all chunks for [repoId] with their embeddings.
     * Loaded into memory by [VectorStore] for cosine-similarity ranking.
     */
    @Query("SELECT * FROM code_chunks WHERE repoId = :repoId AND embeddingJson IS NOT NULL")
    suspend fun getEmbeddedByRepo(repoId: String): List<CodeChunkEntity>

    /** Returns all chunks for a file (used to skip unchanged files). */
    @Query("SELECT * FROM code_chunks WHERE repoId = :repoId AND filePath = :filePath")
    suspend fun getByFile(repoId: String, filePath: String): List<CodeChunkEntity>

    /** Returns a single chunk by composite [id]. */
    @Query("SELECT * FROM code_chunks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CodeChunkEntity?

    /** Observable count — useful for the sync progress UI. */
    @Query("SELECT COUNT(*) FROM code_chunks WHERE repoId = :repoId")
    fun observeCount(repoId: String): Flow<Int>
}
