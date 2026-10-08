package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devos.ai.core.database.entity.SymbolEntity

/**
 * DAO for code symbols.
 *
 * Extraction logic is implemented in DEVOS-023; only the storage contract
 * is defined here.
 */
@Dao
interface SymbolDao {

    /**
     * Inserts or replaces symbol rows.
     *
     * REPLACE handles re-indexing after a file changes.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(symbols: List<SymbolEntity>)

    /**
     * Returns all symbols for a repository.
     * DEVOS-022 / DEVOS-023 add filtering by kind, name, and file path.
     */
    @Query("SELECT * FROM symbols WHERE repoId = :repoId")
    suspend fun getByRepo(repoId: String): List<SymbolEntity>

    /** Deletes all symbols for a repository (called before a full re-index). */
    @Query("DELETE FROM symbols WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: String)
}
