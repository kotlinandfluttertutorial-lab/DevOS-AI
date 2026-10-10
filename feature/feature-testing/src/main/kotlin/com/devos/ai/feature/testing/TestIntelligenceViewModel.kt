package com.devos.ai.feature.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Test Intelligence screen. */
sealed interface TestIntelligenceUiState {
    data object Loading : TestIntelligenceUiState

    data class Success(
        val coverage: TestCoverage,
    ) : TestIntelligenceUiState

    data object Empty : TestIntelligenceUiState

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : TestIntelligenceUiState
}

/** One-shot navigation events emitted by [TestIntelligenceViewModel]. */
sealed class TestIntelligenceNavEvent {
    data object NavigateBack : TestIntelligenceNavEvent()
    data class NavigateToCodeViewer(val filePath: String) : TestIntelligenceNavEvent()
    data class GenerateTests(val fileName: String) : TestIntelligenceNavEvent()
}

/**
 * ViewModel for [TestIntelligenceScreen].
 *
 * Loads stub coverage data matching the #s-test-intel mockup:
 * - 67% overall coverage, 80% target, 13% gap
 * - 234 passing / 8 failing / 5 flaky
 * - AI suggestion to add tests for AuthViewModel
 * - 2 uncovered files (AuthViewModel.kt, SecurityRepository.kt)
 *
 * Navigation events emitted via [SharedFlow] — NavController never imported here.
 *
 * DEVOS-048 / DA-61
 */
@HiltViewModel
class TestIntelligenceViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<TestIntelligenceUiState>(TestIntelligenceUiState.Loading)
    val uiState: StateFlow<TestIntelligenceUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<TestIntelligenceNavEvent>()
    val navEvent: SharedFlow<TestIntelligenceNavEvent> = _navEvent.asSharedFlow()

    init {
        loadCoverage()
    }

    private fun loadCoverage() {
        viewModelScope.launch {
            val coverage = TestCoverage(
                overallPercent = 67,
                targetPercent = 80,
                passing = 234,
                failing = 8,
                flaky = 5,
                aiSuggestion = "Add tests for AuthViewModel — 0% coverage on 6 functions",
                uncoveredFiles = listOf(
                    UncoveredFile(
                        name = "AuthViewModel.kt",
                        coveragePercent = 0,
                        untestedFunctions = 6,
                    ),
                    UncoveredFile(
                        name = "SecurityRepository.kt",
                        coveragePercent = 12,
                        untestedFunctions = 4,
                    ),
                ),
            )
            _uiState.value = TestIntelligenceUiState.Success(coverage)
        }
    }

    /** Navigate to AI Chat to generate tests for the given file. */
    fun generateTests(fileName: String) {
        viewModelScope.launch {
            _navEvent.emit(TestIntelligenceNavEvent.GenerateTests(fileName))
        }
    }

    /** Navigate to code viewer for the given file path. */
    fun navigateToFile(filePath: String) {
        viewModelScope.launch {
            _navEvent.emit(TestIntelligenceNavEvent.NavigateToCodeViewer(filePath))
        }
    }

    /** Emit navigate-back event. */
    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(TestIntelligenceNavEvent.NavigateBack) }
    }

    /** Retry loading after an error. */
    fun retry() {
        _uiState.value = TestIntelligenceUiState.Loading
        loadCoverage()
    }
}
