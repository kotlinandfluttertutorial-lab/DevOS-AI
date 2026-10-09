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
import com.devos.ai.feature.repository.list.ProjectListNavEvent
import com.devos.ai.feature.repository.list.ProjectListScreen
import com.devos.ai.feature.repository.list.ProjectListViewModel
import com.devos.ai.feature.repository.overview.OverviewNavEvent
import com.devos.ai.feature.repository.overview.OverviewViewModel
import com.devos.ai.feature.repository.overview.RepositoryOverviewScreen
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
 * Route for the Project / Repository List screen.
 * Mirrors [com.devos.ai.navigation.DevOSRoutes.PROJECT_LIST].
 */
const val ROUTE_PROJECT_LIST = "project_list"

/**
 * Route for the Repository Overview screen.
 * Mirrors [com.devos.ai.navigation.DevOSRoutes.REPOSITORY_OVERVIEW].
 * Feature modules cannot import :app, so the string is kept in sync manually.
 */
const val ROUTE_REPOSITORY_OVERVIEW = "repository/{repoId}"

/**
 * Adds the Project / Repository List screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.PROJECT_LIST) { PlaceholderScreen(...) }` in DevOSNavGraph.
 *
 * This is a primary-tab destination — no back arrow.
 * Nav events from [ProjectListViewModel] are collected here and translated into
 * [NavController] calls — the ViewModel itself never imports NavController.
 */
fun NavGraphBuilder.projectListNavigation(navController: NavController) {
    composable(route = ROUTE_PROJECT_LIST) {
        val viewModel: ProjectListViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    ProjectListNavEvent.NavigateToImport ->
                        navController.navigate(ROUTE_REPOSITORY_IMPORT)
                    is ProjectListNavEvent.NavigateToRepo ->
                        navController.navigate("repository/${event.repoId}")
                }
            }
        }

        ProjectListScreen(
            uiState = uiState,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onFilterLanguage = viewModel::onFilterLanguage,
            onSortChange = viewModel::onSortChange,
            onProjectTap = viewModel::onProjectTap,
            onImportTap = viewModel::onImportTap,
            onRetry = viewModel::onRetry,
        )
    }
}

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
 * Adds the Repository Overview screen to the NavGraph.
 *
 * Replaces `composable(DevOSRoutes.REPOSITORY_OVERVIEW) { PlaceholderScreen(...) }` in DevOSNavGraph.
 *
 * Nav events from [OverviewViewModel] are collected here and translated into
 * [NavController] calls — the ViewModel itself never imports NavController.
 *
 * Destination routes for Git/Files/AI mirror [com.devos.ai.navigation.DevOSRoutes]
 * values. The feature module cannot import :app, so these strings are kept in
 * sync with the route constants manually.
 */
fun NavGraphBuilder.overviewNavigation(navController: NavController) {
    composable(
        route = ROUTE_REPOSITORY_OVERVIEW,
        arguments = listOf(navArgument("repoId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val repoId = backStackEntry.arguments?.getString("repoId") ?: ""
        val viewModel: OverviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    OverviewNavEvent.NavigateBack      -> navController.popBackStack()
                    OverviewNavEvent.NavigateToGit     -> navController.navigate("repository/$repoId/git")
                    OverviewNavEvent.NavigateToAI      -> navController.navigate("ai_chat")
                    OverviewNavEvent.NavigateToFiles   -> navController.navigate("repository/$repoId/files?path=")
                    is OverviewNavEvent.NavigateToTab  -> { /* reserved for a later ticket */ }
                }
            }
        }

        RepositoryOverviewScreen(
            uiState = uiState,
            onTabSelect = viewModel::onTabSelect,
            onNavigateBack = viewModel::onNavigateBack,
            onGitTap = viewModel::onGitTap,
            onAITap = viewModel::onAITap,
            onFilesTap = viewModel::onFilesTap,
            onRefresh = viewModel::onRefresh,
            onRetry = viewModel::onRetry,
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
            onNavigateBack = viewModel::navigateBack,
        )
    }
}
