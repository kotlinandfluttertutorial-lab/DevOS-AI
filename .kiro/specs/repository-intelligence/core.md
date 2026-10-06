# Spec: Repository Intelligence — Core

**Jira:** DEVOS-013 / DEVOS-014 / DEVOS-015 / DEVOS-016 / DEVOS-017  
**Epic:** DEVOS-E02  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement repository import, sync, indexing, and overview — the foundation for all code intelligence.

## Domain Models

```kotlin
data class Repository(
    val id: String,
    val name: String,
    val owner: String,
    val remoteUrl: String,
    val localPath: String,
    val defaultBranch: String,
    val currentBranch: String,
    val languages: Map<String, Float>,   // language → percentage
    val lastSyncAt: Long?,
    val syncStatus: SyncStatus,
    val health: RepositoryHealth,
)

data class RepositoryHealth(
    val securityScore: Float,    // 0.0–1.0
    val testCoverage: Float,
    val architectureScore: Float,
    val dependencyScore: Float,
)

enum class SyncStatus { NOT_SYNCED, SYNCING, SYNCED, STALE, FAILED }
```

## Repository Interface

```kotlin
interface RepositoryRepository {
    fun getRepositories(): Flow<List<Repository>>
    suspend fun getRepository(id: String): Repository
    suspend fun importRepository(url: String, options: ImportOptions): Repository
    fun syncRepository(id: String): Flow<SyncProgress>
    suspend fun deleteRepository(id: String)
}
```

## Sync Progress Model

```kotlin
sealed interface SyncProgress {
    data class Step(val name: String, val completed: Boolean, val isCurrent: Boolean, val percent: Int) : SyncProgress
    data class Overall(val percent: Int, val message: String) : SyncProgress
    data object Complete : SyncProgress
    data class Failed(val step: String, val error: String) : SyncProgress
}
```

## Screens

### RepositoryImportScreen
- **Route:** `repository/import`
- **States:** Idle → Validating → Preview → Importing → Success/Error
- **Figma:** FIGMA-07

### RepositorySyncScreen
- **Route:** `repository/{id}/sync`
- **States:** Idle → Syncing (steps) → Complete/Failed
- **Figma:** FIGMA-08
- Steps: Fetch remote | Index files | Build symbols | Build graph | Update RAG

### RepositoryOverviewScreen
- **Route:** `repository/{id}`
- **States:** Loading → Success → Stale → Error
- **Figma:** FIGMA-09
- Tabs: Overview | Files | Search | Symbols | Graph | RAG | Git | AI

### ProjectListScreen
- **Route:** `project_list`
- **Figma:** FIGMA-05

### ProjectOverviewScreen
- **Route:** `project/{id}`
- **Figma:** FIGMA-06

## WorkManager Jobs

```kotlin
@HiltWorker
class RepositoryIndexWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val indexRepositoryUseCase: IndexRepositoryUseCase,
) : CoroutineWorker(context, params) {
    companion object {
        const val KEY_REPO_ID = "repoId"
        fun buildRequest(repoId: String) = OneTimeWorkRequestBuilder<RepositoryIndexWorker>()
            .setInputData(workDataOf(KEY_REPO_ID to repoId))
            .setConstraints(Constraints(requiresStorageNotLow = true))
            .build()
    }
}
```

## Acceptance Criteria
- [ ] GitHub repository imports successfully
- [ ] GitLab repository imports successfully
- [ ] Sync shows all 5 steps with progress
- [ ] Cancel stops the background job
- [ ] Repository overview loads with correct language breakdown
- [ ] Stale state shows when sync is >24h old
- [ ] All screens handle Loading/Success/Empty/Error
- [ ] File paths validated (no path traversal)
- [ ] Sync runs in WorkManager (not on main thread)
