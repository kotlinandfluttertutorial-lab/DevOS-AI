package com.devos.ai.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.model.ChatMessage
import com.devos.ai.domain.ai.model.MessageRole
import com.devos.ai.domain.ai.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for [AIChatScreen].
 *
 * Manages conversation state, streaming, context selection, and navigation events.
 *
 * State is exposed via [StateFlow<AIChatUiState>]. One-time navigation events are
 * emitted via [SharedFlow<AIChatNavEvent>] so the ViewModel never imports NavController.
 *
 * Spec: .kiro/specs/ai-platform/core.md
 * DEVOS-026 / DA-38
 */
@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIChatUiState>(
        AIChatUiState.Empty(context = AIContext.Global, actionChips = defaultActionChips),
    )
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<AIChatNavEvent>()
    val navEvent: SharedFlow<AIChatNavEvent> = _navEvent.asSharedFlow()

    /** Kept for backwards compat with existing usages that listen to [events]. */
    private val _events = MutableSharedFlow<AIChatEvent>()
    val events: SharedFlow<AIChatEvent> = _events.asSharedFlow()

    private var currentContext: AIContext = AIContext.Global

    // ── Message sending ────────────────────────────────────────────────────────

    /**
     * Send [text] to the AI.
     *
     * Sanitizes input to prevent prompt injection, adds the user bubble immediately,
     * then streams the AI response into the conversation via the [SendMessageUseCase].
     */
    fun sendMessage(text: String) {
        val sanitized = sanitizeForPrompt(text)
        if (sanitized.isBlank()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = "current",
            role = MessageRole.USER,
            content = sanitized,
        )

        val currentMessages = when (val s = _uiState.value) {
            is AIChatUiState.Success -> s.messages
            else -> emptyList()
        }

        _uiState.value = AIChatUiState.Success(
            messages = currentMessages + userMessage,
            context = currentContext,
            isStreaming = true,
        )

        viewModelScope.launch {
            val streamingId = UUID.randomUUID().toString()
            sendMessageUseCase(sanitized, currentContext)
                .catch { e ->
                    _uiState.update { state ->
                        (state as? AIChatUiState.Success)?.copy(
                            isStreaming = false,
                            streamingError = e.message ?: "AI response failed",
                        ) ?: AIChatUiState.Error(e.message ?: "Unknown error")
                    }
                }
                .collect { chunk ->
                    _uiState.update { state ->
                        (state as? AIChatUiState.Success)?.let { success ->
                            val existing = success.messages.find { it.id == streamingId }
                            val updated = if (existing != null) {
                                success.messages.map { msg ->
                                    if (msg.id == streamingId) chunk.copy(id = streamingId)
                                    else msg
                                }
                            } else {
                                success.messages + chunk.copy(id = streamingId)
                            }
                            success.copy(
                                messages = updated,
                                isStreaming = chunk.isStreaming,
                            )
                        } ?: state
                    }
                }
        }
    }

    // ── Context management ─────────────────────────────────────────────────────

    /** Update the AI context scope. The new context is applied to the next sendMessage call. */
    fun setContext(context: AIContext) {
        currentContext = context
        _uiState.update { state ->
            when (state) {
                is AIChatUiState.Success -> state.copy(context = context)
                is AIChatUiState.Empty   -> state.copy(context = context)
                else                     -> state
            }
        }
    }

    // ── Conversation actions ───────────────────────────────────────────────────

    /** Reset the conversation — go back to Empty with default action chips. */
    fun clearConversation() {
        _uiState.value = AIChatUiState.Empty(
            context = currentContext,
            actionChips = defaultActionChips,
        )
    }

    /**
     * Tap an action chip — sends the chip's prompt prefix as the message.
     *
     * The chip's [ActionChip.promptPrefix] is forwarded to [sendMessage] so
     * it gets sanitized and streamed normally.
     */
    fun onActionChipTap(chip: ActionChip) {
        sendMessage(chip.promptPrefix)
    }

    /** Re-send after a streaming error. */
    fun retry() {
        val errorState = _uiState.value as? AIChatUiState.Error ?: return
        _uiState.value = AIChatUiState.Loading
        // Re-load session — full implementation in data layer ticket
    }

    // ── Navigation events ──────────────────────────────────────────────────────

    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(AIChatNavEvent.NavigateBack) }
    }

    fun onAnswerTapped(answerId: String) {
        viewModelScope.launch {
            _navEvent.emit(AIChatNavEvent.NavigateToAnswer(answerId))
            _events.emit(AIChatEvent.NavigateToAnswer(answerId))
        }
    }

    fun onSourceTapped(filePath: String, line: Int) {
        viewModelScope.launch {
            _navEvent.emit(AIChatNavEvent.NavigateToCode(filePath, line))
            _events.emit(AIChatEvent.NavigateToCode(filePath, line))
        }
    }

    fun onAgentRunTapped(runId: String) {
        viewModelScope.launch {
            _navEvent.emit(AIChatNavEvent.NavigateToAgentRun(runId))
            _events.emit(AIChatEvent.NavigateToAgentRun(runId))
        }
    }

    // ── Security ───────────────────────────────────────────────────────────────

    /**
     * Strip common prompt-injection patterns before forwarding user input to AI.
     *
     * Removes: system-override prefixes, repeated control sequences, null-byte
     * injections, and excessively long whitespace runs that could be used to
     * push previous context out of the model's attention window.
     */
    private fun sanitizeForPrompt(input: String): String = input
        .replace(Regex("""(?i)(ignore\s+(previous|all)\s+instructions?)"""), "")
        .replace(Regex("""(?i)(system\s*:)"""), "")
        .replace(Regex("""(?i)(assistant\s*:)"""), "")
        .replace(Regex("""\u0000"""), "")
        .replace(Regex("""\s{10,}"""), " ")
        .trim()
}

/** Legacy event type kept for backwards compat. Use [AIChatNavEvent] for new code. */
sealed interface AIChatEvent {
    data class NavigateToAnswer(val answerId: String) : AIChatEvent
    data class NavigateToCode(val filePath: String, val line: Int) : AIChatEvent
    data class NavigateToAgentRun(val runId: String) : AIChatEvent
}
