package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devos.ai.core.database.entity.FileEntity

@Dao
interface FileDao {

    /**
     * Inserts or replaces file rows.
     *
     * REPLACE strategy handles incremental re-indexing: changed files are
     * re-inserted with updated hashes; unchanged files are skipped at the
     * worker level before this call.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileEntity>)

    /** Returns all file rows for a repository — used by the indexing worker. */
    @Query("SELECT * FROM repository_files WHERE repoId = :repoId")
    suspend fun getByRepo(repoId: String): List<FileEntity>

    /** Deletes all file rows for a repository (called before a full re-index). */
    @Query("DELETE FROM repository_files WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: String)

    /**
     * Deletes a single file row by relative path.
     * Used during incremental sync to remove rows for files deleted from disk.
     */
    @Query("DELETE FROM repository_files WHERE repoId = :repoId AND path = :path")
    suspend fun deleteByPath(repoId: String, path: String)
}
