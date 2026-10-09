package com.devos.ai.data.ai.provider

import com.devos.ai.domain.ai.model.AIModel
import com.devos.ai.domain.ai.model.ProviderConfig
import kotlinx.coroutines.flow.Flow

/**
 * Contract for a single AI provider's network client.
 *
 * Each implementation (OpenAI, Anthropic, Gemini, Ollama) handles:
 * - Building the request payload in the provider's format
 * - Streaming tokens via SSE / chunked HTTP
 * - Emitting each token as a [StreamToken] until [StreamToken.done] = true
 * - Mapping HTTP 401/403 to [AuthenticationException]
 *
 * Implementations are injected into [AIRepositoryImpl] via a [Map<AIProvider, AIProviderClient>]
 * multibinding. Feature modules never reference a concrete client.
 */
interface AIProviderClient {

    /**
     * Streams a chat completion for [messages] using [config].
     *
     * @param messages  Full conversation history (system + user + assistant turns)
     * @param config    Provider config including model, temperature, maxTokens
     * @param apiKey    Decrypted API key loaded from EncryptedSharedPreferences
     * @return          Flow of [StreamToken] — emit incrementally to the UI
     */
    fun streamChat(
        messages: List<ChatTurn>,
        config: ProviderConfig,
        apiKey: String,
    ): Flow<StreamToken>

    /**
     * Sends a minimal request to verify [apiKey] works for [config.provider].
     * Returns the model display name on success (useful for the test-connection UI).
     */
    suspend fun testConnection(config: ProviderConfig, apiKey: String): Result<String>

    /**
     * Fetches available models from the provider API.
     * Returns [DefaultModels.forProvider] if the API doesn't expose a model list
     * or the call fails.
     */
    suspend fun listModels(config: ProviderConfig, apiKey: String): List<AIModel>
}

/** A single token chunk emitted during streaming. */
data class StreamToken(
    /** Incremental text content of this token. */
    val delta: String,
    /** True on the final (empty) chunk — signals stream end. */
    val done: Boolean = false,
    /** Non-null when the provider signalled an error mid-stream. */
    val error: String? = null,
)

/** One turn in the conversation history. */
data class ChatTurn(
    val role: TurnRole,
    val content: String,
)

enum class TurnRole { SYSTEM, USER, ASSISTANT }

/** Thrown by [AIProviderClient.streamChat] on HTTP 401 / 403. */
class AuthenticationException(provider: String) :
    Exception("API key invalid or expired for $provider")

/** Thrown when the provider returns an unexpected error code. */
class ProviderException(val statusCode: Int, message: String) :
    Exception("Provider error $statusCode: $message")
