package com.devos.ai.feature.chat

import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.model.ChatMessage

/**
 * UI state for [AIChatScreen].
 *
 * Spec: .kiro/specs/ai-platform/core.md
 * Figma: FIGMA-16
 * Kiro prompt: docs/jira/kiro-prompts/DEVOS-026-ai-chat-screen.md
 */
sealed interface AIChatUiState {

    /** Initial load or session restoration in progress. */
    data object Loading : AIChatUiState

    /** Conversation active. May be streaming if [isStreaming] = true. */
    data class Success(
        val messages: List<ChatMessage>,
        val context: AIContext,
        val isStreaming: Boolean = false,
        val streamingError: String? = null,   // inline error, not full-screen
    ) : AIChatUiState

    /** No messages yet — show empty state with suggested prompts. */
    data object Empty : AIChatUiState

    /** Fatal error loading the session. */
    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : AIChatUiState
}
