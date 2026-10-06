# Spec: AI Platform — Core

**Jira:** DEVOS-026 / DEVOS-027 / DEVOS-028 / DEVOS-029 / DEVOS-030 / DEVOS-031 / DEVOS-032 / DEVOS-033 / DEVOS-034  
**Epic:** DEVOS-E04  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement the AI chat interface, RAG pipeline, provider abstraction, source evidence, and AI settings.

## Architecture

```
AIChatScreen
    ↓ sendMessage()
AIChatViewModel
    ↓
SendMessageUseCase
    ├── ContextRepository        → current project/repo/file/symbol
    ├── MemoryRepository         → developer memory entries
    ├── RAGRepository.retrieve() → top-k relevant code chunks
    └── AIRepository.stream()    → provider-agnostic streaming
            ├── OpenAIProviderImpl   (GPT-4o, GPT-4)
            ├── AnthropicProviderImpl (Claude 3.5 Sonnet)
            ├── GeminiProviderImpl   (Gemini 1.5 Pro)
            └── OllamaProviderImpl   (local models)
    ↓
Flow<ChatMessage> → incremental UI updates
```

## Domain Models

```kotlin
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

data class SourceReference(
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val snippet: String,
    val relevance: Float,
)

data class CodeChunk(
    val id: String,
    val repoId: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val content: String,
    val language: String,
    val embedding: FloatArray,
)

enum class MessageRole { USER, AI, SYSTEM }
```

## AI Provider Interface

```kotlin
interface AIProvider {
    val name: String
    val supportedModels: List<AIModel>
    fun streamChat(request: ChatRequest): Flow<ChatChunk>
    suspend fun embed(text: String): FloatArray
    suspend fun testConnection(): Boolean
}

data class ChatRequest(
    val messages: List<ChatMessage>,
    val model: String,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.3f,
    val systemPrompt: String,
)
```

## RAG Pipeline

```
Code chunks (files + symbols)
    ↓ chunk(size=512 tokens, overlap=64)
    ↓ embed(provider.embed())
    ↓ store(VectorStore)

Query time:
    query → embed → similaritySearch(topK=10) → rerank → inject into prompt
```

## Screens

### AIChatScreen (FIGMA-16)
- Message list (LazyColumn, reverse scroll)
- Context selector chip row
- Suggested action chips: Explain | Find | Debug | Analyze | Review | Learn | Build
- DevOSChatInput at bottom with safe area padding
- AI messages: DevOSAIMessage with markdown + sources + agent steps
- User messages: right-aligned bubble
- Streaming: animated cursor at end of in-progress message

### AIAnswerDetailScreen (FIGMA-17)
- Full message with markdown
- Evidence list: DevOSSourceReference per chunk
- Call chain / data flow (optional visual)
- Actions: Open source | Explain more | Show dependencies | Ask follow-up

### AISourceEvidenceScreen (FIGMA-18)
- Sources sorted by relevance
- Each source: file path + line range + snippet (3 lines max)
- Tap → CodeViewerScreen at that line

### AISettingsScreen (FIGMA-34)
- Default model selector (per provider)
- RAG settings: chunk size, top-k, min relevance threshold
- Agent max steps
- Memory toggle
- Token usage stats (current month)

### ProviderSettingsScreen (FIGMA-35)
- Provider cards: OpenAI | Anthropic | Gemini | Ollama | Custom
- Per provider: API key (masked) + test connection button
- Endpoint override for Ollama/Custom
- Status badge: Connected | Error | Not configured

## Security
- API keys → EncryptedSharedPreferences only
- Test connection never logs the key
- System prompt sanitizes repository content before injection

## Acceptance Criteria
- [ ] AI chat streams responses character-by-character
- [ ] Context selector changes RAG retrieval scope
- [ ] Sources shown on every grounded answer
- [ ] Agent steps shown inline, expandable
- [ ] All 4 providers configurable
- [ ] Test connection verifies key without exposing it
- [ ] Empty state when no messages yet
- [ ] Error state on provider failure (retryable)
- [ ] RAG retrieves relevant chunks for test queries (precision >70%)
