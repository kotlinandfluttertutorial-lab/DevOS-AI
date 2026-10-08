package com.devos.ai.feature.home.model

/**
 * An AI-generated recommendation shown on the Home Dashboard.
 *
 * This is a local stub model — real domain wiring is DEVOS-058.
 */
data class AIRecommendation(
    val id: String,
    val type: RecommendationType,
    val title: String,
    val description: String,
)
