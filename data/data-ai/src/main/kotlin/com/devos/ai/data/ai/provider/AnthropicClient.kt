package com.devos.ai.data.ai.provider

import com.devos.ai.domain.ai.model.AIModel
import com.devos.ai.domain.ai.model.AIProvider
import com.devos.ai.domain.ai.model.DefaultModels
import com.devos.ai.domain.ai.model.ProviderConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * Anthropic Messages API client with SSE streaming.
 *
 * Endpoint: `POST https://api.anthropic.com/v1/messages`
 * Auth header: `x-api-key: <key>`
 * Streaming events: `content_block_delta` with `delta.type = text_delta`
 *
 * Anthropic's format separates the system prompt from messages and uses a
 * different SSE event structure than OpenAI.
 */
class AnthropicClient @Inject constructor(
    @Named("ai_http_client") private val okHttpClient: OkHttpClient,
) : AIProviderClient {

    companion object {
        private const val BASE_URL        = "https://api.anthropic.com/v1"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private val JSON_MEDIA_TYPE       = "application/json; charset=utf-8".toMediaType()
    }

    override fun streamChat(
        messages: List<ChatTurn>,
        config: ProviderConfig,
        apiKey: String,
    ): Flow<StreamToken> = callbackFlow {

        Timber.d("Anthropic: streaming with model %s, key %s****",
            config.selectedModel.id, apiKey.take(4))

        // Anthropic separates system messages from the conversation
        val systemPrompt = messages.filter { it.role == TurnRole.SYSTEM }
            .joinToString("\n") { it.content }
        val convoMessages = messages.filter { it.role != TurnRole.SYSTEM }

        val body = JSONObject().apply {
            put("model",      config.selectedModel.id)
            put("max_tokens", config.maxTokens)
            put("stream",     true)
            if (systemPrompt.isNotBlank()) put("system", systemPrompt)
            put("messages",   JSONArray().also { arr ->
                convoMessages.forEach { turn ->
                    arr.put(JSONObject().apply {
                        put("role",    if (turn.role == TurnRole.USER) "user" else "assistant")
                        put("content", turn.content)
                    })
                }
            })
        }.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$BASE_URL/messages")
            .header("x-api-key",         apiKey)
            .header("anthropic-version",  ANTHROPIC_VERSION)
            .header("Accept",             "text/event-stream")
            .post(body)
            .build()

        val source = EventSources.createFactory(okHttpClient)
            .newEventSource(request, object : EventSourceListener() {

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String,
                ) {
                    when (type) {
                        "content_block_delta" -> {
                            try {
                                val delta = JSONObject(data).getJSONObject("delta")
                                if (delta.optString("type") == "text_delta") {
                                    val text = delta.optString("text", "")
                                    if (text.isNotEmpty()) trySend(StreamToken(text))
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Anthropic: failed to parse delta event")
                            }
                        }
                        "message_stop" -> {
                            trySend(StreamToken("", done = true))
                            close()
                        }
                        "error" -> {
                            val msg = runCatching {
                                JSONObject(data).getString("message")
                            }.getOrDefault("Stream error")
                            close(ProviderException(0, msg))
                        }
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?,
                ) {
                    val code = response?.code ?: 0
                    val msg  = response?.message ?: t?.message ?: "Unknown"
                    val ex   = when (code) {
                        401, 403 -> AuthenticationException(AIProvider.ANTHROPIC.displayName)
                        else     -> ProviderException(code, msg)
                    }
                    close(ex)
                }
            })

        awaitClose { source.cancel() }
    }

    override suspend fun testConnection(config: ProviderConfig, apiKey: String): Result<String> =
        runCatching {
            val body = JSONObject().apply {
                put("model",      config.selectedModel.id)
                put("max_tokens", 1)
                put("messages",   JSONArray().put(JSONObject().apply {
                    put("role", "user"); put("content", "Hi")
                }))
            }.toString().toRequestBody(JSON_MEDIA_TYPE)

            val response = okHttpClient.newCall(
                Request.Builder()
                    .url("$BASE_URL/messages")
                    .header("x-api-key",        apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .post(body)
                    .build(),
            ).execute()

            val code = response.code
            response.close()
            if (code == 401 || code == 403) throw AuthenticationException(AIProvider.ANTHROPIC.displayName)
            if (!response.isSuccessful)     throw ProviderException(code, response.message)
            config.selectedModel.displayName
        }

    override suspend fun listModels(config: ProviderConfig, apiKey: String): List<AIModel> =
        DefaultModels.ANTHROPIC  // Anthropic doesn't expose a public models endpoint
}
