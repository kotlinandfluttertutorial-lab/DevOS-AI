package com.devos.ai.feature.chat

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Route constant — must match DevOSRoutes.AI_CHAT in :app. */
const val ROUTE_AI_CHAT = "ai_chat"

// Local route mirrors — feature-ai-chat cannot import :app's DevOSRoutes directly.
// Values MUST stay in sync with DevOSRoutes.kt in :app.
private const val ROUTE_AI_ANSWER  = "ai/answer"
private const val ROUTE_AGENT_RUN  = "agent/run"
private const val ROUTE_CODE_VIEWER = "repository"

/**
 * Adds the AI Chat screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.AI_CHAT) { PlaceholderScreen(...) }` in DevOSNavGraph.
 *
 * Nav events emitted by [AIChatViewModel] are collected here and translated into
 * [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-026 / DA-38, DEVOS-027 / DA-42, DEVOS-028 / DA-44
 */
fun NavGraphBuilder.aiChatNavigation(navController: NavController) {
    composable(route = ROUTE_AI_CHAT) {
        val viewModel: AIChatViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Collect one-shot navigation events
        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    AIChatNavEvent.NavigateBack ->
                        navController.popBackStack()

                    is AIChatNavEvent.NavigateToAnswer ->
                        navController.navigate("$ROUTE_AI_ANSWER/${event.answerId}")

                    is AIChatNavEvent.NavigateToCode ->
                        navController.navigate(
                            "$ROUTE_CODE_VIEWER/devos-ai/file?path=${event.filePath}&line=${event.line}"
                        )

                    is AIChatNavEvent.NavigateToAgentRun ->
                        navController.navigate("$ROUTE_AGENT_RUN/${event.runId}")
                }
            }
        }

        AIChatScreen(
            uiState = uiState,
            onSendMessage = viewModel::sendMessage,
            onActionChipTap = viewModel::onActionChipTap,
            onSetContext = viewModel::setContext,
            onClearConversation = viewModel::clearConversation,
            onNavigateBack = viewModel::navigateBack,
            onNavigateToAnswer = viewModel::onAnswerTapped,
            onNavigateToCode = viewModel::onSourceTapped,
            onNavigateToAgentRun = viewModel::onAgentRunTapped,
        )
    }
}
