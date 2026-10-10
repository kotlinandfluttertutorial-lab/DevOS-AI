package com.devos.ai.feature.security

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** Route constant — must match DevOSRoutes.SECURITY_FINDINGS in :app. */
const val ROUTE_SECURITY_FINDINGS = "project/{projectId}/security"

/**
 * Adds the Security Findings screen to the NavGraph.
 *
 * Replaces the placeholder composable for SECURITY_FINDINGS in DevOSNavGraph.
 * Navigation events from [SecurityFindingsViewModel] are collected here and translated
 * into [NavController] calls — the ViewModel never imports NavController.
 *
 * DEVOS-046 / DA-59
 */
fun NavGraphBuilder.securityNavigation(navController: NavController) {
    composable(
        route = ROUTE_SECURITY_FINDINGS,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
    ) {
        val viewModel: SecurityFindingsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Collect one-shot navigation events
        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is SecurityNavEvent.NavigateBack -> navController.popBackStack()
                    is SecurityNavEvent.NavigateToAIChat -> {
                        navController.navigate("ai_chat")
                    }
                }
            }
        }

        SecurityFindingsScreen(
            uiState = uiState,
            onNavigateBack = viewModel::navigateBack,
            onFilterChange = viewModel::onFilterChange,
            onMarkFixed = viewModel::markFixed,
            onAskAI = viewModel::onAskAI,
            onRetry = viewModel::retry,
        )
    }
}
