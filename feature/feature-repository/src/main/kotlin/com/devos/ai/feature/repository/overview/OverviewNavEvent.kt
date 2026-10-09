package com.devos.ai.feature.repository.overview

import com.devos.ai.feature.repository.model.OverviewTab

/**
 * One-shot navigation events emitted by [OverviewViewModel].
 *
 * The ViewModel never imports [androidx.navigation.NavController]; the
 * navigation function translates these events into NavController calls.
 */
sealed class OverviewNavEvent {
    /** Top-bar back arrow. */
    data object NavigateBack : OverviewNavEvent()

    /**
     * Reserved: emitted when a tab links to a dedicated sub-screen in a later ticket.
     * Not emitted in DEVOS-016 — tab switching is in-page via the HorizontalPager.
     */
    data class NavigateToTab(val tab: OverviewTab) : OverviewNavEvent()

    /** "Git →" see-all link (Overview tab) and the Git tab placeholder action. */
    data object NavigateToGit : OverviewNavEvent()

    /** AI tab placeholder action. */
    data object NavigateToAI : OverviewNavEvent()

    /** Files tab placeholder action. */
    data object NavigateToFiles : OverviewNavEvent()
}
