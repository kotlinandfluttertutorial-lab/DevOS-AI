package com.devos.ai.domain.ai.model

/**
 * Represents a single agent execution session.
 *
 * An [AgentRun] is created by [AgentRepository.startRun] and updated as the
 * [ReActEngine] progresses through its Thought→Action→Observation loop.
 *
 * Pure Kotlin — zero Android imports.
 */
data class AgentRun(
    val id: String,
    val goal: String,
    val repoId: String?,
    val status: AgentRunStatus,
    val steps: List<AgentStep> = emptyList(),
    val finalAnswer: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

enum class AgentRunStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED,
}
