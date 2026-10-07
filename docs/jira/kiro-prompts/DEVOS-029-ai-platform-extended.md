# Kiro Prompt — DEVOS-029: AI Platform Extended (Answer Detail, Source Evidence, Providers, Settings)

**Jira:** DEVOS-029 / DEVOS-030 / DEVOS-032 / DEVOS-033 / DEVOS-034  
**Epic:** DEVOS-E04  
**Figma:** FIGMA-17 / FIGMA-18 / FIGMA-34 / FIGMA-35  
**Kiro Spec:** `.kiro/specs/ai-platform/providers.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the extended AI Platform module for DevOS AI: AI answer detail screen, source evidence screen, multi-provider abstraction layer, AI settings screen, and provider settings with encrypted API key management.

**Existing files to read first:**
- `.kiro/specs/ai-platform/providers.md`
- `docs/figma/screen-inventory.md` — Screens FIGMA-17, FIGMA-18, FIGMA-34, FIGMA-35
- `docs/figma/component-inventory.md` — DevOSAIMessage, DevOSCodeBlock, DevOSMarkdownText
- `feature/feature-ai-chat/` — existing AI chat screen (DEVOS-026) for patterns to follow
- `domain/domain-ai/` — existing AI domain interfaces

**Modules:**
- `feature/feature-ai-chat/` — answer detail + source evidence screens
- `feature/feature-settings/` — AI settings + provider settings screens
- `domain/domain-ai/` — AIProvider interface (new)
- `data/data-ai/` — provider implementations

**Package:** `com.devos.ai.feature`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- AI responses MUST stream via `Flow<StreamChunk>` — never buffer full response before updating UI.
- API keys stored ONLY in EncryptedSharedPreferences with AES256_GCM — never BuildConfig or plaintext SharedPreferences.
- `AIProvider` interface lives in `domain-ai` — implementations live in `data-ai` only; feature modules import only the interface.
- `ProviderSettingsViewModel` must NOT hold the raw API key in UiState after saving — only the masked form.
- Provider settings screen never logs or displays full API keys.

**What to implement:**

### 1. AIProvider Interface (DEVOS-032)

File: `domain/domain-ai/src/main/kotlin/com/devos/ai/domain/ai/AIProvider.kt`

```kotlin
interface AIProvider {
    val id: String          // "openai", "anthropic", "gemini", "ollama"
    val displayName: String
    val supportedModels: List<String>

    /** Stream a chat response. Never returns the full response at once. */
    fun chat(
        messages: List<Message>,
        context: AIContext,
        model: String,
    ): Flow<StreamChunk>

    /** Compute a text embedding for RAG retrieval. */
    suspend fun embed(text: String): Result<FloatArray>

    /** Validate the stored API key by making a minimal API call. */
    suspend fun testConnection(): Result<Unit>
}

data class StreamChunk(
    val delta: String,      // partial text
    val isComplete: Boolean,
    val usage: TokenUsage?,
)

data class TokenUsage(val promptTokens: Int, val completionTokens: Int)

data class AIContext(
    val repositoryContext: String?,
    val fileContext: String?,
    val symbolContext: String?,
    val learningContext: String?,
    val memoryContext: String?,
)
```

Implementations in `data/data-ai/`:

```kotlin
// OpenAIProvider — POST https://api.openai.com/v1/chat/completions (stream=true)
// AnthropicProvider — POST https://api.anthropic.com/v1/messages (stream=true)
// GeminiProvider — POST https://generativelanguage.googleapis.com/v1beta/... (stream=true)
// OllamaProvider — POST http://localhost:11434/api/chat (stream=true, local only)
```

All use OkHttp `EventSourceListener` for SSE. Token loaded from `SecureTokenRepository.getToken(provider.id)`.

Hilt binding in `data/data-ai/di/AIModule.kt`:
```kotlin
@Module @InstallIn(SingletonComponent::class)
object AIModule {
    @Provides @Singleton
    fun provideAIProviderFactory(
        openAI: OpenAIProvider,
        anthropic: AnthropicProvider,
        gemini: GeminiProvider,
        ollama: OllamaProvider,
    ): AIProviderFactory = AIProviderFactory(mapOf(
        "openai" to openAI,
        "anthropic" to anthropic,
        "gemini" to gemini,
        "ollama" to ollama,
    ))
}
```

### 2. AIAnswerDetailScreen (DEVOS-029, FIGMA-17)

Route: `AI_ANSWER_DETAIL/{answerId}`

UiState:
```kotlin
sealed interface AIAnswerDetailUiState {
    data object Loading : AIAnswerDetailUiState
    data class Success(
        val question: String,
        val answer: String,
        val sources: List<SourceReference>,
        val callChain: List<CallChainStep>,
    ) : AIAnswerDetailUiState
    data object Empty : AIAnswerDetailUiState
    data class Error(val message: String, val retryable: Boolean) : AIAnswerDetailUiState
}

