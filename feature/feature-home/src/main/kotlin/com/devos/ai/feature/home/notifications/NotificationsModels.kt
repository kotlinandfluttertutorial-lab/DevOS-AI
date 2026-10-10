package com.devos.ai.feature.home.notifications

import androidx.compose.runtime.Immutable

/**
 * Notification type — determines the dot color and filter chip label.
 */
enum class NotificationType {
    SECURITY,
    AI,
    LEARNING,
    PR,
    TESTING;

    val displayName: String
        get() = when (this) {
            SECURITY -> "Security"
            AI       -> "AI"
            LEARNING -> "Learning"
            PR       -> "PRs"
            TESTING  -> "Tests"
        }
}

/**
 * A single DevOS notification item.
 *
 * [subtitle] is the secondary body text shown beneath the title.
 * [relativeTime] is a pre-formatted string (e.g. "2h ago", "1d ago").
 * [isRead] drives the unread dot and background tint.
 */
data class DevOSNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val subtitle: String,
    val relativeTime: String,
    val isRead: Boolean,
)

/**
 * UiState for [NotificationsViewModel].
 */
sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    @Immutable
    data class Success(
        val notifications: List<DevOSNotification>,
        val filteredNotifications: List<DevOSNotification>,
        val activeFilter: NotificationType?,
        val unreadCount: Int,
    ) : NotificationsUiState
    data object Empty : NotificationsUiState
    data class Error(val message: String, val retryable: Boolean) : NotificationsUiState
}

/**
 * One-shot navigation events emitted by [NotificationsViewModel].
 */
sealed interface NotificationsNavEvent {
    data object NavigateBack : NotificationsNavEvent
    data class NavigateToRoute(val route: String) : NotificationsNavEvent
}
