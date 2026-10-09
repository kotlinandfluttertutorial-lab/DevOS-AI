package com.devos.ai.feature.code.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devos.ai.feature.code.architecture.ArchitectureNavEvent
import com.devos.ai.feature.code.architecture.ArchitectureScreen
import com.devos.ai.feature.code.architecture.ArchitectureViewModel
import com.devos.ai.feature.code.explorer.FileExplorerNavEvent
import com.devos.ai.feature.code.explorer.FileExplorerScreen
import com.devos.ai.feature.code.explorer.FileExplorerViewModel
import com.devos.ai.feature.code.graph.DependencyGraphNavEvent
import com.devos.ai.feature.code.graph.DependencyGraphScreen
import com.devos.ai.feature.code.graph.DependencyGraphViewModel
import com.devos.ai.feature.code.search.CodeSearchNavEvent
import com.devos.ai.feature.code.search.CodeSearchScreen
import com.devos.ai.feature.code.search.CodeSearchViewModel
import com.devos.ai.feature.code.symbol.SymbolDetailsNavEvent
import com.devos.ai.feature.code.symbol.SymbolDetailsScreen
import com.devos.ai.feature.code.symbol.SymbolDetailsViewModel
import com.devos.ai.feature.code.viewer.AiActionChip
import com.devos.ai.feature.code.viewer.CodeViewerNavEvent
import com.devos.ai.feature.code.viewer.CodeViewerScreen
import com.devos.ai.feature.code.viewer.CodeViewerViewModel

/**
 * Route constants for code intelligence screens.
 * Must stay in sync with DevOSRoutes in :app. Feature modules cannot import :app.
 */
const val ROUTE_FILE_EXPLORER    = "repository/{repoId}/files?path={path}"
const val ROUTE_CODE_VIEWER      = "repository/{repoId}/file?path={path}&line={line}"
const val ROUTE_CODE_SEARCH      = "repository/{repoId}/search?q={query}"
const val ROUTE_SYMBOL_DETAILS   = "repository/{repoId}/symbol/{symbolId}"
const val ROUTE_DEPENDENCY_GRAPH = "repository/{repoId}/graph"
const val ROUTE_ARCHITECTURE     = "repository/{repoId}/architecture"

/**
 * File Explorer screen — DEVOS-018.
 * Replaces the placeholder composable in DevOSNavGraph.
 */
fun NavGraphBuilder.fileExplorerNavigation(navController: NavController) {
    composable(
        route = ROUTE_FILE_EXPLORER,
        arguments = listOf(
            navArgument("repoId") { type = NavType.StringType },
            navArgument("path") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { backStackEntry ->
        val repoId = backStackEntry.arguments?.getString("repoId") ?: ""
        val viewModel: FileExplorerViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is FileExplorerNavEvent.NavigateToCodeViewer ->
                        navController.navigate(
                            "repository/${event.repoId}/file?path=${event.filePath}&line=0",
                        )
                    FileExplorerNavEvent.NavigateBack ->
                        navController.popBackStack()
                }
            }
        }

        FileExplorerScreen(
            uiState = uiState,
            onFilterChange = viewModel::onFilterChange,
            onFolderTap = viewModel::onFolderTap,
            onFileTap = viewModel::onFileTap,
            onBreadcrumbTap = viewModel::onBreadcrumbTap,
            onSearchTap = { navController.navigate("repository/$repoId/search?q=") },
            onNavigateBack = viewModel::onNavigateBack,
        )
    }
}

/**
 * Code Viewer screen — DEVOS-019/020.
 * Replaces the placeholder composable in DevOSNavGraph.
 */
fun NavGraphBuilder.codeViewerNavigation(navController: NavController) {
    composable(
        route = ROUTE_CODE_VIEWER,
        arguments = listOf(
            navArgument("repoId") { type = NavType.StringType },
            navArgument("path") { type = NavType.StringType; defaultValue = "" },
            navArgument("line") { type = NavType.StringType; defaultValue = "0" },
        ),
    ) { backStackEntry ->
        val repoId = backStackEntry.arguments?.getString("repoId") ?: ""
        val viewModel: CodeViewerViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is CodeViewerNavEvent.NavigateToAiChat ->
                        navController.navigate("ai_chat")
                    is CodeViewerNavEvent.NavigateToSymbol ->
                        navController.navigate(
                            "repository/${event.repoId}/symbol/${event.symbolId}",
                        )
                    CodeViewerNavEvent.NavigateBack ->
                        navController.popBackStack()
                    CodeViewerNavEvent.NavigateToSearch ->
                        navController.navigate("repository/$repoId/search?q=")
                }
            }
        }

        CodeViewerScreen(
            uiState = uiState,
            activeAiChip = AiActionChip.EXPLAIN,
            onLineTap = viewModel::onLineTap,
            onDismissTooltip = viewModel::onDismissTooltip,
            onAiActionTap = viewModel::onAiActionTap,
            onSearchTap = viewModel::onSearchTap,
            onNavigateBack = viewModel::onNavigateBack,
        )
    }
}

