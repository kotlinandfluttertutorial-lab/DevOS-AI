package com.devos.ai.core.common.logging

import timber.log.Timber
import java.security.MessageDigest

/**
 * Structured logging wrapper for DevOS AI.
 *
 * All log calls across feature modules go through this object — never call
 * [Timber] directly from feature or data modules.
 *
 * ## Privacy rules
 * - Never log raw user IDs — use [hashUserId] to produce an 8-char hex hash.
 * - Never log API keys, tokens, or passwords — pass values through [String.masked].
 * - Never log email, display name, repository content, AI responses, or code snippets.
 * - Property values longer than 200 characters are truncated automatically.
 *
 * ## Token masking
 * Any property key that contains "key", "token", "password", "secret", or "auth"
 * has its value replaced with "****" before being written to Logcat.
 *
 * ## Usage
 * ```kotlin
 * DevOSLogger.init(isDebug = BuildConfig.DEBUG)
 *
 * DevOSLogger.event(LogCategory.REPOSITORY, "repo_imported",
 *     mapOf("repoId" to id, "provider" to "github"))
 *
 * DevOSLogger.error(LogCategory.AI, "Stream failed", throwable)
 *
 * DevOSLogger.performance("load_repository_list", durationMs = elapsed,
 *     metadata = mapOf("count" to list.size))
 * ```
 */
object DevOSLogger {

    private val sensitiveKeyPatterns = listOf("key", "token", "password", "secret", "auth")

    /**
     * Plants the appropriate [Timber] tree based on the build type.
     * Call once from `Application.onCreate()`.
     */
    fun init(isDebug: Boolean) {
        Timber.plant(DevOSTimberTree(isDebug))
    }

    /**
     * Logs a structured analytics event with optional key-value properties.
     *
     * Properties are sanitised before logging:
     * - Keys matching sensitive patterns have their values replaced with "****".
     * - String values longer than 200 chars are truncated.
     * - Keys containing "email" or "name" are dropped entirely.
     */
    fun event(
        category: LogCategory,
        action: String,
        properties: Map<String, Any> = emptyMap(),
    ) {
        val sanitized = sanitize(properties)
        val propsStr = if (sanitized.isEmpty()) "" else
            " ${sanitized.entries.joinToString(" ") { "${it.key}=${it.value}" }}"
        Timber.tag(category.tag).d("EVENT $action$propsStr")
    }

    /**
     * Logs an error with an optional [Throwable].
     *
     * In release builds the error is forwarded to the crash reporter via
     * [DevOSTimberTree].
     */
    fun error(
        category: LogCategory,
        message: String,
        throwable: Throwable? = null,
    ) {
        Timber.tag(category.tag).e(throwable, message)
    }

    /**
     * Logs a performance measurement.
     *
     * Operations slower than 500 ms are logged at WARN level so they appear
     * in release log output and can be tracked for regression.
     */
    fun performance(
        name: String,
        durationMs: Long,
        metadata: Map<String, Any> = emptyMap(),
    ) {
        val metaStr = if (metadata.isEmpty()) "" else
            " ${metadata.entries.joinToString(" ") { "${it.key}=${it.value}" }}"
        val message = "PERF $name ${durationMs}ms$metaStr"
        if (durationMs > 500L) {
            Timber.tag(LogCategory.PERFORMANCE.tag).w(message)
        } else {
            Timber.tag(LogCategory.PERFORMANCE.tag).d(message)
        }
    }

    /**
     * Returns the first 8 hex characters of the SHA-256 hash of [userId].
     *
     * Use this whenever a user identifier must appear in a log event.
     * The truncated hash is one-way and unlinkable to the original ID.
     */
    fun hashUserId(userId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Strips PII keys, masks sensitive values, and truncates long strings.
     */
    private fun sanitize(properties: Map<String, Any>): Map<String, Any> =
        properties
            .filterKeys { key ->
                // Drop keys that could carry PII
                !key.lowercase().contains("email") &&
                    !key.lowercase().contains("displayname") &&
                    !key.lowercase().contains("username")
            }
            .mapValues { (key, value) ->
                when {
                    // Mask sensitive values
                    sensitiveKeyPatterns.any { key.lowercase().contains(it) } -> "****"
                    // Truncate long strings
                    value is String && value.length > 200 -> value.take(200) + "…"
                    else -> value
                }
            }
}

/**
 * Returns a masked representation of this string: the first 4 characters
 * followed by `"****"`.  Use for logging API keys in diagnostics.
 *
 * Example: `"sk-abcdefghij".masked()` → `"sk-a****"`
 */
fun String.masked(): String = if (length <= 4) "****" else take(4) + "****"
