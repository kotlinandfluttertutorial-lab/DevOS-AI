# DevOS AI — Android Architecture

**Version:** 1.0  
**Date:** 2026-10-07  
**Stack:** Kotlin · Jetpack Compose · Material 3 · MVVM · Clean Architecture · Hilt · Room · Retrofit

---

## 1. Architecture Overview

DevOS AI follows **Clean Architecture** with **MVVM** at the presentation layer, organized into a **multi-module Gradle project**.

```
┌─────────────────────────────────────────────────────┐
│                   Presentation Layer                 │
│  Compose UI → ViewModel → UiState (sealed class)    │
├─────────────────────────────────────────────────────┤
│                    Domain Layer                      │
│  UseCases → Domain Models → Repository Interfaces   │
├─────────────────────────────────────────────────────┤
│                     Data Layer                       │
│  Repository Impls → Remote (Retrofit/WS) → Local (Room) │
├─────────────────────────────────────────────────────┤
│                   Platform / Core                    │
│  Navigation · DI (Hilt) · Coroutines · Security     │
└─────────────────────────────────────────────────────┘
```

---

## 2. Module Structure

```
DevOS-AI/
├── app/                          # App module — DI wiring, NavGraph, Activity
├── core/
│   ├── core-common/              # Extension functions, utilities, base classes
│   ├── core-network/             # Retrofit clients, interceptors, OkHttp
│   ├── core-database/            # Room DB, DAOs, entities, migrations
│   ├── core-security/            # EncryptedSharedPrefs, Keystore, token management
│   ├── core-ui/                  # Base Compose utilities, theme, state helpers
│   └── core-testing/             # Shared test utilities, fakes, test dispatchers
├── designsystem/
│   ├── theme/                    # Color, Typography, Shape, Spacing, Theme
│   └── components/               # All DevOS* components (40 components)
├── feature/
│   ├── feature-auth/             # Login, onboarding, splash
│   ├── feature-home/             # Home dashboard, notifications, search, profile
│   ├── feature-project/          # Project list, project overview, project settings
│   ├── feature-repository/       # Repo import, sync, overview, file explorer
│   ├── feature-code/             # Code viewer, code search, symbols, graph, architecture
│   ├── feature-ai-chat/          # AI chat, answers, evidence, context selector
│   ├── feature-agents/           # Agent run, tool execution, MCP tools
│   ├── feature-git/              # Git history, diff viewer
│   ├── feature-issues/           # Issue list, issue detail
│   ├── feature-prs/              # PR list, PR AI review
│   ├── feature-security/         # Security findings
│   ├── feature-testing/          # Test intelligence
│   ├── feature-learning/         # Learning dashboard, course, lesson, quiz
│   ├── feature-memory/           # Developer memory
│   └── feature-settings/         # AI settings, provider settings
├── domain/
│   ├── domain-repository/        # Repository intelligence domain
│   ├── domain-code/              # Code intelligence domain
│   ├── domain-ai/                # AI platform domain
│   ├── domain-git/               # Git intelligence domain
│   └── domain-learning/          # Learning domain
└── data/
    ├── data-repository/          # Repository data source impls
    ├── data-code/                # Code indexer, symbol extractor
    ├── data-ai/                  # AI provider impls, RAG, embeddings
    ├── data-git/                 # Git operations, GitHub/GitLab API
    └── data-learning/            # Learning content data
```

---

## 3. Dependency Graph (Module Level)

```
app
 ├── feature-* (all features)
 ├── designsystem
 └── core-*

feature-*
 ├── domain-* (relevant domains)
 ├── designsystem
 └── core-common, core-ui

domain-*
 └── (pure Kotlin, no Android deps)

data-*
 ├── domain-* (implements interfaces)
 ├── core-network
 ├── core-database
 └── core-security

designsystem
 └── core-ui
```

**Rule:** Feature modules depend only on domain interfaces, never on data implementations. Data modules are injected via Hilt at the `app` level.

---

## 4. Feature Module Internal Structure

Every feature module follows this structure:

