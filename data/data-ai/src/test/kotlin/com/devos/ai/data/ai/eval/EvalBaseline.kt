package com.devos.ai.data.ai.eval

import org.json.JSONObject

/**
 * Baseline scores for the AI evaluation framework.
 *
 * Loaded from `test-fixtures/eval-baselines.json` at test runtime.
 * A regression is detected when a metric's live score drops more than
 * [regressionThreshold] (5%) below the baseline.
 *
 * ## Adding a new metric
 * 1. Add a field here.
 * 2. Add the key to `eval-baselines.json`.
 * 3. Write a corresponding `*EvaluationTest`.
 *
 * @param ragPrecision       Minimum acceptable Precision\@k for RAG retrieval (0–1).
 * @param groundingAccuracy  Minimum fraction of cited code lines that are real (0–1).
 * @param agentSuccessRate   Minimum fraction of agent tasks that complete successfully (0–1).
 * @param regressionThreshold Allowed drop before a metric is considered regressed (0–1).
 */
data class EvalBaseline(
    val ragPrecision: Double,
    val groundingAccuracy: Double,
    val agentSuccessRate: Double,
    val regressionThreshold: Double = 0.05,
) {
    companion object {
        /**
         * Parses an [EvalBaseline] from the JSON string in `eval-baselines.json`.
         *
         * Expected format:
         * ```json
         * {
         *   "metrics": {
         *     "ragPrecision": 0.70,
         *     "groundingAccuracy": 0.80,
         *     "agentSuccessRate": 0.75
         *   },
         *   "regressionThreshold": 0.05
         * }
         * ```
         */
        fun fromJson(json: String): EvalBaseline {
            val root = JSONObject(json)
            val metrics = root.getJSONObject("metrics")
            return EvalBaseline(
                ragPrecision = metrics.getDouble("ragPrecision"),
                groundingAccuracy = metrics.getDouble("groundingAccuracy"),
                agentSuccessRate = metrics.getDouble("agentSuccessRate"),
                regressionThreshold = if (root.has("regressionThreshold"))
                    root.getDouble("regressionThreshold") else 0.05,
            )
        }
    }
}
