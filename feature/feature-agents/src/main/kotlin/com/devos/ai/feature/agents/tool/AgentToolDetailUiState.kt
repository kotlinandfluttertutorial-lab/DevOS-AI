package com.devos.ai.feature.agents.tool

/**
 * UI state for the Agent Tool Execution Detail screen (DEVOS-036 / FIGMA-20).
 */
sealed interface AgentToolDetailUiState {
    data object Loading : AgentToolDetailUiState
    data class Success(
        val toolName: String,
        val toolDescription: String,
        val statusLabel: String,
        val isSuccess: Boolean,
        val durationMs: Long,
        /** Formatted JSON string for the input parameters code block. */
        val inputJson: String,
        /** Formatted JSON string (or plain text) for the output code block. */
        val outputJson: String,
        val showRawJson: Boolean = false,
    ) : AgentToolDetailUiState
    data class Error(val message: String) : AgentToolDetailUiState
}

sealed class ToolDetailNavEvent {
    data object NavigateBack : ToolDetailNavEvent()
}
