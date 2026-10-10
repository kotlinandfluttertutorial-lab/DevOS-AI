package com.devos.ai.feature.issues

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

/** UI state for the Issue List screen. */
sealed interface IssueListUiState {
    data object Loading : IssueListUiState

    @Immutable
    data class Success(
        val issues: List<Issue>,
        val filter: IssueFilter,
        val showCloseConfirmation: Boolean = false,
        val issueToClose: String? = null,
    ) : IssueListUiState

    data object Empty : IssueListUiState

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : IssueListUiState
}

/** One-shot navigation events emitted by [IssueListViewModel]. */
sealed class IssueListNavEvent {
    data object NavigateBack : IssueListNavEvent()
    data class NavigateToDetail(val issueId: String) : IssueListNavEvent()
    data object NavigateToCreateIssue : IssueListNavEvent()
}

/**
 * ViewModel for [IssueListScreen].
 *
 * Loads stub issues matching the #s-issues mockup. Supports:
 * - Open/Closed/My Issues/Bug/Feature filter chips
 * - AlertDialog confirmation before closing an issue
 * - Issue detail navigation
 *
 * Navigation events emitted via [SharedFlow] — NavController never imported here.
 *
 * DEVOS-042 / DA-54
 */
@HiltViewModel
class IssueListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    @Suppress("unused")
    private val projectId: String = savedStateHandle["projectId"] ?: ""

    private val _uiState = MutableStateFlow<IssueListUiState>(IssueListUiState.Loading)
    val uiState: StateFlow<IssueListUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<IssueListNavEvent>()
    val navEvent: SharedFlow<IssueListNavEvent> = _navEvent.asSharedFlow()

    private var allIssues: List<Issue> = emptyList()
    private var currentFilter = IssueFilter()

    init {
        loadIssues()
    }

    /** Apply the selected filter. */
    fun onFilterChange(filter: IssueFilter) {
        currentFilter = filter
        applyFilter()
    }

    /** Navigate to issue detail. */
    fun onIssueClick(issueId: String) {
        viewModelScope.launch { _navEvent.emit(IssueListNavEvent.NavigateToDetail(issueId)) }
    }

    /** Show close confirmation dialog. */
    fun requestCloseIssue(issueId: String) {
        val current = _uiState.value
        if (current is IssueListUiState.Success) {
            _uiState.value = current.copy(
                showCloseConfirmation = true,
                issueToClose = issueId,
            )
        }
    }

    /** Dismiss close confirmation dialog without action. */
    fun dismissCloseConfirmation() {
        val current = _uiState.value
        if (current is IssueListUiState.Success) {
            _uiState.value = current.copy(
                showCloseConfirmation = false,
                issueToClose = null,
            )
        }
    }

    /** Confirm close issue — actually closes the issue after confirmation. */
    fun confirmCloseIssue() {
        val current = _uiState.value as? IssueListUiState.Success ?: return
        val idToClose = current.issueToClose ?: return
        allIssues = allIssues.map { issue ->
            if (issue.id == idToClose) issue.copy(state = IssueState.CLOSED) else issue
        }
        _uiState.value = current.copy(showCloseConfirmation = false, issueToClose = null)
        applyFilter()
    }

    /** Navigate to create issue (FAB). */
    fun onCreateIssue() {
        viewModelScope.launch { _navEvent.emit(IssueListNavEvent.NavigateToCreateIssue) }
    }

    /** Navigate back. */
    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(IssueListNavEvent.NavigateBack) }
    }

    /** Retry loading after an error. */
    fun retry() {
        _uiState.value = IssueListUiState.Loading
        loadIssues()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadIssues() {
        viewModelScope.launch {
            allIssues = buildStubIssues()
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = allIssues.filter { issue ->
            val stateMatch = if (currentFilter.showOpen) {
                issue.state == IssueState.OPEN
            } else {
                issue.state == IssueState.CLOSED
            }
            val myIssuesMatch = if (currentFilter.myIssues) {
                issue.author == "dev"
            } else {
                true
            }
            val labelMatch = if (currentFilter.labelFilter != null) {
                issue.labels.any { it.lowercase() == currentFilter.labelFilter?.lowercase() }
            } else {
                true
            }
            stateMatch && myIssuesMatch && labelMatch
        }

        _uiState.value = if (filtered.isEmpty()) {
            IssueListUiState.Empty
        } else {
            IssueListUiState.Success(
                issues = filtered,
                filter = currentFilter,
            )
        }
    }

    /** Stub issues matching the #s-issues mockup. */
    private fun buildStubIssues(): List<Issue> = listOf(
        Issue(
            id = "issue-47",
            number = 47,
            title = "feat: add AI chat context selector",
            body = "We need a context selector in the AI chat screen to allow users to " +
                "select which repository context to use when asking questions.",
            state = IssueState.OPEN,
            labels = listOf("enhancement", "AI"),
            openedAt = "2h ago",
            author = "dev",
            commentCount = 3,
            aiPriority = AiPriority.HIGH,
            aiFixHint = "3 related files",
        ),
        Issue(
            id = "issue-45",
            number = 45,
            title = "Navigation back stack causes memory leak",
            body = """
                ## Problem
                When navigating back after screen rotation, the NavBackStack does not properly
                release ViewModel references, causing a memory leak of approximately 2–3MB per
                rotation cycle.

                ## Steps to Reproduce
                1. Open any screen with a ViewModel
                2. Rotate the device
                3. Navigate back
                4. Repeat 10 times
                5. Monitor heap using Android Studio Profiler

                ## Expected Behavior
                Memory should be released after navigation back.
            """.trimIndent(),
            state = IssueState.OPEN,
            labels = listOf("bug", "P1"),
            openedAt = "1d ago",
            author = "dev",
            commentCount = 7,
            aiPriority = AiPriority.HIGH,
            aiFixHint = "Likely in NavBackStackEntry handling",
        ),
        Issue(
            id = "issue-44",
            number = 44,
            title = "Add streaming response UI indicator",
            body = "The AI chat response currently shows no indicator while streaming. " +
                "We should add a subtle animation to indicate the response is still loading.",
            state = IssueState.OPEN,
            labels = listOf("enhancement"),
            openedAt = "3d ago",
            author = "dev",
            commentCount = 1,
            aiPriority = AiPriority.MEDIUM,
            aiFixHint = null,
        ),
        Issue(
            id = "issue-43",
            number = 43,
            title = "Code block syntax highlighting flickers",
            body = "On certain Android versions, the syntax highlighting in `DevOSCodeBlock` " +
                "flickers when scrolling. This is likely a recomposition issue.",
            state = IssueState.OPEN,
            labels = listOf("bug", "performance"),
            openedAt = "5d ago",
            author = "dev",
            commentCount = 2,
            aiPriority = AiPriority.LOW,
            aiFixHint = null,
        ),
    )
}
