package com.devos.ai.domain.repository.model

/**
 * Domain model for per-file test coverage data parsed from a JaCoCo XML report.
 *
 * DEVOS-049 / DA-60
 */
data class FileCoverage(
    val filePath: String,
    /** Line coverage ratio in [0.0, 1.0]. */
    val lineCoverage: Float,
    val coveredLines: Int,
    val totalLines: Int,
)
