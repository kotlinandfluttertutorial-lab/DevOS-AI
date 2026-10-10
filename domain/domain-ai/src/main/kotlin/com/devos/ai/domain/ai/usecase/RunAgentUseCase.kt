package com.devos.ai.domain.ai.usecase

import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.repository.AgentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Starts an agent run and streams live [AgentRun] updates until the run reaches
 * a terminal state (COMPLETED / CANCELLED / FAILED).
 *
 * Usage:
 * ```kotlin
 * runAgentUseCase("Explain the auth module", repoId = "repo-1")
 *     .collect { run -> updateUi(run) }
 * ```
 */
class RunAgentUseCase @Inject constructor(
    private val agentRepository: AgentRepository,
) {
    operator fun invoke(
        goal: String,
        repoId: String? = null,
        maxSteps: Int = 20,
    ): Flow<AgentRun?> =
        kotlinx.coroutines.flow.flow {
            val runId = agentRepository.startRun(goal, repoId, maxSteps)
            agentRepository.observeRun(runId).collect { emit(it) }
        }
}
