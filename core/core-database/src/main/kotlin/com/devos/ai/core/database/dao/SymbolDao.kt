package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devos.ai.core.database.entity.SymbolEntity

/**
 * Data-access object for [SymbolEntity].
 *
 * Extraction logic is **intentionally stubbed** in DEVOS-015.
 * DEVOS-023 (Symbol indexing service) will call [insertAll] with real data
 * once the AST parser is wired up.  The table and DAO are defined here so
 * the schema is ready when DEVOS-023 lands.
 */
@Dao
interface SymbolDao {

    /**
     * Bulk insert symbols extracted by the AST parser.
     *
     * REPLACE on conflict handles re-indexing without manual deletes.
     *
     * **Stub in DEVOS-015 — will be called from DEVOS-023.**
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(symbols: List<SymbolEntity>)

    /** Returns all symbols for [repoId] (used by code search and symbol details). */
    @Query("SELECT * FROM symbols WHERE repoId = :repoId")
    suspend fun getByRepo(repoId: String): List<SymbolEntity>

    /** Removes all symbol records for a repository before a full re-index. */
    @Query("DELETE FROM symbols WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: String)
}
