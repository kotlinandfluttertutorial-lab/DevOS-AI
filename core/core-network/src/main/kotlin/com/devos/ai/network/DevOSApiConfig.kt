package com.devos.ai.network

/**
 * API configuration constants for the DevOS AI backend.
 *
 * The active BASE_URL is injected at build time by the product flavor via BuildConfig:
 *   - local     → http://10.0.2.2:8000/v1/   (Android emulator → host localhost)
 *   - staging   → https://api-staging.devos.ai/v1/
 *   - production → https://api.devos.ai/v1/
 *
 * This object provides the fallback for debug builds and tests.
 * Do NOT hardcode environment-specific URLs in feature modules or data modules.
 */
object DevOSApiConfig {
    /** Fallback URL for local development (Android emulator → host machine). */
    const val DEFAULT_LOCAL_URL = "http://10.0.2.2:8000/v1/"
}
