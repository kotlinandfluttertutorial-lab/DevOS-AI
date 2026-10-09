package com.devos.ai.feature.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.devos.ai.feature.home.notifications.NotificationsNavEvent
import com.devos.ai.feature.home.notifications.NotificationsUiState
import com.devos.ai.feature.home.notifications.NotificationsViewModel
import com.devos.ai.feature.home.notifications.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [NotificationsViewModel].
 *
 * Covers: initial state, filter changes, mark-all-read, and navigation event on tap.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = NotificationsViewModel(ioDispatcher = testDispatcher)

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state loads 5 stub notifications`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as NotificationsUiState.Success
        assertThat(state.notifications.size).isEqualTo(5)
    }

    @Test
    fun `initial state has null active filter (All)`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as NotificationsUiState.Success
        assertThat(state.activeFilter).isNull()
    }

    @Test
    fun `initial unread count is 3`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as NotificationsUiState.Success
        assertThat(state.unreadCount).isEqualTo(3)
    }

    // ── Filter chips ──────────────────────────────────────────────────────────

    @Test
    fun `onFilterChange to SECURITY narrows list to 1 item`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(NotificationType.SECURITY)
        val state = viewModel.uiState.value as NotificationsUiState.Success
        assertThat(state.filteredNotifications.size).isEqualTo(1)
        assertThat(state.filteredNotifications.first().type).isEqualTo(NotificationType.SECURITY)
    }

    @Test
    fun `onFilterChange to AI narrows list to 1 item`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(NotificationType.AI)
        val state = viewModel.uiState.value as NotificationsUiState.Success
        assertThat(state.filteredNotifications.size).isEqualTo(1)
        assertThat(state.activeFilter).isEqualTo(NotificationType.AI)
    }

    @Test
    fun `onFilterChange to null restores all notifications`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(NotificationType.SECURITY)
        viewModel.onFilterChange(null)
        val state = viewModel.uiState.value as NotificationsUiState.Success
        assertThat(state.filteredNotifications.size).isEqualTo(5)
    }

    // ── Mark all read ─────────────────────────────────────────────────────────

    @Test
    fun `markAllRead sets unread count to 0`() = runTest {
        val viewModel = createViewModel()
        viewModel.markAllRead()
        val state = viewModel.uiState.value as NotificationsUiState.Success
        assertThat(state.unreadCount).isEqualTo(0)
    }

    @Test
    fun `markAllRead marks all notifications as read`() = runTest {
        val viewModel = createViewModel()
        viewModel.markAllRead()
        val state = viewModel.uiState.value as NotificationsUiState.Success
        assertThat(state.notifications.all { it.isRead }).isEqualTo(true)
    }

    // ── Navigation event on tap ───────────────────────────────────────────────

    @Test
    fun `onNotificationTap emits NavigateToRoute nav event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onNotificationTap("n1")
            assertThat(awaitItem()).isInstanceOf(NotificationsNavEvent.NavigateToRoute::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onNotificationTap marks notification as read`() = runTest {
        val viewModel = createViewModel()
        viewModel.onNotificationTap("n1")
        val state = viewModel.uiState.value as NotificationsUiState.Success
        val notification = state.notifications.first { it.id == "n1" }
        assertThat(notification.isRead).isEqualTo(true)
    }

    @Test
    fun `onBack emits NavigateBack nav event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onBack()
            assertThat(awaitItem()).isEqualTo(NotificationsNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
