package com.devos.ai.feature.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** Route constant — must match DevOSRoutes.PROJECT_SETTINGS in :app. */
const val ROUTE_PROJECT_SETTINGS = "settings/project/{projectId}"

/**
 * Adds the Project Settings screen to the NavGraph.
 *
 * Replaces the `composable(DevOSRoutes.PROJECT_SETTINGS, ...)` placeholder in DevOSNavGraph.
 *
 * DEVOS-063 / DA-75
 */
fun NavGraphBuilder.projectSettingsNavigation(navController: NavController) {
    composable(
        route = ROUTE_PROJECT_SETTINGS,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
    ) {
        val viewModel: ProjectSettingsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    ProjectSettingsNavEvent.NavigateBack -> navController.popBackStack()
                }
            }
        }

        ProjectSettingsScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onUpdateName = viewModel::updateName,
            onUpdateDescription = viewModel::updateDescription,
            onUpdateAutoInclude = viewModel::updateAutoInclude,
            onShowDeleteConfirmation = viewModel::showDeleteConfirmation,
            onCancelDelete = viewModel::cancelDelete,
            onConfirmDelete = viewModel::confirmDelete,
        )
    }
}
