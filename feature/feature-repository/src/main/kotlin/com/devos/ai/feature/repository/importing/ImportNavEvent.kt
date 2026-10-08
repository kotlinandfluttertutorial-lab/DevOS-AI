package com.devos.ai.feature.repository.importing

/**
 * One-shot navigation events emitted by [ImportViewModel].
 * Collected in [RepositoryNavigation] — ViewModel never imports NavController.
 */
sealed class ImportNavEvent {
    /** Navigate to the sync progress screen for the given repository. */
    data class NavigateToSync(val repoId: String) : ImportNavEvent()

    /** Navigate back to the previous screen. */
    data object NavigateBack : ImportNavEvent()
}
