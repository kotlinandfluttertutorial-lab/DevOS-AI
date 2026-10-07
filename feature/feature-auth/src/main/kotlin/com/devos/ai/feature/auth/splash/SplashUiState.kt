package com.devos.ai.feature.auth.splash

/** UI state for the Splash screen — driven by SplashViewModel. */
sealed interface SplashUiState {
    /** Initial state while auth check is running. */
    data object Loading : SplashUiState

    /** User has never onboarded — navigate to Onboarding. */
    data object NavigateToOnboarding : SplashUiState

    /** User is authenticated — navigate directly to Home. */
    data object NavigateToHome : SplashUiState
}
