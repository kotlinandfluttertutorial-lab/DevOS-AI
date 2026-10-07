# Kiro Prompt — DEVOS-013: Repository Intelligence

**Jira:** DEVOS-013 / DEVOS-014 / DEVOS-015 / DEVOS-016 / DEVOS-017  
**Epic:** DEVOS-E02  
**Figma:** FIGMA-05 / FIGMA-07 / FIGMA-08 / FIGMA-09  
**Kiro Spec:** `.kiro/specs/repository-intelligence/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the complete Repository Intelligence module for DevOS AI: repository list, import, sync progress, and overview screens, plus the background clone and indexing service.

**Existing files to read first:**
- `.kiro/specs/repository-intelligence/core.md`
- `docs/figma/screen-inventory.md` — Screens FIGMA-05, FIGMA-07, FIGMA-08, FIGMA-09
- `docs/figma/component-inventory.md` — DevOSHealthIndicator, DevOSStatusBadge, DevOSCard
- `core/core-database/src/main/kotlin/com/devos/ai/database/` — existing Room setup

**Modules:**
- `feature/feature-repository/` — screens + ViewModels
- `domain/domain-repository/` — repository interfaces + domain models
- `data/data-repository/` — repository implementations + Room DAOs
- `core/core-database/` — Room entities

**Package:** `com.devos.ai.feature.repository`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- Repository indexing MUST run in WorkManager — never on main thread or in a ViewModel coroutine.
- Room migration strategies must be explicit — never use `fallbackToDestructiveMigration()` in production.
- Feature module imports `domain-repository` interfaces only — never `data-repository` directly.

**What to implement:**

### 1. Domain Models

File: `domain/domain-repository/src/main/kotlin/com/devos/ai/domain/repository/`

```kotlin
data class Repository(
    val id: String,
    val name: String,
    val owner: String,
    val fullName: String,       // "owner/name"
    val description: String?,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val defaultBranch: String,
    val cloneUrl: String,
    val provider: RepositoryProvider, // GITHUB, GITLAB, LOCAL
    val healthScore: Float,     // 0.0–1.0
    val lastSyncAt: Instant?,
    val syncStatus: SyncStatus, // IDLE, SYNCING, SYNCED, ERROR
)

enum class RepositoryProvider { GITHUB, GITLAB, LOCAL }
enum class SyncStatus { IDLE, SYNCING, SYNCED, ERROR }

data class SyncProgress(
    val repoId: String,
    val currentStep: SyncStep,
    val completedSteps: List<SyncStep>,
    val errorMessage: String?,
)

enum class SyncStep(val label: String) {
    CLONE("Clone repository"),
    PARSE("Parse files"),
    INDEX_SYMBOLS("Index symbols"),
    BUILD_VECTORS("Build vector store"),
    DONE("Complete"),
}
```

Repository interface:
```kotlin
interface RepositoryRepository {
    fun observeRepositories(): Flow<List<Repository>>
    fun observeRepository(repoId: String): Flow<Repository?>
    suspend fun importRepository(cloneUrl: String, provider: RepositoryProvider): Result<String>
    suspend fun syncRepository(repoId: String): Result<Unit>
    suspend fun cancelSync(repoId: String): Result<Unit>
    suspend fun deleteRepository(repoId: String): Result<Unit>
    fun observeSyncProgress(repoId: String): Flow<SyncProgress?>
}
```

### 2. Room Entities (core-database)

```kotlin
@Entity(tableName = "repositories")
data class RepositoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val owner: String,
    val description: String?,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val defaultBranch: String,
    val cloneUrl: String,
    val provider: String,
    val healthScore: Float,
    val lastSyncAt: Long?,      // epoch millis
    val syncStatus: String,
    val localPath: String?,
)

@Entity(tableName = "repository_files",
    foreignKeys = [ForeignKey(entity = RepositoryEntity::class,
        parentColumns = ["id"], childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE)])
data class FileEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    val path: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val contentHash: String,
)

@Entity(tableName = "symbols",
    foreignKeys = [ForeignKey(entity = RepositoryEntity::class,
        parentColumns = ["id"], childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE)])
