package com.devos.ai.domain.learning

import javax.inject.Inject

/**
 * Single-responsibility use case: given a list of code symbols extracted from
 * a repository, return the course topic IDs recommended for the developer.
 *
 * Delegates rule evaluation to [LearningRecommendationEngine] so the engine
 * can be tested independently.
 *
 * DEVOS-054 / DA-66
 */
class GetRecommendationsUseCase @Inject constructor(
    private val engine: LearningRecommendationEngine,
) {
    /**
     * @param symbols Symbol names / annotations from the project's source files.
     * @return List of course topic IDs; empty if no patterns matched.
     */
    operator fun invoke(symbols: List<String>): List<String> =
        engine.analyzeSymbols(symbols)
}
