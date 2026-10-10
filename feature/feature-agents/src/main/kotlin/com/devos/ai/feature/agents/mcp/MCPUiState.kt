package com.devos.ai.feature.agents.mcp

import com.devos.ai.domain.ai.model.MCPServer
import com.devos.ai.domain.ai.model.MCPTool

/**
 * UI state for the MCP Tools screen (DEVOS-039 / DA-51).
 *
 * Follows the DevOS 4-state pattern: Loading | Success | Empty | Error.
 */
sealed interface MCPUiState {
    data object Loading : MCPUiState

    data class Success(
        val servers: List<MCPServer>,
        val selectedServer: MCPServer?,
        val tools: List<MCPTool>,
        /** Tool pending destructive confirmation dialog. Null = no dialog shown. */
        val pendingDestructiveTool: MCPTool? = null,
    ) : MCPUiState

    data object Empty : MCPUiState

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : MCPUiState
}

/** One-shot navigation events emitted by [MCPViewModel]. */
sealed class MCPNavEvent {
    data object NavigateBack : MCPNavEvent()
    data class NavigateToToolExecution(val serverId: String, val toolName: String) : MCPNavEvent()
}
