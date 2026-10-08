package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devos.ai.core.database.entity.RepositoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for [RepositoryEntity].
 *
 * All observable queries return [Flow] so Room emits a fresh list whenever
 * the underlying table changes — no manual invalidation needed.
 */
@Dao
interface RepositoryDao {

    /** Emits the full list of repositories; re-emits on every table change. */
    @Query("SELECT * FROM repositories ORDER BY name ASC")
    fun observeAll(): Flow<List<RepositoryEntity>>

    /** Emits the repository with [id], or null if deleted. */
    @Query("SELECT * FROM repositories WHERE id = :id")
    fun observeById(id: String): Flow<RepositoryEntity?>

    /**
     * Insert-or-update a repository row.
     * Uses Room's [@Upsert] (insert + replace on conflict) to handle both
     * new imports and refreshes.
     */
    @Upsert
    suspend fun upsert(entity: RepositoryEntity)

    /** Updates only the syncStatus column — avoids a full-row write during worker progress. */
    @Query("UPDATE repositories SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    /** Stores the absolute path to the local clone directory once cloning is complete. */
    @Query("UPDATE repositories SET localPath = :path WHERE id = :id")
    suspend fun updateLocalPath(id: String, path: String)

    /** Hard-deletes the repository row; cascades to repository_files and symbols. */
    @Query("DELETE FROM repositories WHERE id = :id")
    suspend fun delete(id: String)
}
