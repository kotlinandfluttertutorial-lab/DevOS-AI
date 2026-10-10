package com.devos.ai.domain.ai.repository

import com.devos.ai.domain.ai.model.MemoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Developer Memory entries.
 *
 * Implemented in :data:data-ai via [MemoryRepositoryImpl].
 * Bound via Hilt in :data:data-ai's MemoryModule.
 *
 * DEVOS-056 / DA-67
 */
interface MemoryRepository {

    /**
     * Emits all memory entries for [userId] ordered by pinned first, then newest.
     * Re-emits on every database change.
     */
    fun observeEntries(userId: String): Flow<List<MemoryEntry>>

    /**
     * Persists a new [entry] (insert or replace).
     *
     * @return [Result.success] on success, [Result.failure] on DB error.
     */
    suspend fun saveEntry(entry: MemoryEntry): Result<Unit>

    /**
     * Deletes the entry identified by [entryId].
     *
     * @return [Result.success] on success, [Result.failure] if not found or DB error.
     */
    suspend fun deleteEntry(entryId: String): Result<Unit>

    /**
     * Deletes all entries belonging to [userId].
     *
     * @return [Result.success] on success, [Result.failure] on DB error.
     */
    suspend fun clearAll(userId: String): Result<Unit>

    /**
     * Returns entries for [userId] whose [MemoryEntry.content] contains any keyword
     * from [query] (case-insensitive, space-split).
     *
     * This is a lightweight local keyword match — not semantic search.
     * Suitable for injecting relevant memory into AI context.
     */
    fun getRelevantMemory(userId: String, query: String): Flow<List<MemoryEntry>>
}
