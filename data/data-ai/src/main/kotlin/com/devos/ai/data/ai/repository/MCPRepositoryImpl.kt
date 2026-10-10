package com.devos.ai.data.ai.repository

import com.devos.ai.domain.ai.model.MCPServer
import com.devos.ai.domain.ai.model.MCPServerStatus
import com.devos.ai.domain.ai.model.MCPTool
import com.devos.ai.domain.ai.repository.MCPRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of [MCPRepository].
 *
 * Returns hardcoded demo data (GitHub MCP + AWS Docs servers with tools)
 * until real MCP server connectivity is implemented.
 *
 * DEVOS-038 / DA-49
 */
@Singleton
class MCPRepositoryImpl @Inject constructor() : MCPRepository {

    private val stubServers = listOf(
        MCPServer(
            id          = "github-mcp",
            name        = "GitHub MCP",
            url         = "https://mcp.github.com",
            isConnected = true,
            toolCount   = 4,
            status      = MCPServerStatus.CONNECTED,
        ),
        MCPServer(
            id          = "aws-docs",
            name        = "AWS Docs",
            url         = "https://mcp.aws.amazon.com/docs",
            isConnected = false,
            toolCount   = 3,
            status      = MCPServerStatus.IDLE,
        ),
    )

    private val stubTools = mapOf(
        "github-mcp" to listOf(
            MCPTool(
                name         = "search_repositories",
                description  = "Search GitHub repositories by query",
                serverId     = "github-mcp",
                isDestructive = false,
            ),
            MCPTool(
                name         = "get_file_contents",
                description  = "Fetch file from repo at path",
                serverId     = "github-mcp",
                isDestructive = false,
            ),
            MCPTool(
                name         = "create_issue",
                description  = "Create a new GitHub issue",
                serverId     = "github-mcp",
                isDestructive = false,
            ),
            MCPTool(
                name         = "merge_pull_request",
                description  = "⚠ Destructive — requires confirmation",
                serverId     = "github-mcp",
                isDestructive = true,
            ),
        ),
        "aws-docs" to listOf(
            MCPTool(
                name         = "search_docs",
                description  = "Search AWS documentation by keyword",
                serverId     = "aws-docs",
                isDestructive = false,
            ),
            MCPTool(
                name         = "get_service_docs",
                description  = "Retrieve docs for a specific AWS service",
                serverId     = "aws-docs",
                isDestructive = false,
            ),
            MCPTool(
                name         = "get_api_reference",
                description  = "Fetch API reference for an AWS service",
                serverId     = "aws-docs",
                isDestructive = false,
            ),
        ),
    )

    override fun observeServers(): Flow<List<MCPServer>> = flowOf(stubServers)

    override suspend fun getToolsForServer(serverId: String): List<MCPTool> =
        stubTools[serverId] ?: emptyList()

    override suspend fun executeTool(serverId: String, toolName: String): Result<String> =
        Result.success("Stub execution: $toolName on $serverId completed successfully.")
}
