package com.devos.ai.feature.code.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.code.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState ──────────────────────────────────────────────────────────────────

sealed interface CodeSearchUiState {
    data object Idle : CodeSearchUiState
    data object Searching : CodeSearchUiState
    data class Success(
        val results: List<SearchResult>,
        val query: String,
        val totalCount: Int,
    ) : CodeSearchUiState
    data object Empty : CodeSearchUiState
    data class Error(val message: String, val retryable: Boolean) : CodeSearchUiState
}

enum class SearchMode(val label: String) {
    FULL_TEXT("Full Text"),
    SEMANTIC("Semantic"),
}

// ── Nav events ────────────────────────────────────────────────────────────────

sealed interface CodeSearchNavEvent {
    data class NavigateToCodeViewer(val repoId: String, val filePath: String, val line: Int) : CodeSearchNavEvent
    data object NavigateBack : CodeSearchNavEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CodeSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repoId: String = savedStateHandle["repoId"] ?: ""

    private val _query = MutableStateFlow(savedStateHandle.get<String>("q") ?: "")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<CodeSearchUiState>(CodeSearchUiState.Idle)
    val uiState: StateFlow<CodeSearchUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<CodeSearchNavEvent>()
    val navEvent: SharedFlow<CodeSearchNavEvent> = _navEvent.asSharedFlow()

    val searchMode = MutableStateFlow(SearchMode.FULL_TEXT)
    val caseEnabled = MutableStateFlow(false)
    val regexEnabled = MutableStateFlow(false)
    val semanticEnabled = MutableStateFlow(false)

    init {
        observeQueryDebounced()
        // Trigger search if initial query present
        if (_query.value.isNotBlank()) {
            performSearch(_query.value)
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeQueryDebounced() {
        viewModelScope.launch {
            _query
                .debounce(300L)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { q -> performSearch(q) }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _uiState.value = CodeSearchUiState.Idle
        }
    }

    fun onSearchModeChange(mode: SearchMode) {
        searchMode.value = mode
        semanticEnabled.value = mode == SearchMode.SEMANTIC
        val q = _query.value
        if (q.isNotBlank()) performSearch(q)
    }

    fun onCaseToggle() {
        caseEnabled.value = !caseEnabled.value
        val q = _query.value
        if (q.isNotBlank()) performSearch(q)
    }

    fun onRegexToggle() {
        regexEnabled.value = !regexEnabled.value
        val q = _query.value
        if (q.isNotBlank()) performSearch(q)
    }

    fun onSemanticToggle() {
        semanticEnabled.value = !semanticEnabled.value
        searchMode.value = if (semanticEnabled.value) SearchMode.SEMANTIC else SearchMode.FULL_TEXT
        val q = _query.value
        if (q.isNotBlank()) performSearch(q)
    }

    fun onResultTap(result: SearchResult) {
        viewModelScope.launch {
            _navEvent.emit(
                CodeSearchNavEvent.NavigateToCodeViewer(repoId, result.filePath, result.lineNumber),
            )
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _navEvent.emit(CodeSearchNavEvent.NavigateBack)
        }
    }

    fun retry() {
        val q = _query.value
        if (q.isNotBlank()) performSearch(q)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = CodeSearchUiState.Searching

            // Stub results based on query
            val results = stubSearchResults(query)
            if (results.isEmpty()) {
                _uiState.value = CodeSearchUiState.Empty
            } else {
                _uiState.value = CodeSearchUiState.Success(
                    results = results.take(10),
                    query = query,
                    totalCount = results.size,
                )
            }
        }
    }

    // ── Stub data ────────────────────────────────────────────────────────────

    private fun stubSearchResults(query: String): List<SearchResult> {
        val allResults = listOf(
            SearchResult(
                "OverviewViewModel.kt",
                "feature/feature-repository/src/.../OverviewViewModel.kt",
                12, "class OverviewViewModel @Inject constructor(", 6, 22,
            ),
            SearchResult(
                "CodeSearchViewModel.kt",
                "feature/feature-code/src/.../CodeSearchViewModel.kt",
                24, "    private val repository: SymbolRepository,", 20, 36,
            ),
            SearchResult(
                "RepositoryRepository.kt",
                "domain/domain-repository/src/.../RepositoryRepository.kt",
                3, "interface RepositoryRepository {", 10, 28,
            ),
            SearchResult(
                "AIChatViewModel.kt",
                "feature/feature-ai-chat/src/.../AIChatViewModel.kt",
                18, "    private val aiRepository: AIRepository,", 12, 24,
            ),
            SearchResult(
                "DevOSNavGraph.kt",
                "app/src/.../DevOSNavGraph.kt",
                45, "        composable(route = DevOSRoutes.FILE_EXPLORER,", 8, 20,
            ),
            SearchResult(
                "HomeViewModel.kt",
                "feature/feature-home/src/.../HomeViewModel.kt",
                8, "    fun onNavigateToRepository(repoId: String) {", 8, 34,
            ),
            SearchResult(
                "SymbolRepository.kt",
                "domain/domain-repository/src/.../SymbolRepository.kt",
                15, "    fun searchSymbols(query: String,", 4, 17,
            ),
            SearchResult(
                "MainActivity.kt",
                "app/src/.../MainActivity.kt",
                22, "    fun onCreate(savedInstanceState: Bundle?) {", 8, 17,
            ),
        )
        return allResults.filter {
            it.lineContent.contains(query, ignoreCase = !caseEnabled.value) ||
                it.fileName.contains(query, ignoreCase = true)
        }
    }
}
