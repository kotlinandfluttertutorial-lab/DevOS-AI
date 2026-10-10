package com.devos.ai.feature.prs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** Route constants — must match DevOSRoutes in :app. */
const val ROUTE_PR_LIST = "project/{projectId}/prs"
const val ROUTE_PR_REVIEW = "project/{projectId}/prs/{prId}/review"

/**
 * Adds the Pull Request List screen to the NavGraph.
 *
 * Navigation events from [PRListViewModel] are collected here and translated
 * into [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-044 / DA-55
 */
fun NavGraphBuilder.prListNavigation(navController: NavController) {
    composable(
        route = ROUTE_PR_LIST,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
        val viewModel: PRListViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is PRListNavEvent.NavigateBack -> navController.popBackStack()
                    is PRListNavEvent.NavigateToPRReview -> {
                        navController.navigate("project/$projectId/prs/${event.prId}/review")
                    }
                }
            }
        }

        PRListScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onFilterChange = viewModel::onFilterChange,
            onPRClick = viewModel::onPRClick,
            onRetry = viewModel::retry,
        )
    }
}

/**
 * Adds the PR AI Review screen to the NavGraph.
 *
 * Navigation events from [PRReviewViewModel] are collected here and translated
 * into [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-045 / DA-57
 */
fun NavGraphBuilder.prReviewNavigation(navController: NavController) {
    composable(
        route = ROUTE_PR_REVIEW,
        arguments = listOf(
            navArgument("projectId") { type = NavType.StringType },
            navArgument("prId") { type = NavType.StringType },
        ),
    ) {
        val viewModel: PRReviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is PRReviewNavEvent.NavigateBack -> navController.popBackStack()
                    is PRReviewNavEvent.CopyReview -> {
                        // Copy review text to clipboard
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("AI Code Review", event.reviewText),
                        )
                    }
                    is PRReviewNavEvent.PostToGitHub -> {
                        // TODO(DEVOS-045): invoke GitHub API to post the review comment
                    }
                }
            }
        }

        PRAIReviewScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onCopyReview = viewModel::onCopyReview,
            onPostReview = viewModel::onPostReview,
            onConfirmPost = viewModel::confirmPost,
            onDismissPost = viewModel::dismissPost,
            onRetry = viewModel::retry,
        )
    }
}
