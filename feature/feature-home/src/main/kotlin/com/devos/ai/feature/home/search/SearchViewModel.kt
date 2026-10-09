package com.devos.ai.feature.home.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.core.common.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Search screen.
 *
 * Manages query state, scope filtering, debounced search (300ms), and stub results.
 * TODO(DEVOS-060): wire real search use case.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    companion object {
        private const val DEBOUNCE_MS = 300L
    }

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<SearchNavEvent>()
    val navEvent: SharedFlow<SearchNavEvent> = _navEvent.asSharedFlow()

    private var searchJob: Job? = null
    private var currentScope = "all"

    /**
     * Called every time the query text changes.
     * If empty, transitions to Idle. Otherwise debounces 300ms then searches.
     */
    fun onQueryChange(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Searching
            delay(DEBOUNCE_MS)
            ensureActive()
            performSearch(query, currentScope)
        }
    }

    /**
     * Changes the active scope chip and re-runs the last search if there is a query.
     */
    fun onScopeChange(scopeId: String) {
        currentScope = scopeId
        val state = _uiState.value
        if (state is SearchUiState.Success) {
            viewModelScope.launch { performSearch(state.query, scopeId) }
        }
    }

    /**
     * Tapping a result emits a navigation event.
     */
    fun onResultTap(result: SearchResult) {
        viewModelScope.launch { _navEvent.emit(SearchNavEvent.NavigateToRoute(result.route)) }
    }

    fun onBack() {
        viewModelScope.launch { _navEvent.emit(SearchNavEvent.NavigateBack) }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun performSearch(query: String, scope: String) {
        try {
            val results = withContext(ioDispatcher) { stubResults(query, scope) }
            val semantic = withContext(ioDispatcher) { stubSemanticMatch(query) }
            if (results.values.all { it.isEmpty() } && semantic == null) {
                _uiState.value = SearchUiState.Empty
            } else {
                _uiState.value = SearchUiState.Success(
                    query = query,
                    scope = scope,
                    results = results,
                    semanticMatch = semantic,
                )
            }
        } catch (e: Exception) {
            _uiState.value = SearchUiState.Error(
                message = e.message ?: "Search failed",
                retryable = true,
            )
        }
    }

    // ── Stub data — replace with SearchUseCase after DEVOS-060 ───────────────

    private fun stubResults(
        query: String,
        scope: String,
    ): Map<SearchResultType, List<SearchResult>> {
        val codeResults = listOf(
            SearchResult(
                id = "c1",
                type = SearchResultType.CODE,
                title = "HomeViewModel.kt",
                subtitle = "feature/feature-home/src/main/kotlin/…",
                route = "home",
            ),
            SearchResult(
                id = "c2",
                type = SearchResultType.CODE,
                title = "AISettingsViewModel.kt",
                subtitle = "feature/feature-settings/src/main/kotlin/…",
                route = "settings/ai",
            ),
            SearchResult(
                id = "c3",
                type = SearchResultType.CODE,
                title = "DevOSNavGraph.kt",
                subtitle = "app/src/main/kotlin/com/devos/ai/navigation/…",
                route = "home",
            ),
        )
        val issueResults = listOf(
            SearchResult(
                id = "i1",
                type = SearchResultType.ISSUE,
                title = "Fix navigation memory leak",
                subtitle = "OPEN · DevOS AI",
                route = "home",
            ),
        )
        val learningResults = listOf(
            SearchResult(
                id = "l1",
                type = SearchResultType.LEARNING,
                title = "Kotlin Coroutines in Depth",
                subtitle = "8 lessons · Intermediate",
                route = "learning_dashboard",
            ),
        )

        return when (scope) {
            "code"   -> mapOf(SearchResultType.CODE to codeResults)
            "issues" -> mapOf(SearchResultType.ISSUE to issueResults)
            "learn"  -> mapOf(SearchResultType.LEARNING to learningResults)
            else     -> mapOf(
                SearchResultType.CODE     to codeResults,
                SearchResultType.ISSUE    to issueResults,
                SearchResultType.LEARNING to learningResults,
            )
        }
    }

    private fun stubSemanticMatch(query: String): SemanticMatch? {
        return SemanticMatch(
            text = "Based on your codebase, \"$query\" is used in 3 ViewModels and referenced " +
                "in 7 Compose screens across the project.",
        )
    }
}
