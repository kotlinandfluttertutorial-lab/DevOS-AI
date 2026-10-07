package com.devos.ai.feature.auth

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.devos.ai.feature.auth.onboarding.OnboardingNavEvent
import com.devos.ai.feature.auth.onboarding.OnboardingUiState
import com.devos.ai.feature.auth.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDataStore: FakeDataStore

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDataStore = FakeDataStore()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): OnboardingViewModel = OnboardingViewModel(fakeDataStore)

    @Test
    fun `initial state shows 4 pages starting at page 0`() = runTest {
        val vm = buildViewModel()
        val state = vm.uiState.value as OnboardingUiState.Success
        assertThat(state.pages.size).isEqualTo(4)
        assertThat(state.currentPage).isEqualTo(0)
    }

    @Test
    fun `nextPage increments currentPage`() = runTest {
        val vm = buildViewModel()
        vm.nextPage()
        val state = vm.uiState.value as OnboardingUiState.Success
        assertThat(state.currentPage).isEqualTo(1)
    }

    @Test
    fun `nextPage does not go past last page`() = runTest {
        val vm = buildViewModel()
        repeat(10) { vm.nextPage() }
        val state = vm.uiState.value as OnboardingUiState.Success
        // last index of 4-page list
        assertThat(state.currentPage).isEqualTo(3)
    }

    @Test
    fun `previousPage does not go below 0`() = runTest {
        val vm = buildViewModel()
        vm.previousPage()
        val state = vm.uiState.value as OnboardingUiState.Success
        assertThat(state.currentPage).isEqualTo(0)
    }

    @Test
    fun `skipOnboarding emits ToLogin nav event`() = runTest {
        val vm = buildViewModel()

        vm.navEvent.test {
            vm.skipOnboarding()
            assertThat(awaitItem()).isEqualTo(OnboardingNavEvent.ToLogin)
        }
    }

    @Test
    fun `completeOnboarding emits ToLogin nav event`() = runTest {
        val vm = buildViewModel()
        // Advance to last page
        repeat(3) { vm.nextPage() }

        vm.navEvent.test {
            vm.completeOnboarding()
            assertThat(awaitItem()).isEqualTo(OnboardingNavEvent.ToLogin)
        }
    }

    @Test
    fun `skipOnboarding sets onboarding_complete true in DataStore`() = runTest {
        val vm = buildViewModel()
        vm.skipOnboarding()

        val saved = fakeDataStore.savedPrefs[OnboardingViewModel.KEY_ONBOARDING_COMPLETE]
        assertThat(saved).isEqualTo(true)
    }

    @Test
    fun `completeOnboarding sets onboarding_complete true in DataStore`() = runTest {
        val vm = buildViewModel()
        vm.completeOnboarding()

        val saved = fakeDataStore.savedPrefs[OnboardingViewModel.KEY_ONBOARDING_COMPLETE]
        assertThat(saved).isEqualTo(true)
    }
}

/**
 * Minimal in-memory [DataStore<Preferences>] for tests.
 *
 * Stores the most recent [updateData] result in [savedPrefs] so tests can assert
 * DataStore writes without relying on the real implementation.
 */
private class FakeDataStore : DataStore<Preferences> {

    val savedPrefs = mutableMapOf<Preferences.Key<*>, Any?>()

    private val _data = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> = _data

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val current = _data.value
        val updated = transform(current)
        _data.value = updated
        // Capture written keys for assertion
        updated.asMap().forEach { (key, value) -> savedPrefs[key] = value }
        return updated
    }
}
