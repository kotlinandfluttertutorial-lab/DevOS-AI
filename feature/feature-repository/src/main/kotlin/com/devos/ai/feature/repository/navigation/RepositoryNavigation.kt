package com.devos.ai.feature.repository.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devos.ai.feature.repository.importing.ImportNavEvent
import com.devos.ai.feature.repository.importing.ImportViewModel
import com.devos.ai.feature.repository.importing.RepositoryImportScreen
import com.devos.ai.feature.repository.sync.RepositorySyncScreen
import com.devos.ai.feature.repository.sync.SyncNavEvent
import com.devos.ai.feature.repository.sync.SyncViewModel

/**
 * Route constants — must stay in sync with DevOSRoutes.kt in :app.
 * Feature modules cannot import :app directly, so these mirror the values.
 */
const val ROUTE_REPOSITORY_IMPORT = "repository/import"
const val ROUTE_REPOSITORY_SYNC = "repository/{repoId}/sync"

/**
 * Adds the Repository Import screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.REPOSITORY_IMPORT) { PlaceholderScreen(...) }` in DevOSNavGraph.
 *
 * Nav events from [ImportViewModel] are collected here and translated into
 * [NavController] calls — the ViewModel itself never imports NavController.
 */
fun NavGraphBuilder.repositoryImportNavigation(navController: NavController) {
    composable(route = ROUTE_REPOSITORY_IMPORT) {
        val viewModel: ImportViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val url by viewModel.url.collectAsStateWithLifecycle()
        val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
        val branch by viewModel.branch.collectAsStateWithLifecycle()
        val buildAiIndex by viewModel.buildAiIndex.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is ImportNavEvent.NavigateToSync ->
                        navController.navigate("repository/${event.repoId}/sync")
                    ImportNavEvent.NavigateBack ->
                        navController.popBackStack()
                }
            }
        }

        RepositoryImportScreen(
            uiState = uiState,
            url = url,
            selectedProvider = selectedProvider,
            branch = branch,
            buildAiIndex = buildAiIndex,
            onUrlChange = viewModel::onUrlChange,
            onProviderSelect = viewModel::onProviderSelect,
            onValidate = viewModel::validate,
            onBranchChange = viewModel::onBranchChange,
            onBuildAiIndexToggle = viewModel::onBuildAiIndexToggle,
            onImport = viewModel::import,
            onNavigateBack = viewModel::navigateBack,
        )
    }
}

/**
 * Adds the Repository Sync progress screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.REPOSITORY_SYNC) { PlaceholderScreen(...) }` in DevOSNavGraph.
 *
 * Nav events from [SyncViewModel] are collected here and translated into
 * [NavController] calls — the ViewModel itself never imports NavController.
 */
fun NavGraphBuilder.repositorySyncNavigation(navController: NavController) {
    composable(
        route = ROUTE_REPOSITORY_SYNC,
        arguments = listOf(
            navArgument("repoId") { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val repoId = backStackEntry.arguments?.getString("repoId") ?: ""
        val viewModel: SyncViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is SyncNavEvent.NavigateToOverview ->
                        navController.navigate("repository/${event.repoId}") {
                            popUpTo(ROUTE_REPOSITORY_IMPORT) { inclusive = true }
                        }
                    SyncNavEvent.NavigateBack ->
                        navController.popBackStack()
                }
            }
        }

        RepositorySyncScreen(
            uiState = uiState,
            repoName = repoId, // DEVOS-015 will supply the real name via ViewModel
            onCancel = viewModel::cancel,
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
