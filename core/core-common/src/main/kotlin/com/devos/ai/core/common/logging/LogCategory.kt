package com.devos.ai.core.common.logging

/**
 * Log categories that identify the subsystem emitting an event or error.
 *
 * All log calls pass one of these constants so that logs can be filtered
 * by subsystem in Logcat (e.g. `tag:AI`) and forwarded to the appropriate
 * analytics / crash-reporting channel.
 */
enum class LogCategory(val tag: String) {
    AUTH("AUTH"),
    REPOSITORY("REPO"),
    AI("AI"),
    AGENTS("AGENTS"),
    NAVIGATION("NAV"),
    PERFORMANCE("PERF"),
    SECURITY("SEC"),
}
