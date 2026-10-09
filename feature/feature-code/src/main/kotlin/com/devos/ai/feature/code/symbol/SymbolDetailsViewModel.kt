package com.devos.ai.feature.code.symbol

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.code.model.CodeSymbolUi
import com.devos.ai.feature.code.model.SymbolMethod
import com.devos.ai.feature.code.model.SymbolRef
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

sealed interface SymbolDetailsUiState {
    data object Loading : SymbolDetailsUiState
    data class Success(
        val symbol: CodeSymbolUi,
    ) : SymbolDetailsUiState
    data object Empty : SymbolDetailsUiState
    data class Error(val message: String, val retryable: Boolean) : SymbolDetailsUiState
}

// ── Nav events ────────────────────────────────────────────────────────────────

sealed interface SymbolDetailsNavEvent {
    data class NavigateToCodeViewer(val repoId: String, val filePath: String, val line: Int) : SymbolDetailsNavEvent
    data class NavigateToAiChat(val context: String) : SymbolDetailsNavEvent
    data object NavigateBack : SymbolDetailsNavEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class SymbolDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repoId: String = savedStateHandle["repoId"] ?: ""
    private val symbolId: String = savedStateHandle["symbolId"] ?: ""

    private val _uiState = MutableStateFlow<SymbolDetailsUiState>(SymbolDetailsUiState.Loading)
    val uiState: StateFlow<SymbolDetailsUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<SymbolDetailsNavEvent>()
    val navEvent: SharedFlow<SymbolDetailsNavEvent> = _navEvent.asSharedFlow()

    init {
        loadSymbol()
    }

    fun onFileLinkTap() {
        val current = _uiState.value as? SymbolDetailsUiState.Success ?: return
        viewModelScope.launch {
            _navEvent.emit(
                SymbolDetailsNavEvent.NavigateToCodeViewer(
                    repoId, current.symbol.filePath, current.symbol.lineNumber,
                ),
            )
        }
    }

    fun onReferenceTap(ref: SymbolRef) {
        viewModelScope.launch {
            _navEvent.emit(
                SymbolDetailsNavEvent.NavigateToCodeViewer(repoId, ref.fileName, ref.lineNumber),
            )
        }
    }

    fun onAskAiTap() {
        val current = _uiState.value as? SymbolDetailsUiState.Success ?: return
        viewModelScope.launch {
            _navEvent.emit(
                SymbolDetailsNavEvent.NavigateToAiChat(
                    "Explain symbol: ${current.symbol.name} in ${current.symbol.filePath}",
                ),
            )
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _navEvent.emit(SymbolDetailsNavEvent.NavigateBack)
        }
    }

    fun retry() = loadSymbol()

    private fun loadSymbol() {
        viewModelScope.launch {
            _uiState.value = SymbolDetailsUiState.Loading
            val symbol = stubSymbol()
            if (symbol == null) {
                _uiState.value = SymbolDetailsUiState.Empty
            } else {
                _uiState.value = SymbolDetailsUiState.Success(symbol)
            }
        }
    }

    // ── Stub data ────────────────────────────────────────────────────────────

    private fun stubSymbol(): CodeSymbolUi? = CodeSymbolUi(
        id = symbolId.ifBlank { "stub-symbol-1" },
        name = "OverviewViewModel",
        kind = "class",
        packageName = "com.devos.ai.feature.repository.overview",
        filePath = "feature/feature-repository/src/main/kotlin/com/devos/ai/feature/repository/overview/OverviewViewModel.kt",
        lineNumber = 12,
        signature = "@HiltViewModel\nclass OverviewViewModel @Inject constructor(\n    private val repository: RepositoryRepository,\n) : ViewModel()",
        aiExplanation = "OverviewViewModel manages the UI state for the Repository Overview screen. It follows the MVVM pattern with a clean separation of concerns — it exposes a StateFlow<OverviewUiState> to the Compose UI, handles navigation events via SharedFlow, and delegates data fetching to the RepositoryRepository domain interface injected via Hilt.",
        references = listOf(
            SymbolRef("RepositoryNavigation.kt", 78, "val viewModel: OverviewViewModel = hiltViewModel()"),
            SymbolRef("RepositoryNavigation.kt", 85, "viewModel.navEvent.collect { event ->"),
            SymbolRef("RepositoryOverviewScreen.kt", 34, "// state hoisted from OverviewViewModel"),
        ),
        methods = listOf(
            SymbolMethod("onTabSelect(tab: OverviewTab)", "Switches the active section tab"),
            SymbolMethod("onNavigateBack()", "Emits NavigateBack nav event"),
            SymbolMethod("onGitTap()", "Emits NavigateToGit nav event"),
            SymbolMethod("onAITap()", "Emits NavigateToAI nav event"),
            SymbolMethod("onFilesTap()", "Emits NavigateToFiles nav event"),
            SymbolMethod("onRefresh()", "Reloads repository overview data"),
            SymbolMethod("onRetry()", "Retries after error state"),
        ),
    )
}
