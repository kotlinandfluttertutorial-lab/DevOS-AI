# Kiro Prompt — DEVOS-055: Developer Memory (Screen + Service)

**Jira:** DEVOS-055 / DEVOS-056  
**Epic:** DEVOS-E09  
**Figma:** FIGMA-32  
**Kiro Spec:** `.kiro/specs/developer-memory/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the Developer Memory module for DevOS AI: the memory management screen where users can view and manage what DevOS AI has learned about them, and the memory service that persists, retrieves, and injects memory into AI context.

**Existing files to read first:**
- `.kiro/specs/developer-memory/core.md`
- `docs/figma/screen-inventory.md` — Screen FIGMA-32
- `docs/figma/component-inventory.md` — DevOSCard, DevOSSearchBar
- `domain/domain-ai/` — existing AI domain models
- `feature/feature-ai-chat/` — AIChatViewModel (memory context injected here)

**Modules:**
- `feature/feature-memory/` — MemoryScreen + MemoryViewModel
- `domain/domain-ai/` — MemoryRepository interface + domain models
- `data/data-ai/` — MemoryRepositoryImpl + MemoryService
- `core/core-database/` — MemoryEntryEntity

**Package:** `com.devos.ai.feature.memory`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- Memory entries are scoped per-user AND per-project — never mix entries across projects unless category is `GLOBAL`.
- Memory entries must NOT contain raw secrets, tokens, or passwords — sanitize before storing.
- Memory is injected into AI context as system-level context — keep each entry ≤ 500 characters to stay within token budgets.
- Expiry policy: entries expire after 90 days of no access unless pinned.

**What to implement:**

### 1. Domain Models

File: `domain/domain-ai/src/main/kotlin/com/devos/ai/domain/ai/memory/`

```kotlin
data class MemoryEntry(
    val id: String,
    val userId: String,
    val projectId: String?,         // null = GLOBAL scope
    val content: String,            // max 500 chars
    val category: MemoryCategory,
    val source: MemorySource,
    val createdAt: Instant,
    val lastAccessedAt: Instant,
    val expiresAt: Instant?,        // null = pinned
    val isPinned: Boolean,
)

enum class MemoryCategory {
    CODE_PREFERENCE,    // "prefers Kotlin coroutines over RxJava"
    DECISION,           // "decided to use Room over Realm on 2025-01-15"
    ARCHITECTURE,       // "project uses Clean Architecture with MVVM"
    STYLE,              // "prefers data classes over sealed classes for DTOs"
    GLOBAL,             // cross-project preferences
}

enum class MemorySource {
    AI_CHAT,            // extracted from conversation
    USER_EXPLICIT,      // user manually added
    CODE_ANALYSIS,      // inferred from code patterns
}

interface MemoryRepository {
    fun observeMemoryEntries(
        userId: String,
        projectId: String? = null,
        query: String = "",
    ): Flow<List<MemoryEntry>>

