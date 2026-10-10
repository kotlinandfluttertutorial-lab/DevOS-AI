package com.devos.ai.data.ai.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.core.security.TokenKey
import com.devos.ai.data.ai.provider.AIProviderClient
import com.devos.ai.data.ai.provider.StreamToken
import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.model.AIProvider
import com.devos.ai.domain.ai.model.DefaultModels
import com.devos.ai.domain.ai.model.MessageRole
import com.devos.ai.domain.ai.model.ProviderConfig
import com.devos.ai.domain.ai.repository.CodeChunk
import com.devos.ai.domain.ai.repository.RAGRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AIRepositoryImpl].
 *
 * All external dependencies mocked with MockK.
 * Verifies the complete sendMessage pipeline: RAG retrieval → prompt building
 * → provider streaming → incremental ChatMessage emission.
 */
class AIRepositoryImplTest {

    private lateinit var providerRepository: AIProviderRepositoryImpl
    private lateinit var ragRepository: RAGRepository
    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var mockProviderClient: AIProviderClient
    private lateinit var impl: AIRepositoryImpl

    private val repoId  = "repo-1"
    private val context = AIContext.Repository(repoId, "TestRepo")

    private val defaultConfig = ProviderConfig(
        provider      = AIProvider.OPENAI,
        selectedModel = DefaultModels.defaultFor(AIProvider.OPENAI),
        isConfigured  = true,
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        providerRepository    = mockk(relaxed = true)
        ragRepository         = mockk(relaxed = true)
        secureTokenRepository = mockk(relaxed = true)
        mockProviderClient    = mockk(relaxed = true)

        val clients: Map<AIProvider, AIProviderClient> = mapOf(
            AIProvider.OPENAI to mockProviderClient,
        )

        impl = AIRepositoryImpl(
            providerRepository    = providerRepository,
            ragRepository         = ragRepository,
            secureTokenRepository = secureTokenRepository,
            providerClients       = clients,
        )
    }

    // ── sendMessage ───────────────────────────────────────────────────────────

    @Test
    fun `sendMessage emits streaming tokens then final message`() = runTest {
        coEvery { providerRepository.getActiveConfig() } returns defaultConfig
        coEvery { secureTokenRepository.getToken(any<TokenKey>()) } returns "sk-test-key"
        coEvery { ragRepository.retrieve(any(), any(), any(), any()) } returns emptyList()

        every { mockProviderClient.streamChat(any(), any(), any()) } returns flowOf(
            StreamToken("Hello"),
            StreamToken(", "),
            StreamToken("world!", done = true),
        )

        impl.sendMessage("Hi", context).test {
            val chunk1 = awaitItem()
            assertThat(chunk1.isStreaming).isTrue()
            assertThat(chunk1.content).isEqualTo("Hello")
            assertThat(chunk1.role).isEqualTo(MessageRole.AI)

            val chunk2 = awaitItem()
            assertThat(chunk2.isStreaming).isTrue()
            assertThat(chunk2.content).isEqualTo("Hello, ")

            val final = awaitItem()
            assertThat(final.isStreaming).isFalse()
            assertThat(final.content).isEqualTo("Hello, world!")

            awaitComplete()
        }
    }

    @Test
    fun `sendMessage emits error message when no API key configured`() = runTest {
        coEvery { providerRepository.getActiveConfig() } returns defaultConfig
        coEvery { secureTokenRepository.getToken(any<TokenKey>()) } returns null

        impl.sendMessage("test", context).test {
            val msg = awaitItem()
            assertThat(msg.content.contains("No API key")).isTrue()
            assertThat(msg.isStreaming).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `sendMessage emits error when provider not registered`() = runTest {
        // Use a config whose provider has no client in the map
        val geminiConfig = ProviderConfig(
            provider      = AIProvider.GEMINI,
            selectedModel = DefaultModels.defaultFor(AIProvider.GEMINI),
            isConfigured  = true,
        )
        coEvery { providerRepository.getActiveConfig() } returns geminiConfig
        coEvery { secureTokenRepository.getToken(any<TokenKey>()) } returns "key"

        impl.sendMessage("test", context).test {
            val msg = awaitItem()
            assertThat(msg.content.contains("not available")).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `sendMessage includes RAG chunks in prompt when available`() = runTest {
        coEvery { providerRepository.getActiveConfig() } returns defaultConfig
        coEvery { secureTokenRepository.getToken(any<TokenKey>()) } returns "sk-test"

        val chunks = listOf(
            CodeChunk("c1", repoId, "Foo.kt", 1, 10, "class UserRepository {}", "kotlin", 0.9f),
        )
        coEvery { ragRepository.retrieve(any(), any(), any(), any()) } returns chunks

        val capturedTurns = slot<List<com.devos.ai.data.ai.provider.ChatTurn>>()
        every { mockProviderClient.streamChat(capture(capturedTurns), any(), any()) } returns
            flowOf(StreamToken("ok", done = true))

        impl.sendMessage("explain UserRepository", context).test {
            awaitItem(); awaitComplete()
        }

        // System prompt should contain the RAG chunk content
        val systemContent = capturedTurns.captured.find {
            it.role == com.devos.ai.data.ai.provider.TurnRole.SYSTEM
        }?.content ?: ""
        assertThat(systemContent.contains("UserRepository")).isTrue()
    }

    @Test
    fun `sendMessage proceeds without RAG when retrieval fails`() = runTest {
        coEvery { providerRepository.getActiveConfig() } returns defaultConfig
        coEvery { secureTokenRepository.getToken(any<TokenKey>()) } returns "sk-test"
        coEvery { ragRepository.retrieve(any(), any(), any(), any()) } throws
            RuntimeException("DB error")

        every { mockProviderClient.streamChat(any(), any(), any()) } returns
            flowOf(StreamToken("fallback", done = true))

        impl.sendMessage("test", context).test {
            val msg = awaitItem()
            assertThat(msg.content).isEqualTo("fallback")
            awaitComplete()
        }
    }

    @Test
    fun `sendMessage final message has sources from RAG`() = runTest {
        coEvery { providerRepository.getActiveConfig() } returns defaultConfig
        coEvery { secureTokenRepository.getToken(any<TokenKey>()) } returns "sk-test"

        val chunks = listOf(
            CodeChunk("c1", repoId, "Auth.kt", 5, 20, "class AuthRepository", "kotlin", 0.85f),
        )
        coEvery { ragRepository.retrieve(any(), any(), any(), any()) } returns chunks

        every { mockProviderClient.streamChat(any(), any(), any()) } returns
            flowOf(StreamToken("answer", done = true))

        impl.sendMessage("auth", context).test {
            val finalMsg = awaitItem()
            assertThat(finalMsg.isStreaming).isFalse()
            assertThat(finalMsg.sources).isNotEmpty()
            assertThat(finalMsg.sources.first().filePath).isEqualTo("Auth.kt")
            awaitComplete()
        }
    }

    // ── session management ────────────────────────────────────────────────────

    @Test
    fun `createSession returns unique IDs`() = runTest {
        val id1 = impl.createSession(context)
        val id2 = impl.createSession(context)
        assertThat(id1 != id2).isTrue()
    }

    @Test
    fun `deleteSession removes session`() = runTest {
        val id = impl.createSession(context)
        impl.deleteSession(id)

        impl.getSessionMessages(id).test {
            assertThat(awaitItem()).isEqualTo(emptyList())
            awaitComplete()
        }
    }
}
