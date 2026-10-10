package com.devos.ai.feature.prs

/**
 * Domain models for the Pull Requests screens.
 *
 * DEVOS-044 / DEVOS-045 / DA-55 / DA-57
 */

/** CI pipeline status for a pull request. */
enum class CIStatus { PASSING, FAILING, RUNNING }

/** AI-generated verdict for the PR review. */
enum class PRVerdict { APPROVE, CHANGES_REQUESTED, NEUTRAL }

/** Per-file review status in the AI review. */
enum class FileReviewStatus { NO_ISSUES, HAS_SUGGESTION, HAS_ISSUE }

/** Active filter selection on the PR list. */
enum class PRFilter { OPEN, DRAFT, MY_PRS, NEEDS_REVIEW }

/** A pull request in the repository. */
data class PullRequest(
    val id: String,
    val number: Int,
    val title: String,
    val fromBranch: String,
    val toBranch: String,
    val author: String,
    val relativeTime: String,
    val ciStatus: CIStatus,
    val aiScore: Int,
    val aiComment: String,
    val isDraft: Boolean,
)

/** A single file reviewed by AI in a PR review. */
data class FileReview(
    val fileName: String,
    val additions: Int,
    val deletions: Int,
    val status: FileReviewStatus,
    val description: String,
)

/** A single bullet point in the AI review summary. */
data class ReviewPoint(
    /** Emoji icon prefix — e.g. "✅", "⚠️", "ℹ️" */
    val icon: String,
    val text: String,
)

/** Full AI review for a single pull request. */
data class PRReview(
    val pr: PullRequest,
    val aiScore: Int,
    val verdict: PRVerdict,
    val summaryPoints: List<ReviewPoint>,
    val fileReviews: List<FileReview>,
)
