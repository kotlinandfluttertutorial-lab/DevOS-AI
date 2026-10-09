package com.devos.ai.domain.ai.model

/**
 * Supported AI providers.
 *
 * [displayName] is shown in the Provider Settings screen (DEVOS-034).
 * [prefKey] is the key used in [SecureTokenRepository] for the API key.
 * [supportsStreaming] indicates whether the provider supports SSE streaming.
 *
 * Pure Kotlin — zero Android imports.
 */
enum class AIProvider(
    val displayName: String,
    val prefKey: String,
    val supportsStreaming: Boolean = true,
    val requiresApiKey: Boolean = true,
) {
    OPENAI(
        displayName      = "OpenAI",
        prefKey          = "openai_api_key",
        supportsStreaming = true,
        requiresApiKey   = true,
    ),
    ANTHROPIC(
        displayName      = "Anthropic",
        prefKey          = "anthropic_api_key",
        supportsStreaming = true,
        requiresApiKey   = true,
    ),
    GEMINI(
        displayName      = "Google Gemini",
        prefKey          = "gemini_api_key",
        supportsStreaming = true,
        requiresApiKey   = true,
    ),
    OLLAMA(
        displayName      = "Ollama (Local)",
        prefKey          = "ollama_base_url",
        supportsStreaming = true,
        requiresApiKey   = false,  // Ollama uses a local URL, not an API key
    ),
}
