package com.devos.ai.domain.repository.model

/**
 * Severity of a SAST security finding.
 * Values stored as enum names in Room (e.g. "CRITICAL").
 */
enum class Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
}

/**
 * Lifecycle status of a security finding.
 * Values stored as enum names in Room (e.g. "OPEN").
 */
enum class FindingStatus {
    OPEN,
    FIXED,
    DISMISSED,
}

/**
 * Domain model for a single SAST finding produced by [SecurityScanWorker].
 *
 * DEVOS-047 / DA-58
 */
data class SecurityFinding(
    val id: String,
    val repoId: String,
    val severity: Severity,
    val ruleId: String,
    val title: String,
    val description: String,
    val filePath: String,
    val lineNumber: Int,
    val status: FindingStatus,
)
