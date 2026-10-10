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

/** UI state for the PR AI Review screen. */
sealed interface PRReviewUiState {
    data object Loading : PRReviewUiState

    @Immutable
    data class Success(
        val review: PRReview,
        val showPostConfirmation: Boolean = false,
    ) : PRReviewUiState

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : PRReviewUiState
}

/** One-shot navigation/action events emitted by [PRReviewViewModel]. */
sealed class PRReviewNavEvent {
    data object NavigateBack : PRReviewNavEvent()
    data class CopyReview(val reviewText: String) : PRReviewNavEvent()
    data object PostToGitHub : PRReviewNavEvent()
}

/**
 * ViewModel for the PR AI Review screen.
 *
 * Shows the AI-generated review for PR #47 matching the #s-pr-review mockup.
 * "Post to GitHub" requires user confirmation via an AlertDialog before firing.
 *
 * Navigation/action events emitted via [SharedFlow] — NavController never imported here.
 *
 * DEVOS-045 / DA-57
 */
@HiltViewModel
class PRReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    @Suppress("unused")
    private val prId: String = savedStateHandle["prId"] ?: ""

    private val _uiState = MutableStateFlow<PRReviewUiState>(PRReviewUiState.Loading)
    val uiState: StateFlow<PRReviewUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<PRReviewNavEvent>()
    val navEvent: SharedFlow<PRReviewNavEvent> = _navEvent.asSharedFlow()

    init {
        loadReview()
    }

    /** Navigate back. */
    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(PRReviewNavEvent.NavigateBack) }
    }

    /** Copy the review text to clipboard (emits nav event with review text). */
    fun onCopyReview() {
        val current = _uiState.value as? PRReviewUiState.Success ?: return
        val reviewText = buildReviewText(current.review)
        viewModelScope.launch { _navEvent.emit(PRReviewNavEvent.CopyReview(reviewText)) }
    }

    /**
     * Called when user taps "Post to GitHub".
     * Sets [PRReviewUiState.Success.showPostConfirmation] to true — UI shows AlertDialog.
     * Does NOT post until [confirmPost] is called.
     */
    fun onPostReview() {
        val current = _uiState.value as? PRReviewUiState.Success ?: return
        _uiState.value = current.copy(showPostConfirmation = true)
    }

    /** Confirmed post — dismiss dialog and emit [PRReviewNavEvent.PostToGitHub]. */
    fun confirmPost() {
        val current = _uiState.value as? PRReviewUiState.Success ?: return
        _uiState.value = current.copy(showPostConfirmation = false)
        viewModelScope.launch { _navEvent.emit(PRReviewNavEvent.PostToGitHub) }
    }

    /** User cancelled the post confirmation dialog. */
    fun dismissPost() {
        val current = _uiState.value as? PRReviewUiState.Success ?: return
        _uiState.value = current.copy(showPostConfirmation = false)
    }

    /** Retry after error. */
    fun retry() {
        _uiState.value = PRReviewUiState.Loading
        loadReview()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadReview() {
        viewModelScope.launch {
            _uiState.value = PRReviewUiState.Success(review = buildStubReview())
        }
    }

    /** Formats review data for clipboard copy. */
    private fun buildReviewText(review: PRReview): String = buildString {
        appendLine("AI Code Review — PR #${review.pr.number}: ${review.pr.title}")
        appendLine("Score: ${review.aiScore}/100 (${review.verdict.name})")
        appendLine()
        appendLine("Summary:")
        review.summaryPoints.forEach { appendLine("${it.icon} ${it.text}") }
        appendLine()
        appendLine("Files reviewed (${review.fileReviews.size}):")
        review.fileReviews.forEach { file ->
            appendLine("  ${file.fileName}: +${file.additions} -${file.deletions} — ${file.description}")
        }
    }

    /** Stub PR #47 review matching the #s-pr-review mockup. */
    private fun buildStubReview(): PRReview = PRReview(
        pr = PullRequest(
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
        aiScore = 94,
        verdict = PRVerdict.APPROVE,
        summaryPoints = listOf(
            ReviewPoint("✅", "Clean implementation following MVVM patterns"),
            ReviewPoint("✅", "Proper StateFlow usage for UI state"),
            ReviewPoint("✅", "Good test coverage (89% on new code)"),
            ReviewPoint("⚠️", "Minor: Consider adding @TestOnly annotation to internal functions"),
            ReviewPoint("ℹ️", "Consider extracting context switching logic to a UseCase"),
        ),
        fileReviews = listOf(
            FileReview(
                fileName = "AIChatViewModel.kt",
                additions = 127,
                deletions = 23,
                status = FileReviewStatus.NO_ISSUES,
                description = "Context selector implementation",
            ),
            FileReview(
                fileName = "AIChatScreen.kt",
                additions = 84,
                deletions = 12,
                status = FileReviewStatus.HAS_SUGGESTION,
                description = "UI composable",
            ),
            FileReview(
                fileName = "ContextSelectorDialog.kt",
                additions = 64,
                deletions = 0,
                status = FileReviewStatus.NO_ISSUES,
                description = "New dialog component",
            ),
            FileReview(
                fileName = "GetContextUseCase.kt",
                additions = 35,
                deletions = 5,
                status = FileReviewStatus.HAS_SUGGESTION,
                description = "Business logic layer",
            ),
            FileReview(
                fileName = "AIRepository.kt",
                additions = 28,
                deletions = 14,
                status = FileReviewStatus.NO_ISSUES,
                description = "Repository interface update",
            ),
            FileReview(
                fileName = "AIRepositoryImpl.kt",
                additions = 42,
                deletions = 18,
                status = FileReviewStatus.HAS_ISSUE,
                description = "Repository implementation",
            ),
            FileReview(
                fileName = "AIChatViewModelTest.kt",
                additions = 89,
                deletions = 7,
                status = FileReviewStatus.NO_ISSUES,
                description = "Unit tests",
            ),
            FileReview(
                fileName = "AIChatScreenTest.kt",
                additions = 56,
                deletions = 3,
                status = FileReviewStatus.NO_ISSUES,
                description = "UI tests",
            ),
        ),
    )
}
