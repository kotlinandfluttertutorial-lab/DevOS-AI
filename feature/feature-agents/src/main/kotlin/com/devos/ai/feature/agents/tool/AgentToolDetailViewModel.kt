package com.devos.ai.feature.agents.tool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.domain.ai.model.AgentStepStatus
import com.devos.ai.domain.ai.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

/**
 * ViewModel for the Agent Tool Execution Detail screen (DEVOS-036 / DA-50).
 *
 * Loads the specific [AgentStep] from [AgentRepository] using [runId] and [stepId]
 * from [SavedStateHandle].
 */
@HiltViewModel
class AgentToolDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val agentRepository: AgentRepository,
) : ViewModel() {

    private val runId:  String = checkNotNull(savedStateHandle["runId"])
    private val stepId: String = checkNotNull(savedStateHandle["stepId"])

    private val _uiState  = MutableStateFlow<AgentToolDetailUiState>(AgentToolDetailUiState.Loading)
    val uiState: StateFlow<AgentToolDetailUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ToolDetailNavEvent>()
    val navEvent: SharedFlow<ToolDetailNavEvent> = _navEvent.asSharedFlow()

    init { loadStep() }

    fun onNavigateBack() {
        viewModelScope.launch { _navEvent.emit(ToolDetailNavEvent.NavigateBack) }
    }

    fun onToggleRawJson() {
        val current = _uiState.value as? AgentToolDetailUiState.Success ?: return
        _uiState.value = current.copy(showRawJson = !current.showRawJson)
    }

    private fun loadStep() {
        viewModelScope.launch {
            val run  = agentRepository.getRun(runId)
            val step = run?.steps?.find { it.id == stepId }

            if (step == null) {
                _uiState.value = AgentToolDetailUiState.Error("Step not found.")
                return@launch
            }

            val toolName  = step.toolName ?: "unknown"
            val inputJson = step.toolInput
                ?.let { prettyJson(it) }
                ?: "{}"
            val outputJson = step.toolOutput
                ?.let { prettyOutputJson(it) }
                ?: "{}"
            val isSuccess  = step.status == AgentStepStatus.COMPLETED
            val statusLabel = if (isSuccess) "Done" else step.status.name.lowercase()
                .replaceFirstChar { it.uppercase() }

            _uiState.value = AgentToolDetailUiState.Success(
                toolName        = toolName,
                toolDescription = toolDescription(toolName),
                statusLabel     = statusLabel,
                isSuccess       = isSuccess,
                durationMs      = step.durationMs,
                inputJson       = inputJson,
                outputJson      = outputJson,
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun prettyJson(map: Map<String, Any>): String =
        runCatching { JSONObject(map as Map<*, *>).toString(2) }.getOrDefault("{}")

    private fun prettyOutputJson(text: String): String =
        runCatching { JSONObject(text).toString(2) }
            .getOrElse {
                // Plain text output — wrap in a JSON object for consistency
                JSONObject().put("output", text).toString(2)
            }

    private fun toolDescription(name: String): String = when (name) {
        "read_file"      -> "File reader tool"
        "search_symbols" -> "Symbol search tool"
        "search_code"    -> "Code search tool"
        "list_files"     -> "File listing tool"
        else             -> "$name tool"
    }
}
