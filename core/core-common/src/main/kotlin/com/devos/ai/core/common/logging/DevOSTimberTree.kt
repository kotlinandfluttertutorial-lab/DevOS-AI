package com.devos.ai.core.common.logging

import android.util.Log
import timber.log.Timber

/**
 * Custom [Timber.Tree] for DevOS AI.
 *
 * ## Debug builds
 * - All log levels are written to Logcat.
 * - Each log message is prefixed with the thread name for easier tracing.
 *
 * ## Release builds
 * - Only WARN and above are written.
 * - ERROR logs are forwarded to the crash reporter (stub — wire to
 *   Firebase Crashlytics or Sentry in the production integration sprint).
 * - Sensitive patterns (`api_key`, `token`, `password`) are masked even in
 *   the tag/message before emission.
 *
 * ## Token masking
 * Any log message that contains a pattern resembling
 * `key=<value>`, `token=<value>`, or `password=<value>` has the value
 * replaced with `"****"` to prevent accidental secret leakage.
 */
class DevOSTimberTree(private val isDebug: Boolean) : Timber.Tree() {

    // Patterns that indicate a value following `=` should be masked.
    private val sensitivePatternRegex = Regex(
        pattern = """(?i)(api_key|apikey|token|password|secret|bearer|authorization)\s*=\s*\S+""",
    )

    override fun isLoggable(tag: String?, priority: Int): Boolean =
        if (isDebug) true else priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val sanitizedTag = tag ?: "DevOS"
        val sanitizedMessage = maskSensitiveValues(message)
        val fullMessage = if (isDebug) {
            "[${Thread.currentThread().name}] $sanitizedMessage"
        } else {
            sanitizedMessage
        }

        when (priority) {
            Log.VERBOSE -> Log.v(sanitizedTag, fullMessage, t)
            Log.DEBUG   -> Log.d(sanitizedTag, fullMessage, t)
            Log.INFO    -> Log.i(sanitizedTag, fullMessage, t)
            Log.WARN    -> Log.w(sanitizedTag, fullMessage, t)
            Log.ERROR   -> {
                Log.e(sanitizedTag, fullMessage, t)
                if (!isDebug) {
                    reportToCrashReporter(sanitizedTag, fullMessage, t)
                }
            }
            Log.ASSERT  -> Log.wtf(sanitizedTag, fullMessage, t)
        }
    }

    /**
     * Replaces sensitive key=value patterns in the message with `key=****`.
     *
     * Input:  `"Connecting with api_key=sk-abcdef token=abc123"`
     * Output: `"Connecting with api_key=**** token=****"`
     */
    internal fun maskSensitiveValues(message: String): String =
        sensitivePatternRegex.replace(message) { match ->
            val keyPart = match.value.substringBefore("=").trimEnd()
            "$keyPart=****"
        }

    /**
     * Stub crash-reporter integration.
     *
     * Replace the body of this function with the production crash-reporting
     * SDK call (e.g. `FirebaseCrashlytics.getInstance().recordException()`).
     * The stub is intentionally a no-op so the module compiles without a
     * Firebase dependency during the MVP phase.
     */
    private fun reportToCrashReporter(tag: String, message: String, t: Throwable?) {
        // TODO(platform): wire to Firebase Crashlytics or Sentry
        // FirebaseCrashlytics.getInstance().apply {
        //     setCustomKey("log_tag", tag)
        //     recordException(t ?: Exception(message))
        // }
    }
}
