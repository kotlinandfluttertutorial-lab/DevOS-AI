package com.devos.ai.domain.ai.model

/**
 * A prioritised signal that drives an AI recommendation on the Home Dashboard.
 *
 * Signals are produced by signal providers and combined by [GetAIRecommendationsUseCase].
 * The final list shown in the UI is the top 8, sorted by [priority] descending.
 *
 * DEVOS-058 / DA-70
 */
data class AIRecommendationSignal(
    /** Category of recommendation. */
    val type: RecommendationType,
    /**
     * Priority (higher = shown first).
     * Security: 100 + securityCount. Missing tests: 50. Learning: 10.
     */
    val priority: Int,
    /** Short display title, e.g. "Security: SQL Injection Risk". */
    val title: String,
    /** One-line description shown in the card. */
    val description: String,
    /** Navigation route to activate when the user taps "Fix" or "Act". */
    val actionRoute: String,
)

/**
 * Type of AI recommendation signal.
 *
 * Used by [AIRecommendationSignal] in the domain layer.
 * The feature-home layer maps these to its own [com.devos.ai.feature.home.model.RecommendationType].
 *
 * DEVOS-058 / DA-70
 */
enum class RecommendationType {
    SECURITY,
    MISSING_TESTS,
    LEARNING,
    ARCHITECTURE,
}
