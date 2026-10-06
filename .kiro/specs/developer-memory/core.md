# Spec: Developer Memory — Core

**Jira:** DEVOS-055 / DEVOS-056  
**Epic:** DEVOS-E09  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement persistent developer memory: capture, store, retrieve, and surface developer preferences and decisions into AI context.

## Domain Models

```kotlin
data class MemoryEntry(
    val id: String,
    val userId: String,
    val scope: MemoryScope,
    val category: MemoryCategory,
    val content: String,
    val tags: List<String>,
    val repoId: String?,         // null = global
    val projectId: String?,
    val confidence: Float,       // 0.0–1.0
    val source: MemorySource,    // AI_EXTRACTED, USER_CREATED
    val createdAt: Long,
    val lastUsedAt: Long,
    val expiresAt: Long?,        // null = no expiry
)

enum class MemoryScope { GLOBAL, PROJECT, REPOSITORY }

enum class MemoryCategory {
    CODE_PREFERENCE,          // "User prefers Kotlin coroutines over RxJava"
    ARCHITECTURE_DECISION,    // "This project uses Clean Architecture"
    NAMING_CONVENTION,        // "Use camelCase for function names"
    TOOL_PREFERENCE,          // "Prefer MockK for mocking"
    DOMAIN_KNOWLEDGE,         // "AuthViewModel handles OAuth token refresh"
    WORKFLOW_PREFERENCE,      // "Always run tests before committing"
}

enum class MemorySource { AI_EXTRACTED, USER_CREATED }
```

## Memory Service

```kotlin
interface MemoryRepository {
    fun getMemoryEntries(scope: MemoryScope? = null, repoId: String? = null): Flow<List<MemoryEntry>>
    suspend fun getRelevantMemory(query: String, context: AIContext): List<MemoryEntry>
    suspend fun saveEntry(entry: MemoryEntry)
    suspend fun deleteEntry(id: String)
    suspend fun clearAll(scope: MemoryScope?)
}

class MemoryRepositoryImpl @Inject constructor(
    private val memoryDao: MemoryDao,
    private val vectorStore: VectorStore,
) : MemoryRepository {

    // Extract and save memory from conversation
    suspend fun extractFromConversation(messages: List<ChatMessage>) {
        val extracted = memoryExtractor.extract(messages)
        extracted.forEach { entry ->
            memoryDao.insert(entry.toEntity())
            vectorStore.indexEntry(entry)
        }
    }

    override suspend fun getRelevantMemory(query: String, context: AIContext): List<MemoryEntry> {
        val embedding = vectorStore.embed(query)
        val scopeFilter = buildScopeFilter(context)
        return vectorStore.similaritySearch(embedding, topK = 5, filter = scopeFilter)
            .map { it.toMemoryEntry() }
    }
}
```

## Memory Extraction from Conversations

When AI detects patterns in conversation that indicate preferences or decisions, extract them:

```
"I prefer using coroutines"           → CODE_PREFERENCE: "User prefers Kotlin Coroutines"
"We use Clean Architecture here"      → ARCHITECTURE_DECISION: "Project uses Clean Architecture"
"Don't use Mockito, use MockK"        → TOOL_PREFERENCE: "Use MockK for mocking"
```

## Screen: DeveloperMemoryScreen (FIGMA-32)

**States:** Loading | Success | Empty | Error

**Sections:**
1. **Recent Decisions** — last 5 AI-extracted decisions
2. **Code Preferences** — preferences by category
3. **Repository-Specific** — memory scoped to current repo
4. **Global Preferences** — cross-project memory
5. **Search** — search all memory entries

**Per Entry Actions:**
- Edit (user-created entries)
- Delete (with undo snackbar)
- Scope badge: Global | Project | Repository

**Clear All** — confirmation dialog + undo window

## Integration with AI Chat

Memory entries injected into every AI prompt:
```
## Developer Memory
- User prefers Kotlin Coroutines over RxJava
- This repository uses Clean Architecture (MVVM)  
- Test with MockK, not Mockito
```

## Acceptance Criteria
- [ ] Memory entries saved from AI conversations automatically
- [ ] Manual entries creatable from memory screen
- [ ] Entries scoped correctly (global / project / repo)
- [ ] Search finds entries by content
- [ ] Relevant memory injected into AI prompts
- [ ] Delete with undo works
- [ ] Clear all requires confirmation
- [ ] Entries survive app restart (Room persistence)
- [ ] Memory screen loads all states correctly
