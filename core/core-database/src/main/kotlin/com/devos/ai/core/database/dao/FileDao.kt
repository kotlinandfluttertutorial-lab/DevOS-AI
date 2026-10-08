package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devos.ai.core.database.entity.FileEntity

/**
 * Data-access object for [FileEntity].
 *
 * Designed for the incremental indexing pattern used by
 * [com.devos.ai.data.repository.worker.RepositoryIndexingWorker]:
 * - Only changed/new files are inserted (via [insertAll] with REPLACE).
 * - Files deleted from disk are removed individually via [deleteByPath].
 * - Full wipe on re-clone via [deleteByRepo].
 */
@Dao
interface FileDao {

    /**
     * Bulk insert with REPLACE on conflict.
     *
     * The composite primary key "$repoId:$path" means re-inserting an
     * unchanged file is a no-op at the storage level (same data replaced).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileEntity>)

    /** Returns all file records for [repoId] — used by the incremental hash check. */
    @Query("SELECT * FROM repository_files WHERE repoId = :repoId")
    suspend fun getByRepo(repoId: String): List<FileEntity>

    /** Removes the record for a single file that was deleted from disk. */
    @Query("DELETE FROM repository_files WHERE repoId = :repoId AND path = :path")
    suspend fun deleteByPath(repoId: String, path: String)

    /** Removes all file records for a repository (e.g. before a full re-index). */
    @Query("DELETE FROM repository_files WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: String)
}
