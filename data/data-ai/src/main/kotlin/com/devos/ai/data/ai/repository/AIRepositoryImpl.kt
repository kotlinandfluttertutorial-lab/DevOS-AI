package com.devos.ai.data.ai.repository

import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.model.ChatMessage
import com.devos.ai.domain.ai.model.MessageRole
import com.devos.ai.domain.ai.repository.AIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Stub implementation of [AIRepository].
 *
 * Full implementation will be provided in DEVOS-026 (AI Chat feature).
 * This stub allows the app to compile and Hilt graph to resolve.
 */
class AIRepositoryImpl @Inject constructor() : AIRepository {

    override fun sendMessage(message: String, context: AIContext): Flow<ChatMessage> = flow {
        // Stub: emit a placeholder response
        emit(
            ChatMessage(
                id = "stub",
                sessionId = "stub",
                role = MessageRole.AI,
                content = "AI feature not yet implemented.",
                isStreaming = false,
            )
        )
    }

    override suspend fun getAnswer(answerId: String): ChatMessage =
        ChatMessage(
            id = answerId,
            sessionId = "stub",
            role = MessageRole.AI,
            content = "",
        )

    override fun getSessionMessages(sessionId: String): Flow<List<ChatMessage>> = flow {
        emit(emptyList())
    }

    override suspend fun createSession(context: AIContext): String = "stub-session"

    override suspend fun deleteSession(sessionId: String) { /* stub */ }
}
