package com.devos.ai.feature.auth.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.devos.ai.feature.auth.onboarding.OnboardingScreen
import com.devos.ai.feature.auth.onboarding.OnboardingViewModel
import com.devos.ai.feature.auth.splash.SplashScreen
import com.devos.ai.feature.auth.splash.SplashViewModel

/** Route constants — must match [DevOSRoutes]. */
private const val ROUTE_SPLASH = "splash"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_LOGIN = "login"

/**
 * Adds the Splash screen to the NavGraph.
 *
 * Replace the placeholder `composable(DevOSRoutes.SPLASH)` in DevOSNavGraph with
 * this call: `splashNavigation(navController)`.
 */
fun NavGraphBuilder.splashNavigation(navController: NavController) {
    composable(route = ROUTE_SPLASH) {
        val viewModel: SplashViewModel = hiltViewModel()

        SplashScreen(
            navEvent = viewModel.navEvent,
            onNavigateToOnboarding = {
                navController.navigate(ROUTE_ONBOARDING) {
                    popUpTo(ROUTE_SPLASH) { inclusive = true }
                }
            },
            onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo(ROUTE_SPLASH) { inclusive = true }
                }
            },
            onNavigateToLogin = {
                navController.navigate(ROUTE_LOGIN) {
                    popUpTo(ROUTE_SPLASH) { inclusive = true }
                }
            },
        )
    }
}

/**
 * Adds the Onboarding screen to the NavGraph.
 *
 * Replace the placeholder `composable(DevOSRoutes.ONBOARDING)` with this call.
 */
fun NavGraphBuilder.onboardingNavigation(navController: NavController) {
    composable(route = ROUTE_ONBOARDING) {
        val viewModel: OnboardingViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        OnboardingScreen(
            uiState = uiState,
            navEvent = viewModel.navEvent,
            onNavigateToLogin = {
                navController.navigate(ROUTE_LOGIN) {
                    popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                }
            },
            onNextPage = viewModel::nextPage,
            onSetPage = viewModel::setPage,
            onSkip = viewModel::skipOnboarding,
            onComplete = viewModel::completeOnboarding,
        )
    }
}
