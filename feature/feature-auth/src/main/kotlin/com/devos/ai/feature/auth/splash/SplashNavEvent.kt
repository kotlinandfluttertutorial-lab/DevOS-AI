package com.devos.ai.feature.auth.splash

/** One-shot navigation events emitted from SplashViewModel via SharedFlow. */
sealed class SplashNavEvent {
    /** Navigate to the Onboarding carousel (first launch or unauthenticated). */
    data object ToOnboarding : SplashNavEvent()

    /** Navigate to the Home screen (already authenticated). */
    data object ToHome : SplashNavEvent()

    /** Navigate to Login (onboarding complete but not authenticated). */
    data object ToLogin : SplashNavEvent()
}
