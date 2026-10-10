package com.devos.ai.domain.ai.model

/**
 * A tool that an AI agent can call during a [AgentRun].
 *
 * Tools are registered in [AgentRepository] and exposed to the AI provider in
 * the system prompt. The agent selects a tool name and supplies a JSON argument
 * string; the engine dispatches to the matching implementation.
 *
 * Pure Kotlin — zero Android imports.
 */
data class AgentTool(
    /** Unique snake_case name the AI uses to invoke this tool, e.g. "read_file". */
    val name: String,
    /** Short description injected into the agent's system prompt. */
    val description: String,
    /**
     * JSON Schema string describing the parameters object.
     * The AI must supply valid JSON that conforms to this schema.
     * Example: `{"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}`
     */
    val parametersSchema: String,
    /**
     * When true the engine will surface a confirmation request to the user before
     * executing this tool (satisfies the non-destructive-by-default rule).
     */
    val requiresConfirmation: Boolean = false,
)
