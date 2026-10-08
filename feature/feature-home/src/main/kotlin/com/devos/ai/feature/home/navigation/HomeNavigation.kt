package com.devos.ai.feature.home.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.devos.ai.feature.home.dashboard.HomeNavEvent
import com.devos.ai.feature.home.dashboard.HomeScreen
import com.devos.ai.feature.home.dashboard.HomeViewModel

/** Route constant — must match [DevOSRoutes.HOME]. */
const val ROUTE_HOME = "home"

// Local route mirrors — feature-home cannot import :app's DevOSRoutes directly.
// These values MUST stay in sync with DevOSRoutes.kt in :app.
private const val ROUTE_SEARCH         = "search?q="
private const val ROUTE_AI_CHAT        = "ai_chat"
private const val ROUTE_NOTIFICATIONS  = "notifications"
private const val ROUTE_PROFILE        = "profile"
private const val ROUTE_REPO_IMPORT    = "repository/import"
private const val ROUTE_PROJECT_LIST   = "project_list"

/**
 * Adds the Home Dashboard screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.HOME) { PlaceholderScreen(...) }` in DevOSNavGraph.
 *
 * Nav events emitted by [HomeViewModel] are collected here and translated into
 * [NavController] calls — the ViewModel itself never imports NavController.
 */
fun NavGraphBuilder.homeNavigation(navController: NavController) {
    composable(route = ROUTE_HOME) {
        val viewModel: HomeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Collect one-shot navigation events
        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    HomeNavEvent.NavigateToSearch ->
                        navController.navigate(ROUTE_SEARCH)

                    is HomeNavEvent.NavigateToProject ->
                        navController.navigate("project/${event.id}")

                    is HomeNavEvent.NavigateToChat ->
                        navController.navigate(ROUTE_AI_CHAT)

                    HomeNavEvent.NavigateToNotifications ->
                        navController.navigate(ROUTE_NOTIFICATIONS)

                    HomeNavEvent.NavigateToProfile ->
                        navController.navigate(ROUTE_PROFILE)

                    HomeNavEvent.NavigateToImport ->
                        navController.navigate(ROUTE_REPO_IMPORT)

                    HomeNavEvent.NavigateToProjectList ->
                        navController.navigate(ROUTE_PROJECT_LIST)

                    HomeNavEvent.NavigateToAllSessions ->
                        navController.navigate(ROUTE_AI_CHAT)
                }
            }
        }

        HomeScreen(
            uiState = uiState,
            onSearchTap = viewModel::onSearchTap,
            onProjectTap = viewModel::onProjectTap,
            onSessionTap = viewModel::onSessionTap,
            onNotificationTap = viewModel::onNotificationTap,
            onProfileTap = viewModel::onProfileTap,
            onImportTap = viewModel::onImportTap,
            onSeeAllProjectsTap = viewModel::onSeeAllProjectsTap,
            onSeeAllSessionsTap = viewModel::onSeeAllSessionsTap,
            onDismissRecommendation = viewModel::dismissRecommendation,
            onRetry = viewModel::loadDashboard,
        )
    }
}
