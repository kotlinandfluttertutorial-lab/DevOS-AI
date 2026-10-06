---
name: implement-ai-feature
description: Implement an AI-powered feature in DevOS AI including RAG, streaming, context injection, and agent tool calls
---

# Skill: Implement DevOS AI Feature

## When to Use
Use when implementing any feature that involves AI chat, RAG retrieval, agent execution, or AI-grounded answers.

## AI Architecture Overview

```
User Input
    ↓
AIChatViewModel.sendMessage()
    ↓
SendMessageUseCase
    ↓
AIRepository.streamChat()
    ├── ContextRepository.getCurrentContext()   ← project/repo/file/symbol
    ├── MemoryRepository.getRelevantMemory()    ← developer memory
    ├── RAGRepository.retrieve()                ← code chunks (top-k)
    └── AIProviderApi.streamChat()              ← SSE/WS stream
    ↓
Flow<ChatMessage> → ViewModel → UI (incremental updates)
```

## Implementation Steps

### 1. Define the AI Context
```kotlin
sealed class AIContext {
    data object Global : AIContext()
    data class Project(val projectId: String, val name: String) : AIContext()
    data class Repository(val repoId: String, val name: String) : AIContext()
    data class File(val repoId: String, val path: String) : AIContext()
    data class Symbol(val repoId: String, val symbolId: String, val name: String) : AIContext()
}
```

### 2. Implement Streaming in ViewModel
```kotlin
fun sendMessage(text: String) {
    val currentMessages = (_uiState.value as? AIChatUiState.Success)?.messages ?: emptyList()
    val userMessage = ChatMessage(id = uuid(), role = MessageRole.USER, content = text)

    _uiState.value = AIChatUiState.Success(
        messages = currentMessages + userMessage,
        isStreaming = true,
    )

    viewModelScope.launch {
        val streamingMessageId = uuid()
        sendMessageUseCase(text, currentContext)
            .catch { e ->
                _uiState.update { state ->
                    (state as? AIChatUiState.Success)?.copy(
                        isStreaming = false,
                        streamingError = e.message,
                    ) ?: AIChatUiState.Error(e.message ?: "AI error", retryable = true)
                }
            }
            .collect { chunk ->
                _uiState.update { state ->
                    (state as? AIChatUiState.Success)?.let { success ->
                        val updated = success.messages.updateOrAppendStreaming(streamingMessageId, chunk)
                        success.copy(messages = updated, isStreaming = !chunk.isComplete)
                    } ?: state
                }
            }
    }
}
```

### 3. Implement RAG Retrieval
```kotlin
class RAGRepositoryImpl @Inject constructor(
    private val vectorStore: VectorStore,
    private val chunkDao: CodeChunkDao,
) : RAGRepository {

    override suspend fun retrieve(
        query: String,
        context: AIContext,
        topK: Int = 10,
    ): List<CodeChunk> {
        val embedding = vectorStore.embed(query)
        val scopeFilter = when (context) {
            is AIContext.Repository -> "repoId = '${context.repoId}'"
            is AIContext.File       -> "repoId = '${context.repoId}' AND path = '${context.path}'"
            else                   -> null
        }
        return vectorStore.similaritySearch(embedding, topK, scopeFilter)
    }
}
```

### 4. Build Grounded Prompt
```kotlin
fun buildGroundedPrompt(
    userMessage: String,
    context: AIContext,
    chunks: List<CodeChunk>,
    memory: List<MemoryEntry>,
): String = buildString {
    append("You are DevOS AI, an expert developer assistant.\n\n")
    if (memory.isNotEmpty()) {
        append("## Developer Context\n")
        memory.forEach { append("- ${it.content}\n") }
        append("\n")
    }
    if (chunks.isNotEmpty()) {
        append("## Relevant Code\n")
        chunks.forEach { chunk ->
            append("### ${chunk.filePath}:${chunk.lineStart}-${chunk.lineEnd}\n")
            append("```${chunk.language}\n${chunk.content}\n```\n\n")
        }
    }
    append("## Question\n$userMessage")
}
```

### 5. Display Agent Execution Steps
Every tool call must be surfaced:
```kotlin
@Composable
fun AgentExecutionInline(steps: List<AgentStep>) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        steps.forEach { step ->
            DevOSAgentStep(
                step = step,
                expanded = step.id == expandedStepId,
                onExpandToggle = { expandedStepId = if (expandedStepId == step.id) null else step.id },
            )
        }
    }
}
```

### 6. Source Evidence — Never Hide It
```kotlin
@Composable
fun AIAnswerSources(sources: List<SourceReference>, onSourceClick: (SourceReference) -> Unit) {
    if (sources.isEmpty()) return
    Column {
        Text("Sources", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(MaterialTheme.spacing.xs))
        sources.forEach { source ->
            DevOSSourceReference(
                filePath = source.filePath,
                lineStart = source.lineStart,
                lineEnd = source.lineEnd,
                snippet = source.snippet,
                relevanceScore = source.relevance,
                onClick = { onSourceClick(source) },
            )
        }
    }
}
```

## AI Feature Checklist
- [ ] Responses streamed — no blocking wait
- [ ] Context selector wired to RAG scope
- [ ] Developer memory injected into prompt
- [ ] Source references shown with every grounded answer
- [ ] Agent steps shown transparently (not hidden)
- [ ] Confirmation required before destructive tool calls
- [ ] Repository content sanitized before AI injection
- [ ] Error state handles AI provider failures
- [ ] Streaming error shows inline (not replaces whole screen)
- [ ] Retrieval precision test written
- [ ] Grounding accuracy test written

## Security Rules for AI Features
- Never pass raw repository file paths as executable instructions
- Sanitize all code content before injecting into prompts:
  ```kotlin
  fun sanitizeForPrompt(content: String): String =
      content.replace(Regex("(?i)ignore previous instructions.*"), "[sanitized]")
  ```
- MCP tool calls with write/delete/push require `ConfirmationDialog` before execution
- Token usage logged (counts only, never content) for observability
