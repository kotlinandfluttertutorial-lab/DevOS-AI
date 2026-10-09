package com.devos.ai.feature.agents

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devos.ai.feature.agents.run.AgentRunNavEvent
import com.devos.ai.feature.agents.run.AgentRunScreen
import com.devos.ai.feature.agents.run.AgentRunViewModel
import com.devos.ai.feature.agents.tool.AgentToolDetailScreen
import com.devos.ai.feature.agents.tool.AgentToolDetailViewModel
import com.devos.ai.feature.agents.tool.ToolDetailNavEvent

// Route mirrors — feature-agents cannot import :app's DevOSRoutes directly.
private const val ROUTE_AGENT_RUN       = "agent/run/{runId}"
private const val ROUTE_AGENT_TOOL_EXEC = "agent/run/{runId}/tool/{toolId}"

// ── Agent Run ─────────────────────────────────────────────────────────────────

/**
 * Adds the Agent Run screen to the NavGraph.
 * Replaces `composable(DevOSRoutes.AGENT_RUN) { PlaceholderScreen(...) }`.
 *
 * DEVOS-035 / DA-48
 */
fun NavGraphBuilder.agentRunNavigation(navController: NavController) {
    composable(
        route     = ROUTE_AGENT_RUN,
        arguments = listOf(navArgument("runId") { type = NavType.StringType }),
    ) {
        val viewModel: AgentRunViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is AgentRunNavEvent.NavigateBack -> navController.popBackStack()
                    is AgentRunNavEvent.NavigateToToolDetail ->
                        navController.navigate(
                            "agent/run/${event.runId}/tool/${event.stepId}",
                        )
                }
            }
        }

        AgentRunScreen(
            uiState        = uiState,
            onNavigateBack = viewModel::onNavigateBack,
            onCancelRun    = viewModel::onCancelRun,
            onStepTap      = viewModel::onStepTap,
            onRetry        = viewModel::onRetry,
        )
    }
}

// ── Agent Tool Execution Detail ───────────────────────────────────────────────

/**
 * Adds the Agent Tool Execution Detail screen to the NavGraph.
 * Replaces `composable(DevOSRoutes.AGENT_TOOL_EXEC) { PlaceholderScreen(...) }`.
 *
 * DEVOS-036 / DA-50
 */
fun NavGraphBuilder.agentToolDetailNavigation(navController: NavController) {
    composable(
        route     = ROUTE_AGENT_TOOL_EXEC,
        arguments = listOf(
            navArgument("runId")  { type = NavType.StringType },
            navArgument("toolId") { type = NavType.StringType },
        ),
    ) {
        val viewModel: AgentToolDetailViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    ToolDetailNavEvent.NavigateBack -> navController.popBackStack()
                }
            }
        }

        AgentToolDetailScreen(
            uiState         = uiState,
            onNavigateBack  = viewModel::onNavigateBack,
            onToggleRawJson = viewModel::onToggleRawJson,
        )
    }
}
