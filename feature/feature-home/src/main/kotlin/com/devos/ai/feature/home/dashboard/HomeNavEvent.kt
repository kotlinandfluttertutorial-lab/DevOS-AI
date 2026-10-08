package com.devos.ai.feature.home.dashboard

/**
 * One-shot navigation events emitted by [HomeViewModel] via SharedFlow.
 *
 * Collected by [HomeNavigation] which translates them into NavController calls.
 * The ViewModel never imports NavController.
 */
sealed class HomeNavEvent {
    data object NavigateToSearch : HomeNavEvent()
    data class NavigateToProject(val id: String) : HomeNavEvent()
    data class NavigateToChat(val sessionId: String) : HomeNavEvent()
    data object NavigateToNotifications : HomeNavEvent()
    data object NavigateToProfile : HomeNavEvent()
    data object NavigateToImport : HomeNavEvent()
    data object NavigateToProjectList : HomeNavEvent()
    data object NavigateToAllSessions : HomeNavEvent()
}
