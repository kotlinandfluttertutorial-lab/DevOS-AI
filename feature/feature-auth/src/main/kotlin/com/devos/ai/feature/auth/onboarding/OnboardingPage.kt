package com.devos.ai.feature.auth.onboarding

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data for a single page in the Onboarding carousel.
 *
 * @param icon Material icon shown prominently at the top.
 * @param title Short feature title — displayed as [MaterialTheme.typography.titleLarge].
 * @param description Supporting copy explaining the feature benefit.
 */
data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
)
