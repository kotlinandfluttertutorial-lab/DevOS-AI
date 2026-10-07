package com.devos.ai.feature.auth.onboarding

/** UI state for the Onboarding screen — driven by OnboardingViewModel. */
sealed interface OnboardingUiState {
    /** Pages are being prepared. */
    data object Loading : OnboardingUiState

    /**
     * Pages are ready to display.
     *
     * @param pages The 4 onboarding pages.
     * @param currentPage Zero-based index of the active page.
     */
    data class Success(
        val pages: List<OnboardingPage>,
        val currentPage: Int,
    ) : OnboardingUiState
}
