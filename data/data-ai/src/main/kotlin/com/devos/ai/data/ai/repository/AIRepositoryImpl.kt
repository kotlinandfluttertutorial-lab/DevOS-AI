package com.devos.ai.data.ai.repository

import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.data.ai.provider.AIProviderClient
import com.devos.ai.data.ai.provider.ChatTurn
import com.devos.ai.data.ai.provider.TurnRole
import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.model.AIProvider
import com.devos.ai.domain.ai.model.ChatMessage
import com.devos.ai.domain.ai.model.MessageRole
import com.devos.ai.domain.ai.model.SourceReference
import com.devos.ai.domain.ai.repository.AIRepository
import com.devos.ai.domain.ai.repository.RAGRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [AIRepository].
 *
 * ## Flow for sendMessage
 * 1. Load active provider config from [AIProviderRepositoryImpl].
 * 2. Retrieve the API key from [SecureTokenRepository].
 * 3. Run RAG retrieval to gather context chunks.
 * 4. Build the conversation prompt (system + RAG context + conversation + user message).
 * 5. Stream tokens from the selected [AIProviderClient].
 * 6. Emit incremental [ChatMessage] chunks with `isStreaming = true`.
 * 7. Emit final message with `isStreaming = false` and populated `sources`.
 *
 * ## Security
 * - API keys loaded at call time from [SecureTokenRepository] — never stored as fields.
 * - Keys are never logged; only `key.take(4)+"****"` in debug output.
 */
@Singleton
class AIRepositoryImpl @Inject constructor(
    private val providerRepository: AIProviderRepositoryImpl,
    private val ragRepository: RAGRepository,
    private val secureTokenRepository: SecureTokenRepository,
    private val providerClients: Map<AIProvider, @JvmSuppressWildcards AIProviderClient>,
) : AIRepository {

    // Minimal in-memory session store — DEVOS-056 will persist to Room
    private val sessions = mutableMapOf<String, MutableList<ChatMessage>>()

    override fun sendMessage(message: String, context: AIContext): Flow<ChatMessage> = flow {
        val config  = providerRepository.getActiveConfig()
        val client  = providerClients[config.provider]
            ?: run {
                Timber.e("No client for provider %s", config.provider)
                emit(errorMessage("Provider ${config.provider.displayName} is not available."))
                return@flow
            }

        val apiKey = secureTokenRepository.getToken(config.provider.asTokenKey())
            ?: run {
                emit(errorMessage("No API key configured for ${config.provider.displayName}. " +
                    "Please add one in Settings → AI Providers."))
                return@flow
            }

        // RAG retrieval — returns top-k relevant code chunks
        val ragChunks = runCatching {
            ragRepository.retrieve(message, context, topK = 8, minRelevance = 0.2f)
        }.getOrElse {
            Timber.w(it, "RAG retrieval failed — proceeding without context")
            emptyList()
        }

        // Build source references from RAG results
        val sources = ragChunks.map { chunk ->
            SourceReference(
                filePath  = chunk.filePath,
                lineStart = chunk.lineStart,
                lineEnd   = chunk.lineEnd,
                snippet   = chunk.content.take(200),
                relevance = chunk.relevance,
            )
        }

        // Build the prompt
        val turns = buildPrompt(message, context, ragChunks.map { it.content })

        val sessionId  = UUID.randomUUID().toString()
        val messageId  = UUID.randomUUID().toString()
        val startedAt  = System.currentTimeMillis()
        val buf        = StringBuilder()

        // Stream tokens from provider
        client.streamChat(turns, config, apiKey)
            .collect { token ->
                if (token.error != null) {
                    emit(errorMessage(token.error))
                    return@collect
                }
                buf.append(token.delta)
                // Emit incremental update
                emit(
                    ChatMessage(
                        id          = messageId,
                        sessionId   = sessionId,
                        role        = MessageRole.AI,
                        content     = buf.toString(),
                        isStreaming = !token.done,
                        sources     = if (token.done) sources else emptyList(),
                        timestamp   = startedAt,
                    ),
                )
            }
    }

    override suspend fun getAnswer(answerId: String): ChatMessage =
        sessions.values.flatten().find { it.id == answerId }
            ?: ChatMessage(
                id        = answerId,
                sessionId = "unknown",
                role      = MessageRole.AI,
                content   = "",
            )

    override fun getSessionMessages(sessionId: String): Flow<List<ChatMessage>> =
        flowOf(sessions[sessionId] ?: emptyList())

    override suspend fun createSession(context: AIContext): String {
        val id = UUID.randomUUID().toString()
        sessions[id] = mutableListOf()
        return id
    }

    override suspend fun deleteSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildPrompt(
        userMessage: String,
        context: AIContext,
        ragChunks: List<String>,
    ): List<ChatTurn> {
        val systemPrompt = buildString {
            appendLine("You are DevOS AI — an expert Android development assistant.")
            appendLine("You help developers understand, debug, and improve their code.")
            appendLine("Always ground your answers in the provided code context.")
            appendLine("Be concise, precise, and use Markdown formatting in your responses.")
            if (ragChunks.isNotEmpty()) {
                appendLine("\n## Relevant code context")
                ragChunks.forEachIndexed { i, chunk ->
                    appendLine("\n### Chunk ${i + 1}")
                    appendLine("```")
                    appendLine(chunk.take(800))
                    appendLine("```")
                }
            }
        }

        return listOf(
            ChatTurn(TurnRole.SYSTEM, systemPrompt),
            ChatTurn(TurnRole.USER,   userMessage),
        )
    }

    private fun errorMessage(text: String) = ChatMessage(
        id          = UUID.randomUUID().toString(),
        sessionId   = "error",
        role        = MessageRole.AI,
        content     = text,
        isStreaming = false,
    )
}

private fun AIProvider.asTokenKey() = object : com.devos.ai.core.security.TokenKey {
    override val prefKey = this@asTokenKey.prefKey
    override val name    = this@asTokenKey.displayName
}
