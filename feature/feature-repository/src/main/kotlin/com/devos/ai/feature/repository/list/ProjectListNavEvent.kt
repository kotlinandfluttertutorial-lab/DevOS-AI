package com.devos.ai.feature.repository.list

/**
 * One-shot navigation events emitted by [ProjectListViewModel].
 *
 * The ViewModel never imports [androidx.navigation.NavController]; the
 * navigation function translates these events into NavController calls.
 */
sealed class ProjectListNavEvent {
    /** Navigate to the Import Repository screen. */
    data object NavigateToImport : ProjectListNavEvent()

    /** Navigate to the Repository Overview screen for the given repo. */
    data class NavigateToRepo(val repoId: String) : ProjectListNavEvent()
}
