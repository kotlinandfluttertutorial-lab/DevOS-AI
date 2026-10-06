package com.devos.ai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

/**
 * Root navigation graph for DevOS AI.
 *
 * All routes are defined in [DevOSRoutes].
 * Each feature module registers its routes via NavGraphBuilder extension functions.
 *
 * Navigation hierarchy:
 *   splash → onboarding (first launch) | home (returning user)
 *   home → project | repository | ai_chat | learning | more
 *   more → search | git | agents | mcp | memory | notifications | settings | profile
 */
@Composable
fun DevOSNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = DevOSRoutes.SPLASH,
    ) {
        // Feature navigation registrations
        // Each will be implemented by the corresponding feature module:
        //
        // splashNavigation(navController)
        // onboardingNavigation(navController)
        // authNavigation(navController)
        // homeNavigation(navController)
        // projectNavigation(navController)
        // repositoryNavigation(navController)
        // codeNavigation(navController)
        // aiChatNavigation(navController)
        // agentNavigation(navController)
        // gitNavigation(navController)
        // issuesNavigation(navController)
        // prNavigation(navController)
        // securityNavigation(navController)
        // testingNavigation(navController)
        // learningNavigation(navController)
        // memoryNavigation(navController)
        // settingsNavigation(navController)
    }
}
