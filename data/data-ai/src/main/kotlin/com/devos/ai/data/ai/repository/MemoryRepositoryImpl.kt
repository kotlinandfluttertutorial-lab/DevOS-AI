package com.devos.ai.data.ai.repository

import com.devos.ai.core.database.dao.MemoryEntryDao
import com.devos.ai.core.database.entity.MemoryEntryEntity
import com.devos.ai.domain.ai.model.MemoryCategory
import com.devos.ai.domain.ai.model.MemoryEntry
import com.devos.ai.domain.ai.model.MemorySource
import com.devos.ai.domain.ai.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [MemoryRepository] backed by Room via [MemoryEntryDao].
 *
 * Entity ↔ domain mapping converts String-stored enums (category / source) safely,
 * falling back to [MemoryCategory.GLOBAL] / [MemorySource.AI_CHAT] on unknown values
 * so future enum additions in older DB rows don't crash.
 *
 * [getRelevantMemory] uses a simple in-memory keyword filter over [observeEntries]
 * because the memory table is expected to be small (≤ 500 entries per user).
 *
 * DEVOS-056 / DA-67
 */
@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val dao: MemoryEntryDao,
) : MemoryRepository {

    override fun observeEntries(userId: String): Flow<List<MemoryEntry>> =
        dao.observeAllByUser(userId).map { entities -> entities.map(::toDomain) }

    override suspend fun saveEntry(entry: MemoryEntry): Result<Unit> = runCatching {
        dao.insert(toEntity(entry))
    }.onFailure { Timber.e(it, "MemoryRepository: saveEntry failed for id=%s", entry.id) }

    override suspend fun deleteEntry(entryId: String): Result<Unit> = runCatching {
        dao.delete(entryId)
    }.onFailure { Timber.e(it, "MemoryRepository: deleteEntry failed for id=%s", entryId) }

    override suspend fun clearAll(userId: String): Result<Unit> = runCatching {
        dao.deleteAll(userId, projectId = null)
    }.onFailure { Timber.e(it, "MemoryRepository: clearAll failed for userId=%s", userId) }

    override fun getRelevantMemory(userId: String, query: String): Flow<List<MemoryEntry>> {
        val keywords = query.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return observeEntries(userId).map { entries ->
            if (keywords.isEmpty()) {
                entries
            } else {
                entries.filter { entry ->
                    val lower = entry.content.lowercase()
                    keywords.any { keyword -> lower.contains(keyword) }
                }
            }
        }
    }

    // ── Entity ↔ Domain ───────────────────────────────────────────────────────

    private fun toDomain(entity: MemoryEntryEntity) = MemoryEntry(
        id        = entity.id,
        userId    = entity.userId,
        content   = entity.content,
        category  = runCatching { MemoryCategory.valueOf(entity.category) }
            .getOrDefault(MemoryCategory.GLOBAL),
        source    = runCatching { MemorySource.valueOf(entity.source) }
            .getOrDefault(MemorySource.AI_CHAT),
        createdAt = entity.createdAt,
        isPinned  = entity.isPinned,
    )

    private fun toEntity(domain: MemoryEntry): MemoryEntryEntity {
        val now = System.currentTimeMillis()
        return MemoryEntryEntity(
            id             = domain.id,
            userId         = domain.userId,
            projectId      = null,                          // GLOBAL scope
            content        = domain.content.take(500),      // enforce 500-char cap
            category       = domain.category.name,
            source         = domain.source.name,
            createdAt      = domain.createdAt,
            lastAccessedAt = now,
            expiresAt      = if (domain.isPinned) null else now + DEFAULT_TTL_MS,
            isPinned       = domain.isPinned,
        )
    }

    companion object {
        /** Default entry TTL: 90 days in milliseconds. */
        private const val DEFAULT_TTL_MS = 90L * 24 * 60 * 60 * 1_000
    }
}
