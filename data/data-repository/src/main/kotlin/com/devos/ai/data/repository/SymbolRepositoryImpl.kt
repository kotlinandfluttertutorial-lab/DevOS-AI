package com.devos.ai.data.repository

import com.devos.ai.core.database.dao.SymbolDao
import com.devos.ai.core.database.entity.SymbolEntity
import com.devos.ai.domain.repository.SymbolRepository
import com.devos.ai.domain.repository.model.CodeSymbol
import com.devos.ai.domain.repository.model.SymbolKind
import com.devos.ai.domain.repository.model.SymbolVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [SymbolRepository].
 *
 * Owns the mapping between [SymbolEntity] (Room) ↔ [CodeSymbol] (domain).
 *
 * Feature modules must **never** import this class directly — they inject
 * [SymbolRepository] via Hilt, which resolves to this implementation through
 * [com.devos.ai.data.repository.di.SymbolModule].
 */
@Singleton
class SymbolRepositoryImpl @Inject constructor(
    private val symbolDao: SymbolDao,
) : SymbolRepository {

    // ── Observe ───────────────────────────────────────────────────────────────

    override fun observeSymbols(repoId: String): Flow<List<CodeSymbol>> =
        symbolDao.observeByRepo(repoId).map { entities ->
            entities.map { it.toDomain() }
        }

    // ── Search ────────────────────────────────────────────────────────────────

    override fun searchSymbols(
        query: String,
        repoId: String?,
        kinds: Set<SymbolKind>,
    ): Flow<List<CodeSymbol>> {
        val repoFilter = repoId ?: "%"

        return if (kinds.isEmpty()) {
            // No kind filter — search across all kinds
            symbolDao.searchByName(query, repoFilter).map { entities ->
                entities.map { it.toDomain() }
            }
        } else if (kinds.size == 1) {
            // Single kind — use the optimised query
            symbolDao.searchByNameAndKind(
                query    = query,
                kind     = kinds.first().name,
                repoId   = repoFilter,
            ).map { entities -> entities.map { it.toDomain() } }
        } else {
            // Multiple kinds — filter in-memory (still bounded by the 200-row SQL LIMIT)
            val kindNames = kinds.map { it.name }.toSet()
            symbolDao.searchByName(query, repoFilter).map { entities ->
                entities
                    .filter { it.kind in kindNames }
                    .map { it.toDomain() }
            }
        }
    }

    // ── Single-shot reads ─────────────────────────────────────────────────────

    override suspend fun getSymbolsForFile(repoId: String, filePath: String): List<CodeSymbol> =
        symbolDao.getByFilePath(repoId, filePath).map { it.toDomain() }

    override suspend fun getSymbol(id: String): CodeSymbol? =
        symbolDao.getById(id)?.toDomain()

    // ── Writes ────────────────────────────────────────────────────────────────

    override suspend fun deleteByRepo(repoId: String) =
        symbolDao.deleteByRepo(repoId)

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun SymbolEntity.toDomain(): CodeSymbol = CodeSymbol(
        id         = id,
        repoId     = repoId,
        name       = name,
        kind       = runCatching { SymbolKind.valueOf(kind) }.getOrDefault(SymbolKind.FUNCTION),
        filePath   = filePath,
        lineStart  = lineStart,
        lineEnd    = lineEnd,
        signature  = signature,
        docComment = docComment,
        visibility = runCatching { SymbolVisibility.valueOf(visibility) }
            .getOrDefault(SymbolVisibility.PUBLIC),
    )
}