    suspend fun addEntry(entry: MemoryEntry): Result<Unit>
    suspend fun updateEntry(entryId: String, content: String): Result<Unit>
    suspend fun deleteEntry(entryId: String): Result<Unit>
    suspend fun clearAllEntries(userId: String, projectId: String?): Result<Unit>
    suspend fun pinEntry(entryId: String, pinned: Boolean): Result<Unit>
    fun getMemoryContext(userId: String, projectId: String?): Flow<List<MemoryEntry>>
}
```

### 2. Room Entity (core-database)

```kotlin
@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val projectId: String?,
    val content: String,
    val category: String,
    val source: String,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val expiresAt: Long?,
    val isPinned: Boolean,
)
```

### 3. MemoryScreen (DEVOS-055, FIGMA-32)

Route: `MEMORY`

UiState:
```kotlin
sealed interface MemoryUiState {
    data object Loading : MemoryUiState
    data class Success(
        val entries: List<MemoryEntry>,
        val searchQuery: String,
        val selectedCategory: MemoryCategory?,
        val editingEntry: MemoryEntry?,
    ) : MemoryUiState
    data object Empty : MemoryUiState
    data class Error(val message: String, val retryable: Boolean) : MemoryUiState
}
```

Layout:
```kotlin
Scaffold(
    topBar = {
        Column {
            DevOSTopBar(title = "Developer Memory")
            DevOSSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchChange,
                placeholder = "Search memories…",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.base),
            )
        }
    },
    floatingActionButton = {
        FloatingActionButton(onClick = onAddEntry) {
            Icon(Icons.Outlined.Add, contentDescription = "Add memory entry")
        }
    }
) { padding ->
    when (val s = uiState) {
        is Loading -> DevOSLoadingState()
        is Empty   -> DevOSEmptyState(
            icon = Icons.Outlined.Psychology,
            title = "No memories yet",
            description = "DevOS AI will remember your preferences and decisions as you use it",
        )
        is Error   -> DevOSErrorState(description = s.message,
            onRetry = if (s.retryable) viewModel::retry else null)
        is Success -> Column(Modifier.padding(padding)) {
            // Category filter chips
            LazyRow(Modifier.padding(horizontal = MaterialTheme.spacing.base),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                item {
                    FilterChip(selected = s.selectedCategory == null,
                        onClick = { onCategoryFilter(null) },
                        label = { Text("All") })
                }
                items(MemoryCategory.entries) { cat ->
                    FilterChip(selected = s.selectedCategory == cat,
                        onClick = { onCategoryFilter(cat) },
                        label = { Text(cat.displayName) })
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            LazyColumn {
                items(s.entries, key = { it.id }) { entry ->
                    MemoryEntryCard(
                        entry = entry,
                        onEdit = { onEditEntry(entry) },
                        onDelete = { onDeleteEntry(entry.id) },
                        onPin = { onPinEntry(entry.id, !entry.isPinned) },
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryEntryCard(entry: MemoryEntry, onEdit: () -> Unit,
                    onDelete: () -> Unit, onPin: () -> Unit) {
    DevOSCard {
        Column(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DevOSStatusBadge(entry.category.displayName, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(MaterialTheme.spacing.sm))
                DevOSStatusBadge(entry.source.displayName, MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.weight(1f))
                if (entry.isPinned) {
                    Icon(Icons.Outlined.PushPin, contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.xs))
            Text(entry.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(MaterialTheme.spacing.xs))
            Text(entry.createdAt.toRelativeTimeString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onPin) {
                    Icon(if (entry.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (entry.isPinned) "Unpin" else "Pin")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit memory")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete memory",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
```

Delete action must show `AlertDialog` confirmation before deleting.  
"Clear All" in overflow menu must show confirmation dialog with project scope warning.

### 4. Memory Service (DEVOS-056)

File: `data/data-ai/src/main/kotlin/com/devos/ai/data/ai/memory/MemoryService.kt`

**Memory extraction from AI chat:**
```kotlin
class MemoryExtractionService @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val aiRepository: AIRepository,
) {
    /** Called after each AI chat turn to extract memorable information. */
    suspend fun extractAndStore(
        userId: String,
        projectId: String?,
        userMessage: String,
        aiResponse: String,
    ) {
        // Use AI to identify memorable decisions/preferences in the conversation
        val extractionPrompt = """
            Analyze this conversation turn and identify any developer preferences, 
            architectural decisions, or code style choices worth remembering.
            Return JSON: [{"content": "...", "category": "DECISION|CODE_PREFERENCE|ARCHITECTURE|STYLE"}]
            If nothing memorable, return [].
            
            User: $userMessage
            AI: $aiResponse
        """.trimIndent()

        // Sanitize inputs before sending to AI — strip any secrets patterns
        val sanitized = sanitizeForAI(extractionPrompt)
        // Parse response, create MemoryEntry for each item, store via memoryRepository
    }

    private fun sanitizeForAI(text: String): String {
        // Remove patterns that look like API keys, tokens, passwords
        return text
            .replace(Regex("""[A-Za-z0-9]{32,}"""), "[REDACTED]")  // long tokens
            .replace(Regex("""password\s*=\s*\S+""", IGNORE_CASE), "password=[REDACTED]")
    }
}
```

**Memory context injection into AI chat (`AIChatViewModel`):**
```kotlin
// In AIChatViewModel.sendMessage():
val memoryContext = memoryRepository
    .getMemoryContext(userId, projectId)
    .first()
    .take(10)  // max 10 entries to stay within token budget
    .joinToString("\n") { "- ${it.content}" }

val context = AIContext(
    repositoryContext = ...,
    memoryContext = if (memoryContext.isNotEmpty())
        "Developer preferences and decisions:\n$memoryContext" else null,
)
```

**Expiry cleanup:** `MemoryCleanupWorker` (WorkManager, daily) deletes entries where `expiresAt < now()` and `isPinned = false`.

**Tests:**
- `MemoryRepositoryImplTest`: entries stored per-user-per-project; search query filters correctly; clear all by scope; expiry cleanup removes expired entries; pin prevents deletion
- `MemoryExtractionServiceTest`: decision extracted from conversation; secrets sanitized before storage; non-memorable turn returns empty; stored entry ≤ 500 chars
- `MemoryViewModelTest`: entries load; search debounced; category filter works; delete shows confirmation; edit updates entry
- `MemoryScreenTest`: entries rendered; search bar filters; category chips filter; pin icon state correct; delete dialog appears

**Acceptance Criteria:**
- AC1 (DEVOS-055): Memory screen shows recent decisions, code preferences, and repo-specific memory
- AC2 (DEVOS-055): Search filters entries by content
- AC3 (DEVOS-055): Category filter chips (All/Decision/Code Preference/Architecture/Style/Global) work
- AC4 (DEVOS-055): Entry editing dialog allows updating content
- AC5 (DEVOS-055): Delete shows confirmation dialog; confirmed delete removes entry
- AC6 (DEVOS-055): Pin/unpin entry prevents/allows expiry
- AC7 (DEVOS-055): "Clear All" with confirmation clears entries for current project scope
- AC8 (DEVOS-056): Memory entries persisted in Room and survive app restart
- AC9 (DEVOS-056): Memory entries injected into AI chat system context (verified by AI referencing memory in response)
- AC10 (DEVOS-056): Entries scoped per-user and per-project — switching projects changes memory context
- AC11 (DEVOS-056): Entries expire after 90 days unless pinned (cleanup runs daily via WorkManager)
- AC12 (DEVOS-056): Memory extraction from AI chat identifies decisions and preferences automatically
- AC13 (DEVOS-056): No secrets, tokens, or passwords stored in memory entries (sanitized before storage)
