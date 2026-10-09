package com.devos.ai.data.ai.agent

import com.devos.ai.domain.ai.model.AgentTool

/**
 * Contract for a single executable agent tool.
 *
 * Each implementation handles one [AgentTool.name] and:
 * - Validates its JSON arguments string
 * - Executes the operation (file read, symbol search, etc.)
 * - Returns a plain-text observation string that the engine feeds back into
 *   the next Thought step
 *
 * Implementations are registered in [AgentModule] via a multibinding map
 * keyed by [AgentTool.name].
 */
interface AgentToolExecutor {

    /** The [AgentTool] this executor handles. */
    val tool: AgentTool

    /**
     * Executes the tool with the provided JSON argument string.
     *
     * @param argsJson  JSON object matching [AgentTool.parametersSchema]
     * @param repoId    Active repository context (may be null for global tools)
     * @return          Plain-text observation that the engine appends to the
     *                  conversation as a "Tool result" message
     */
    suspend fun execute(argsJson: String, repoId: String?): String
}
