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
 * OpenAI Chat Completions API client with SSE streaming.
 *
 * Endpoint: `POST https://api.openai.com/v1/chat/completions`
 * Streaming: `stream: true` → `text/event-stream` with `data: {...}` lines.
 *
 * Security: [apiKey] is loaded from EncryptedSharedPreferences at call time
 * and NEVER logged — only `apiKey.take(4)+"****"` appears in debug output.
 */
class OpenAIClient @Inject constructor(
    @Named("ai_http_client") private val okHttpClient: OkHttpClient,
) : AIProviderClient {

    companion object {
        private const val BASE_URL  = "https://api.openai.com/v1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override fun streamChat(
        messages: List<ChatTurn>,
        config: ProviderConfig,
        apiKey: String,
    ): Flow<StreamToken> = callbackFlow {

        Timber.d("OpenAI: streaming with model %s, key %s****",
            config.selectedModel.id, apiKey.take(4))

        val body = JSONObject().apply {
            put("model",       config.selectedModel.id)
            put("stream",      true)
            put("max_tokens",  config.maxTokens)
            put("temperature", config.temperature.toDouble())
            put("messages",    JSONArray().also { arr ->
                messages.forEach { turn ->
                    arr.put(JSONObject().apply {
                        put("role",    turn.role.name.lowercase())
                        put("content", turn.content)
                    })
                }
            })
        }.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        val factory = EventSources.createFactory(okHttpClient)
        val source  = factory.newEventSource(request, object : EventSourceListener() {

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                if (data == "[DONE]") {
                    trySend(StreamToken("", done = true))
                    close()
                    return
                }
                try {
                    val delta = JSONObject(data)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("delta")
                        .optString("content", "")
                    if (delta.isNotEmpty()) trySend(StreamToken(delta))
                } catch (e: Exception) {
                    Timber.w(e, "OpenAI: failed to parse SSE event")
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                val code = response?.code ?: 0
                val msg  = response?.message ?: t?.message ?: "Unknown error"
                Timber.e("OpenAI SSE failure: $code $msg")
                val ex = when (code) {
                    401, 403 -> AuthenticationException(AIProvider.OPENAI.displayName)
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
                    put("role",    "user")
                    put("content", "Hi")
                }))
            }.toString().toRequestBody(JSON_MEDIA_TYPE)

            val response = okHttpClient.newCall(
                Request.Builder()
                    .url("$BASE_URL/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .post(body)
                    .build(),
            ).execute()

            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                if (code == 401 || code == 403) throw AuthenticationException(AIProvider.OPENAI.displayName)
                throw ProviderException(code, response.message)
            }
            response.close()
            config.selectedModel.displayName
        }

    override suspend fun listModels(config: ProviderConfig, apiKey: String): List<AIModel> =
        runCatching {
            val response = okHttpClient.newCall(
                Request.Builder()
                    .url("$BASE_URL/models")
                    .header("Authorization", "Bearer $apiKey")
                    .get()
                    .build(),
            ).execute()

            if (!response.isSuccessful) { response.close(); return DefaultModels.OPENAI }

            val body = response.body?.string() ?: return DefaultModels.OPENAI
            response.close()

            val arr = JSONObject(body).getJSONArray("data")
            (0 until arr.length())
                .map { arr.getJSONObject(it).getString("id") }
                .filter { it.startsWith("gpt-") }
                .sortedDescending()
                .map { id ->
                    DefaultModels.OPENAI.find { m -> m.id == id }
                        ?: AIModel(id, id, AIProvider.OPENAI, 128_000)
                }
        }.getOrDefault(DefaultModels.OPENAI)
}
