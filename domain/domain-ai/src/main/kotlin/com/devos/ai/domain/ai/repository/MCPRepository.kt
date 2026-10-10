package com.devos.ai.domain.ai.repository

import com.devos.ai.domain.ai.model.MCPServer
import com.devos.ai.domain.ai.model.MCPTool
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for MCP (Model Context Protocol) server management.
 *
 * Implemented in data-ai by MCPRepositoryImpl.
 * Pure Kotlin — zero Android imports.
 * DEVOS-038 / DA-49
 */
interface MCPRepository {

    /** Returns all known MCP servers. */
    fun observeServers(): Flow<List<MCPServer>>

    /** Returns all tools registered for a given server ID. */
    suspend fun getToolsForServer(serverId: String): List<MCPTool>

    /** Executes a tool by name on the given server. Returns result or throws. */
    suspend fun executeTool(serverId: String, toolName: String): Result<String>
}
