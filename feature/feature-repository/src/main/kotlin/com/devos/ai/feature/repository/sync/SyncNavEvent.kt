package com.devos.ai.feature.repository.sync

/**
 * One-shot navigation events emitted by [SyncViewModel].
 * Collected in [RepositoryNavigation] — ViewModel never imports NavController.
 */
sealed class SyncNavEvent {
    /** Navigate to the repository overview screen after a successful sync. */
    data class NavigateToOverview(val repoId: String) : SyncNavEvent()

    /** Navigate back (user tapped Cancel). */
    data object NavigateBack : SyncNavEvent()
}
