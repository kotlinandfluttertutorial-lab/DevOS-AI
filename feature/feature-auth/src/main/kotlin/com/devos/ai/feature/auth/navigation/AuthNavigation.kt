package com.devos.ai.feature.auth.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.devos.ai.feature.auth.login.AuthViewModel
import com.devos.ai.feature.auth.login.LoginScreen
import com.devos.ai.feature.auth.model.OAuthProvider
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

/**
 * Adds the Login screen to the NavGraph, including the OAuth deep link handler.
 *
 * Deep link: devos://auth/callback?code={code}&provider={provider}
 * When the browser returns from GitHub/GitLab, Android routes the intent here and
 * [AuthViewModel.handleAuthCallback] exchanges the code for a token.
 */
fun NavGraphBuilder.loginNavigation(navController: NavController) {
    composable(
        route = ROUTE_LOGIN,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "devos://auth/callback?code={code}&provider={provider}"
            },
        ),
    ) { backStackEntry ->
        val viewModel: AuthViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val code = backStackEntry.arguments?.getString("code")
        val providerName = backStackEntry.arguments?.getString("provider")
        LaunchedEffect(code, providerName) {
            if (!code.isNullOrEmpty() && !providerName.isNullOrEmpty()) {
                runCatching { OAuthProvider.valueOf(providerName.uppercase()) }
                    .getOrNull()
                    ?.let { provider -> viewModel.handleAuthCallback(code, provider) }
            }
        }

        LoginScreen(
            uiState = uiState,
            navEvent = viewModel.navEvent,
            onLoginGitHub = viewModel::loginWithGitHub,
            onLoginGitLab = viewModel::loginWithGitLab,
            onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo(ROUTE_LOGIN) { inclusive = true }
                }
            },
        )
    }
}
