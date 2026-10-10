package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devos.ai.core.database.entity.SecurityFindingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [SecurityFindingEntity].
 *
 * All write operations are `suspend fun`; reactive queries return [Flow].
 *
 * DEVOS-047 / DA-58
 */
@Dao
interface SecurityFindingDao {

    /**
     * Emits all findings for [repoId] ordered by severity index then detectedAt
     * (newest first within same severity). Re-emits whenever the table changes.
     */
    @Query(
        """
        SELECT * FROM security_findings
        WHERE repoId = :repoId
        ORDER BY
          CASE severity
            WHEN 'CRITICAL' THEN 0
            WHEN 'HIGH'     THEN 1
            WHEN 'MEDIUM'   THEN 2
            WHEN 'LOW'      THEN 3
            ELSE                 4
          END,
          detectedAt DESC
        """,
    )
    fun observeByRepo(repoId: String): Flow<List<SecurityFindingEntity>>

    /**
     * Inserts or replaces all findings in [entities].
     * REPLACE strategy ensures re-scan always reflects the latest result.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SecurityFindingEntity>)

    /**
     * Updates the [status] field for a single finding.
     * Used by "Mark Fixed" and "Dismiss" actions.
     */
    @Query("UPDATE security_findings SET status = :status WHERE id = :findingId")
    suspend fun updateStatus(findingId: String, status: String)

    /**
     * Deletes all findings for [repoId].
     * Called at the start of a fresh SAST scan so stale findings are removed.
     */
    @Query("DELETE FROM security_findings WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: String)

    /** Returns all findings for [repoId] as a snapshot (non-reactive). */
    @Query("SELECT * FROM security_findings WHERE repoId = :repoId")
    suspend fun getByRepo(repoId: String): List<SecurityFindingEntity>
}
