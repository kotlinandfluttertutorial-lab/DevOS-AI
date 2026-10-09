package com.devos.ai.feature.home.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devos.ai.feature.home.notifications.NotificationsNavEvent
import com.devos.ai.feature.home.notifications.NotificationsScreen
import com.devos.ai.feature.home.notifications.NotificationsViewModel
import com.devos.ai.feature.home.profile.ProfileNavEvent
import com.devos.ai.feature.home.profile.ProfileScreen
import com.devos.ai.feature.home.profile.ProfileViewModel
import com.devos.ai.feature.home.search.SearchNavEvent
import com.devos.ai.feature.home.search.SearchScreen
import com.devos.ai.feature.home.search.SearchUiState
import com.devos.ai.feature.home.search.SearchViewModel

// Local route mirrors — feature-home cannot import :app's DevOSRoutes directly.
// These values MUST stay in sync with DevOSRoutes.kt in :app.
private const val ROUTE_NOTIFICATIONS = "notifications"
private const val ROUTE_PROFILE       = "profile"
private const val ROUTE_SEARCH        = "search?q={query}"
private const val ROUTE_SPLASH        = "splash"
// ROUTE_HOME already declared in HomeNavigation.kt in the same package — reuse it

/**
 * Adds the Notifications screen to the NavGraph (DEVOS-059).
 *
 * Replaces `composable(DevOSRoutes.NOTIFICATIONS) { PlaceholderScreen(...) }`.
 */
fun NavGraphBuilder.notificationsNavigation(navController: NavController) {
    composable(route = ROUTE_NOTIFICATIONS) {
        val viewModel: NotificationsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    NotificationsNavEvent.NavigateBack ->
                        navController.popBackStack()

                    is NotificationsNavEvent.NavigateToRoute ->
                        navController.navigate(event.route)
                }
            }
        }

        NotificationsScreen(
            uiState = uiState,
            onBack = viewModel::onBack,
            onMarkAllRead = viewModel::markAllRead,
            onFilterChange = viewModel::onFilterChange,
            onNotificationTap = viewModel::onNotificationTap,
            onRetry = viewModel::loadNotifications,
        )
    }
}

/**
 * Adds the Search screen to the NavGraph (DEVOS-060).
 *
 * Replaces the placeholder `composable(DevOSRoutes.SEARCH) { PlaceholderScreen(...) }`.
 */
fun NavGraphBuilder.searchNavigation(navController: NavController) {
    composable(
        route = ROUTE_SEARCH,
        arguments = listOf(
            navArgument("query") { type = NavType.StringType; defaultValue = "" },
        ),
    ) {
        val viewModel: SearchViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Maintain query and scope as composable state, driven by ViewModel
        var query by remember { mutableStateOf("") }
        var scope by remember { mutableStateOf("all") }

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    SearchNavEvent.NavigateBack ->
                        navController.popBackStack()

                    is SearchNavEvent.NavigateToRoute ->
                        navController.navigate(event.route)
                }
            }
        }

        SearchScreen(
            uiState = uiState,
            query = query,
            activeScope = scope,
            onBack = viewModel::onBack,
            onQueryChange = { newQuery ->
                query = newQuery
                viewModel.onQueryChange(newQuery)
            },
            onScopeChange = { newScope ->
                scope = newScope
                viewModel.onScopeChange(newScope)
            },
            onResultTap = viewModel::onResultTap,
            onRetry = { viewModel.onQueryChange(query) },
        )
    }
}

/**
 * Adds the Profile screen to the NavGraph (DEVOS-061).
 *
 * Replaces `composable(DevOSRoutes.PROFILE) { PlaceholderScreen(...) }`.
 */
fun NavGraphBuilder.profileNavigation(navController: NavController) {
    composable(route = ROUTE_PROFILE) {
        val viewModel: ProfileViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    ProfileNavEvent.NavigateBack ->
                        navController.popBackStack()

                    ProfileNavEvent.NavigateToLogin -> {
                        navController.navigate(ROUTE_SPLASH) {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            }
        }

        ProfileScreen(
            uiState = uiState,
            onBack = viewModel::onBack,
            onSignOut = viewModel::onSignOut,
            onRetry = viewModel::loadProfile,
        )
    }
}
