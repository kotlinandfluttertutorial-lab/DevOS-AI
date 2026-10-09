package com.devos.ai.feature.security

/**
 * Domain models for the Security Findings screen.
 *
 * DEVOS-046 / DA-59
 */

/** Severity level of a security finding. */
enum class Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
}

/** A single security finding detected in the codebase. */
data class SecurityFinding(
    val id: String,
    val title: String,
    val severity: Severity,
    val filePath: String,
    val lineNumber: Int,
    val owaspCategory: String,
    val aiSuggestion: String?,
)
