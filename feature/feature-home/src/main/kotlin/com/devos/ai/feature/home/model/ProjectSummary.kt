package com.devos.ai.feature.home.model

/**
 * Summary of a project shown on the Home Dashboard.
 *
 * This is a local stub model — real domain wiring is DEVOS-058.
 */
data class ProjectSummary(
    val id: String,
    val name: String,
    val language: String,
    val healthStatus: HealthStatus,
)
