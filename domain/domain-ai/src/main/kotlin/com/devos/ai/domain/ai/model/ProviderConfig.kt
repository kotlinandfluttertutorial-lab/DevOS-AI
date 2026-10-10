package com.devos.ai.domain.ai.model

/**
 * Runtime configuration for a single AI provider.
 *
 * Stored via [AIProviderRepository]; never persisted as plain text.
 * The API key / base URL is loaded from EncryptedSharedPreferences at call time.
 *
 * Pure Kotlin — zero Android imports.
 */
data class ProviderConfig(
    val provider: AIProvider,
    val selectedModel: AIModel,
    /** True when the user has saved a valid API key / base URL for this provider. */
    val isConfigured: Boolean,
    /** Base URL override — used for Ollama (default: http://localhost:11434) and proxies. */
    val baseUrl: String? = null,
    /** Maximum tokens to generate in a single response. */
    val maxTokens: Int = 4_096,
    /** Sampling temperature (0.0 = deterministic, 1.0 = creative). */
    val temperature: Float = 0.7f,
)
