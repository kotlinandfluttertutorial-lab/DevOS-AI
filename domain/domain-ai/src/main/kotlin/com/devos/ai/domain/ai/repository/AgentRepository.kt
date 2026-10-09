package com.devos.ai.domain.ai.repository

import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.model.AgentTool
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for agent execution.
 *
 * Implemented in data-ai by [AgentRepositoryImpl].
 * Pure Kotlin — zero Android imports.
 */
interface AgentRepository {

    /** Starts a new agent run for [goal] scoped to [repoId]. Returns the run ID. */
    suspend fun startRun(
        goal: String,
        repoId: String?,
        maxSteps: Int = 20,
    ): String

    /** Emits live updates to [AgentRun] as the engine progresses. */
    fun observeRun(runId: String): Flow<AgentRun?>

    /** Returns a snapshot of [AgentRun] or null if not found. */
    suspend fun getRun(runId: String): AgentRun?

    /** Requests cancellation of [runId]. Idempotent. */
    suspend fun cancelRun(runId: String)

    /** Returns all runs, newest first. */
    fun observeAllRuns(): Flow<List<AgentRun>>

    /** Returns the list of registered tools available to agents. */
    fun getRegisteredTools(): List<AgentTool>
}
