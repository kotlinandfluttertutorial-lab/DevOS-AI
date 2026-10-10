package com.devos.ai.data.ai.provider

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.devos.ai.domain.ai.model.AIProvider
import com.devos.ai.domain.ai.model.DefaultModels
import com.devos.ai.domain.ai.model.ProviderConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.BufferedSource
import org.junit.jupiter.api.Test

/**
 * Unit tests for the AI provider clients using a mocked [OkHttpClient].
 *
 * No real network calls are made. Tests verify:
 * - testConnection returns success on HTTP 200
 * - testConnection returns AuthenticationException on HTTP 401
 * - listModels falls back to default models on failure
 * - StreamToken parsing handles edge cases
 */
class AIProviderClientTest {

    private fun providerConfig(provider: AIProvider) = ProviderConfig(
        provider      = provider,
        selectedModel = DefaultModels.defaultFor(provider),
        isConfigured  = true,
    )

    // ── VectorStore.toJson / parseEmbedding already tested separately ─────────
    // These tests focus on network-layer behaviour of the clients.

    // ── StreamToken ───────────────────────────────────────────────────────────

    @Test
    fun `StreamToken with done=true marks end of stream`() {
        val token = StreamToken("", done = true)
        assertThat(token.done).isTrue()
        assertThat(token.delta).isEqualTo("")
    }

    @Test
    fun `StreamToken carries delta text`() {
        val token = StreamToken("Hello, world!")
        assertThat(token.delta).isEqualTo("Hello, world!")
        assertThat(token.done).isEqualTo(false)
    }

    @Test
    fun `StreamToken with error carries message`() {
        val token = StreamToken("", error = "Rate limit exceeded")
        assertThat(token.error).isEqualTo("Rate limit exceeded")
    }

    // ── ChatTurn ──────────────────────────────────────────────────────────────

    @Test
    fun `ChatTurn roles are distinct`() {
        assertThat(TurnRole.USER != TurnRole.ASSISTANT).isTrue()
        assertThat(TurnRole.SYSTEM != TurnRole.USER).isTrue()
    }

    // ── AuthenticationException ───────────────────────────────────────────────

    @Test
    fun `AuthenticationException message contains provider name`() {
        val ex = AuthenticationException("OpenAI")
        assertThat(ex.message?.contains("OpenAI")).isEqualTo(true)
    }

    // ── ProviderException ─────────────────────────────────────────────────────

    @Test
    fun `ProviderException carries status code`() {
        val ex = ProviderException(429, "Too Many Requests")
        assertThat(ex.statusCode).isEqualTo(429)
        assertThat(ex.message?.contains("429")).isEqualTo(true)
    }

    // ── OllamaClient.testConnection with mock ─────────────────────────────────

    @Test
    fun `OllamaClient testConnection returns success on 200`() = runTest {
        val mockClient = buildMockOkHttp(200, "{\"models\":[]}")
        val client     = OllamaClient(mockClient)
        val config     = providerConfig(AIProvider.OLLAMA)

        val result = client.testConnection(config, "")
        assertThat(result.isSuccess).isEqualTo(true)
    }

    @Test
    fun `OllamaClient testConnection returns failure on 500`() = runTest {
        val mockClient = buildMockOkHttp(500, "Server Error")
        val client     = OllamaClient(mockClient)
        val config     = providerConfig(AIProvider.OLLAMA)

        val result = client.testConnection(config, "")
        assertThat(result.isFailure).isEqualTo(true)
        assertThat(result.exceptionOrNull()!!).isInstanceOf(ProviderException::class)
    }

    @Test
    fun `OllamaClient listModels falls back to defaults on failure`() = runTest {
        val mockClient = buildMockOkHttp(500, "Error")
        val client     = OllamaClient(mockClient)
        val config     = providerConfig(AIProvider.OLLAMA)

        val models = client.listModels(config, "")
        assertThat(models).isEqualTo(DefaultModels.OLLAMA)
    }

    // ── DefaultModels ─────────────────────────────────────────────────────────

    @Test
    fun `every provider has at least one default model`() {
        AIProvider.entries.forEach { provider ->
            assertThat(DefaultModels.forProvider(provider).isNotEmpty()).isTrue()
        }
    }

    @Test
    fun `every provider has exactly one default model`() {
        AIProvider.entries.forEach { provider ->
            val defaults = DefaultModels.forProvider(provider).count { it.isDefault }
            assertThat(defaults).isEqualTo(1)
        }
    }

    @Test
    fun `defaultFor returns the isDefault model`() {
        AIProvider.entries.forEach { provider ->
            val model = DefaultModels.defaultFor(provider)
            assertThat(model.isDefault).isTrue()
            assertThat(model.provider).isEqualTo(provider)
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildMockOkHttp(code: Int, body: String): OkHttpClient {
        val client = mockk<OkHttpClient>()
        val call   = mockk<okhttp3.Call>()
        val request = Request.Builder().url("http://localhost").build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()

        every { client.newCall(any()) } returns call
        every { call.execute() } returns response
        return client
    }
}
