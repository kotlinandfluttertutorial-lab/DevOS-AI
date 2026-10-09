package com.devos.ai.feature.learning

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.devos.ai.feature.learning.dashboard.LearningDashboardNavEvent
import com.devos.ai.feature.learning.dashboard.LearningDashboardUiState
import com.devos.ai.feature.learning.dashboard.LearningDashboardViewModel
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
 * Unit tests for [LearningDashboardViewModel].
 *
 * Uses [UnconfinedTestDispatcher] for eager coroutine execution.
 * Turbine is used for Flow/SharedFlow assertions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LearningDashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = LearningDashboardViewModel()

    // ── Init / stub data ───────────────────────────────────────────────────────

    @Test
    fun `init loads stub data and emits Success state`() = runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(LearningDashboardUiState.Success::class)
    }

    @Test
    fun `Success state has correct lessonsToday stub value`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        assertThat(state.lessonsToday).isEqualTo(3)
    }

    @Test
    fun `Success state has correct dailyGoal stub value`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        assertThat(state.dailyGoal).isEqualTo(5)
    }

    @Test
    fun `Success state has correct streak stub value`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        assertThat(state.streak).isEqualTo(12)
    }

    @Test
    fun `Success state has current course set`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        assertThat(state.currentCourse).isNotNull()
    }

    @Test
    fun `Success state has 3 recommendations`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        assertThat(state.recommendations.size).isEqualTo(3)
    }

    @Test
    fun `Success state has 2 recent quiz scores`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        assertThat(state.recentScores.size).isEqualTo(2)
    }

    @Test
    fun `dailyFraction computed correctly from lessonsToday and dailyGoal`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        // 3 / 5 = 0.6
        assertThat(state.dailyFraction).isEqualTo(0.6f)
    }

    @Test
    fun `current course has correct title`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        assertThat(state.currentCourse?.title).isEqualTo("Kotlin Coroutines & Flow")
    }

    @Test
    fun `current course fraction is 50 percent`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        val course = state.currentCourse!!
        // 4 completed of 8 = 0.5
        assertThat(course.fraction).isEqualTo(0.5f)
    }

    // ── loadDashboard retry ────────────────────────────────────────────────────

    @Test
    fun `loadDashboard can be called multiple times and returns Success`() = runTest {
        val viewModel = createViewModel()
        viewModel.loadDashboard()
        assertThat(viewModel.uiState.value).isInstanceOf(LearningDashboardUiState.Success::class)
    }

    // ── Navigation events ──────────────────────────────────────────────────────

    @Test
    fun `onContinueCourse emits NavigateToCourse with correct courseId`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        val courseId = state.currentCourse!!.courseId

        viewModel.navEvent.test {
            viewModel.onContinueCourse(courseId)
            val event = awaitItem()
            assertThat(event).isInstanceOf(LearningDashboardNavEvent.NavigateToCourse::class)
            assertThat((event as LearningDashboardNavEvent.NavigateToCourse).courseId).isEqualTo(courseId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecommendationTap emits NavigateToCourse with recommendation id`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as LearningDashboardUiState.Success
        val recId = state.recommendations.first().id

        viewModel.navEvent.test {
            viewModel.onRecommendationTap(recId)
            val event = awaitItem()
            assertThat(event).isInstanceOf(LearningDashboardNavEvent.NavigateToCourse::class)
            assertThat((event as LearningDashboardNavEvent.NavigateToCourse).courseId).isEqualTo(recId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchTap emits NavigateToSearch`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onSearchTap()
            assertThat(awaitItem()).isInstanceOf(LearningDashboardNavEvent.NavigateToSearch::class)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
