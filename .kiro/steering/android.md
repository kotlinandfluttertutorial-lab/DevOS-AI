---
inclusion: always
---

# DevOS AI — Android Engineering Standards

## Architecture Rules

### Clean Architecture Layers
```
Compose UI  →  ViewModel  →  UseCase  →  Repository Interface  →  Repository Impl  →  Remote/Local
```
- **Never skip layers.** Compose never calls UseCases directly.
- **Domain is pure Kotlin.** No Android imports in `domain-*` modules.
- **Data modules implement domain interfaces.** Bound via Hilt.
- **Feature modules import domain, not data.**

### ViewModel Pattern
```kotlin
// Always use sealed interface for UiState
sealed interface MyUiState {
    data object Loading : MyUiState
    data class Success(val data: MyData) : MyUiState
    data object Empty : MyUiState
    data class Error(val message: String, val retryable: Boolean) : MyUiState
}

// Always expose StateFlow, never MutableStateFlow
val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

// Always collect with lifecycle awareness
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

### UseCase Pattern
```kotlin
// Single responsibility — one action per UseCase
class GetRepositoryUseCase @Inject constructor(
    private val repository: RepositoryRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(repoId: String): Result<Repository> =
        withContext(dispatcher) { repository.getRepository(repoId) }
}
```

---

## Jetpack Compose Rules

- Use `@Composable` functions that are stateless wherever possible — hoist state to ViewModel.
- Use `LazyColumn` / `LazyRow` for all lists. Never use `Column` with `forEach` for list data.
- Use `collectAsStateWithLifecycle()` — never `collectAsState()` alone.
- Screen composables accept only callbacks and state — no ViewModel reference passed through the tree.
- Use `key {}` in LazyColumn items when items can reorder.
- Always provide `modifier = Modifier` parameter on every public composable.
- Use `Modifier.semantics {}` for accessibility where needed.
- Avoid `remember { mutableStateOf() }` in screens — prefer ViewModel state.

### Navigation
- Screen composables are called from `NavGraphBuilder` extensions only.
- Navigation events flow up via callbacks: `onNavigateToX: () -> Unit`.
- Use typed `NavArgs` or `SavedStateHandle` — never raw string parsing.

---

## Coroutines & Flow Rules

- Long-running operations always in `viewModelScope.launch {}` or `viewModelScope.async {}`.
- IO operations always on `Dispatchers.IO` (injected, not hardcoded).
- Use `Flow` for streams (AI streaming, DB updates, sync progress).
- Use `suspend fun` for single-shot operations.
- Handle cancellation: check `isActive` in loops; use `ensureActive()`.
- Use `catch {}` operator on flows before `collect {}`.
- Expose `SharedFlow` for one-time events (navigation, snackbars), not StateFlow.

---

## Room Database Rules

- Define all entities in `core-database`.
- DAOs return `Flow<T>` for observable queries, `suspend fun` for writes.
- Use `@Transaction` for multi-table operations.
- Always define explicit migration strategies — never use `fallbackToDestructiveMigration()` in production.
- Entities use `@Entity(tableName = "snake_case_name")`.
- Foreign keys declared explicitly with `onDelete` strategy.

---

## Retrofit / Network Rules

- Define API interfaces in `core-network`.
- All API calls return `suspend fun` results wrapped in `Result<T>` or custom `NetworkResult<T>`.
- Never expose Retrofit responses directly to domain — map to domain models in repository.
- Use `@Header` for auth tokens — never hardcode tokens.
- Implement retry logic with exponential backoff for transient failures.
- SSE / streaming endpoints use `Flow<T>` with OkHttp `EventSourceListener`.

---

## Security Rules

```kotlin
// CORRECT — encrypted storage
val prefs = EncryptedSharedPreferences.create(
    context,
    "devos_secure_prefs",
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
)

// WRONG — never do this
val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
prefs.edit().putString("api_key", apiKey).apply()  // cleartext!
```

- Never add secrets to `BuildConfig` or `local.properties` in VCS.
- Always mask API keys in logs: `key.take(4) + "****"`.
- Validate all file paths before reading — prevent path traversal in repo file loading.

---

## Performance Rules

- **Lazy loading:** Always paginate lists with `Pager` + `PagingSource`.
- **Background indexing:** Use `WorkManager` for repository indexing — never on main thread.
- **Code viewer:** Render only visible lines — use viewport-aware rendering.
- **AI streaming:** Update UI incrementally — never buffer entire response.
- **Images:** Use Coil with `AsyncImage` — never load bitmaps manually.
- **Avoid recomposition:** Use `remember`, `derivedStateOf`, stable data classes.
- **Profile with:** Android Studio Profiler, Compose Layout Inspector.

---

## Testing Rules

- Every ViewModel has a unit test file.
- Every UseCase has a unit test file.
- Every Repository implementation has an integration test with Room in-memory DB.
- Every screen has a `@Composable` UI test via `ComposeTestRule`.
- Use `TestCoroutineDispatcher` / `UnconfinedTestDispatcher` in all async tests.
- Use `MockK` for mocking — not Mockito.
- Use `turbine` library for Flow testing.

---

## Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Screen composable | `PascalCaseScreen` | `AIChatScreen` |
| ViewModel | `PascalCaseViewModel` | `AIChatViewModel` |
| UiState | `PascalCaseUiState` | `AIChatUiState` |
| UseCase | `VerbNounUseCase` | `SendMessageUseCase` |
| Repository interface | `NounRepository` | `AIRepository` |
| Repository impl | `NounRepositoryImpl` | `AIRepositoryImpl` |
| Entity | `NounEntity` | `MessageEntity` |
| DAO | `NounDao` | `MessageDao` |
| Route constant | `SCREAMING_SNAKE_CASE` | `AI_CHAT` |
| Module | `feature-kebab-case` | `feature-ai-chat` |
| Package | `com.devos.ai.feature.chat` | — |
