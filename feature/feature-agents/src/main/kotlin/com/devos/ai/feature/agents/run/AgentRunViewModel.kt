package com.devos.ai.feature.agents.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.domain.ai.model.AgentRunStatus
import com.devos.ai.domain.ai.usecase.CancelAgentRunUseCase
import com.devos.ai.domain.ai.usecase.RunAgentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * ViewModel for the Agent Run screen (DEVOS-035 / DA-48).
 *
 * Accepts [goal] and optional [repoId] via [SavedStateHandle].
 * Starts the agent run at init and streams live [AgentRun] updates
 * via [RunAgentUseCase] into [uiState].
 *
 * Emits one-shot [AgentRunNavEvent]s for navigation — never imports NavController.
 */
@HiltViewModel
class AgentRunViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runAgentUseCase: RunAgentUseCase,
    private val cancelAgentRunUseCase: CancelAgentRunUseCase,
) : ViewModel() {

    private val goal: String   = savedStateHandle["goal"]   ?: "Analyze repository"
    private val repoId: String? = savedStateHandle["repoId"]

    private val _uiState   = MutableStateFlow<AgentRunUiState>(AgentRunUiState.Loading)
    val uiState: StateFlow<AgentRunUiState> = _uiState.asStateFlow()

    private val _navEvent  = MutableSharedFlow<AgentRunNavEvent>()
    val navEvent: SharedFlow<AgentRunNavEvent> = _navEvent.asSharedFlow()

    private var currentRunId: String? = null
    private var elapsedJob: Job? = null
    private val startedAtMs = AtomicLong(0L)

    init {
        startRun()
    }

    private fun startRun() {
        viewModelScope.launch {
            startedAtMs.set(System.currentTimeMillis())
            startElapsedTimer()

            runAgentUseCase(goal, repoId, maxSteps = 20)
                .collect { run ->
                    run ?: return@collect
                    currentRunId = run.id

                    val completedSteps = run.steps.count { it.status.name == "COMPLETED" }
                    val totalSteps     = run.steps.size.coerceAtLeast(1)
                    val progress       = completedSteps.toFloat() / totalSteps.coerceAtLeast(1)
                    val elapsed        = formatElapsed(System.currentTimeMillis() - startedAtMs.get())
                    val stepLabel      = "Step $completedSteps of $totalSteps"

                    _uiState.value = when (run.status) {
                        AgentRunStatus.PENDING, AgentRunStatus.RUNNING ->
                            AgentRunUiState.Running(run, progress, elapsed, stepLabel)

                        AgentRunStatus.COMPLETED ->
                            AgentRunUiState.Completed(run, run.finalAnswer ?: "Task complete.")

                        AgentRunStatus.CANCELLED ->
                            AgentRunUiState.Cancelled(run)

                        AgentRunStatus.FAILED ->
                            AgentRunUiState.Error(run, run.finalAnswer ?: "Agent failed.", retryable = true)
                    }
                }
        }
    }

    fun onCancelRun() {
        viewModelScope.launch {
            currentRunId?.let { cancelAgentRunUseCase(it) }
        }
    }

    fun onStepTap(stepId: String) {
        val runId = currentRunId ?: return
        viewModelScope.launch {
            _navEvent.emit(AgentRunNavEvent.NavigateToToolDetail(runId, stepId))
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch { _navEvent.emit(AgentRunNavEvent.NavigateBack) }
    }

    fun onRetry() {
        _uiState.value = AgentRunUiState.Loading
        startRun()
    }

    override fun onCleared() {
        super.onCleared()
        elapsedJob?.cancel()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun startElapsedTimer() {
        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val current = _uiState.value
                if (current is AgentRunUiState.Running) {
                    val elapsed = formatElapsed(System.currentTimeMillis() - startedAtMs.get())
                    _uiState.value = current.copy(elapsedLabel = elapsed)
                }
            }
        }
    }

    private fun formatElapsed(ms: Long): String {
        val secs = ms / 1_000
        return if (secs < 60) "${secs}s" else "${secs / 60}m ${secs % 60}s"
    }
}
