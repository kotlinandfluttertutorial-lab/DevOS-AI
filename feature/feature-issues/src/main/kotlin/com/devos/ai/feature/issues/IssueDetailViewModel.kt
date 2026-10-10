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

/** A related code reference linked to an issue. */
data class CodeRef(
    val filePath: String,
    val lineNumber: Int,
    val description: String,
)

/** UI state for the Issue Detail screen. */
sealed interface IssueDetailUiState {
    data object Loading : IssueDetailUiState

    @Immutable
    data class Success(
        val issue: Issue,
        val aiSummary: String?,
        val isLoadingAiSummary: Boolean,
        val relatedCodeRefs: List<CodeRef>,
        val showCloseConfirmation: Boolean = false,
    ) : IssueDetailUiState

    data object Empty : IssueDetailUiState

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : IssueDetailUiState
}

/** One-shot navigation events emitted by [IssueDetailViewModel]. */
sealed class IssueDetailNavEvent {
    data object NavigateBack : IssueDetailNavEvent()
    data class NavigateToCodeViewer(val path: String, val line: Int) : IssueDetailNavEvent()
    data object NavigateToAIChat : IssueDetailNavEvent()
}

/**
 * ViewModel for [IssueDetailScreen].
 *
 * Loads the issue identified by `issueId` from SavedStateHandle.
 * Close issue requires AlertDialog confirmation — emits confirmation state rather than navigating.
 *
 * Navigation events emitted via [SharedFlow] — NavController never imported here.
 *
 * DEVOS-043 / DA-56
 */
@HiltViewModel
class IssueDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val issueId: String = savedStateHandle["issueId"] ?: ""

    private val _uiState = MutableStateFlow<IssueDetailUiState>(IssueDetailUiState.Loading)
    val uiState: StateFlow<IssueDetailUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<IssueDetailNavEvent>()
    val navEvent: SharedFlow<IssueDetailNavEvent> = _navEvent.asSharedFlow()

    init {
        loadIssue()
    }

    /** Show the close confirmation dialog. */
    fun requestCloseIssue() {
        val current = _uiState.value as? IssueDetailUiState.Success ?: return
        _uiState.value = current.copy(showCloseConfirmation = true)
    }

    /** Dismiss close confirmation without action. */
    fun dismissCloseConfirmation() {
        val current = _uiState.value as? IssueDetailUiState.Success ?: return
        _uiState.value = current.copy(showCloseConfirmation = false)
    }

    /** Confirm close — marks the issue closed. */
    fun confirmCloseIssue() {
        val current = _uiState.value as? IssueDetailUiState.Success ?: return
        val closedIssue = current.issue.copy(state = IssueState.CLOSED)
        _uiState.value = current.copy(
            issue = closedIssue,
            showCloseConfirmation = false,
        )
    }

    /** Navigate to code viewer for a related code ref. */
    fun onCodeRefClick(ref: CodeRef) {
        viewModelScope.launch {
            _navEvent.emit(IssueDetailNavEvent.NavigateToCodeViewer(ref.filePath, ref.lineNumber))
        }
    }

    /** Navigate to AI chat for fix suggestion. */
    fun onAIFixSuggestion() {
        viewModelScope.launch { _navEvent.emit(IssueDetailNavEvent.NavigateToAIChat) }
    }

    /** Navigate back. */
    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(IssueDetailNavEvent.NavigateBack) }
    }

    /** Retry loading after an error. */
    fun retry() {
        _uiState.value = IssueDetailUiState.Loading
        loadIssue()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadIssue() {
        viewModelScope.launch {
            val issue = findStubIssue(issueId)
            if (issue == null) {
                _uiState.value = IssueDetailUiState.Error(
                    message = "Issue not found: $issueId",
                    retryable = false,
                )
                return@launch
            }

            val codeRefs = when (issueId) {
                "issue-45" -> listOf(
                    CodeRef("DevOSNavGraph.kt", 24, "NavHostController usage"),
                    CodeRef("MainActivity.kt", 38, "NavController retain"),
                )
                else -> emptyList()
            }

            _uiState.value = IssueDetailUiState.Success(
                issue = issue,
                aiSummary = buildAiSummary(issue),
                isLoadingAiSummary = false,
                relatedCodeRefs = codeRefs,
            )
        }
    }

    private fun buildAiSummary(issue: Issue): String = when (issue.id) {
        "issue-45" -> "Root cause likely in `NavHostController` not clearing ViewModelStore on " +
            "config change. Suggest checking `rememberNavController()` lifecycle."
        "issue-47" -> "This feature requires changes to `AIChatScreen` and the `AIChatViewModel`. " +
            "Context selection should be persisted via `EncryptedSharedPreferences`."
        else -> "No additional AI analysis available for this issue."
    }

    private fun findStubIssue(id: String): Issue? = listOf(
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
    ).firstOrNull { it.id == id }
}
