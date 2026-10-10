package com.devos.ai.feature.agents.mcp

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

// Route mirrors — feature-agents cannot import :app's DevOSRoutes directly.
private const val ROUTE_MCP_TOOLS = "mcp"

/**
 * Adds the MCP Tools screen to the NavGraph.
 * Replaces `composable(DevOSRoutes.MCP_TOOLS) { PlaceholderScreen(...) }`.
 *
 * DEVOS-039 / DA-51
 */
fun NavGraphBuilder.mcpNavigation(navController: NavController) {
    composable(route = ROUTE_MCP_TOOLS) {
        val viewModel: MCPViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is MCPNavEvent.NavigateBack -> navController.popBackStack()
                    is MCPNavEvent.NavigateToToolExecution ->
                        // Navigate to agent tool execution screen when implemented
                        navController.navigate(
                            "agent/run/${event.serverId}/tool/${event.toolName}",
                        )
                }
            }
        }

        MCPToolsScreen(
            uiState              = uiState,
            onNavigateBack       = viewModel::onNavigateBack,
            onSelectServer       = viewModel::selectServer,
            onRunTool            = viewModel::runTool,
            onConfirmDestructive = viewModel::confirmDestructiveTool,
            onCancelDestructive  = viewModel::cancelDestructiveTool,
            onRetry              = viewModel::retry,
        )
    }
}
