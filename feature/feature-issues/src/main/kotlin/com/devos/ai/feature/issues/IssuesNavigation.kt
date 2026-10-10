package com.devos.ai.feature.issues

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** Route constants — must match DevOSRoutes in :app. */
const val ROUTE_ISSUE_LIST = "project/{projectId}/issues"
const val ROUTE_ISSUE_DETAIL = "project/{projectId}/issues/{issueId}"

/**
 * Adds the Issue List screen to the NavGraph.
 *
 * Navigation events from [IssueListViewModel] are collected here and translated
 * into [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-042 / DA-54
 */
fun NavGraphBuilder.issueListNavigation(navController: NavController) {
    composable(
        route = ROUTE_ISSUE_LIST,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
        val viewModel: IssueListViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is IssueListNavEvent.NavigateBack -> navController.popBackStack()
                    is IssueListNavEvent.NavigateToDetail -> {
                        navController.navigate("project/$projectId/issues/${event.issueId}")
                    }
                    is IssueListNavEvent.NavigateToCreateIssue -> {
                        // TODO(DEVOS-044): open create issue screen or Custom Tab
                    }
                }
            }
        }

        IssueListScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onFilterChange = viewModel::onFilterChange,
            onIssueClick = viewModel::onIssueClick,
            onRequestCloseIssue = viewModel::requestCloseIssue,
            onConfirmClose = viewModel::confirmCloseIssue,
            onDismissClose = viewModel::dismissCloseConfirmation,
            onCreateIssue = viewModel::onCreateIssue,
            onRetry = viewModel::retry,
        )
    }
}

/**
 * Adds the Issue Detail screen to the NavGraph.
 *
 * Navigation events from [IssueDetailViewModel] are collected here and translated
 * into [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-043 / DA-56
 */
fun NavGraphBuilder.issueDetailNavigation(navController: NavController) {
    composable(
        route = ROUTE_ISSUE_DETAIL,
        arguments = listOf(
            navArgument("projectId") { type = NavType.StringType },
            navArgument("issueId") { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
        val viewModel: IssueDetailViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is IssueDetailNavEvent.NavigateBack -> navController.popBackStack()
                    is IssueDetailNavEvent.NavigateToCodeViewer -> {
                        // Build the code viewer route directly — avoid importing :app routes
                        navController.navigate(
                            "repository/default/file?path=${event.path}&line=${event.line}",
                        )
                    }
                    is IssueDetailNavEvent.NavigateToAIChat -> {
                        navController.navigate("ai_chat")
                    }
                }
            }
        }

        IssueDetailScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onRequestClose = viewModel::requestCloseIssue,
            onConfirmClose = viewModel::confirmCloseIssue,
            onDismissClose = viewModel::dismissCloseConfirmation,
            onCodeRefClick = viewModel::onCodeRefClick,
            onAIFixSuggestion = viewModel::onAIFixSuggestion,
            onRetry = viewModel::retry,
        )
    }
}
