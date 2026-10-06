package com.devos.ai.domain.ai.repository

import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for AI operations.
 *
 * Implemented in :data:data-ai via [AIRepositoryImpl].
 * Bound via Hilt in :data:data-ai's AIModule.
 */
interface AIRepository {

    /**
     * Stream a response to [message] given [context].
     * Emits [ChatMessage] chunks with [ChatMessage.isStreaming] = true until the
     * final chunk which has [ChatMessage.isStreaming] = false and populated [ChatMessage.sources].
     */
    fun sendMessage(message: String, context: AIContext): Flow<ChatMessage>

    /** Retrieve a previously completed answer by ID. */
    suspend fun getAnswer(answerId: String): ChatMessage

    /** Get all messages in a session, ordered by timestamp. */
    fun getSessionMessages(sessionId: String): Flow<List<ChatMessage>>

    /** Persist a new session. */
    suspend fun createSession(context: AIContext): String  // returns sessionId

    /** Delete a session and all its messages. */
    suspend fun deleteSession(sessionId: String)
}
