package com.devos.ai.data.ai.agent

import com.devos.ai.data.ai.provider.ChatTurn
import com.devos.ai.data.ai.provider.StreamToken
import com.devos.ai.data.ai.provider.TurnRole
import com.devos.ai.data.ai.repository.AIProviderRepositoryImpl
import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.model.AgentRunStatus
import com.devos.ai.domain.ai.model.AgentStep
import com.devos.ai.domain.ai.model.AgentStepStatus
import com.devos.ai.domain.ai.model.AgentTool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * ReAct (Reasoning + Acting) agent execution engine.
 *
 * ## Loop
 * ```
 * SYSTEM: goal + tool descriptions
 * repeat up to maxSteps:
 *   USER: current observations
 *   ASSISTANT: Thought: ... \n Action: tool_name \n Action Input: {...}
 *   tool result → next observation
 *   if ASSISTANT says "Final Answer:" → done
 * if maxSteps exhausted → emit FAILED
 * ```
 *
 * ## Prompt format
 * We use the classic ReAct prompt format:
 * - `Thought:` — the model's reasoning
 * - `Action:` — the tool name to call
 * - `Action Input:` — JSON args
 * - `Final Answer:` — terminal response
 *
 * ## Cancellation
 * The coroutine context is checked between each step via [kotlinx.coroutines.ensureActive].
 * Callers cancel the coroutine scope to stop the engine.
 */
