package com.devos.ai.feature.git

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** Route constant — must match DevOSRoutes.GIT_HISTORY in :app. */
const val ROUTE_GIT_HISTORY = "repository/{repoId}/git"

/**
 * Adds the Git History screen to the NavGraph.
 *
 * Replaces the placeholder composable for GIT_HISTORY in DevOSNavGraph.
 * Navigation events from [GitHistoryViewModel] are collected here and translated
 * into [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-040 / DA-53
 */
fun NavGraphBuilder.gitHistoryNavigation(navController: NavController) {
    composable(
        route = ROUTE_GIT_HISTORY,
        arguments = listOf(navArgument("repoId") { type = NavType.StringType }),
    ) {
        val viewModel: GitHistoryViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Collect one-shot navigation events
        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is GitNavEvent.NavigateBack -> navController.popBackStack()
                    is GitNavEvent.NavigateToCommitDetail -> {
                        // TODO(DEVOS-041): Navigate to commit detail screen when available
                    }
                }
            }
        }

        GitHistoryScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onBranchSelected = viewModel::onBranchSelected,
            onCommitClick = viewModel::onCommitClick,
            onRetry = viewModel::retry,
        )
    }
}
