package com.devos.ai.feature.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.devos.ai.feature.home.profile.DevOSPlan
import com.devos.ai.feature.home.profile.ProfileNavEvent
import com.devos.ai.feature.home.profile.ProfileUiState
import com.devos.ai.feature.home.profile.ProfileViewModel
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
 * Unit tests for [ProfileViewModel].
 *
 * Covers: profile load, stub data assertions, and sign-out navigation event.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ProfileViewModel(ioDispatcher = testDispatcher)

    // ── Profile load ──────────────────────────────────────────────────────────

    @Test
    fun `initial state loads profile as Success`() = runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(ProfileUiState.Success::class)
    }

    @Test
    fun `profile has correct stub name`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as ProfileUiState.Success
        assertThat(state.profile.name).isEqualTo("Firoj Mohammad")
    }

    @Test
    fun `profile has PRO plan`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as ProfileUiState.Success
        assertThat(state.profile.plan).isEqualTo(DevOSPlan.PRO)
    }

    @Test
    fun `profile has GitHub and GitLab handles`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as ProfileUiState.Success
        assertThat(state.profile.githubHandle != null).isEqualTo(true)
        assertThat(state.profile.gitlabHandle != null).isEqualTo(true)
    }

    @Test
    fun `profile has 247 AI sessions`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as ProfileUiState.Success
        assertThat(state.profile.aiSessions).isEqualTo(247)
    }

    @Test
    fun `profile has 12-day streak`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as ProfileUiState.Success
        assertThat(state.profile.streak).isEqualTo(12)
    }

    // ── Sign out ──────────────────────────────────────────────────────────────

    @Test
    fun `onSignOut emits NavigateToLogin nav event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onSignOut()
            assertThat(awaitItem()).isEqualTo(ProfileNavEvent.NavigateToLogin)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onBack emits NavigateBack nav event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onBack()
            assertThat(awaitItem()).isEqualTo(ProfileNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
