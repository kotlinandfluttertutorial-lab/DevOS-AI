package com.devos.ai.domain.ai.model

/**
 * Represents a connected MCP (Model Context Protocol) server.
 *
 * Pure Kotlin — zero Android imports.
 * DEVOS-038 / DA-49
 */
data class MCPServer(
    val id: String,
    val name: String,
    val url: String,
    val isConnected: Boolean,
    val toolCount: Int,
    val status: MCPServerStatus,
)

enum class MCPServerStatus {
    CONNECTED,
    IDLE,
    DISCONNECTED,
}
