package com.devos.ai.domain.ai.model

/**
 * A single message in an AI conversation.
 *
 * Sources are populated on AI messages when the answer is grounded in code.
 * AgentSteps are populated when the AI used tool-calling to produce the answer.
 */
data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val sources: List<SourceReference> = emptyList(),
    val agentSteps: List<AgentStep> = emptyList(),
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class MessageRole { USER, AI, SYSTEM }

data class SourceReference(
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val snippet: String,
    val relevance: Float,    // 0.0–1.0 cosine similarity score
)

data class AgentStep(
    val id: String,
    val index: Int,
    val thought: String,
    val toolName: String?,
    val toolInput: Map<String, Any>?,
    val toolOutput: String?,
    val status: AgentStepStatus,
    val durationMs: Long,
)

enum class AgentStepStatus { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }
