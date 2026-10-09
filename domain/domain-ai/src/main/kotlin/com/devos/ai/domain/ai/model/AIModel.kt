package com.devos.ai.domain.ai.model

/**
 * A specific AI model available from a [AIProvider].
 *
 * [id] is the API identifier sent in requests (e.g. "gpt-4o", "claude-3-5-sonnet-20241022").
 * [contextWindow] is the maximum token context in tokens.
 *
 * Pure Kotlin — zero Android imports.
 */
data class AIModel(
    val id: String,
    val displayName: String,
    val provider: AIProvider,
    val contextWindow: Int,
    val supportsVision: Boolean = false,
    val isDefault: Boolean = false,
)

/**
 * Well-known models for each provider, used when the API does not return a model list.
 */
object DefaultModels {

    val OPENAI = listOf(
        AIModel("gpt-4o",               "GPT-4o",                  AIProvider.OPENAI,    128_000, supportsVision = true,  isDefault = true),
        AIModel("gpt-4o-mini",          "GPT-4o mini",             AIProvider.OPENAI,    128_000),
        AIModel("gpt-4-turbo",          "GPT-4 Turbo",             AIProvider.OPENAI,    128_000, supportsVision = true),
        AIModel("gpt-3.5-turbo",        "GPT-3.5 Turbo",           AIProvider.OPENAI,     16_385),
    )

    val ANTHROPIC = listOf(
        AIModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", AIProvider.ANTHROPIC, 200_000, isDefault = true),
        AIModel("claude-3-5-haiku-20241022",  "Claude 3.5 Haiku",  AIProvider.ANTHROPIC, 200_000),
        AIModel("claude-3-opus-20240229",      "Claude 3 Opus",     AIProvider.ANTHROPIC, 200_000),
    )

    val GEMINI = listOf(
        AIModel("gemini-2.0-flash",  "Gemini 2.0 Flash",  AIProvider.GEMINI, 1_000_000, isDefault = true),
        AIModel("gemini-1.5-pro",    "Gemini 1.5 Pro",    AIProvider.GEMINI, 2_000_000, supportsVision = true),
        AIModel("gemini-1.5-flash",  "Gemini 1.5 Flash",  AIProvider.GEMINI, 1_000_000),
    )

    val OLLAMA = listOf(
        AIModel("llama3.2",    "Llama 3.2",    AIProvider.OLLAMA, 128_000, isDefault = true),
        AIModel("codellama",   "Code Llama",   AIProvider.OLLAMA, 100_000),
        AIModel("mistral",     "Mistral 7B",   AIProvider.OLLAMA,  32_000),
        AIModel("deepseek-r1", "DeepSeek R1",  AIProvider.OLLAMA, 128_000),
    )

    fun forProvider(provider: AIProvider): List<AIModel> = when (provider) {
        AIProvider.OPENAI    -> OPENAI
        AIProvider.ANTHROPIC -> ANTHROPIC
        AIProvider.GEMINI    -> GEMINI
        AIProvider.OLLAMA    -> OLLAMA
    }

    fun defaultFor(provider: AIProvider): AIModel =
        forProvider(provider).first { it.isDefault }
}
