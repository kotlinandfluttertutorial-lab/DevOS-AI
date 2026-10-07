# Kiro Prompt — DEVOS-015: Repository Clone and Indexing Service

**Jira:** DA-27 (DEVOS-015)  
**Epic:** DEVOS-E02  
**Assignee:** JetpackCompose (Track B — Backend / Data / Services)  
**AI-SDLC Phase:** IMPLEMENT  
**Status in Jira:** In Progress  

> **This prompt is for JetpackCompose's Kiro session only.**  
> Open this workspace in Kiro on your machine, start a new chat, and paste the prompt below.

---

## Setup before you start

1. Make sure you have the latest code: `git pull origin main`  
2. Confirm these commits exist in your log (`git log --oneline -8`):
   - `feat(security): add EncryptedSharedPreferences token repository and task plans` — DEVOS-012 ✅
   - `feat(auth): DEVOS-009/010 splash + onboarding screens` — DEVOS-009/010 ✅
3. Open Kiro in this workspace: `j:\Android\AndroidStudioProjects\Kiro\DevOS\DevOS-AI`

---

## Prompt (paste this into your Kiro chat)

You are implementing **DEVOS-015: Repository clone and indexing service** for DevOS AI — an Android app (Kotlin, Jetpack Compose, Clean Architecture, Hilt, Room, WorkManager).

**Your role:** You are the Track B (Backend / Data / Services) developer. Do NOT touch Compose screens or ViewModels — those belong to the other developer running in parallel on Track A. Your output is the data layer that their screens call into.

**Jira ticket:** DA-27 — already set to "In Progress" and assigned to you. Transition it to "Done" only after all ACs pass with evidence.

---

### Step 0 — Read these files first (in order)

1. `docs/jira/kiro-prompts/DEVOS-013-repository-intelligence.md` — full epic spec; only implement the domain/data sections
2. `.kiro/specs/repository-intelligence/core.md` — architecture spec for this epic
3. `core/core-security/src/main/kotlin/com/devos/ai/core/security/SecureTokenRepository.kt`
4. `core/core-security/src/main/kotlin/com/devos/ai/core/security/TokenKey.kt`
5. `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/model/OAuthProvider.kt` — implements TokenKey; use for token retrieval
6. `core/core-database/build.gradle.kts`
7. `data/data-repository/build.gradle.kts`
8. `domain/domain-repository/build.gradle.kts`
9. `gradle/libs.versions.toml` — check before adding any new dependency
10. `.kiro/implementation-status.md` — current project state

---

### Step 1 — Domain layer (`domain/domain-repository/`)

This module is **pure Kotlin — zero Android imports**. Create these files:

```kotlin
// Repository.kt
data class Repository(
    val id: String,
    val name: String,
    val owner: String,
    val fullName: String,           // "owner/name"
    val description: String?,
    val language: String?,
    val stars: Int = 0,
    val forks: Int = 0,
    val defaultBranch: String = "main",
    val cloneUrl: String,
    val provider: RepositoryProvider,
    val healthScore: Float = 0f,
    val lastSyncAt: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
)

enum class RepositoryProvider { GITHUB, GITLAB, LOCAL }
enum class SyncStatus { IDLE, SYNCING, SYNCED, ERROR }
```

```kotlin
// SyncProgress.kt
data class SyncProgress(
    val repoId: String,
    val currentStep: SyncStep,
    val completedSteps: List<SyncStep> = emptyList(),
    val errorMessage: String? = null,
)

enum class SyncStep(val label: String) {
    CLONE("Clone repository"),
    PARSE("Parse files"),
    INDEX_SYMBOLS("Index symbols"),
    BUILD_VECTORS("Build vector store"),
    DONE("Complete"),
}
```

```kotlin
// RepositoryRepository.kt
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

> **These method signatures are frozen.** Track A's ViewModels inject this interface. Changing signatures breaks their work.

---

### Step 2 — Room entities + DAOs (`core/core-database/`)

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
    val lastSyncAt: Long?,
    val syncStatus: String,
    val localPath: String?,
)

@Entity(
    tableName = "repository_files",
    foreignKeys = [ForeignKey(
        entity = RepositoryEntity::class,
        parentColumns = ["id"], childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("repoId")],
)
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

@Entity(
    tableName = "symbols",
    foreignKeys = [ForeignKey(
        entity = RepositoryEntity::class,
        parentColumns = ["id"], childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("repoId")],
)
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
    val visibility: String,
)
```

