package com.devos.ai.feature.home.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.core.common.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Notifications screen.
 *
 * Manages filter chip state, mark-all-read, and individual notification tap navigation.
 * Stub data is used until a real notification domain layer is wired (DEVOS-059).
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<NotificationsNavEvent>()
    val navEvent: SharedFlow<NotificationsNavEvent> = _navEvent.asSharedFlow()

    // Keep the full list for filtering without re-loading
    private var allNotifications: List<DevOSNotification> = emptyList()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            try {
                val notifications = withContext(ioDispatcher) { stubNotifications() }
                allNotifications = notifications
                applyFilter(null, notifications)
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(
                    message = e.message ?: "Failed to load notifications",
                    retryable = true,
                )
            }
        }
    }

    /**
     * Changes the active filter chip.
     * Passing null shows all notifications.
     */
    fun onFilterChange(filter: NotificationType?) {
        val current = _uiState.value
        if (current is NotificationsUiState.Success) {
            applyFilter(filter, allNotifications)
        }
    }

    /**
     * Marks all notifications as read.
     */
    fun markAllRead() {
        allNotifications = allNotifications.map { it.copy(isRead = true) }
        val current = _uiState.value
        if (current is NotificationsUiState.Success) {
            applyFilter(current.activeFilter, allNotifications)
        }
    }

    /**
     * Marks a single notification as read and emits a navigation event to its source.
     */
    fun onNotificationTap(id: String) {
        allNotifications = allNotifications.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        val current = _uiState.value
        if (current is NotificationsUiState.Success) {
            applyFilter(current.activeFilter, allNotifications)
        }
        viewModelScope.launch {
            _navEvent.emit(NotificationsNavEvent.NavigateToRoute(route = "home"))
        }
    }

    fun onBack() {
        viewModelScope.launch { _navEvent.emit(NotificationsNavEvent.NavigateBack) }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun applyFilter(filter: NotificationType?, notifications: List<DevOSNotification>) {
        val filtered = if (filter == null) notifications
        else notifications.filter { it.type == filter }

        if (filtered.isEmpty() && notifications.isEmpty()) {
            _uiState.value = NotificationsUiState.Empty
        } else {
            _uiState.value = NotificationsUiState.Success(
                notifications = notifications,
                filteredNotifications = filtered,
                activeFilter = filter,
                unreadCount = notifications.count { !it.isRead },
            )
        }
    }

    // ── Stub data — replace with domain use case after DEVOS-059 ─────────────

    private fun stubNotifications() = listOf(
        DevOSNotification(
            id = "n1",
            type = NotificationType.SECURITY,
            title = "🔒 Security: Critical finding",
            subtitle = "SQL Injection risk in DatabaseHelper.kt · DevOS AI",
            relativeTime = "2h ago",
            isRead = false,
        ),
        DevOSNotification(
            id = "n2",
            type = NotificationType.AI,
            title = "🤖 AI Review completed",
            subtitle = "PR #47 reviewed · Score 94 · Ready to merge",
            relativeTime = "3h ago",
            isRead = false,
        ),
        DevOSNotification(
            id = "n3",
            type = NotificationType.LEARNING,
            title = "🎓 Daily goal complete!",
            subtitle = "You finished 5 lessons today · 12-day streak 🔥",
            relativeTime = "5h ago",
            isRead = false,
        ),
        DevOSNotification(
            id = "n4",
            type = NotificationType.PR,
            title = "🔀 PR #45 CI failed",
            subtitle = "fix/nav-leak · 2 tests failing",
            relativeTime = "1d ago",
            isRead = true,
        ),
        DevOSNotification(
            id = "n5",
            type = NotificationType.TESTING,
            title = "🧪 Coverage dropped",
            subtitle = "Test coverage fell from 78% to 34% · feature-home",
            relativeTime = "2d ago",
            isRead = true,
        ),
    )
}
