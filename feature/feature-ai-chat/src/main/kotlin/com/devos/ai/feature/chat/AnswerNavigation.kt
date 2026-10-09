package com.devos.ai.feature.chat

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devos.ai.feature.chat.answer.AIAnswerDetailScreen
import com.devos.ai.feature.chat.answer.AnswerDetailViewModel
import com.devos.ai.feature.chat.answer.AnswerNavEvent
import com.devos.ai.feature.chat.evidence.AISourceEvidenceScreen
import com.devos.ai.feature.chat.evidence.EvidenceNavEvent
import com.devos.ai.feature.chat.evidence.SourceEvidenceViewModel

// Local route mirrors — feature-ai-chat cannot import :app's DevOSRoutes directly.
// Values MUST stay in sync with DevOSRoutes in :app.
private const val ROUTE_AI_ANSWER_DETAIL      = "ai/answer/{answerId}"
private const val ROUTE_AI_SOURCE_EVIDENCE    = "ai/evidence/{answerId}"

// Used for building navigation destinations (without the brace placeholders)
private const val ROUTE_AI_EVIDENCE_BASE      = "ai/evidence"
private const val ROUTE_CODE_VIEWER_BASE      = "repository"
private const val ROUTE_ANSWER_TO_AI_CHAT     = "ai_chat"

/**
 * Adds the AI Answer Detail screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.AI_ANSWER_DETAIL) { PlaceholderScreen(...) }` in DevOSNavGraph.
 *
 * DEVOS-029 / DA-40
 */
fun NavGraphBuilder.answerDetailNavigation(navController: NavController) {
    composable(
        route = ROUTE_AI_ANSWER_DETAIL,
        arguments = listOf(navArgument("answerId") { type = NavType.StringType }),
    ) {
        val viewModel: AnswerDetailViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    AnswerNavEvent.NavigateBack ->
                        navController.popBackStack()

                    is AnswerNavEvent.NavigateToSources ->
                        navController.navigate("$ROUTE_AI_EVIDENCE_BASE/${event.answerId}")

                    is AnswerNavEvent.NavigateToCode ->
                        navController.navigate(
                            "$ROUTE_CODE_VIEWER_BASE/devos-ai/file?path=${event.filePath}&line=${event.line}"
                        )

                    is AnswerNavEvent.NavigateToFollowUp ->
                        navController.navigate(ROUTE_ANSWER_TO_AI_CHAT)
                }
            }
        }

        AIAnswerDetailScreen(
            uiState = uiState,
            onNavigateBack = viewModel::onNavigateBack,
            onCopyAnswer = { /* clipboard handled by screen in future iteration */ },
            onViewSources = viewModel::onViewSources,
            onAskFollowUp = viewModel::onAskFollowUp,
            onSourceTap = viewModel::onSourceTap,
            onRetry = viewModel::retry,
        )
    }
}

/**
 * Adds the AI Source Evidence screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.AI_SOURCE_EVIDENCE) { PlaceholderScreen(...) }` in DevOSNavGraph.
 *
 * DEVOS-030 / DA-43
 */
fun NavGraphBuilder.sourceEvidenceNavigation(navController: NavController) {
    composable(
        route = ROUTE_AI_SOURCE_EVIDENCE,
        arguments = listOf(navArgument("answerId") { type = NavType.StringType }),
    ) {
        val viewModel: SourceEvidenceViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    EvidenceNavEvent.NavigateBack ->
                        navController.popBackStack()

                    is EvidenceNavEvent.NavigateToCode ->
                        navController.navigate(
                            "$ROUTE_CODE_VIEWER_BASE/devos-ai/file?path=${event.filePath}&line=${event.line}"
                        )
                }
            }
        }

        AISourceEvidenceScreen(
            uiState = uiState,
            onNavigateBack = viewModel::onNavigateBack,
            onOpenInCodeViewer = viewModel::onOpenInCodeViewer,
            onRetry = viewModel::retry,
        )
    }
}
