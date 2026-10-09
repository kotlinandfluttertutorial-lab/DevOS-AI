package com.devos.ai.feature.home.search

/**
 * Result type — drives the icon and section header in the results list.
 */
enum class SearchResultType {
    CODE,
    ISSUE,
    LEARNING;

    val displayName: String
        get() = when (this) {
            CODE     -> "Code"
            ISSUE    -> "Issues"
            LEARNING -> "Learning"
        }
}

/**
 * A single search result item.
 *
 * [route] is the nav destination to push when the user taps this result.
 */
data class SearchResult(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val subtitle: String,
    val route: String,
)

/**
 * AI semantic match card shown above keyword results.
 */
data class SemanticMatch(
    val text: String,
)

/**
 * Scope selector chip data.
 */
data class SearchScope(
    val id: String,
    val label: String,
)

val allSearchScopes = listOf(
    SearchScope("all", "All"),
    SearchScope("code", "Code"),
    SearchScope("issues", "Issues"),
    SearchScope("prs", "PRs"),
    SearchScope("learn", "Learn"),
)

/**
 * UiState for [SearchViewModel].
 */
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Searching : SearchUiState
    data class Success(
        val query: String,
        val scope: String,
        val results: Map<SearchResultType, List<SearchResult>>,
        val semanticMatch: SemanticMatch?,
    ) : SearchUiState
    data object Empty : SearchUiState
    data class Error(val message: String, val retryable: Boolean) : SearchUiState
}

/**
 * One-shot navigation events emitted by [SearchViewModel].
 */
sealed interface SearchNavEvent {
    data object NavigateBack : SearchNavEvent
    data class NavigateToRoute(val route: String) : SearchNavEvent
}
