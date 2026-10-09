package com.devos.ai.feature.settings

/**
 * Stub model representing the AI configuration settings.
 *
 * Will be backed by DataStore in a future ticket.
 * DEVOS-033 / DA-46
 */
data class AISettings(
    val defaultModel: String = "claude-sonnet-4.5",
    val topKResults: Int = 10,
    val chunkSize: Int = 512,
    val agentMaxSteps: Int = 25,
    val autoApproveSafeTools: Boolean = true,
    val memoryEnabled: Boolean = true,
    val tokenUsage: Int = 248_420,
    val tokenLimit: Int = 500_000,
)