data class CallChainStep(
    val name: String,      // "Query", "Retrieve", "Augment", "Generate"
    val durationMs: Long,
    val details: String?,
)
```

Layout:
```kotlin
Scaffold(topBar = { DevOSTopBar(title = "Answer Detail", onBack = onBack) }) { padding ->
    LazyColumn(Modifier.padding(padding).padding(MaterialTheme.spacing.base)) {
        item {
            Text("Question", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(question, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(MaterialTheme.spacing.base))
        }
        item {
            SectionHeader("Answer")
            DevOSMarkdownText(markdown = answer)
        }
        item { SectionHeader("Evidence Sources") }
        items(sources) { source ->
            SourceChip(source, onClick = { onNavigateToSource(source.id) })
        }
        item { SectionHeader("Processing Steps") }
        items(callChain) { step ->
            CallChainStepItem(step)
        }
        item {
            Spacer(Modifier.height(MaterialTheme.spacing.base))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                DevOSButton("Open Source", onClick = { onNavigateToSource(sources.firstOrNull()?.id) },
                    style = DevOSButtonStyle.Secondary)
                DevOSButton("Follow Up", onClick = onFollowUp)
            }
        }
    }
}
```

### 3. AISourceEvidenceScreen (DEVOS-030, FIGMA-18)

Route: `AI_SOURCE_EVIDENCE/{answerId}/{sourceId}`

UiState:
```kotlin
sealed interface AISourceEvidenceUiState {
    data object Loading : AISourceEvidenceUiState
    data class Success(
        val filePath: String,
        val lineStart: Int,
        val lineEnd: Int,
        val snippet: String,
        val language: String,
        val relevanceScore: Float,  // 0.0–1.0
    ) : AISourceEvidenceUiState
    data object Empty : AISourceEvidenceUiState
    data class Error(val message: String, val retryable: Boolean) : AISourceEvidenceUiState
}
```

Layout:
```kotlin
Scaffold(
    topBar = { DevOSTopBar(title = "Source Evidence", onBack = onBack) },
    bottomBar = {
        DevOSButton(
            text = "Open in Code Viewer",
            onClick = { onNavigateToCodeViewer(filePath, lineStart) },
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.base),
        )
    }
) { padding ->
    Column(Modifier.padding(padding).padding(MaterialTheme.spacing.base)) {
        Text(filePath, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Lines $lineStart–$lineEnd", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(MaterialTheme.spacing.sm))
        // Relevance bar
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            Text("Relevance", style = MaterialTheme.typography.labelSmall)
            LinearProgressIndicator(progress = relevanceScore, Modifier.weight(1f))
            Text("${(relevanceScore * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(MaterialTheme.spacing.base))
        DevOSCodeBlock(
            code = snippet,
            language = language,
            highlightLine = lineStart,
        )
    }
}
```

### 4. AISettingsScreen (DEVOS-033, FIGMA-34)

Route: `AI_SETTINGS`

UiState:
```kotlin
data class AISettingsUiState(
    val activeProviderId: String,
    val activeModel: String,
    val availableModels: List<String>,
    val ragEnabled: Boolean,
    val ragChunkSize: Int,
    val agentMaxSteps: Int,
    val memoryEnabled: Boolean,
    val tokenUsageToday: Int,
    val tokenUsageMonth: Int,
)
```

Layout: `LazyColumn` of settings groups:
- **Model** — provider selector dropdown + model selector dropdown
- **RAG** — toggle + chunk size slider (256–2048, steps of 256)
- **Agent** — max steps stepper (1–50)
- **Memory** — toggle
- **Usage** — stats card showing today/month token counts, estimated cost

All settings persisted via `AISettingsRepository` (DataStore).

### 5. ProviderSettingsScreen (DEVOS-034, FIGMA-35)

Route: `PROVIDER_SETTINGS`

UiState:
```kotlin
data class ProviderSettingsUiState(
    val providers: List<ProviderStatus>,
    val isTestingConnection: String?,   // provider id being tested, or null
)

data class ProviderStatus(
    val provider: String,
    val displayName: String,
    val isConfigured: Boolean,
    val maskedKey: String?,             // "sk-1234****" — never full key
    val isExpanded: Boolean,
)
```

Layout:
```kotlin
LazyColumn {
    items(providers) { provider ->
        ProviderCard(
            provider = provider,
            onExpand = { onExpandProvider(provider.provider) },
            onSave = { key -> onSaveKey(provider.provider, key) },
            onTest = { onTestConnection(provider.provider) },
            isTestingConnection = isTestingConnection == provider.provider,
        )
    }
}

@Composable
fun ProviderCard(provider: ProviderStatus, onExpand: () -> Unit, onSave: (String) -> Unit,
                 onTest: () -> Unit, isTestingConnection: Boolean) {
    DevOSCard(onClick = onExpand) {
        Column(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(provider.displayName, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                DevOSStatusBadge(
                    if (provider.isConfigured) "Connected" else "Not configured",
                    if (provider.isConfigured) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                )
            }
            if (provider.maskedKey != null) {
                Text(provider.maskedKey, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Expanded form
            AnimatedVisibility(visible = provider.isExpanded) {
                var keyInput by remember { mutableStateOf("") }
                var showKey by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("API Key") },
                        visualTransformation = if (showKey) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(if (showKey) Icons.Outlined.VisibilityOff
                                     else Icons.Outlined.Visibility,
                                    contentDescription = if (showKey) "Hide key" else "Show key")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                        DevOSButton("Save", onClick = { onSave(keyInput); keyInput = "" },
                            enabled = keyInput.isNotBlank())
                        DevOSButton("Test", onClick = onTest,
                            style = DevOSButtonStyle.Secondary,
                            isLoading = isTestingConnection)
                    }
                }
            }
        }
    }
}
```

`onSaveKey` calls `SaveProviderApiKeyUseCase` which calls `SecureTokenRepository.saveToken(providerId, key)`. The raw key is immediately discarded from ViewModel state — only masked form stored in UiState.

**Tests:**
- `AIProviderFactoryTest`: correct provider returned by id; unknown id throws
- `OpenAIProviderTest`: streaming SSE parsed correctly; cancellation stops stream; connection test passes with valid key
- `AIAnswerDetailViewModelTest`: answer loads; sources populated; call chain steps correct
- `AISourceEvidenceViewModelTest`: snippet loads; relevance score correct; navigation to code viewer with correct line
- `AISettingsViewModelTest`: settings load from DataStore; changes persisted; model list updates on provider change
- `ProviderSettingsViewModelTest`: key saved to EncryptedSharedPreferences; raw key not in UiState after save; masked key shown; test connection fires provider.testConnection()

**Acceptance Criteria:**
- AC1 (DEVOS-029): Full markdown response rendered via `DevOSMarkdownText`
- AC2 (DEVOS-029): Evidence section shows ranked source chips
- AC3 (DEVOS-029): Call chain visualization (Query → Retrieve → Augment → Generate) with durations
- AC4 (DEVOS-029): "Open Source" navigates to source evidence; "Follow Up" pre-fills AI chat input
- AC5 (DEVOS-030): File path and line range shown in header
- AC6 (DEVOS-030): Code snippet rendered in `DevOSCodeBlock` with correct language
- AC7 (DEVOS-030): Relevance score shown as progress bar and percentage
- AC8 (DEVOS-030): "Open in Code Viewer" navigates to `CodeViewerScreen` at the correct line
- AC9 (DEVOS-032): `AIProvider` interface defined in `domain-ai` with `chat()`, `embed()`, `testConnection()`
- AC10 (DEVOS-032): OpenAI, Anthropic, Gemini, and Ollama implementations exist and stream via Flow
- AC11 (DEVOS-032): Provider can be switched in settings; all implementations pass contract tests
- AC12 (DEVOS-033): Active model selector changes the model used in chat
- AC13 (DEVOS-033): RAG toggle and chunk size slider functional and persisted
- AC14 (DEVOS-033): Agent max steps setting persisted and respected
- AC15 (DEVOS-033): Token usage stats (today / month) displayed accurately
- AC16 (DEVOS-034): Provider list shows all 4 providers with connected/not-configured status
- AC17 (DEVOS-034): API key input masked by default; show/hide toggle works
- AC18 (DEVOS-034): "Test Connection" validates key against provider API; shows success/failure
- AC19 (DEVOS-034): Keys stored in EncryptedSharedPreferences; verified by inspecting app storage (no plaintext)
- AC20 (DEVOS-034): Saved key displayed only as masked form in UI (`sk-1234****`)
