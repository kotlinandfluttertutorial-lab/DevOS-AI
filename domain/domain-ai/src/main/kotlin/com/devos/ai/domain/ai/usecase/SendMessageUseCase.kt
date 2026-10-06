package com.devos.ai.domain.ai.usecase

import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.model.ChatMessage
import com.devos.ai.domain.ai.repository.AIRepository
import com.devos.ai.domain.ai.repository.RAGRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Send a message to the AI with RAG-grounded context.
 *
 * Orchestrates:
 * 1. Context retrieval (RAG chunks)
 * 2. Developer memory injection
 * 3. AI provider streaming
 *
 * Spec: .kiro/specs/ai-platform/core.md
 * Kiro prompt: docs/jira/kiro-prompts/DEVOS-026-ai-chat-screen.md
 */
class SendMessageUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val ragRepository: RAGRepository,
) {
    /**
     * @param message The user's message text
     * @param context The AI context scope (project/repo/file/symbol)
     * @return Flow of [ChatMessage] chunks — stream to update UI incrementally
     */
    operator fun invoke(message: String, context: AIContext): Flow<ChatMessage> =
        aiRepository.sendMessage(message, context)
}