class ReActEngine @Inject constructor(
    private val providerRepository: AIProviderRepositoryImpl,
    private val toolExecutors: Map<String, @JvmSuppressWildcards AgentToolExecutor>,
) {

    /**
     * Runs the ReAct loop, emitting [AgentRun] updates into [runFlow] until done.
     *
     * @param run      Initial [AgentRun] (PENDING status, empty steps)
     * @param runFlow  MutableStateFlow to publish state updates to observers
     * @param maxSteps Maximum number of tool-calling iterations before giving up
     * @param repoId   Repository context (passed to tool executors)
     */
    suspend fun execute(
        run: AgentRun,
        runFlow: MutableStateFlow<AgentRun>,
        maxSteps: Int,
        repoId: String?,
    ) {
        val config  = providerRepository.getActiveConfig()
        val apiKey  = providerRepository.getApiKey(config.provider)

        if (apiKey == null) {
            runFlow.value = run.copy(
                status      = AgentRunStatus.FAILED,
                finalAnswer = "No API key configured for ${config.provider.displayName}.",
                completedAt = System.currentTimeMillis(),
            )
            return
        }

        val client = providerRepository.getClient(config.provider)
        if (client == null) {
            runFlow.value = run.copy(
                status      = AgentRunStatus.FAILED,
                finalAnswer = "Provider ${config.provider.displayName} is not available.",
                completedAt = System.currentTimeMillis(),
            )
            return
        }

        val conversation = mutableListOf<ChatTurn>()
        conversation.add(ChatTurn(TurnRole.SYSTEM, buildSystemPrompt(run.goal)))

        var currentRun = run.copy(status = AgentRunStatus.RUNNING)
        runFlow.value  = currentRun

        try {
            coroutineScope {
                for (stepIndex in 0 until maxSteps) {
                    ensureActive()

                    // ── THINK step ────────────────────────────────────────────
                    val step = AgentStep(
                        id         = UUID.randomUUID().toString(),
                        index      = stepIndex,
                        thought    = "",
                        toolName   = null,
                        toolInput  = null,
                        toolOutput = null,
                        status     = AgentStepStatus.RUNNING,
                        durationMs = 0L,
                    )
                    currentRun = currentRun.copy(steps = currentRun.steps + step)
                    runFlow.value = currentRun

                    val stepStart = System.currentTimeMillis()
                    val assistantBuffer = StringBuilder()

                    // Stream the model's thought + action decision
                    val userMsg = buildUserMessage(stepIndex, conversation)
                    conversation.add(ChatTurn(TurnRole.USER, userMsg))

                    client.streamChat(conversation, config, apiKey)
                        .collect { token ->
                            ensureActive()
                            if (token.error == null) {
                                assistantBuffer.append(token.delta)
                                if (!token.done) {
                                    // Emit incremental thought update
                                    val updatedStep = step.copy(thought = assistantBuffer.toString())
                                    currentRun = currentRun.copy(
                                        steps = currentRun.steps.dropLast(1) + updatedStep,
                                    )
                                    runFlow.value = currentRun
                                }
                            }
                        }

                    val assistantResponse = assistantBuffer.toString()
                    conversation.add(ChatTurn(TurnRole.ASSISTANT, assistantResponse))

                    // ── Check for Final Answer ────────────────────────────────
                    val finalAnswer = extractFinalAnswer(assistantResponse)
                    if (finalAnswer != null) {
                        val doneStep = step.copy(
                            thought    = assistantResponse,
                            status     = AgentStepStatus.COMPLETED,
                            durationMs = System.currentTimeMillis() - stepStart,
                        )
                        currentRun = currentRun.copy(
                            status      = AgentRunStatus.COMPLETED,
                            finalAnswer = finalAnswer,
                            steps       = currentRun.steps.dropLast(1) + doneStep,
                            completedAt = System.currentTimeMillis(),
                        )
                        runFlow.value = currentRun
                        Timber.d("ReAct: completed in ${stepIndex + 1} steps")
                        return@coroutineScope
                    }

                    // ── Parse Action + Input ──────────────────────────────────
                    val toolName = extractAction(assistantResponse)
                    val toolArgs = extractActionInput(assistantResponse)

                    if (toolName == null) {
                        // Model didn't follow the format — nudge it next turn
                        val nudgeStep = step.copy(
                            thought    = assistantResponse,
                            status     = AgentStepStatus.COMPLETED,
                            durationMs = System.currentTimeMillis() - stepStart,
                        )
                        currentRun = currentRun.copy(
                            steps = currentRun.steps.dropLast(1) + nudgeStep,
                        )
                        runFlow.value = currentRun
                        conversation.add(
                            ChatTurn(
                                TurnRole.USER,
                                "Observation: Please respond with exactly: " +
                                    "Thought: <reasoning>\nAction: <tool_name>\nAction Input: <json>",
                            ),
                        )
                        continue
                    }

                    // ── ACT step ──────────────────────────────────────────────
                    val executor = toolExecutors[toolName]
                    val observation: String
                    val actStatus: AgentStepStatus

                    if (executor == null) {
                        observation = "Error: unknown tool '$toolName'. Available: ${toolExecutors.keys.joinToString()}"
                        actStatus   = AgentStepStatus.FAILED
                    } else {
                        observation = try {
                            executor.execute(toolArgs ?: "{}", repoId)
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) {
                            Timber.w(e, "Tool $toolName threw an exception")
                            "Error: ${e.message}"
                        }
                        actStatus = AgentStepStatus.COMPLETED
                    }

                    val completedStep = step.copy(
                        thought    = assistantResponse,
                        toolName   = toolName,
                        toolInput  = toolArgs?.let { mapOf("args" to it) },
                        toolOutput = observation,
                        status     = actStatus,
                        durationMs = System.currentTimeMillis() - stepStart,
                    )
                    currentRun = currentRun.copy(
                        steps = currentRun.steps.dropLast(1) + completedStep,
                    )
                    runFlow.value = currentRun
                    conversation.add(ChatTurn(TurnRole.USER, "Observation: $observation"))
                }

                // maxSteps exhausted without Final Answer
                currentRun = currentRun.copy(
                    status      = AgentRunStatus.FAILED,
                    finalAnswer = "Agent reached the maximum of $maxSteps steps without " +
                        "completing the task. Please try a more specific goal.",
                    completedAt = System.currentTimeMillis(),
                )
                runFlow.value = currentRun
            }
        } catch (e: CancellationException) {
            runFlow.value = currentRun.copy(
                status      = AgentRunStatus.CANCELLED,
                completedAt = System.currentTimeMillis(),
            )
            Timber.d("ReAct: run ${run.id} cancelled")
        } catch (e: Exception) {
            Timber.e(e, "ReAct: run ${run.id} failed with exception")
            runFlow.value = currentRun.copy(
                status      = AgentRunStatus.FAILED,
                finalAnswer = "Agent failed: ${e.message}",
                completedAt = System.currentTimeMillis(),
            )
        }
    }

    // ── Prompt helpers ────────────────────────────────────────────────────────

    private fun buildSystemPrompt(goal: String): String {
        val toolDescriptions = toolExecutors.values.joinToString("\n") { exec ->
            "- ${exec.tool.name}: ${exec.tool.description}\n  Parameters: ${exec.tool.parametersSchema}"
        }
        return """
            You are DevOS AI Agent — an autonomous coding assistant.
            Your goal: $goal
            
            You have access to the following tools:
            $toolDescriptions
            
            ALWAYS respond in this exact format:
            Thought: <your reasoning about what to do next>
            Action: <tool_name>
            Action Input: <valid JSON matching the tool's parameter schema>
            
            When you have enough information to answer the goal completely, respond:
            Thought: I now have all the information needed.
            Final Answer: <your complete answer to the goal>
            
            NEVER make up file contents. ALWAYS use tools to read real data.
            Repeat Thought/Action/Observation until you can provide a Final Answer.
        """.trimIndent()
    }

    private fun buildUserMessage(stepIndex: Int, conversation: List<ChatTurn>): String {
        return if (stepIndex == 0) "Begin working towards the goal."
        else "Continue working towards the goal based on the above observations."
    }

    // ── Response parsers ──────────────────────────────────────────────────────

    private fun extractFinalAnswer(response: String): String? {
        val marker = "Final Answer:"
        val idx    = response.indexOf(marker)
        return if (idx >= 0) response.substring(idx + marker.length).trim() else null
    }

    private fun extractAction(response: String): String? {
        val line = response.lines().firstOrNull { it.trimStart().startsWith("Action:") }
            ?: return null
        return line.substringAfter("Action:").trim().lowercase().replace(" ", "_")
    }

    private fun extractActionInput(response: String): String? {
        val line = response.lines().firstOrNull { it.trimStart().startsWith("Action Input:") }
            ?: return null
        return line.substringAfter("Action Input:").trim()
    }
}
