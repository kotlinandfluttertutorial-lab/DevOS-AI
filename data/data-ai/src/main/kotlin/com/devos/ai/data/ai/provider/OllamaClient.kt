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
 * Ollama local LLM client.
 *
 * Endpoint: `POST <baseUrl>/api/chat` (default: http://localhost:11434/api/chat)
 * Streaming: NDJSON — each line is a JSON object with `message.content` and `done` flag.
 * Auth: none required (local server).
 *
 * The [ProviderConfig.baseUrl] overrides the default Ollama address, enabling
 * remote Ollama servers on the local network.
 */
class OllamaClient @Inject constructor(
    @Named("ai_http_client") private val okHttpClient: OkHttpClient,
) : AIProviderClient {

    companion object {
        private const val DEFAULT_BASE_URL = "http://localhost:11434"
        private val JSON_MEDIA_TYPE        = "application/json; charset=utf-8".toMediaType()
    }

    private fun baseUrl(config: ProviderConfig): String =
        config.baseUrl?.trimEnd('/') ?: DEFAULT_BASE_URL

    override fun streamChat(
        messages: List<ChatTurn>,
        config: ProviderConfig,
        apiKey: String,  // For Ollama this is the base URL override (ignored here — use config.baseUrl)
    ): Flow<StreamToken> = flow {

        Timber.d("Ollama: streaming with model %s at %s",
            config.selectedModel.id, baseUrl(config))

        val body = JSONObject().apply {
            put("model",  config.selectedModel.id)
            put("stream", true)
            put("options", JSONObject().apply {
                put("num_predict",  config.maxTokens)
                put("temperature",  config.temperature.toDouble())
            })
            put("messages", JSONArray().also { arr ->
                messages.forEach { turn ->
                    arr.put(JSONObject().apply {
                        put("role", when (turn.role) {
                            TurnRole.SYSTEM    -> "system"
                            TurnRole.USER      -> "user"
                            TurnRole.ASSISTANT -> "assistant"
                        })
                        put("content", turn.content)
                    })
                }
            })
        }.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("${baseUrl(config)}/api/chat")
            .post(body)
            .build()

        withContext(Dispatchers.IO) {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                throw ProviderException(code, "Ollama returned $code — is the server running?")
            }
            response.body?.source()?.use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue
                    try {
                        val obj  = JSONObject(line)
                        val text = obj.optJSONObject("message")?.optString("content", "") ?: ""
                        val done = obj.optBoolean("done", false)
                        if (text.isNotEmpty()) emit(StreamToken(text))
                        if (done) { emit(StreamToken("", done = true)); break }
                    } catch (e: Exception) {
                        Timber.w(e, "Ollama: failed to parse NDJSON line")
                    }
                }
            }
            response.close()
        }
    }

    override suspend fun testConnection(config: ProviderConfig, apiKey: String): Result<String> =
        runCatching {
            val response = okHttpClient.newCall(
                Request.Builder()
                    .url("${baseUrl(config)}/api/tags")
                    .get()
                    .build(),
            ).execute()
            val code = response.code
            response.close()
            if (!response.isSuccessful) throw ProviderException(code, "Ollama not reachable at ${baseUrl(config)}")
            "Connected to Ollama at ${baseUrl(config)}"
        }

    override suspend fun listModels(config: ProviderConfig, apiKey: String): List<AIModel> =
        runCatching {
            val response = okHttpClient.newCall(
                Request.Builder()
                    .url("${baseUrl(config)}/api/tags")
                    .get()
                    .build(),
            ).execute()

            if (!response.isSuccessful) { response.close(); return DefaultModels.OLLAMA }
            val body = response.body?.string() ?: return DefaultModels.OLLAMA
            response.close()

            val models = JSONObject(body).getJSONArray("models")
            (0 until models.length()).map { i ->
                val name = models.getJSONObject(i).getString("name")
                DefaultModels.OLLAMA.find { m -> m.id == name }
                    ?: AIModel(name, name, AIProvider.OLLAMA, 128_000)
            }
        }.getOrDefault(DefaultModels.OLLAMA)
}
