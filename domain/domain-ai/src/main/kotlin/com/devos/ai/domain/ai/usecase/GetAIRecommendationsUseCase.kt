package com.devos.ai.domain.ai.usecase

import com.devos.ai.domain.ai.model.AIRecommendationSignal
import com.devos.ai.domain.ai.model.RecommendationType
import javax.inject.Inject

// ── Signal providers ──────────────────────────────────────────────────────────

/**
 * Produces [AIRecommendationSignal]s for security findings.
 *
 * Priority increases with the number of findings so critical-rich projects
 * are surfaced above single-finding projects.
 *
 * DEVOS-058 / DA-70
 */
class SecuritySignalProvider @Inject constructor() {

    /**
     * @param securityCount Number of open security findings.
     * @return A security signal, or null if [securityCount] == 0.
     */
    fun provide(securityCount: Int): AIRecommendationSignal? {
        if (securityCount <= 0) return null
        return AIRecommendationSignal(
            type        = RecommendationType.SECURITY,
            priority    = 100 + securityCount,   // 101+ for security
            title       = "Security: $securityCount open finding${if (securityCount > 1) "s" else ""}",
            description = "Review and fix security findings to protect your codebase.",
            actionRoute = "security_findings",
        )
    }
}

/**
 * Produces an [AIRecommendationSignal] when test coverage is below 60 %.
 *
 * DEVOS-058 / DA-70
 */
class TestCoverageSignalProvider @Inject constructor() {

    /**
     * @param coverage Ratio in [0.0, 1.0].
     * @return A missing-tests signal if [coverage] < 0.6, otherwise null.
     */
    fun provide(coverage: Float): AIRecommendationSignal? {
        if (coverage >= COVERAGE_THRESHOLD) return null
        val coveragePct = (coverage * 100).toInt()
        return AIRecommendationSignal(
            type        = RecommendationType.MISSING_TESTS,
            priority    = 50,
            title       = "Add tests — coverage is $coveragePct %",
            description = "Test coverage is below 60 %. Generate tests for uncovered files.",
            actionRoute = "test_intelligence",
        )
    }

    companion object {
        const val COVERAGE_THRESHOLD = 0.6f
    }
}

/**
 * Always emits one learning recommendation so the user always has a next step.
 *
 * DEVOS-058 / DA-70
 */
class LearningSignalProvider @Inject constructor() {

    fun provide(): AIRecommendationSignal = AIRecommendationSignal(
        type        = RecommendationType.LEARNING,
        priority    = 10,
        title       = "Continue learning",
        description = "Pick up where you left off in your learning path.",
        actionRoute = "learning_dashboard",
    )
}

// ── Use case ──────────────────────────────────────────────────────────────────

/**
 * Combines signals from all providers, deduplicates by type, and returns
 * the top [MAX_SIGNALS] sorted by priority descending.
 *
 * All providers currently use stub input — real data wiring is a future task.
 *
 * DEVOS-058 / DA-70
 */
class GetAIRecommendationsUseCase @Inject constructor(
    private val securityProvider:     SecuritySignalProvider,
    private val testCoverageProvider: TestCoverageSignalProvider,
    private val learningProvider:     LearningSignalProvider,
) {

    /**
     * @param securityCount  Number of open security findings (stub default 2).
     * @param testCoverage   Test coverage ratio 0..1 (stub default 0.34).
     * @return Up to [MAX_SIGNALS] signals sorted by priority descending.
     */
    operator fun invoke(
        securityCount: Int   = DEFAULT_SECURITY_COUNT,
        testCoverage: Float  = DEFAULT_TEST_COVERAGE,
    ): List<AIRecommendationSignal> {
        val signals = buildList {
            securityProvider.provide(securityCount)?.let { add(it) }
            testCoverageProvider.provide(testCoverage)?.let { add(it) }
            add(learningProvider.provide())
        }

        return signals
            .sortedByDescending { it.priority }
            .take(MAX_SIGNALS)
    }

    companion object {
        const val MAX_SIGNALS            = 8
        const val DEFAULT_SECURITY_COUNT = 2
        const val DEFAULT_TEST_COVERAGE  = 0.34f
    }
}
