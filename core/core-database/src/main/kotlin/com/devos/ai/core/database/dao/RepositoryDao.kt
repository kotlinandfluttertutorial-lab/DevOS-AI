package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devos.ai.core.database.entity.RepositoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepositoryDao {

    /** Emits all repositories, ordered by name, whenever any row changes. */
    @Query("SELECT * FROM repositories ORDER BY name ASC")
    fun observeAll(): Flow<List<RepositoryEntity>>

    /** Emits a single repository by [id], or null if deleted. */
    @Query("SELECT * FROM repositories WHERE id = :id")
    fun observeById(id: String): Flow<RepositoryEntity?>

    /** Returns a snapshot of a repository — useful for one-shot reads in workers. */
    @Query("SELECT * FROM repositories WHERE id = :id")
    suspend fun getById(id: String): RepositoryEntity?

    /** Insert or update a repository row. */
    @Upsert
    suspend fun upsert(entity: RepositoryEntity)

    /** Atomically updates the sync status without touching other columns. */
    @Query("UPDATE repositories SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    /**
     * Stores the local clone path once the CLONE step completes.
     * Also records the sync timestamp.
     */
    @Query("UPDATE repositories SET localPath = :path WHERE id = :id")
    suspend fun updateLocalPath(id: String, path: String)

    /**
     * Updates lastSyncAt to [epochMillis] after a successful full sync.
     */
    @Query("UPDATE repositories SET lastSyncAt = :epochMillis WHERE id = :id")
    suspend fun updateLastSyncAt(id: String, epochMillis: Long)

    /** Deletes the repository row; cascades to repository_files and symbols. */
    @Query("DELETE FROM repositories WHERE id = :id")
    suspend fun delete(id: String)
}
