package com.devos.ai.data.ai.provider

import com.devos.ai.domain.ai.model.AIModel
import com.devos.ai.domain.ai.model.AIProvider
import com.devos.ai.domain.ai.model.DefaultModels
import com.devos.ai.domain.ai.model.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * Google Gemini (Generative Language API) client.
 *
 * Endpoint: `POST https://generativelanguage.googleapis.com/v1beta/models/<model>:streamGenerateContent`
 * Auth: `?key=<apiKey>` query parameter
 * Streaming: `alt=sse` returns `text/event-stream` with newline-delimited JSON chunks.
 *
 * Note: Gemini uses a different conversation format — roles are "user" and "model",
 * and system instructions are passed in a separate `system_instruction` field.
 */
class GeminiClient @Inject constructor(
    @Named("ai_http_client") private val okHttpClient: OkHttpClient,
) : AIProviderClient {

    companion object {
        private const val BASE_URL     = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE    = "application/json; charset=utf-8".toMediaType()
    }

    override fun streamChat(
        messages: List<ChatTurn>,
        config: ProviderConfig,
        apiKey: String,
    ): Flow<StreamToken> = flow {

        Timber.d("Gemini: streaming with model %s, key %s****",
            config.selectedModel.id, apiKey.take(4))

        val systemPrompt = messages.filter { it.role == TurnRole.SYSTEM }
            .joinToString("\n") { it.content }
        val convoMessages = messages.filter { it.role != TurnRole.SYSTEM }

        val bodyObj = JSONObject().apply {
            if (systemPrompt.isNotBlank()) {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
            }
            put("contents", JSONArray().also { arr ->
                convoMessages.forEach { turn ->
                    arr.put(JSONObject().apply {
                        put("role", if (turn.role == TurnRole.USER) "user" else "model")
                        put("parts", JSONArray().put(JSONObject().put("text", turn.content)))
                    })
                }
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", config.maxTokens)
                put("temperature",     config.temperature.toDouble())
            })
        }

        val url = "$BASE_URL/${config.selectedModel.id}:streamGenerateContent" +
            "?key=$apiKey&alt=sse"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .post(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        withContext(Dispatchers.IO) {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                if (code == 401 || code == 403)
                    throw AuthenticationException(AIProvider.GEMINI.displayName)
                throw ProviderException(code, response.message)
            }
            response.body?.source()?.use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") {
                            emit(StreamToken("", done = true))
                            break
                        }
                        try {
                            val text = JSONObject(data)
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .optString("text", "")
                            if (text.isNotEmpty()) emit(StreamToken(text))
                        } catch (e: Exception) {
                            Timber.w(e, "Gemini: failed to parse SSE line")
                        }
                    }
                }
            }
            response.close()
        }
    }

    override suspend fun testConnection(config: ProviderConfig, apiKey: String): Result<String> =
        runCatching {
            val body = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", "Hi")))
                }))
                put("generationConfig", JSONObject().put("maxOutputTokens", 1))
            }.toString().toRequestBody(JSON_MEDIA_TYPE)

            val url = "$BASE_URL/${config.selectedModel.id}:generateContent?key=$apiKey"
            val response = okHttpClient.newCall(
                Request.Builder().url(url).post(body).build(),
            ).execute()

            val code = response.code
            response.close()
            if (code == 401 || code == 403) throw AuthenticationException(AIProvider.GEMINI.displayName)
            if (!response.isSuccessful)     throw ProviderException(code, response.message)
            config.selectedModel.displayName
        }

    override suspend fun listModels(config: ProviderConfig, apiKey: String): List<AIModel> =
        DefaultModels.GEMINI
}
