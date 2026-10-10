package com.devos.ai.network

/**
 * Exception thrown by data-layer repository implementations when a backend API
 * call fails.
 *
 * @param code  HTTP status code, or 0 for connectivity / timeout failures.
 * @param message  Human-readable error description.
 */
class NetworkException(
    val code: Int,
    override val message: String,
) : Exception(if (code > 0) "HTTP $code: $message" else message)