DAOs:
- **RepositoryDao:** `observeAll(): Flow<List<RepositoryEntity>>`, `observeById(id): Flow<RepositoryEntity?>`, `upsert(entity)`, `updateSyncStatus(id, status)`, `updateLocalPath(id, path)`, `delete(id)`
- **FileDao:** `insertAll(files)`, `getByRepo(repoId): List<FileEntity>`, `deleteByRepo(repoId)`
- **SymbolDao:** `insertAll(symbols)` stub, `getByRepo(repoId)`, `deleteByRepo(repoId)` — leave extraction logic empty; DEVOS-023 fills that in

Register all three entities on the app's `RoomDatabase` and bump the schema version with an **explicit migration** (never `fallbackToDestructiveMigration()`).

---

### Step 3 — `RepositoryIndexingWorker` (`data/data-repository/`)

```kotlin
@HiltWorker
class RepositoryIndexingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repositoryDao: RepositoryDao,
    private val fileDao: FileDao,
    private val tokenRepository: SecureTokenRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_REPO_ID   = "repo_id"
        const val KEY_CLONE_URL = "clone_url"
        const val KEY_PROVIDER  = "provider"
        const val KEY_STEP      = "current_step"
        const val KEY_ERROR     = "error_message"
    }

    override suspend fun doWork(): Result {
        val repoId    = inputData.getString(KEY_REPO_ID)   ?: return Result.failure()
        val cloneUrl  = inputData.getString(KEY_CLONE_URL) ?: return Result.failure()
        val provider  = OAuthProvider.valueOf(
            inputData.getString(KEY_PROVIDER) ?: OAuthProvider.GITHUB.name
        )

        return try {
            repositoryDao.updateSyncStatus(repoId, SyncStatus.SYNCING.name)
            setProgress(workDataOf(KEY_STEP to SyncStep.CLONE.name))

            val localPath = cloneRepository(cloneUrl, repoId, provider)
            repositoryDao.updateLocalPath(repoId, localPath)
            setProgress(workDataOf(KEY_STEP to SyncStep.PARSE.name))

            val files = parseFiles(localPath, repoId)
            fileDao.insertAll(files)
            setProgress(workDataOf(KEY_STEP to SyncStep.DONE.name))

            repositoryDao.updateSyncStatus(repoId, SyncStatus.SYNCED.name)
            Result.success()
        } catch (e: CancellationException) {
            repositoryDao.updateSyncStatus(repoId, SyncStatus.IDLE.name)
            Result.failure()
        } catch (e: Exception) {
            repositoryDao.updateSyncStatus(repoId, SyncStatus.ERROR.name)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    private suspend fun cloneRepository(
        url: String, repoId: String, provider: OAuthProvider,
    ): String = withContext(Dispatchers.IO) {
        val localDir = File(applicationContext.filesDir, "repos/$repoId")
        localDir.mkdirs()
        val token = tokenRepository.getToken(provider)
        val cmd = Git.cloneRepository().setURI(url).setDirectory(localDir).setDepth(1)
        if (token != null) {
            // NEVER log raw token — only token.take(4)+"****"
            cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider("oauth2", token))
        }
        cmd.call().close()
        localDir.absolutePath
    }

    private suspend fun parseFiles(
        localPath: String, repoId: String,
    ): List<FileEntity> = withContext(Dispatchers.IO) {
        val root     = File(localPath)
        val skipDirs = setOf(".git", "build", ".gradle", ".idea", "node_modules")

        // Incremental: fetch existing hashes
        val existing = fileDao.getByRepo(repoId).associateBy { it.path }
        val onDisk   = mutableSetOf<String>()

        val changed = root.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                skipDirs.none { skip ->
                    file.absolutePath.contains(File.separator + skip + File.separator) ||
                    file.absolutePath.endsWith(File.separator + skip)
                }
            }
            .mapNotNull { file ->
                // PATH TRAVERSAL PROTECTION — reject paths that escape the repo root
                val resolved = file.canonicalPath
                require(resolved.startsWith(root.canonicalPath)) {
                    "Path traversal rejected: $resolved"
                }
                val rel = resolved.removePrefix(root.canonicalPath).trimStart(File.separatorChar)
                onDisk += rel
                val hash = computeHash(file)
                if (existing[rel]?.contentHash == hash) null  // unchanged — skip
                else FileEntity(
                    id           = "$repoId:$rel",
                    repoId       = repoId,
                    path         = rel,
                    name         = file.name,
                    extension    = file.extension,
                    sizeBytes    = file.length(),
                    lastModified = file.lastModified(),
                    contentHash  = hash,
                )
            }
            .toList()

        // Delete rows for files removed from disk
        val removed = existing.keys - onDisk
        if (removed.isNotEmpty()) {
            removed.forEach { path ->
                existing[path]?.let { fileDao.deleteByPath(repoId, path) }
            }
        }
        changed
    }

    private fun computeHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(8192)
            var n: Int
            while (stream.read(buf).also { n = it } != -1) digest.update(buf, 0, n)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
```

