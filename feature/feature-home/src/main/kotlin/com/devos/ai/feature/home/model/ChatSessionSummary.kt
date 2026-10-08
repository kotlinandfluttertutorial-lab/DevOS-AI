package com.devos.ai.feature.home.model

/**
 * Summary of a recent AI chat session shown on the Home Dashboard.
 *
 * This is a local stub model — real domain wiring is DEVOS-058.
 */
data class ChatSessionSummary(
    val id: String,
    val title: String,
    val projectName: String,
    val relativeTime: String,
    val iconEmoji: String,
)
