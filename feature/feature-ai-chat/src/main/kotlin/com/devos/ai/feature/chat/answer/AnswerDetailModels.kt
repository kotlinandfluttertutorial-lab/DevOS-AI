package com.devos.ai.feature.chat.answer

/**
 * Data model for a fully-expanded AI answer, including its grounding sources.
 *
 * Used by [AnswerDetailViewModel] / [AIAnswerDetailScreen].
 * Populated from stub data until DEVOS-031 (RAG pipeline) lands.
 *
 * DEVOS-029 / DA-40
 */
data class AnswerDetail(
    val id: String,
    val question: String,
    val answer: String,
    val sources: List<SourceEvidence>,
)

/**
 * A single grounding evidence item: a code snippet from the repository that was
 * retrieved by the RAG pipeline and used to ground the AI answer.
 *
 * DEVOS-029 / DA-40 — DEVOS-030 / DA-43
 */
data class SourceEvidence(
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val packageName: String,
    val snippet: String,
    val relevance: Float,       // 0.0–1.0  (higher = more relevant)
    val language: String,
)

// ── Stub data ─────────────────────────────────────────────────────────────────

/**
 * Stub answer used until the real RAG data layer is wired.
 * Matches the evidence shown in the #s-ai-answer and #s-ai-evidence mockups.
 */
val stubAnswerDetail = AnswerDetail(
    id = "answer-stub-001",
    question = "Explain AIChatViewModel and its dependencies",
    answer = """
**AIChatViewModel** is the UI-layer brain for the chat screen. It's a Hilt-injected ViewModel that:

• Collects streaming AI responses via `Flow`
• Manages chat history persistence
• Handles context switching (repo / file / symbol)

**Dependencies:**

```kotlin
AIChatViewModel
├── SendMessageUseCase
│   └── AIProviderRepository (OpenAI / Anthropic)
├── GetChatHistoryUseCase
│   └── ChatDatabase (Room)
└── SetContextUseCase
    └── ContextRepository
```

The ViewModel exposes a `StateFlow<AIChatUiState>` so the Compose screen observes only the
minimum data it needs, recomposing efficiently when the stream emits new chunks.
""".trimIndent(),
    sources = listOf(
        SourceEvidence(
            filePath = "feature/feature-ai-chat/AIChatViewModel.kt",
            lineStart = 8,
            lineEnd = 17,
            packageName = "com.devos.ai.feature.chat",
            snippet = """@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val getHistory: GetChatHistoryUseCase,
) : ViewModel()""",
            relevance = 0.98f,
            language = "kotlin",
        ),
        SourceEvidence(
            filePath = "domain/domain-ai/usecase/SendMessageUseCase.kt",
            lineStart = 1,
            lineEnd = 42,
            packageName = "com.devos.ai.domain.ai.usecase",
            snippet = """class SendMessageUseCase @Inject constructor(
    private val provider: AIProviderRepository
) {
    suspend fun invoke(msg: String): Flow<String>""",
            relevance = 0.91f,
            language = "kotlin",
        ),
        SourceEvidence(
            filePath = "domain/domain-ai/repository/AIProviderRepository.kt",
            lineStart = 12,
            lineEnd = 28,
            packageName = "com.devos.ai.domain.ai",
            snippet = """interface AIProviderRepository {
    fun chat(messages: List<Message>): Flow<StreamChunk>
    suspend fun embed(text: String): Result<FloatArray>
}""",
            relevance = 0.84f,
            language = "kotlin",
        ),
    ),
)
