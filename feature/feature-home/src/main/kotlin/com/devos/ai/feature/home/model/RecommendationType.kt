package com.devos.ai.feature.home.model

import com.devos.ai.designsystem.components.DevOSBadgeStatus

/**
 * Type of AI recommendation shown on the Home Dashboard.
 *
 * Maps to [DevOSBadgeStatus] for pill color and carries a display label.
 */
enum class RecommendationType(
    val label: String,
    val badgeStatus: DevOSBadgeStatus,
) {
    SECURITY("Security", DevOSBadgeStatus.ERROR),
    TESTS("Testing", DevOSBadgeStatus.WARNING),
    LEARNING("Learning", DevOSBadgeStatus.INFO),
    ARCHITECTURE("Architecture", DevOSBadgeStatus.PENDING),
}
