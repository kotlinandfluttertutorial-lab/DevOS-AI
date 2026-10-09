package com.devos.ai.feature.testing

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** Route constant — must match DevOSRoutes.TEST_INTELLIGENCE in :app. */
const val ROUTE_TEST_INTELLIGENCE = "project/{projectId}/tests"

/**
 * Adds the Test Intelligence screen to the NavGraph.
 *
 * Replaces the placeholder composable for TEST_INTELLIGENCE in DevOSNavGraph.
 * Navigation events from [TestIntelligenceViewModel] are collected here and translated
 * into [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-048 / DA-61
 */
fun NavGraphBuilder.testIntelligenceNavigation(navController: NavController) {
    composable(
        route = ROUTE_TEST_INTELLIGENCE,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
    ) {
        val viewModel: TestIntelligenceViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Collect one-shot navigation events
        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is TestIntelligenceNavEvent.NavigateBack -> navController.popBackStack()
                    is TestIntelligenceNavEvent.NavigateToCodeViewer -> {
                        // Navigate to code viewer — route constructed from file name
                        navController.navigate("repository/devos-ai/file?path=${event.filePath}&line=0")
                    }
                    is TestIntelligenceNavEvent.GenerateTests -> {
                        navController.navigate("ai_chat")
                    }
                }
            }
        }

        TestIntelligenceScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onGenerateTests = viewModel::generateTests,
            onNavigateToFile = viewModel::navigateToFile,
            onRetry = viewModel::retry,
        )
    }
}
