package com.devos.ai.feature.code.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.code.model.CodeLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState ──────────────────────────────────────────────────────────────────

sealed interface CodeViewerUiState {
    data object Loading : CodeViewerUiState
    data class Success(
        val filePath: String,
        val fileName: String,
        val lines: List<CodeLine>,
        val selectedLine: Int,
        val language: String,
        val showSymbolTooltip: Boolean,
        val tooltipSymbolName: String,
    ) : CodeViewerUiState
    data object Empty : CodeViewerUiState
    data class Error(val message: String, val retryable: Boolean) : CodeViewerUiState
}

enum class AiActionChip(val label: String) {
    EXPLAIN("Explain"),
    DEBUG("Debug"),
    USAGES("Usages"),
    GEN_TESTS("Gen Tests"),
    ASK_AI("Ask AI"),
}

// ── Nav events ────────────────────────────────────────────────────────────────

sealed interface CodeViewerNavEvent {
    data class NavigateToAiChat(val context: String) : CodeViewerNavEvent
    data class NavigateToSymbol(val repoId: String, val symbolId: String) : CodeViewerNavEvent
    data object NavigateBack : CodeViewerNavEvent
    data object NavigateToSearch : CodeViewerNavEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CodeViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repoId: String = savedStateHandle["repoId"] ?: ""
    private val filePath: String = savedStateHandle["path"] ?: "MainViewModel.kt"
    private val highlightLine: Int = savedStateHandle.get<String>("line")?.toIntOrNull() ?: 11

    private val _uiState = MutableStateFlow<CodeViewerUiState>(CodeViewerUiState.Loading)
    val uiState: StateFlow<CodeViewerUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<CodeViewerNavEvent>()
    val navEvent: SharedFlow<CodeViewerNavEvent> = _navEvent.asSharedFlow()

    init {
        loadFile()
    }

    fun onLineTap(lineNumber: Int) {
        val current = _uiState.value as? CodeViewerUiState.Success ?: return
        // Show tooltip for any tap on a line — simulate symbol tooltip
        _uiState.value = current.copy(
            selectedLine = lineNumber,
            showSymbolTooltip = true,
            tooltipSymbolName = "UserRepository",
        )
    }

    fun onDismissTooltip() {
        val current = _uiState.value as? CodeViewerUiState.Success ?: return
        _uiState.value = current.copy(showSymbolTooltip = false)
    }

    fun onAiActionTap(chip: AiActionChip) {
        viewModelScope.launch {
            val context = when (chip) {
                AiActionChip.EXPLAIN -> "Explain this file: $filePath"
                AiActionChip.DEBUG -> "Help debug issues in: $filePath"
                AiActionChip.USAGES -> "Find usages for file: $filePath"
                AiActionChip.GEN_TESTS -> "Generate tests for: $filePath"
                AiActionChip.ASK_AI -> "Ask about: $filePath"
            }
            _navEvent.emit(CodeViewerNavEvent.NavigateToAiChat(context))
        }
    }

    fun onSearchTap() {
        viewModelScope.launch {
            _navEvent.emit(CodeViewerNavEvent.NavigateToSearch)
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _navEvent.emit(CodeViewerNavEvent.NavigateBack)
        }
    }

    fun retry() = loadFile()

    private fun loadFile() {
        viewModelScope.launch {
            _uiState.value = CodeViewerUiState.Loading

            val fileName = filePath.substringAfterLast("/").ifBlank { filePath }
            val lines = stubCodeLines()

            _uiState.value = CodeViewerUiState.Success(
                filePath = filePath,
                fileName = fileName,
                lines = lines,
                selectedLine = highlightLine,
                language = "kotlin",
                showSymbolTooltip = false,
                tooltipSymbolName = "",
            )
        }
    }

    // ── Stub data ────────────────────────────────────────────────────────────

    private fun stubCodeLines(): List<CodeLine> = listOf(
        CodeLine(1, "package com.devos.ai.feature.repository.overview"),
        CodeLine(2, ""),
        CodeLine(3, "import androidx.lifecycle.ViewModel"),
        CodeLine(4, "import androidx.lifecycle.viewModelScope"),
        CodeLine(5, "import dagger.hilt.android.lifecycle.HiltViewModel"),
        CodeLine(6, "import kotlinx.coroutines.flow.MutableStateFlow"),
        CodeLine(7, "import kotlinx.coroutines.flow.StateFlow"),
        CodeLine(8, "import kotlinx.coroutines.launch"),
        CodeLine(9, "import javax.inject.Inject"),
        CodeLine(10, ""),
        CodeLine(11, "@HiltViewModel", isHighlighted = highlightLine == 11),
        CodeLine(12, "class OverviewViewModel @Inject constructor("),
        CodeLine(13, "    private val repository: RepositoryRepository,"),
        CodeLine(14, ") : ViewModel() {"),
        CodeLine(15, ""),
        CodeLine(16, "    private val _uiState = MutableStateFlow<OverviewUiState>("),
        CodeLine(17, "        OverviewUiState.Loading"),
        CodeLine(18, "    )"),
        CodeLine(19, "    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()"),
        CodeLine(20, ""),
        CodeLine(21, "    init {"),
        CodeLine(22, "        loadOverview()"),
        CodeLine(23, "    }"),
        CodeLine(24, ""),
        CodeLine(25, "    private fun loadOverview() {"),
        CodeLine(26, "        viewModelScope.launch {"),
        CodeLine(27, "            _uiState.value = OverviewUiState.Loading"),
        CodeLine(28, "            // TODO: load from repository"),
        CodeLine(29, "        }"),
        CodeLine(30, "    }"),
        CodeLine(31, "}"),
    )
}
