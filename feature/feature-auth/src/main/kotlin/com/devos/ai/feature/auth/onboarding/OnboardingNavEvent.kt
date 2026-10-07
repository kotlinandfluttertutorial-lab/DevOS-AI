package com.devos.ai.feature.auth.onboarding

/** One-shot navigation events emitted from OnboardingViewModel via SharedFlow. */
sealed class OnboardingNavEvent {
    /** Navigate to the Login screen after onboarding is complete or skipped. */
    data object ToLogin : OnboardingNavEvent()
}
