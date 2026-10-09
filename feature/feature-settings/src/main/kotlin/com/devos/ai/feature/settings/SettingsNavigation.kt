package com.devos.ai.feature.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Route constant — must match DevOSRoutes.SETTINGS in :app. */
const val ROUTE_SETTINGS = "settings"

// Route mirrors — must stay in sync with DevOSRoutes in :app.
private const val ROUTE_AI_SETTINGS_MIRROR = "settings/ai"
private const val ROUTE_PROVIDER_SETTINGS_MIRROR = "settings/ai/providers"
private const val ROUTE_DEVELOPER_MEMORY_MIRROR = "memory"
private const val ROUTE_PROFILE_MIRROR = "profile"
private const val ROUTE_NOTIFICATIONS_MIRROR = "notifications"

/**
 * Adds the Settings Root screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.SETTINGS)` placeholder in DevOSNavGraph.
 *
 * DEVOS-062 / DA-78
 */
fun NavGraphBuilder.settingsRootNavigation(navController: NavController) {
    composable(route = ROUTE_SETTINGS) {
        val viewModel: SettingsRootViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    SettingsRootNavEvent.NavigateBack ->
                        navController.popBackStack()

                    SettingsRootNavEvent.NavigateToAISettings ->
                        navController.navigate(ROUTE_AI_SETTINGS_MIRROR)

                    SettingsRootNavEvent.NavigateToProviderSettings ->
                        navController.navigate(ROUTE_PROVIDER_SETTINGS_MIRROR)

                    SettingsRootNavEvent.NavigateToDeveloperMemory ->
                        navController.navigate(ROUTE_DEVELOPER_MEMORY_MIRROR)

                    SettingsRootNavEvent.NavigateToProfile ->
                        navController.navigate(ROUTE_PROFILE_MIRROR)

                    SettingsRootNavEvent.NavigateToNotifications ->
                        navController.navigate(ROUTE_NOTIFICATIONS_MIRROR)
                }
            }
        }

        SettingsRootScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onNavigateToAISettings = viewModel::navigateToAISettings,
            onNavigateToProviderSettings = viewModel::navigateToProviderSettings,
            onNavigateToDeveloperMemory = viewModel::navigateToDeveloperMemory,
            onToggleDarkMode = viewModel::toggleDarkMode,
            onNavigateToProfile = viewModel::navigateToProfile,
            onNavigateToNotifications = viewModel::navigateToNotifications,
        )
    }
}
