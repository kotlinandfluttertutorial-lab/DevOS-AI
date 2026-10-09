package com.devos.ai.feature.memory

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Route constant — must match DevOSRoutes.DEVELOPER_MEMORY in :app. */
const val ROUTE_DEVELOPER_MEMORY = "memory"

/**
 * Adds the Developer Memory screen to the NavGraph.
 *
 * Replaces the `composable(DevOSRoutes.DEVELOPER_MEMORY)` placeholder in DevOSNavGraph.
 *
 * Navigation events emitted by [DeveloperMemoryViewModel] are collected here and translated
 * into [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-055 / DA-68
 */
fun NavGraphBuilder.memoryNavigation(navController: NavController) {
    composable(route = ROUTE_DEVELOPER_MEMORY) {
        val viewModel: DeveloperMemoryViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Collect one-shot navigation events
        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    MemoryNavEvent.NavigateBack -> navController.popBackStack()
                }
            }
        }

        DeveloperMemoryScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onDismissEntry = viewModel::dismissEntry,
            onClearAll = viewModel::showClearAllDialog,
            onCancelClear = viewModel::cancelClearAll,
            onConfirmClear = viewModel::confirmClearAll,
            onQueryChange = viewModel::onQueryChange,
        )
    }
}
