package com.devos.ai.domain.ai.model

/**
 * Represents a single tool exposed by an MCP server.
 *
 * Pure Kotlin — zero Android imports.
 * DEVOS-038 / DA-49
 */
data class MCPTool(
    val name: String,
    val description: String,
    val serverId: String,
    val isDestructive: Boolean = false,
)
