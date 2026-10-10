package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devos.ai.core.database.entity.FileCoverageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [FileCoverageEntity].
 *
 * DEVOS-049 / DA-60
 */
@Dao
interface FileCoverageDao {

    /**
     * Emits all file coverage rows for [repoId] ordered by line coverage ascending
     * (lowest coverage — highest impact — first). Re-emits on table changes.
     */
    @Query(
        "SELECT * FROM file_coverage WHERE repoId = :repoId ORDER BY lineCoverage ASC",
    )
    fun getByRepo(repoId: String): Flow<List<FileCoverageEntity>>

    /**
     * Inserts or replaces all coverage rows.
     * Used after a fresh JaCoCo parse run.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FileCoverageEntity>)

    /**
     * Deletes all coverage rows for [repoId].
     * Called before inserting a new parse result.
     */
    @Query("DELETE FROM file_coverage WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: String)

    /** Returns a snapshot of all rows for [repoId] (non-reactive). */
    @Query("SELECT * FROM file_coverage WHERE repoId = :repoId")
    suspend fun snapshotByRepo(repoId: String): List<FileCoverageEntity>
}
