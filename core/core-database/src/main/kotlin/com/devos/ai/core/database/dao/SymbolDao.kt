package com.devos.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devos.ai.core.database.entity.SymbolEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for [SymbolEntity].
 *
 * Filled in by DEVOS-023 — observation and search queries added here.
 * DEVOS-015 created the table; this ticket adds the full query surface
 * used by the Symbol Details (DEVOS-022) and Code Search (DEVOS-021) screens.
 */
@Dao
interface SymbolDao {

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Bulk-insert symbols extracted from a repository.
     *
     * [OnConflictStrategy.REPLACE] handles incremental re-indexing: re-inserting
     * a symbol that did not change is a no-op at the data level.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(symbols: List<SymbolEntity>)

    /** Removes all symbol records for [repoId] before a full re-index. */
    @Query("DELETE FROM symbols WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: String)

    /** Removes all symbol records for a single file path within [repoId]. */
    @Query("DELETE FROM symbols WHERE repoId = :repoId AND filePath = :filePath")
    suspend fun deleteByFile(repoId: String, filePath: String)

    // ── Single-shot reads ─────────────────────────────────────────────────────

    /** Returns all symbols for [repoId] — used by the incremental indexer. */
    @Query("SELECT * FROM symbols WHERE repoId = :repoId ORDER BY name ASC")
    suspend fun getByRepo(repoId: String): List<SymbolEntity>

    /**
     * Returns all symbols of a given [kind] (e.g. "CLASS", "FUNCTION") in [repoId].
     *
     * Kind strings must match [com.devos.ai.domain.repository.model.SymbolKind] names.
     */
    @Query(
        """
        SELECT * FROM symbols
        WHERE repoId = :repoId AND kind = :kind
        ORDER BY name ASC
        """,
    )
    suspend fun getByKind(repoId: String, kind: String): List<SymbolEntity>

    /** Returns all symbols in a single [filePath] within [repoId]. */
    @Query(
        """
        SELECT * FROM symbols
        WHERE repoId = :repoId AND filePath = :filePath
        ORDER BY lineStart ASC
        """,
    )
    suspend fun getByFilePath(repoId: String, filePath: String): List<SymbolEntity>

    /** Returns the single symbol row for composite [id], or null. */
    @Query("SELECT * FROM symbols WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SymbolEntity?

    // ── Observable reads ──────────────────────────────────────────────────────

    /**
     * Emits all symbols for [repoId] and re-emits on every table change.
     * Backed by Room's `Flow` — no manual invalidation needed.
     */
    @Query("SELECT * FROM symbols WHERE repoId = :repoId ORDER BY name ASC")
    fun observeByRepo(repoId: String): Flow<List<SymbolEntity>>

    /**
     * Live full-text search across symbol [name] and [signature] for [repoId].
     *
     * Uses SQL `LIKE` with leading wildcard so it matches substrings.
     * The query is case-insensitive because SQLite `LIKE` is case-insensitive
     * for ASCII characters by default.
     *
     * @param query  Search term, e.g. "ViewModel" or "send"
     * @param repoId Optional repository scope; pass "%" to match all repos.
     */
    @Query(
        """
        SELECT * FROM symbols
        WHERE repoId LIKE :repoId
          AND (name LIKE '%' || :query || '%'
               OR signature LIKE '%' || :query || '%')
        ORDER BY name ASC
        LIMIT 200
        """,
    )
    fun searchByName(query: String, repoId: String = "%"): Flow<List<SymbolEntity>>

    /**
     * Live filtered search limited to a specific [kind].
     *
     * @param kinds  Comma-separated kind names — use SQL `IN` workaround via
     *               repeated calls or switch to `IN` with a fixed list.
     *               For multi-kind filtering, callers may collect [searchByName]
     *               and filter in-memory, or call this per-kind.
     */
    @Query(
        """
        SELECT * FROM symbols
        WHERE repoId LIKE :repoId
          AND kind = :kind
          AND name LIKE '%' || :query || '%'
        ORDER BY name ASC
        LIMIT 200
        """,
    )
    fun searchByNameAndKind(
        query: String,
        kind: String,
        repoId: String = "%",
    ): Flow<List<SymbolEntity>>
}
