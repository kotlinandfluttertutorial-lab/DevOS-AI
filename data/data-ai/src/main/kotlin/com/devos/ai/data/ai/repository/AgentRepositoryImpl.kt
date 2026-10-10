package com.devos.ai.data.ai.repository

import com.devos.ai.data.ai.agent.AgentToolExecutor
import com.devos.ai.data.ai.agent.ReActEngine
import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.model.AgentRunStatus
import com.devos.ai.domain.ai.model.AgentTool
import com.devos.ai.domain.ai.repository.AgentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [AgentRepository].
 *
 * ## Storage
 * Runs are stored in-memory using a [ConcurrentHashMap] of [MutableStateFlow]s.
 * Each active run has a dedicated [CoroutineScope] that owns the [ReActEngine]
 * coroutine — cancelling the scope stops the engine cleanly.
 *
 * TODO(DEVOS-056): persist runs to Room so they survive app restarts.
 *
 * ## Cancellation
 * [cancelRun] cancels the per-run [CoroutineScope]. The engine catches
 * [CancellationException] and marks the run CANCELLED before exiting.
 */
@Singleton
class AgentRepositoryImpl @Inject constructor(
    private val engine: ReActEngine,
    private val toolExecutors: Map<String, @JvmSuppressWildcards AgentToolExecutor>,
) : AgentRepository {

    /** Live state for each run, keyed by runId. */
    private val runs = ConcurrentHashMap<String, MutableStateFlow<AgentRun>>()

    /** Per-run coroutine scopes — cancelled to stop the engine. */
    private val runScopes = ConcurrentHashMap<String, CoroutineScope>()

    // ── Start ─────────────────────────────────────────────────────────────────

    override suspend fun startRun(
        goal: String,
        repoId: String?,
        maxSteps: Int,
    ): String {
        val runId = UUID.randomUUID().toString()
        val run   = AgentRun(
            id     = runId,
            goal   = goal,
            repoId = repoId,
            status = AgentRunStatus.PENDING,
        )

        val runFlow = MutableStateFlow(run)
        runs[runId] = runFlow

        // Each run gets its own SupervisorJob scope so cancelling one run
        // doesn't affect others.
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        runScopes[runId] = scope

        scope.launch {
            try {
                engine.execute(run, runFlow, maxSteps, repoId)
            } finally {
                runScopes.remove(runId)
            }
        }

        Timber.d("AgentRepository: started run %s for goal: %s", runId, goal.take(60))
        return runId
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    override fun observeRun(runId: String): Flow<AgentRun?> =
        runs[runId]?.asStateFlow() ?: flow { emit(null) }

    override fun observeAllRuns(): Flow<List<AgentRun>> {
        // Merge all run flows into a single sorted list
        // Simple implementation: re-emit on any change using a combined flow
        val allIds = runs.keys().toList()
        return flow {
            while (true) {
                val snapshot = runs.values.map { it.value }.sortedByDescending { it.startedAt }
                emit(snapshot)
                kotlinx.coroutines.delay(500) // Poll every 500 ms — good enough for the UI
            }
        }
    }

    // ── Get ───────────────────────────────────────────────────────────────────

    override suspend fun getRun(runId: String): AgentRun? = runs[runId]?.value

    // ── Cancel ────────────────────────────────────────────────────────────────

    override suspend fun cancelRun(runId: String) {
        val scope = runScopes[runId] ?: return
        scope.cancel("User requested cancellation")
        Timber.d("AgentRepository: cancelled run %s", runId)
    }

    // ── Tools ─────────────────────────────────────────────────────────────────

    override fun getRegisteredTools(): List<AgentTool> =
        toolExecutors.values.map { it.tool }
}
