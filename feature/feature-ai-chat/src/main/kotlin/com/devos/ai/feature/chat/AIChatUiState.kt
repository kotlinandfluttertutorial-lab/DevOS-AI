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

    /**
     * No messages yet — show empty state with action chips and suggested prompts.
     *
     * @param context Currently selected AI context
     * @param actionChips Suggested action chips shown in the empty state
     */
    data class Empty(
        val context: AIContext = AIContext.Global,
        val actionChips: List<ActionChip> = defaultActionChips,
    ) : AIChatUiState

    /** Fatal error loading the session. */
    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : AIChatUiState
}

/**
 * A suggested action chip shown when the chat input is empty.
 *
 * Tapping the chip pre-fills the input with [promptPrefix].
 */
data class ActionChip(
    val id: String,
    val label: String,
    val promptPrefix: String,
)

/** Default action chips matching the #s-ai-chat mockup. */
val defaultActionChips: List<ActionChip> = listOf(
    ActionChip(id = "explain",  label = "✨ Explain",  promptPrefix = "Explain "),
    ActionChip(id = "find",     label = "🔍 Find",     promptPrefix = "Find "),
    ActionChip(id = "debug",    label = "🐛 Debug",    promptPrefix = "Debug "),
    ActionChip(id = "analyze",  label = "🔗 Analyze",  promptPrefix = "Analyze "),
    ActionChip(id = "review",   label = "👀 Review",   promptPrefix = "Review "),
)