data class SymbolEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    val name: String,
    val kind: String,           // CLASS, FUNCTION, INTERFACE, PROPERTY, OBJECT
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val signature: String?,
    val docComment: String?,
    val visibility: String,     // PUBLIC, PRIVATE, INTERNAL, PROTECTED
)
```

### 3. RepositoryListScreen (DEVOS-017, FIGMA-05)

Route: `PROJECTS`

UiState:
```kotlin
sealed interface RepositoryListUiState {
    data object Loading : RepositoryListUiState
    data class Success(
        val repositories: List<Repository>,
        val sortOrder: SortOrder,
        val filterLanguage: String?,
    ) : RepositoryListUiState
    data object Empty : RepositoryListUiState
    data class Error(val message: String, val retryable: Boolean) : RepositoryListUiState
}
```

Layout:
```kotlin
Scaffold(
    topBar = { DevOSTopBar(title = "Projects") },
    floatingActionButton = {
        FloatingActionButton(onClick = onImport,
            containerColor = MaterialTheme.colorScheme.primary) {
            Icon(Icons.Outlined.Add, contentDescription = "Import repository")
        }
    }
) { padding ->
    when (val s = uiState) {
        is Loading -> DevOSLoadingState()
        is Empty   -> DevOSEmptyState(
            icon = Icons.Outlined.FolderOff,
            title = "No repositories yet",
            description = "Import a GitHub or GitLab repository to get started",
            action = { DevOSButton("Import Repository", onClick = onImport) }
        )
        is Error   -> DevOSErrorState(description = s.message,
            onRetry = if (s.retryable) viewModel::retry else null)
        is Success -> {
            LazyColumn(Modifier.padding(padding)) {
                // Filter/sort chips
                item {
                    SortFilterRow(sortOrder = s.sortOrder, onSortChange = onSortChange)
                }
                items(s.repositories, key = { it.id }) { repo ->
                    RepositoryCard(repo = repo, onClick = { onNavigateToRepo(repo.id) })
                }
            }
        }
    }
}