/**
 * Code Search screen — DEVOS-021.
 * Replaces the placeholder composable in DevOSNavGraph.
 */
fun NavGraphBuilder.codeSearchNavigation(navController: NavController) {
    composable(
        route = ROUTE_CODE_SEARCH,
        arguments = listOf(
            navArgument("repoId") { type = NavType.StringType },
            navArgument("query") { type = NavType.StringType; defaultValue = "" },
        ),
    ) {
        val viewModel: CodeSearchViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val query by viewModel.query.collectAsStateWithLifecycle()
        val caseEnabled by viewModel.caseEnabled.collectAsStateWithLifecycle()
        val regexEnabled by viewModel.regexEnabled.collectAsStateWithLifecycle()
        val semanticEnabled by viewModel.semanticEnabled.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is CodeSearchNavEvent.NavigateToCodeViewer ->
                        navController.navigate(
                            "repository/${event.repoId}/file?path=${event.filePath}&line=${event.line}",
                        )
                    CodeSearchNavEvent.NavigateBack ->
                        navController.popBackStack()
                }
            }
        }

        CodeSearchScreen(
            uiState = uiState,
            query = query,
            caseEnabled = caseEnabled,
            regexEnabled = regexEnabled,
            semanticEnabled = semanticEnabled,
            onQueryChange = viewModel::onQueryChange,
            onCaseToggle = viewModel::onCaseToggle,
            onRegexToggle = viewModel::onRegexToggle,
            onSemanticToggle = viewModel::onSemanticToggle,
            onResultTap = viewModel::onResultTap,
            onNavigateBack = viewModel::onNavigateBack,
        )
    }
}

/**
 * Symbol Details screen — DEVOS-022.
 * Replaces the placeholder composable in DevOSNavGraph.
 */
fun NavGraphBuilder.symbolDetailsNavigation(navController: NavController) {
    composable(
        route = ROUTE_SYMBOL_DETAILS,
        arguments = listOf(
            navArgument("repoId") { type = NavType.StringType },
            navArgument("symbolId") { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val repoId = backStackEntry.arguments?.getString("repoId") ?: ""
        val viewModel: SymbolDetailsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is SymbolDetailsNavEvent.NavigateToCodeViewer ->
                        navController.navigate(
                            "repository/${event.repoId}/file?path=${event.filePath}&line=${event.line}",
                        )
                    is SymbolDetailsNavEvent.NavigateToAiChat ->
                        navController.navigate("ai_chat")
                    SymbolDetailsNavEvent.NavigateBack ->
                        navController.popBackStack()
                }
            }
        }

        SymbolDetailsScreen(
            uiState = uiState,
            onFileLinkTap = viewModel::onFileLinkTap,
            onReferenceTap = viewModel::onReferenceTap,
            onAskAiTap = viewModel::onAskAiTap,
            onNavigateBack = viewModel::onNavigateBack,
        )
    }
}

/**
 * Dependency Graph screen — DEVOS-024.
 * Replaces the placeholder composable in DevOSNavGraph.
 */
fun NavGraphBuilder.dependencyGraphNavigation(navController: NavController) {
    composable(
        route = ROUTE_DEPENDENCY_GRAPH,
        arguments = listOf(navArgument("repoId") { type = NavType.StringType }),
    ) {
        val viewModel: DependencyGraphViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    DependencyGraphNavEvent.NavigateBack -> navController.popBackStack()
                }
            }
        }

        DependencyGraphScreen(
            uiState = uiState,
            onNavigateBack = viewModel::onNavigateBack,
        )
    }
}

/**
 * Architecture Overview screen — DEVOS-025.
 * Replaces the placeholder composable in DevOSNavGraph.
 */
fun NavGraphBuilder.architectureNavigation(navController: NavController) {
    composable(
        route = ROUTE_ARCHITECTURE,
        arguments = listOf(navArgument("repoId") { type = NavType.StringType }),
    ) {
        val viewModel: ArchitectureViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    ArchitectureNavEvent.NavigateBack -> navController.popBackStack()
                    is ArchitectureNavEvent.NavigateToAiChat -> navController.navigate("ai_chat")
                }
            }
        }

        ArchitectureScreen(
            uiState = uiState,
            onNavigateBack = viewModel::onNavigateBack,
            onAskAiTap = viewModel::onAskAiTap,
        )
    }
}
