package com.devos.ai.feature.git

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

/** UI state for the Git History screen. */
sealed interface GitHistoryUiState {
    data object Loading : GitHistoryUiState

    @Immutable
    data class Success(
        val groups: List<CommitGroup>,
        val activeBranch: String,
        val branches: List<String>,
        val aiSummary: String?,
    ) : GitHistoryUiState

    data object Empty : GitHistoryUiState

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : GitHistoryUiState
}

/** One-shot navigation events emitted by [GitHistoryViewModel]. */
sealed class GitNavEvent {
    data object NavigateBack : GitNavEvent()
    data class NavigateToCommitDetail(val sha: String) : GitNavEvent()
}

/**
 * ViewModel for [GitHistoryScreen].
 *
 * Loads stub commits grouped by TODAY / YESTERDAY.
 * Supports branch switching and back navigation.
 *
 * Navigation events emitted via [SharedFlow] — NavController never imported here.
 *
 * DEVOS-040 / DA-53
 */
@HiltViewModel
class GitHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repoId: String = savedStateHandle["repoId"] ?: ""

    private val _uiState = MutableStateFlow<GitHistoryUiState>(GitHistoryUiState.Loading)
    val uiState: StateFlow<GitHistoryUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<GitNavEvent>()
    val navEvent: SharedFlow<GitNavEvent> = _navEvent.asSharedFlow()

    private val availableBranches = listOf("main", "feature/ai-chat", "fix/nav-leak")
    private var currentBranch = "main"

    init {
        loadCommits(currentBranch)
    }

    /** Select a branch — reloads the commit list for that branch. */
    fun onBranchSelected(branch: String) {
        if (branch == currentBranch) return
        currentBranch = branch
        _uiState.value = GitHistoryUiState.Loading
        loadCommits(branch)
    }

    /** Emit navigate-back event. */
    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(GitNavEvent.NavigateBack) }
    }

    /** Emit navigate-to-commit-detail event. */
    fun onCommitClick(sha: String) {
        viewModelScope.launch { _navEvent.emit(GitNavEvent.NavigateToCommitDetail(sha)) }
    }

    /** Retry loading after an error. */
    fun retry() {
        _uiState.value = GitHistoryUiState.Loading
        loadCommits(currentBranch)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadCommits(branch: String) {
        viewModelScope.launch {
            val groups = buildStubCommits(branch)
            _uiState.value = if (groups.isEmpty()) {
                GitHistoryUiState.Empty
            } else {
                GitHistoryUiState.Success(
                    groups = groups,
                    activeBranch = branch,
                    branches = availableBranches,
                    aiSummary = "Last 7 days: 14 commits. Main focus on AI chat feature and " +
                        "bug fixes. Test coverage improved from 58% → 67%.",
                )
            }
        }
    }

    /**
     * Returns stub commit groups matching the #s-git-history mockup.
     * Branch "feature/ai-chat" returns a shorter list to demonstrate branch switching.
     */
    private fun buildStubCommits(branch: String): List<CommitGroup> = when (branch) {
        "feature/ai-chat" -> listOf(
            CommitGroup(
                dateLabel = "TODAY",
                commits = listOf(
                    GitCommit(
                        sha = "a4f2c3d1e5b6",
                        shortSha = "a4f2c3d",
                        message = "feat: AI chat context selector",
                        author = "dev",
                        relativeTime = "2h ago",
                        additions = 127,
                        deletions = 23,
                    ),
                ),
            ),
        )
        "fix/nav-leak" -> listOf(
            CommitGroup(
                dateLabel = "YESTERDAY",
                commits = listOf(
                    GitCommit(
                        sha = "e8c1f9a2d3b4",
                        shortSha = "e8c1f9a",
                        message = "fix: navigation back stack memory leak",
                        author = "dev",
                        relativeTime = "1d ago",
                        additions = 12,
                        deletions = 45,
                    ),
                ),
            ),
        )
        else -> listOf(
            CommitGroup(
                dateLabel = "TODAY",
                commits = listOf(
                    GitCommit(
                        sha = "a4f2c3d1e5b6",
                        shortSha = "a4f2c3d",
                        message = "feat: AI chat context selector",
                        author = "dev",
                        relativeTime = "2h ago",
                        additions = 127,
                        deletions = 23,
                    ),
                    GitCommit(
                        sha = "b7d8e1f2c3a4",
                        shortSha = "b7d8e1f",
                        message = "test: add AIChatViewModel tests",
                        author = "dev",
                        relativeTime = "4h ago",
                        additions = 89,
                        deletions = 0,
                    ),
                ),
            ),
            CommitGroup(
                dateLabel = "YESTERDAY",
                commits = listOf(
                    GitCommit(
                        sha = "e8c1f9a2d3b4",
                        shortSha = "e8c1f9a",
                        message = "fix: navigation back stack memory leak",
                        author = "dev",
                        relativeTime = "1d ago",
                        additions = 12,
                        deletions = 45,
                    ),
                ),
            ),
        )
    }
}
