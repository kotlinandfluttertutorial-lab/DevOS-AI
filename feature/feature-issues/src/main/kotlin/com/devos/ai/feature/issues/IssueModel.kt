package com.devos.ai.feature.issues

/**
 * Domain models for the Issues screens.
 *
 * DEVOS-042 / DEVOS-043 / DA-54 / DA-56
 */

/** Open/closed state of an issue. */
enum class IssueState { OPEN, CLOSED }

/** AI-inferred priority level. */
enum class AiPriority { HIGH, MEDIUM, LOW }

/** A GitHub/GitLab issue. */
data class Issue(
    val id: String,
    val number: Int,
    val title: String,
    val body: String,
    val state: IssueState,
    val labels: List<String>,
    val openedAt: String,
    val author: String,
    val commentCount: Int,
    val aiPriority: AiPriority?,
    val aiFixHint: String?,
)

/** Active filter state for the issue list. */
data class IssueFilter(
    val showOpen: Boolean = true,
    val myIssues: Boolean = false,
    val labelFilter: String? = null,
)
