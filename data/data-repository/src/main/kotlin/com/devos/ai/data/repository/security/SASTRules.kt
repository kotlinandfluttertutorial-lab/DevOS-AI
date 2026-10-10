package com.devos.ai.data.repository.security

import com.devos.ai.domain.repository.model.Severity

/**
 * Static SAST rule definitions used by [SecurityScanWorker].
 *
 * Each [SASTRule] bundles a regex pattern with a severity and a stable rule
 * identifier string so findings can be deduped and referenced in the UI.
 *
 * DEVOS-047 / DA-58
 */
data class SASTRule(
    val ruleId: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val pattern: Regex,
)

object SASTRules {

    /**
     * Detects hardcoded credentials — API keys, passwords, secrets, or tokens
     * assigned to a string literal of 8+ characters.
     *
     * Severity: CRITICAL — credentials in source code can be scraped from VCS
     * history even after removal.
     */
    val HARDCODED_SECRET = SASTRule(
        ruleId      = "SAST-001",
        title       = "Hardcoded Secret",
        description = "A credential or secret appears to be assigned directly in source code. " +
            "Move it to Android Keystore / EncryptedSharedPreferences.",
        severity    = Severity.CRITICAL,
        pattern     = Regex(
            """(api_key|password|secret|token)\s*=\s*["'][^"']{8,}["']""",
            RegexOption.IGNORE_CASE,
        ),
    )

    /**
     * Detects raw SQL injection risk — a `rawQuery` call whose first argument
     * is a string that contains a dollar sign (Kotlin string interpolation).
     *
     * Severity: HIGH — parameterised queries must be used instead.
     */
    val SQL_INJECTION = SASTRule(
        ruleId      = "SAST-002",
        title       = "SQL Injection Risk",
        description = "rawQuery() called with a string containing user-supplied interpolation. " +
            "Use parameterised queries with selection args instead.",
        severity    = Severity.HIGH,
        pattern     = Regex("""rawQuery\s*\(\s*"[^"]*\$"""),
    )

    /**
     * Detects cleartext HTTP URLs (excluding localhost, which is acceptable for
     * development/emulator use).
     *
     * Severity: HIGH — cleartext traffic can be intercepted on any network.
     */
    val CLEARTEXT_HTTP = SASTRule(
        ruleId      = "SAST-003",
        title       = "Cleartext HTTP Traffic",
        description = "A non-localhost HTTP URL was detected. Use HTTPS to prevent " +
            "man-in-the-middle interception.",
        severity    = Severity.HIGH,
        pattern     = Regex("""http://(?!localhost)"""),
    )

    /** All rules in priority order (highest severity first). */
    val ALL: List<SASTRule> = listOf(HARDCODED_SECRET, SQL_INJECTION, CLEARTEXT_HTTP)
}
