package com.devos.ai.domain.ai.model

/**
 * Domain model for a Developer Memory entry.
 *
 * Memory entries capture preferences, decisions, and architectural patterns
 * learned from AI conversations or explicitly added by the developer.
 *
 * DEVOS-056 / DA-67
 */
data class MemoryEntry(
    val id: String,
    val userId: String,
    val content: String,
    val category: MemoryCategory,
    val source: MemorySource,
    val createdAt: Long,
    val isPinned: Boolean,
)

/**
 * Category for a [MemoryEntry].
 *
 * Determines how the entry is surfaced and used in AI context injection.
 */
enum class MemoryCategory {
    /** Developer code style and tooling preferences. */
    CODE_PREFERENCE,
    /** Architectural or design decisions made by the developer. */
    DECISION,
    /** High-level architectural patterns or guidelines. */
    ARCHITECTURE,
    /** Cross-project global context. */
    GLOBAL,
}

/**
 * How the [MemoryEntry] was created.
 */
enum class MemorySource {
    /** Extracted automatically from an AI chat conversation. */
    AI_CHAT,
    /** Explicitly added by the developer in the memory screen. */
    USER_EXPLICIT,
    /** Inferred from static code analysis. */
    CODE_ANALYSIS,
}
