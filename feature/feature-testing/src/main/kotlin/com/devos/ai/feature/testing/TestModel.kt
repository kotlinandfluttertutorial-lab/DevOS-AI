package com.devos.ai.feature.testing

/**
 * Domain models for the Test Intelligence screen.
 *
 * DEVOS-048 / DA-61
 */

/** A single file with low or zero test coverage. */
data class UncoveredFile(
    val name: String,
    val coveragePercent: Int,   // 0–100
    val untestedFunctions: Int,
)

/** Aggregated test coverage data for the repository. */
data class TestCoverage(
    val overallPercent: Int,    // 0–100
    val targetPercent: Int,
    val passing: Int,
    val failing: Int,
    val flaky: Int,
    val aiSuggestion: String,
    val uncoveredFiles: List<UncoveredFile>,
)
