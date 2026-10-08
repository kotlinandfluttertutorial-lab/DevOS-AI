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
                        navController.navigate("search?q=")

                    is HomeNavEvent.NavigateToProject ->
                        navController.navigate("project/${event.id}")

                    is HomeNavEvent.NavigateToChat ->
                        navController.navigate("ai_chat")

                    HomeNavEvent.NavigateToNotifications ->
                        navController.navigate("notifications")

                    HomeNavEvent.NavigateToProfile ->
                        navController.navigate("profile")

                    HomeNavEvent.NavigateToImport ->
                        navController.navigate("repository/import")

                    HomeNavEvent.NavigateToProjectList ->
                        navController.navigate("project_list")

                    HomeNavEvent.NavigateToAllSessions ->
                        navController.navigate("ai_chat")
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
