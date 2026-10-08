package com.devos.ai.feature.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.devos.ai.feature.home.dashboard.HomeUiState
import com.devos.ai.feature.home.dashboard.HomeViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [HomeViewModel].
 *
 * Uses [StandardTestDispatcher] so all coroutines are advanced explicitly via
 * [advanceUntilIdle]. DataStore is mocked with MockK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // relaxed=true so DataStore.edit (a suspend extension) does not throw
    private val mockDataStore: DataStore<Preferences> = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default stub: no dismissed recommendations
        every { mockDataStore.data } returns flowOf(emptyPreferences())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        dataStore = mockDataStore,
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `initial state is Loading`() {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Loading::class)
    }

    @Test
    fun `loadDashboard emits Success with stub data`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Success::class)
    }

    @Test
    fun `loadDashboard Success state contains 3 stub projects`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value as HomeUiState.Success
        assertThat(state.recentProjects.size).isEqualTo(3)
    }

    @Test
    fun `loadDashboard Success state contains 3 stub recommendations`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value as HomeUiState.Success
        assertThat(state.recommendations.size).isEqualTo(3)
    }

    @Test
    fun `dismissRecommendation removes item from uiState`() = runTest {
        // mockDataStore is relaxed — edit calls do nothing, which is correct for this test
        val viewModel = createViewModel()
        advanceUntilIdle()
        val before = (viewModel.uiState.value as HomeUiState.Success).recommendations

        viewModel.dismissRecommendation(before.first().id)
        advanceUntilIdle()

        val after = (viewModel.uiState.value as HomeUiState.Success).recommendations
        assertThat(after.size).isEqualTo(before.size - 1)
    }

    @Test
    fun `dismissRecommendation with pre-dismissed IDs in DataStore filters them out`() = runTest {
        val prefsWithDismissed: Preferences = mockk()
        every { prefsWithDismissed[any<Preferences.Key<String>>()] } returns "r1"
        every { mockDataStore.data } returns flowOf(prefsWithDismissed)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Success
        assertThat(state.recommendations.none { it.id == "r1" }).isEqualTo(true)
    }

    @Test
    fun `loadDashboard transitions to Error state when DataStore throws`() = runTest {
        every { mockDataStore.data } returns flow { throw RuntimeException("DataStore unavailable") }
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Error::class)
    }

    @Test
    fun `error state is retryable`() = runTest {
        every { mockDataStore.data } returns flow { throw RuntimeException("Network error") }
        val viewModel = createViewModel()
        advanceUntilIdle()
        val error = viewModel.uiState.value as HomeUiState.Error
        assertThat(error.retryable).isEqualTo(true)
    }

    @Test
    fun `retry after error recovers to Success state`() = runTest {
        every { mockDataStore.data } returns flow { throw RuntimeException("Transient error") }
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Error::class)

        // Fix DataStore then retry
        every { mockDataStore.data } returns flowOf(emptyPreferences())
        viewModel.loadDashboard()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Success::class)
    }
}