```
feature-ai-chat/
├── src/main/kotlin/com/devos/ai/chat/
│   ├── presentation/
│   │   ├── AIChatScreen.kt          # @Composable screen
│   │   ├── AIChatViewModel.kt       # StateFlow + UiState
│   │   ├── AIChatUiState.kt         # sealed interface
│   │   └── components/              # Screen-local components
│   ├── navigation/
│   │   └── AIChatNavigation.kt      # NavGraphBuilder extension
│   └── di/
│       └── AIChatModule.kt          # Hilt module (if needed)
└── src/test/
    ├── AIChatViewModelTest.kt
    └── AIChatScreenTest.kt
```

---

## 5. UI State Pattern

Every screen defines a sealed interface:

```kotlin
sealed interface AIChatUiState {
    data object Loading : AIChatUiState
    data class Success(
        val messages: List<ChatMessage>,
        val context: AIContext,
        val isStreaming: Boolean,
    ) : AIChatUiState
    data object Empty : AIChatUiState
    data class Error(val message: String, val retryable: Boolean) : AIChatUiState
}
```

ViewModel exposes via `StateFlow`:

```kotlin
@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val setContextUseCase: SetContextUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIChatUiState>(AIChatUiState.Loading)
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        viewModelScope.launch {
            sendMessageUseCase(text)
                .onStart { _uiState.update { /* streaming start */ } }
                .catch { e -> _uiState.value = AIChatUiState.Error(e.message ?: "Unknown error", true) }
                .collect { message -> /* update success state */ }
        }
    }
}
```

Compose screen collects state:

```kotlin
@Composable
fun AIChatScreen(viewModel: AIChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        is AIChatUiState.Loading -> DevOSLoadingState()
        is AIChatUiState.Success -> AIChatContent(state)
        is AIChatUiState.Empty   -> DevOSEmptyState(...)
        is AIChatUiState.Error   -> DevOSErrorState(state.message) { viewModel.retry() }
    }
}
```

---

## 6. Domain Layer Pattern

```kotlin
// Domain model — pure Kotlin, no Android
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val sources: List<SourceReference>,
    val agentSteps: List<AgentStep>,
    val timestamp: Long,
)

// Repository interface — defined in domain
interface AIRepository {
    fun sendMessage(message: String, context: AIContext): Flow<ChatMessage>
    suspend fun getAnswer(answerId: String): ChatMessage
    suspend fun getEvidence(answerId: String): List<SourceReference>
}

// UseCase — orchestrates domain logic
class SendMessageUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val memoryRepository: MemoryRepository,
    private val contextRepository: ContextRepository,
) {
    operator fun invoke(message: String): Flow<ChatMessage> {
        val context = contextRepository.getCurrentContext()
        val memory = memoryRepository.getRelevantMemory(message)
        return aiRepository.sendMessage(message, context)
    }
}
```

---

## 7. Data Layer Pattern

```kotlin
// Repository implementation — in data module
class AIRepositoryImpl @Inject constructor(
    private val aiProviderApi: AIProviderApi,    // Retrofit/WS
    private val ragRepository: RAGRepository,    // Vector store
    private val messageDao: MessageDao,          // Room
) : AIRepository {

    override fun sendMessage(message: String, context: AIContext): Flow<ChatMessage> = flow {
        val chunks = ragRepository.retrieve(message, context, topK = 10)
        val prompt = buildPrompt(message, context, chunks)
        aiProviderApi.streamChat(prompt).collect { chunk ->
            emit(chunk.toMessage())
        }
    }
}
```

---

## 8. Navigation Architecture

