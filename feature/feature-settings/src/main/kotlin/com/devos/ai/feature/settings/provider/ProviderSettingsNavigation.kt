package com.devos.ai.feature.settings.provider

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Route constant — must match DevOSRoutes.PROVIDER_SETTINGS in :app. */
const val ROUTE_PROVIDER_SETTINGS = "settings/ai/providers"

/**
 * Adds the Provider Settings screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.PROVIDER_SETTINGS) { PlaceholderScreen(...) }`.
 *
 * Navigation events from [ProviderSettingsViewModel] are translated into
 * [NavController] calls here — the ViewModel never imports NavController.
 *
 * DEVOS-034 / DA-45
 */
fun NavGraphBuilder.providerSettingsNavigation(navController: NavController) {
    composable(route = ROUTE_PROVIDER_SETTINGS) {
        val viewModel: ProviderSettingsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    ProviderSettingsNavEvent.NavigateBack -> navController.popBackStack()
                }
            }
        }

        ProviderSettingsScreen(
            uiState             = uiState,
            onNavigateBack      = viewModel::onNavigateBack,
            onEditKey           = viewModel::onEditKey,
            onConfigureProvider = viewModel::onConfigureProvider,
            onSaveKey           = viewModel::onSaveKey,
            onCancelEdit        = viewModel::onCancelEdit,
            onTestConnection    = viewModel::onTestConnection,
        )
    }
}
