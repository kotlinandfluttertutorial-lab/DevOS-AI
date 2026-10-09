package com.devos.ai.feature.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Route constant — must match DevOSRoutes.AI_SETTINGS in :app. */
const val ROUTE_AI_SETTINGS = "settings/ai"

// Local route mirror — feature-settings cannot import :app's DevOSRoutes directly.
// Value MUST stay in sync with DevOSRoutes.PROVIDER_SETTINGS in :app.
private const val ROUTE_PROVIDER_SETTINGS = "settings/ai/providers"

/**
 * Adds the AI Settings screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.AI_SETTINGS) { PlaceholderScreen(...) }` in DevOSNavGraph.
 *
 * Navigation events emitted by [AISettingsViewModel] are collected here and translated
 * into [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-033 / DA-46
 */
fun NavGraphBuilder.aiSettingsNavigation(navController: NavController) {
    composable(route = ROUTE_AI_SETTINGS) {
        val viewModel: AISettingsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Collect one-shot navigation events
        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    AISettingsNavEvent.NavigateBack ->
                        navController.popBackStack()

                    AISettingsNavEvent.NavigateToProviders ->
                        navController.navigate(ROUTE_PROVIDER_SETTINGS)

                    AISettingsNavEvent.NavigateToModelPicker ->
                        // Model picker is a future screen; navigate to providers as fallback
                        navController.navigate(ROUTE_PROVIDER_SETTINGS)
                }
            }
        }

        AISettingsScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onNavigateToModelPicker = viewModel::navigateToModelPicker,
            onNavigateToProviders = viewModel::navigateToProviders,
            onUpdateTopKResults = viewModel::updateTopKResults,
            onUpdateChunkSize = viewModel::updateChunkSize,
            onUpdateAgentMaxSteps = viewModel::updateAgentMaxSteps,
            onUpdateAutoApproveSafeTools = viewModel::updateAutoApproveSafeTools,
            onUpdateMemoryEnabled = viewModel::updateMemoryEnabled,
        )
    }
}