```kotlin
// app/src/main/NavGraph.kt
@Composable
fun DevOSNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = DevOSRoutes.SPLASH,
    ) {
        splashNavigation()
        onboardingNavigation()
        authNavigation(navController)
        homeNavigation(navController)
        projectNavigation(navController)
        repositoryNavigation(navController)
        codeNavigation(navController)
        aiChatNavigation(navController)
        agentNavigation(navController)
        gitNavigation(navController)
        issuesNavigation(navController)
        prNavigation(navController)
        securityNavigation(navController)
        testingNavigation(navController)
        learningNavigation(navController)
        memoryNavigation(navController)
        settingsNavigation(navController)
    }
}

// feature-ai-chat/navigation/AIChatNavigation.kt
fun NavGraphBuilder.aiChatNavigation(navController: NavController) {
    composable(
        route = DevOSRoutes.AI_CHAT,
        deepLinks = listOf(navDeepLink { uriPattern = "devos://ai/chat" }),
    ) {
        AIChatScreen(
            onNavigateToAnswer = { id -> navController.navigate("ai/answer/$id") },
            onNavigateToCode = { repoId, path, line ->
                navController.navigate("repository/$repoId/file?path=$path&line=$line")
            },
        )
    }
    composable(DevOSRoutes.AI_ANSWER_DETAIL) { backStackEntry ->
        AIAnswerDetailScreen(answerId = backStackEntry.arguments?.getString("answerId")!!)
    }
}
```

---

## 9. Dependency Injection (Hilt)

```kotlin
// app/src/main/di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DevOSDatabase =
        Room.databaseBuilder(context, DevOSDatabase::class.java, "devos.db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(LoggingInterceptor())
            .build()
}

// data-ai/di/AIModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {
    @Binds abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository
    @Binds abstract fun bindRAGRepository(impl: RAGRepositoryImpl): RAGRepository
}
```

---

## 10. Background Work

```kotlin
// Repository indexing — WorkManager
@HiltWorker
class RepositoryIndexWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val indexRepositoryUseCase: IndexRepositoryUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repoId = inputData.getString("repoId") ?: return Result.failure()
        return try {
            indexRepositoryUseCase(repoId)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

---

## 11. AI Streaming

```kotlin
// AI responses streamed via Flow
interface AIProviderApi {
    fun streamChat(request: ChatRequest): Flow<ChatChunk>
}

// OpenAI implementation using OkHttp SSE
class OpenAIProviderApi @Inject constructor(
    private val client: OkHttpClient,
    private val tokenRepository: TokenRepository,
) : AIProviderApi {
    override fun streamChat(request: ChatRequest): Flow<ChatChunk> = callbackFlow {
        val httpRequest = buildSSERequest(request, tokenRepository.getOpenAIToken())
        val eventSource = EventSources.createFactory(client)
            .newEventSource(httpRequest, object : EventSourceListener() {
                override fun onEvent(id: String?, type: String?, data: String) {
                    trySend(parseChunk(data))
                }
                override fun onClosed(eventSource: EventSource) { close() }
                override fun onFailure(e: Throwable?, response: Response?) {
                    close(e ?: IOException("SSE failure"))
                }
            })
        awaitClose { eventSource.cancel() }
    }
}
```

---

## 12. Security Architecture

| Concern | Implementation |
|---------|---------------|
| API keys | Android Keystore via `EncryptedSharedPreferences` |
| GitHub tokens | Keystore, never in BuildConfig |
| Network | Certificate pinning for production AI endpoints |
| Repository instructions | Sanitized before AI context injection |
| Destructive operations | Explicit confirmation required in ViewModel |
| Logging | No secrets in Timber/Logcat |

---

## 13. Performance Architecture

| Concern | Implementation |
|---------|---------------|
| Large repository loading | Paginated queries, background indexing via WorkManager |
| Code viewer | Lazy column + viewport-only rendering |
| AI streaming | Flow-based streaming → incremental UI updates |
| File tree | Virtualized tree (lazy rendering of expanded nodes only) |
| Graph rendering | Canvas-based with level-of-detail control |
| Image caching | Coil for avatars and thumbnails |
| DB queries | Room + Kotlin Flows, background dispatcher |

---

## 14. Testing Architecture

```
Unit Tests       → domain/, data/, ViewModel tests (JUnit5 + MockK)
Integration      → Repository + DAO tests (Room in-memory)
UI Tests         → Compose UI tests (ComposeTestRule)
Navigation Tests → navController test helpers
E2E              → Espresso (critical flows only)
AI Tests         → Retrieval eval, grounding eval, agent eval
```

See `docs/testing/test-strategy.md` for full detail.
