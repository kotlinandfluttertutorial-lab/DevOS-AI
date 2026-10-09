package com.devos.ai.domain.repository

import com.devos.ai.domain.repository.model.CodeSymbol
import com.devos.ai.domain.repository.model.SymbolKind
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for the symbol index.
 *
 * Implemented by `data-repository` — feature modules must never import the
 * implementation directly; inject this interface via Hilt.
 *
 * All methods are pure Kotlin — zero Android imports.
 */
interface SymbolRepository {

    /**
     * Emits all symbols for [repoId] and re-emits on any table change.
     * Used by the Symbol Details screen (DEVOS-022).
     */
    fun observeSymbols(repoId: String): Flow<List<CodeSymbol>>

    /**
     * Emits symbols whose [CodeSymbol.name] contains [query] (case-insensitive).
     *
     * Used by Code Search (DEVOS-021) and symbol picker in AI Chat (DEVOS-027).
     *
     * @param repoId Scope the search to a single repository; null = all repos.
     * @param kinds  Optional filter; empty set = all kinds.
     */
    fun searchSymbols(
        query: String,
        repoId: String? = null,
        kinds: Set<SymbolKind> = emptySet(),
    ): Flow<List<CodeSymbol>>

    /**
     * Returns all symbols defined in [filePath] within [repoId].
     * Used by the Code Viewer AI action bar (DEVOS-020).
     */
    suspend fun getSymbolsForFile(repoId: String, filePath: String): List<CodeSymbol>

    /**
     * Returns a single symbol by its composite [id], or null if not found.
     */
    suspend fun getSymbol(id: String): CodeSymbol?

    /**
     * Removes all symbols for [repoId].
     * Called before a full re-index.
     */
    suspend fun deleteByRepo(repoId: String)
}
