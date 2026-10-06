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
 * Spec: .kiro/specs/ai-platform/core.md
 */
@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIChatUiState>(AIChatUiState.Empty)
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AIChatEvent>()
    val events: SharedFlow<AIChatEvent> = _events.asSharedFlow()

    private var currentContext: AIContext = AIContext.Global

    // ── Message sending ────────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = "current",   // TODO: real session ID
            role = MessageRole.USER,
            content = text,
        )

        val currentMessages = (_uiState.value as? AIChatUiState.Success)?.messages ?: emptyList()

        _uiState.value = AIChatUiState.Success(
            messages = currentMessages + userMessage,
            context = currentContext,
            isStreaming = true,
        )

        viewModelScope.launch {
            val streamingId = UUID.randomUUID().toString()
            sendMessageUseCase(text, currentContext)
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
                                success.messages.map {
                                    if (it.id == streamingId) chunk.copy(id = streamingId)
                                    else it
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

    fun setContext(context: AIContext) {
        currentContext = context
        _uiState.update { state ->
            when (state) {
                is AIChatUiState.Success -> state.copy(context = context)
                is AIChatUiState.Empty   -> AIChatUiState.Empty
                else                     -> state
            }
        }
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    fun clearConversation() {
        _uiState.value = AIChatUiState.Empty
    }

    fun retry() {
        // Re-send last user message if state is error
        val errorState = _uiState.value as? AIChatUiState.Error ?: return
        _uiState.value = AIChatUiState.Loading
        // Re-load session — implementation in full feature
    }

    fun onAnswerTapped(answerId: String) {
        viewModelScope.launch {
            _events.emit(AIChatEvent.NavigateToAnswer(answerId))
        }
    }

    fun onSourceTapped(filePath: String, line: Int) {
        viewModelScope.launch {
            _events.emit(AIChatEvent.NavigateToCode(filePath, line))
        }
    }
}

sealed interface AIChatEvent {
    data class NavigateToAnswer(val answerId: String) : AIChatEvent
    data class NavigateToCode(val filePath: String, val line: Int) : AIChatEvent
    data class NavigateToAgentRun(val runId: String) : AIChatEvent
}
