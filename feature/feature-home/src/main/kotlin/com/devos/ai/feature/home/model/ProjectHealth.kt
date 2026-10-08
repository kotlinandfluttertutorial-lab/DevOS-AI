package com.devos.ai.feature.home.model

/**
 * Four-quadrant health snapshot shown in the 2×2 grid on the Home Dashboard.
 *
 * This is a local stub model — real domain wiring is DEVOS-058.
 */
data class ProjectHealth(
    val securityCount: Int,
    val securityLabel: String,
    val testCoverage: Int,
    val architectureGrade: String,
    val dependencyUpdates: Int,
)
