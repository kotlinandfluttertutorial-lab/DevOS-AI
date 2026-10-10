package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a Developer Memory entry.
 *
 * Entries are scoped per [userId] and optionally per [projectId].
 * Entries with [projectId] = null are GLOBAL (cross-project).
 *
 * [category] — stored as MemoryCategory.name() (e.g. "CODE_PREFERENCE").
 * [source]   — stored as MemorySource.name()   (e.g. "AI_CHAT").
 * [createdAt] / [lastAccessedAt] / [expiresAt] — epoch-milliseconds UTC.
 *   expiresAt = null means the entry is pinned and never expires.
 *
 * DEVOS-056 / DA-67
 */
@Entity(
    tableName = "memory_entries",
    indices = [Index("userId"), Index("projectId")],
)
data class MemoryEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val projectId: String?,               // null = GLOBAL scope
    val content: String,                  // max 500 chars
    /** Stored as MemoryCategory.name() */
    val category: String,
    /** Stored as MemorySource.name() */
    val source: String,
    val createdAt: Long,                  // epoch-millis UTC
    val lastAccessedAt: Long,             // epoch-millis UTC
    val expiresAt: Long?,                 // null = pinned; otherwise epoch-millis UTC
    val isPinned: Boolean,
)
