package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devos.ai.core.database.entity.MemoryEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [MemoryEntryEntity].
 *
 * All writes are `suspend fun`; reactive queries return [Flow].
 *
 * DEVOS-056 / DA-67
 */
@Dao
interface MemoryEntryDao {

    /**
     * Emits all memory entries for [userId], optionally scoped to [projectId].
     * When [projectId] is null, returns GLOBAL entries only.
     * Re-emits whenever the table changes.
     */
    @Query(
        """
        SELECT * FROM memory_entries
        WHERE userId = :userId
          AND (projectId = :projectId OR projectId IS NULL)
        ORDER BY isPinned DESC, lastAccessedAt DESC
        """,
    )
    fun observeByUser(userId: String, projectId: String? = null): Flow<List<MemoryEntryEntity>>

    /** Returns all entries for a user regardless of project scope (non-reactive). */
    @Query(
        "SELECT * FROM memory_entries WHERE userId = :userId ORDER BY lastAccessedAt DESC",
    )
    fun observeAllByUser(userId: String): Flow<List<MemoryEntryEntity>>

    /** Inserts or replaces a memory entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MemoryEntryEntity)

    /** Updates the content of a single entry. */
    @Query("UPDATE memory_entries SET content = :content WHERE id = :entryId")
    suspend fun updateContent(entryId: String, content: String)

    /** Toggles the pinned state of an entry. */
    @Query("UPDATE memory_entries SET isPinned = :pinned WHERE id = :entryId")
    suspend fun updatePinned(entryId: String, pinned: Boolean)

    /** Updates the lastAccessedAt timestamp. */
    @Query(
        "UPDATE memory_entries SET lastAccessedAt = :accessedAt WHERE id = :entryId",
    )
    suspend fun updateLastAccessed(entryId: String, accessedAt: Long)

    /** Deletes a single entry by [entryId]. */
    @Query("DELETE FROM memory_entries WHERE id = :entryId")
    suspend fun delete(entryId: String)

    /**
     * Deletes all entries for [userId] scoped to [projectId].
     * When [projectId] is null, deletes only GLOBAL entries for the user.
     */
    @Query(
        """
        DELETE FROM memory_entries
        WHERE userId = :userId
          AND (projectId = :projectId OR (:projectId IS NULL AND projectId IS NULL))
        """,
    )
    suspend fun deleteAll(userId: String, projectId: String?)

    /**
     * Deletes expired, non-pinned entries older than [nowMillis].
     * Called by the daily MemoryCleanupWorker.
     */
    @Query(
        """
        DELETE FROM memory_entries
        WHERE isPinned = 0
          AND expiresAt IS NOT NULL
          AND expiresAt < :nowMillis
        """,
    )
    suspend fun deleteExpired(nowMillis: Long)
}
