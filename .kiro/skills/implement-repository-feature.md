---
name: implement-repository-feature
description: Implement repository intelligence features — import, sync, indexing, file exploration, and code search
---

# Skill: Implement Repository Intelligence Feature

## When to Use
Use when implementing anything that involves reading, importing, indexing, or querying repository content.

## Repository Data Flow

```
User triggers import/sync
    ↓
RepositoryImportUseCase / SyncRepositoryUseCase
    ↓
GitRepository.clone() / .fetch()          ← JGit / git CLI
    ↓
RepositoryIndexWorker (WorkManager)
    ├── FileIndexer → FileEntity (Room)
    ├── SymbolExtractor → SymbolEntity (Room)
    ├── CodeGraphBuilder → GraphEntity (Room)
    └── EmbeddingWorker → VectorStore (embeddings)
    ↓
RepositorySyncScreen shows progress via Flow<SyncProgress>
```

## Key Rules

### Never Block the Main Thread
```kotlin
// CORRECT
@HiltWorker
class RepositoryIndexWorker @AssistedInject constructor(...) : CoroutineWorker(...) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        indexRepositoryUseCase(repoId)
        Result.success()
    }
}

// WRONG
fun indexInViewModel() {
    viewModelScope.launch { // this is still main-thread dispatched by default for heavy work
        Files.walk(repoPath).forEach { /* heavy IO on wrong dispatcher */ }
    }
}
```

### Incremental Sync
Only re-index changed files:
```kotlin
suspend fun incrementalSync(repoId: String, sinceCommit: String) {
    val changedFiles = gitRepository.getChangedFiles(repoId, sinceCommit)
    changedFiles.forEach { path ->
        fileIndexer.reindexFile(repoId, path)
        symbolExtractor.reextractSymbols(repoId, path)
    }
    embeddingWorker.updateEmbeddings(repoId, changedFiles)
}
```

### File Path Security
```kotlin
fun validateFilePath(repoRoot: Path, requestedPath: String): Path {
    val resolved = repoRoot.resolve(requestedPath).normalize()
    require(resolved.startsWith(repoRoot)) {
        "Path traversal detected: $requestedPath"
    }
    return resolved
}
```

### Sync Progress via Flow
```kotlin
sealed interface SyncProgress {
    data class Step(val name: String, val completed: Boolean, val current: Boolean) : SyncProgress
    data class Overall(val percent: Int) : SyncProgress
    data object Complete : SyncProgress
    data class Failed(val step: String, val error: String) : SyncProgress
}

// Repository emits steps:
fun syncRepository(repoId: String): Flow<SyncProgress> = flow {
    emit(SyncProgress.Step("Fetching remote", false, true))
    gitRepository.fetch(repoId)
    emit(SyncProgress.Step("Fetching remote", true, false))
    emit(SyncProgress.Step("Indexing files", false, true))
    // ...
}
```

## File Explorer Implementation Notes

### Lazy Tree Loading
Do NOT load the entire tree upfront:
```kotlin
data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList(), // loaded on expand
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
)

fun onExpandDirectory(path: String) {
    viewModelScope.launch {
        _uiState.update { state ->
            (state as? FileExplorerUiState.Success)?.let { s ->
                s.copy(tree = s.tree.setLoading(path, true))
            } ?: state
        }
        val children = getFilesUseCase(path)
        _uiState.update { state ->
            (state as? FileExplorerUiState.Success)?.let { s ->
                s.copy(tree = s.tree.expandNode(path, children))
            } ?: state
        }
    }
}
```

### Code Viewer — Viewport Rendering
For large files (>1000 lines), only parse tokens for visible viewport:
```kotlin
@Composable
fun SyntaxHighlightedCode(
    lines: List<String>,
    listState: LazyListState = rememberLazyListState(),
) {
    val visibleRange by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val last = min(first + listState.layoutInfo.visibleItemsInfo.size + 5, lines.size)
            first..last
        }
    }
    LazyColumn(state = listState) {
        itemsIndexed(lines) { index, line ->
            val tokens = if (index in visibleRange) syntaxHighlight(line) else listOf(PlainToken(line))
            CodeLine(lineNumber = index + 1, tokens = tokens)
        }
    }
}
```

## Repository Feature Checklist
- [ ] All file I/O on `Dispatchers.IO`
- [ ] Large trees loaded lazily (expand-on-demand)
- [ ] File paths validated against repo root (no traversal)
- [ ] Sync progress emitted as `Flow<SyncProgress>`
- [ ] WorkManager used for background indexing
- [ ] Incremental sync implemented
- [ ] Code viewer renders only visible lines
- [ ] Language detection by file extension
- [ ] Empty state when repository has no files
- [ ] Error state when clone/fetch fails with retry