@Composable
fun RepositoryCard(repo: Repository, onClick: () -> Unit) {
    DevOSCard(onClick = onClick) {
        Row(Modifier.padding(MaterialTheme.spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            DevOSHealthIndicator(score = repo.healthScore, size = 48.dp)
            Column(Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(repo.fullName, style = MaterialTheme.typography.titleSmall)
                if (repo.description != null) {
                    Text(repo.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                    repo.language?.let {
                        DevOSStatusBadge(it, MaterialTheme.colorScheme.secondary)
                    }
                    DevOSStatusBadge(
                        when (repo.syncStatus) {
                            SyncStatus.SYNCED  -> "Synced"
                            SyncStatus.SYNCING -> "Syncing…"
                            SyncStatus.ERROR   -> "Error"
                            SyncStatus.IDLE    -> "Not synced"
                        },
                        when (repo.syncStatus) {
                            SyncStatus.SYNCED  -> MaterialTheme.colorScheme.primary
                            SyncStatus.SYNCING -> MaterialTheme.colorScheme.tertiary
                            SyncStatus.ERROR   -> MaterialTheme.colorScheme.error
                            SyncStatus.IDLE    -> MaterialTheme.colorScheme.outline
                        }
                    )
                }
            }
        }
    }
}
```

### 4. RepositoryImportScreen (DEVOS-013, FIGMA-07)

Route: `REPO_IMPORT`

UiState:
```kotlin
sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Validating : ImportUiState
    data class Preview(val repo: RepositoryPreview) : ImportUiState
    data object Importing : ImportUiState
    data class Error(val message: String) : ImportUiState
}
```

Layout:
```kotlin
Scaffold(topBar = { DevOSTopBar(title = "Import Repository", onBack = onBack) }) { padding ->
    Column(Modifier.padding(padding).padding(MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {

        // Source selector
        Text("Source", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            FilterChip(selected = source == GITHUB, onClick = { onSourceSelect(GITHUB) },
                label = { Text("GitHub") })
            FilterChip(selected = source == GITLAB, onClick = { onSourceSelect(GITLAB) },
                label = { Text("GitLab") })
            FilterChip(selected = source == LOCAL,  onClick = { onSourceSelect(LOCAL) },
                label = { Text("URL") })
        }

        // URL input
        OutlinedTextField(
            value = url, onValueChange = onUrlChange,
            label = { Text("Repository URL") },
            placeholder = { Text("https://github.com/owner/repo") },
            isError = uiState is ImportUiState.Error,
            supportingText = { if (uiState is ImportUiState.Error)
                Text((uiState as ImportUiState.Error).message) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        // Preview card (shown after validation)
        if (uiState is ImportUiState.Preview) {
            RepositoryPreviewCard((uiState as ImportUiState.Preview).repo)
        }

        Spacer(Modifier.weight(1f))
        DevOSButton(
            text = if (uiState is ImportUiState.Preview) "Import" else "Validate",
            onClick = if (uiState is ImportUiState.Preview) onImport else onValidate,
            enabled = url.isNotBlank() && uiState !is ImportUiState.Importing,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

### 5. RepositorySyncScreen (DEVOS-014, FIGMA-08)

Route: `REPO_SYNC/{repoId}`

Layout:
```kotlin
Scaffold(
    topBar = { DevOSTopBar(title = "Syncing Repository", onBack = null) },
    bottomBar = {
        if (syncProgress?.currentStep != SyncStep.DONE) {
            DevOSButton("Cancel", onClick = onCancel,
                style = DevOSButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.base))
        }
    }
) { padding ->
    LazyColumn(Modifier.padding(padding).padding(MaterialTheme.spacing.base)) {
        items(SyncStep.entries) { step ->
            SyncStepItem(
                step = step,
                state = when {
                    syncProgress?.completedSteps?.contains(step) == true -> StepState.Complete
                    syncProgress?.currentStep == step -> StepState.InProgress
                    syncProgress?.errorMessage != null
                        && syncProgress.currentStep == step -> StepState.Failed
                    else -> StepState.Pending
                },
                errorMessage = if (syncProgress?.currentStep == step)
                    syncProgress?.errorMessage else null,
                onRetry = if (syncProgress?.currentStep == step
                    && syncProgress?.errorMessage != null) onRetry else null,
            )
        }
        if (syncProgress?.currentStep == SyncStep.DONE) {
            item {
                DevOSButton("View Repository", onClick = onNavigateToRepo,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
```

### 6. RepositoryOverviewScreen (DEVOS-016, FIGMA-09)

Route: `REPO_OVERVIEW/{repoId}`

UiState:
```kotlin
data class RepositoryOverviewUiState(
    val repository: Repository,
    val selectedTab: OverviewTab,
    val languageBreakdown: List<LanguageBar>,
    val healthMetrics: HealthMetrics,
)

enum class OverviewTab {
    OVERVIEW, FILES, CODE_SEARCH, SYMBOLS, GRAPH, ISSUES, PRS, SETTINGS
}
```

Layout: `Scaffold` with `DevOSTopBar` (repo name + avatar). `TabRow` with 8 `Tab` items. Content area switches on `selectedTab` — each tab navigates to or loads the relevant sub-screen.

### 7. Repository Clone and Indexing Service (DEVOS-015)

File: `data/data-repository/src/main/kotlin/com/devos/ai/data/repository/worker/RepositoryIndexingWorker.kt`

```kotlin
@HiltWorker
class RepositoryIndexingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repositoryDao: RepositoryDao,
    private val fileDao: FileDao,
    private val symbolDao: SymbolDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repoId = inputData.getString(KEY_REPO_ID) ?: return Result.failure()
        val cloneUrl = inputData.getString(KEY_CLONE_URL) ?: return Result.failure()

        return try {
            setForeground(createForegroundInfo(SyncStep.CLONE))
            val localPath = cloneRepository(cloneUrl, repoId)
            setProgress(workDataOf(KEY_STEP to SyncStep.PARSE.name))

            setForeground(createForegroundInfo(SyncStep.PARSE))
            val files = parseFiles(localPath, repoId)
            fileDao.insertAll(files)
            setProgress(workDataOf(KEY_STEP to SyncStep.INDEX_SYMBOLS.name))

            setForeground(createForegroundInfo(SyncStep.INDEX_SYMBOLS))
            val symbols = indexSymbols(files, repoId)
            symbolDao.insertAll(symbols)

            repositoryDao.updateSyncStatus(repoId, SyncStatus.SYNCED.name)
            Result.success()
        } catch (e: CancellationException) {
            repositoryDao.updateSyncStatus(repoId, SyncStatus.IDLE.name)
            Result.failure()
        } catch (e: Exception) {
            repositoryDao.updateSyncStatus(repoId, SyncStatus.ERROR.name)
            Result.failure(workDataOf(KEY_ERROR to e.message))
        }
    }

    private suspend fun cloneRepository(url: String, repoId: String): String {
        // Use ProcessBuilder to run: git clone --depth=1 <url> <localPath>
        // Return local path
    }

    private suspend fun parseFiles(localPath: String, repoId: String): List<FileEntity> {
        // Walk directory tree, create FileEntity for each file
        // Skip .git, build/, .gradle directories
    }

    private suspend fun indexSymbols(files: List<FileEntity>, repoId: String): List<SymbolEntity> {
        // For each .kt / .java file: extract classes, functions, interfaces using regex or tree-sitter
        // Return SymbolEntity list
    }
}
```

Enqueue from `ImportRepositoryUseCase`:
```kotlin
val workRequest = OneTimeWorkRequestBuilder<RepositoryIndexingWorker>()
    .setInputData(workDataOf(
        KEY_REPO_ID to repoId,
        KEY_CLONE_URL to cloneUrl,
    ))
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build())
    .build()
WorkManager.getInstance(context).enqueueUniqueWork(
    "index_$repoId",
    ExistingWorkPolicy.REPLACE,
    workRequest,
)
```

Incremental sync: compare `contentHash` of each file — only re-index files where hash changed.

**Tests:**
- `RepositoryListViewModelTest`: loads repositories; empty state when no repos; error state on failure; sort order changes
- `ImportViewModelTest`: URL validation rejects invalid URLs; preview loads on valid URL; import triggers WorkManager job
- `SyncProgressViewModelTest`: progress steps update correctly; cancel calls `CancelSyncUseCase`; error state shows retry
- `RepositoryIndexingWorkerTest`: clones repo; files indexed in Room; symbols extracted; incremental sync skips unchanged files
- `RepositoryRepositoryImplTest`: Room in-memory DB; CRUD operations; Flow emissions on change

**Acceptance Criteria:**
- AC1 (DEVOS-017): Repository list renders cards with name, language badge, health score, and last sync time
- AC2 (DEVOS-017): Health score shown as `DevOSHealthIndicator` ring on each card
- AC3 (DEVOS-017): FAB opens import screen
- AC4 (DEVOS-017): Sort/filter chips work (by name, last sync, health score)
- AC5 (DEVOS-017): All 4 UiStates render correctly (Loading, Success, Empty, Error)
- AC6 (DEVOS-013): Source selector renders GitHub, GitLab, URL options
- AC7 (DEVOS-013): URL input validated — error shown for non-git-repository URLs
- AC8 (DEVOS-013): Repository preview card shown after successful URL validation
- AC9 (DEVOS-013): Import options configurable (branch, depth)
- AC10 (DEVOS-013): Error state shown on invalid URL or network failure
- AC11 (DEVOS-014): All sync steps shown with pending/in-progress/complete/failed states
- AC12 (DEVOS-014): In-progress step shows animation
- AC13 (DEVOS-014): Cancel button stops the WorkManager job
- AC14 (DEVOS-014): Success state transitions to repository overview
- AC15 (DEVOS-014): Error state shows retry button per failed step
- AC16 (DEVOS-015): Repository cloned via git to local storage
- AC17 (DEVOS-015): All files indexed into Room `FileEntity` table
- AC18 (DEVOS-015): Kotlin/Java symbols extracted into `SymbolEntity` table and queryable
- AC19 (DEVOS-015): Incremental sync on pull only re-indexes files with changed content hash
- AC20 (DEVOS-016): Repository header shows name, owner avatar, stars, forks, and language
- AC21 (DEVOS-016): All 8 tabs (Overview/Files/Code Search/Symbols/Graph/Issues/PRs/Settings) present and navigable
- AC22 (DEVOS-016): Language distribution bars render with correct percentages
- AC23 (DEVOS-016): Health panel shows security score, test coverage, and code quality metrics
- AC24 (DEVOS-016): Last sync timestamp shown and human-readable ("2 hours ago")
