package com.devos.ai.feature.memory

/**
 * A single entry stored in developer memory.
 *
 * @param id       Unique identifier for the entry.
 * @param emoji    Icon character shown in the leading position.
 * @param title    Short description of the remembered preference or decision.
 * @param source   Human-readable source / context of how this was learned.
 * @param isDismissible Whether the user can dismiss this entry via the × button.
 */
data class MemoryEntry(
    val id: String,
    val emoji: String,
    val title: String,
    val source: String,
    val isDismissible: Boolean,
)

/**
 * Aggregated data for the Developer Memory screen.
 *
 * @param codePreferences  Entries shown under the "Code Preferences" section.
 * @param recentDecisions  Entries shown under the "Recent Decisions" section.
 */
data class MemoryData(
    val codePreferences: List<MemoryEntry>,
    val recentDecisions: List<MemoryEntry>,
)