**JGit dependency** — add to `data/data-repository/build.gradle.kts` (check `libs.versions.toml` first; if a `jgit` alias exists, use it):
```
implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")
```

---

### Step 4 — `RepositoryRepositoryImpl` (`data/data-repository/`)

Implements `RepositoryRepository`. Key behaviour:

- `importRepository(cloneUrl, provider)` → generate UUID repoId → insert `RepositoryEntity(syncStatus=IDLE)` → enqueue worker → return `Result.success(repoId)`
- `syncRepository(repoId)` → re-enqueue worker for existing repo
- `cancelSync(repoId)` → `WorkManager.cancelUniqueWork("index_$repoId")`
- `observeSyncProgress(repoId)` → observe `WorkManager.getWorkInfosForUniqueWorkFlow("index_$repoId")` → map `WorkInfo` state + progress data to `SyncProgress`

Enqueue pattern:
```kotlin
WorkManager.getInstance(context).enqueueUniqueWork(
    "index_$repoId",
    ExistingWorkPolicy.REPLACE,
    OneTimeWorkRequestBuilder<RepositoryIndexingWorker>()
        .setInputData(workDataOf(
            KEY_REPO_ID   to repoId,
            KEY_CLONE_URL to cloneUrl,
            KEY_PROVIDER  to provider.name,
        ))
        .setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        )
        .build(),
)
```

---

### Step 5 — Hilt binding (`data/data-repository/`)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRepositoryRepository(
        impl: RepositoryRepositoryImpl,
    ): RepositoryRepository
}
```

---

### Tests to write

| File | What to test |
|------|-------------|
| `PathTraversalTest` | `../../etc/passwd` in parsed path throws `IllegalArgumentException` |
| `RepositoryDaoTest` | Room in-memory DB: upsert, `observeAll` Flow emission, `updateSyncStatus` |
| `FileDaoTest` | `insertAll`, `getByRepo`, `deleteByRepo` |
| `RepositoryRepositoryImplTest` | `importRepository` enqueues unique WorkManager job; `cancelSync` cancels it; `observeSyncProgress` maps `WorkInfo` state → `SyncProgress` |

---

### Architecture rules — non-negotiable

- `domain-repository` is **pure Kotlin** — zero `android.*` imports
- **All indexing runs in `RepositoryIndexingWorker`** (WorkManager) — never on the main thread
- Room migrations **must be explicit** — never `fallbackToDestructiveMigration()`
- Tokens from `SecureTokenRepository` only — never `BuildConfig`, never hardcoded, never logged raw (mask as `token.take(4)+"****"`)
- Feature modules import `domain-repository` only — never `data-repository` directly

---

### Verification (run all before marking Done)

```bash
./gradlew :core:core-database:assembleDebug
./gradlew :domain:domain-repository:assembleDebug
./gradlew :data:data-repository:assembleDebug
./gradlew :data:data-repository:testDebugUnitTest
./gradlew assembleDebug
```

---

### When all pass — update both systems

1. **Local tracker** — `j:\Android\AndroidStudioProjects\Kiro\DevOS\DevOS-AI\.kiro\implementation-status.md`  
   Flip DEVOS-015 row from 🟡 to 🟢, fill in AC count, add a row to the Completion Log table.

2. **Jira** — Transition DA-27 to "Done" (project `DA`, site `androidassistant.atlassian.net`, cloudId `369f94fe-ea26-41c7-82b3-73ce0c43cce8`). Leave a comment on DA-27 with the verification output as evidence.

3. **Commit:**
   ```
   feat(repository): DEVOS-015 repository clone and indexing service
   ```

4. **Your next ticket after this one:** DEVOS-023 (DA-35) — Symbol indexing service. Check `.kiro/implementation-status.md` for the full list of your tickets in order.

---

*Prompt saved: `docs/jira/kiro-prompts/DEVOS-015-repository-indexing-JetpackCompose.md`*
