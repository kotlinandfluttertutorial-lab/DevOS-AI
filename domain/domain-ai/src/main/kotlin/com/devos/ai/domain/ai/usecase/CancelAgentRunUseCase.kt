package com.devos.ai.domain.ai.usecase

import com.devos.ai.domain.ai.repository.AgentRepository
import javax.inject.Inject

/** Cancels the in-flight agent run with [runId]. Idempotent. */
class CancelAgentRunUseCase @Inject constructor(
    private val agentRepository: AgentRepository,
) {
    suspend operator fun invoke(runId: String) = agentRepository.cancelRun(runId)
}
