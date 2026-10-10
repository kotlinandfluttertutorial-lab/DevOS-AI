package com.devos.ai.feature.prs

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Pull Request List screen. */
sealed interface PRListUiState {
    data object Loading : PRListUiState

    @Immutable
    data class Success(
        val prs: List<PullRequest>,
        val filter: PRFilter,
    ) : PRListUiState

    data object Empty : PRListUiState

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : PRListUiState
}

/** One-shot navigation events emitted by [PRListViewModel]. */
sealed class PRListNavEvent {
    data object NavigateBack : PRListNavEvent()
    data class NavigateToPRReview(val prId: String) : PRListNavEvent()
}

/**
 * ViewModel for the Pull Request List screen.
 *
 * Loads stub PRs matching the #s-pull-requests mockup. Supports:
 * - Open / Draft / My PRs / Needs Review filter chips
 * - PR tap navigates to AI review screen
 *
 * Navigation events emitted via [SharedFlow] — NavController never imported here.
 *
 * DEVOS-044 / DA-55
 */
@HiltViewModel
class PRListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    @Suppress("unused")
    private val projectId: String = savedStateHandle["projectId"] ?: ""

    private val _uiState = MutableStateFlow<PRListUiState>(PRListUiState.Loading)
    val uiState: StateFlow<PRListUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<PRListNavEvent>()
    val navEvent: SharedFlow<PRListNavEvent> = _navEvent.asSharedFlow()

    private var allPRs: List<PullRequest> = emptyList()
    private var currentFilter: PRFilter = PRFilter.OPEN

    init {
        loadPRs()
    }

    /** Apply selected filter chip. */
    fun onFilterChange(filter: PRFilter) {
        currentFilter = filter
        applyFilter()
    }

    /** Navigate to PR AI review. */
    fun onPRClick(prId: String) {
        viewModelScope.launch { _navEvent.emit(PRListNavEvent.NavigateToPRReview(prId)) }
    }

    /** Navigate back. */
    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(PRListNavEvent.NavigateBack) }
    }

    /** Retry after error. */
    fun retry() {
        _uiState.value = PRListUiState.Loading
        loadPRs()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadPRs() {
        viewModelScope.launch {
            allPRs = buildStubPRs()
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            PRFilter.OPEN -> allPRs.filter { !it.isDraft }
            PRFilter.DRAFT -> allPRs.filter { it.isDraft }
            PRFilter.MY_PRS -> allPRs.filter { it.author == "dev" }
            PRFilter.NEEDS_REVIEW -> allPRs.filter { it.ciStatus == CIStatus.PASSING && it.aiScore >= 80 }
        }
        _uiState.value = if (filtered.isEmpty()) {
            PRListUiState.Empty
        } else {
            PRListUiState.Success(prs = filtered, filter = currentFilter)
        }
    }

    /** Stub PRs matching the #s-pull-requests mockup. */
    private fun buildStubPRs(): List<PullRequest> = listOf(
        PullRequest(
            id = "pr-47",
            number = 47,
            title = "feat: add AI chat context selector",
            fromBranch = "feature/ai-context",
            toBranch = "main",
            author = "dev",
            relativeTime = "2h ago",
            ciStatus = CIStatus.PASSING,
            aiScore = 94,
            aiComment = "🤖 AI: LGTM — clean implementation, good test coverage",
            isDraft = false,
        ),
        PullRequest(
            id = "pr-45",
            number = 45,
            title = "fix: navigation memory leak",
            fromBranch = "fix/nav-leak",
            toBranch = "main",
            author = "dev",
            relativeTime = "2d ago",
            ciStatus = CIStatus.FAILING,
            aiScore = 61,
            aiComment = "🤖 AI: Changes requested — 2 issues found",
            isDraft = true,
        ),
        PullRequest(
            id = "pr-44",
            number = 44,
            title = "feat: streaming response indicator",
            fromBranch = "feature/streaming",
            toBranch = "main",
            author = "dev",
            relativeTime = "4d ago",
            ciStatus = CIStatus.PASSING,
            aiScore = 88,
            aiComment = "",
            isDraft = false,
        ),
    )
}
