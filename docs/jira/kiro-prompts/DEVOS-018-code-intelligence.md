# Kiro Prompt — DEVOS-018: Code Intelligence (File Explorer, Search, Symbols, Graph, Architecture)

**Jira:** DEVOS-018 / DEVOS-021 / DEVOS-022 / DEVOS-023 / DEVOS-024 / DEVOS-025  
**Epic:** DEVOS-E03  
**Figma:** FIGMA-10 / FIGMA-12 / FIGMA-13 / FIGMA-14 / FIGMA-15  
**Kiro Spec:** `.kiro/specs/code-intelligence/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the Code Intelligence module for DevOS AI: file explorer tree, code search, symbol details, symbol indexing service, dependency graph, and AI-powered architecture overview.

**Existing files to read first:**
- `.kiro/specs/code-intelligence/core.md`
- `docs/figma/screen-inventory.md` — Screens FIGMA-10, FIGMA-12, FIGMA-13, FIGMA-14, FIGMA-15
- `docs/figma/component-inventory.md` — DevOSCodeBlock, DevOSSearchBar
- `core/core-database/` — existing SymbolEntity, FileEntity (written in DEVOS-013)
- `domain/domain-repository/` — existing domain models

**Modules:**
- `feature/feature-code/` — screens + ViewModels
- `domain/domain-code/` — interfaces + models
- `data/data-code/` — implementations

**Package:** `com.devos.ai.feature.code`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- Code viewer renders ONLY visible lines — never load entire file into memory for large files (>1000 lines).
- Search debounce: 300ms minimum before firing a search query.
- Dependency graph Canvas rendering must stay within the 16ms frame budget for ≤100 nodes.
- AI architecture summary streams via Flow — never block UI waiting for full response.

**What to implement:**

### 1. FileExplorerScreen (DEVOS-018, FIGMA-10)

Route: `FILE_EXPLORER/{repoId}`

Domain models:
```kotlin
data class FileTreeNode(
    val id: String,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileTreeNode> = emptyList(),
    val isExpanded: Boolean = false,
    val depth: Int = 0,
    val fileType: FileType,
)

enum class FileType {
    KOTLIN, JAVA, XML, JSON, YAML, MARKDOWN, GRADLE, OTHER, DIRECTORY
}
```

UiState:
```kotlin
sealed interface FileExplorerUiState {
    data object Loading : FileExplorerUiState
    data class Success(
        val tree: List<FileTreeNode>,       // flat list, depth used for indentation
        val breadcrumb: List<String>,
        val currentPath: String,
    ) : FileExplorerUiState
    data object Empty : FileExplorerUiState
    data class Error(val message: String, val retryable: Boolean) : FileExplorerUiState
}
```

Layout:
```kotlin
Scaffold(
    topBar = {
        Column {
            DevOSTopBar(title = "Files", onBack = onBack)
            // Breadcrumb row
            LazyRow(Modifier.padding(horizontal = MaterialTheme.spacing.base)) {
                itemsIndexed(breadcrumb) { index, segment ->
                    Text(segment, style = MaterialTheme.typography.labelSmall,
                        color = if (index == breadcrumb.lastIndex)
                            MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onBreadcrumbTap(index) })
                    if (index < breadcrumb.lastIndex) {
                        Text(" / ", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
) { padding ->
    LazyColumn(Modifier.padding(padding)) {
        items(tree, key = { it.path }) { node ->
            FileTreeRow(
                node = node,
                onToggle = { onToggleExpand(node.path) },
                onFileClick = { onNavigateToFile(node.path) },
            )
        }
    }
}

@Composable
fun FileTreeRow(node: FileTreeNode, onToggle: () -> Unit, onFileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.depth * 16).dp)
            .clickable { if (node.isDirectory) onToggle() else onFileClick() }
            .padding(vertical = MaterialTheme.spacing.xs,
                horizontal = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        if (node.isDirectory) {
            Icon(
                if (node.isExpanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                contentDescription = if (node.isExpanded) "Expanded folder" else "Collapsed folder",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        } else {
            FileTypeIcon(node.fileType)
        }
        Text(node.name, style = MaterialTheme.typography.bodyMedium)
    }
}
```

### 2. CodeSearchScreen (DEVOS-021, FIGMA-12)

Route: `CODE_SEARCH/{repoId}`

UiState:
```kotlin
sealed interface CodeSearchUiState {
    data object Idle : CodeSearchUiState
    data object Searching : CodeSearchUiState
    data class Success(val results: List<SearchResult>, val query: String) : CodeSearchUiState
    data object Empty : CodeSearchUiState
    data class Error(val message: String, val retryable: Boolean) : CodeSearchUiState
}

data class SearchResult(
    val filePath: String,
    val fileName: String,
    val lineNumber: Int,
    val lineContent: String,
    val matchStart: Int,
    val matchEnd: Int,
    val language: String,
)
```

Layout:
```kotlin
Scaffold(topBar = {
    DevOSTopBar(title = "Code Search") {
        DevOSSearchBar(
            query = query,
            onQueryChange = { viewModel.onQueryChange(it) },
            placeholder = "Search code…",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}) { padding ->
    Column(Modifier.padding(padding)) {
        // Filter row
        Row(Modifier.padding(horizontal = MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            FilterChip(selected = searchMode == FULL_TEXT, onClick = { onModeChange(FULL_TEXT) },
                label = { Text("Full Text") })
            FilterChip(selected = searchMode == SEMANTIC, onClick = { onModeChange(SEMANTIC) },
                label = { Text("Semantic") })
            FilterChip(selected = regexEnabled, onClick = { onRegexToggle() },
                label = { Text("Regex") })
        }
        when (val s = uiState) {
            is Idle      -> SearchIdleState()
            is Searching -> DevOSLoadingState()
            is Empty     -> DevOSEmptyState(icon = Icons.Outlined.SearchOff,
                title = "No results", description = "Try a different search term")
            is Error     -> DevOSErrorState(description = s.message,
                onRetry = if (s.retryable) viewModel::retry else null)
            is Success   -> LazyColumn {
                items(s.results, key = { "${it.filePath}:${it.lineNumber}" }) { result ->
                    SearchResultItem(result = result,
                        onClick = { onNavigateToCode(result.filePath, result.lineNumber) })
                }
            }
        }
    }
}
```

Search result item shows: file path chip, line number, match highlighted in `AnnotatedString` using `SpanStyle(background = MaterialTheme.colorScheme.primaryContainer)`.

Debounce: `viewModel.onQueryChange` uses `debounce(300)` on a `MutableStateFlow<String>`.

### 3. SymbolDetailsScreen (DEVOS-022, FIGMA-13)

Route: `SYMBOL_DETAILS/{repoId}/{symbolId}`

UiState:
```kotlin
sealed interface SymbolDetailsUiState {
    data object Loading : SymbolDetailsUiState
    data class Success(
        val symbol: Symbol,
        val references: List<SymbolReference>,
        val dependencies: List<Symbol>,
        val aiExplanation: String?,
        val isLoadingExplanation: Boolean,
    ) : SymbolDetailsUiState
    data object Empty : SymbolDetailsUiState
    data class Error(val message: String, val retryable: Boolean) : SymbolDetailsUiState
}
```

Layout:
```kotlin
Scaffold(topBar = { DevOSTopBar(title = symbol.name, onBack = onBack) }) { padding ->
    LazyColumn(Modifier.padding(padding).padding(MaterialTheme.spacing.base)) {
        item {
            // Symbol header
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                DevOSStatusBadge(symbol.kind.label, MaterialTheme.colorScheme.primary)
                DevOSStatusBadge(symbol.visibility.label, MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            Text(symbol.filePath, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Line ${symbol.lineStart}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (symbol.signature != null) {
            item {
                DevOSCodeBlock(code = symbol.signature, language = "kotlin",
                    showLineNumbers = false)
            }
        }
        if (symbol.docComment != null) {
            item {
                SectionHeader("Documentation")
                DevOSMarkdownText(markdown = symbol.docComment)
            }
        }
        item { SectionHeader("References (${references.size})") }
        items(references) { ref ->
            ReferenceItem(ref, onClick = { onNavigateToCode(ref.filePath, ref.lineNumber) })
        }
        item { SectionHeader("Dependencies") }
        items(dependencies) { dep ->
            SymbolChip(dep, onClick = { onNavigateToSymbol(dep.id) })
        }
        item {
            DevOSButton("Explain with AI", onClick = onExplain,
                leadingIcon = { Icon(Icons.Outlined.SmartToy, null) },
                modifier = Modifier.fillMaxWidth())
        }
    }
}
```

### 4. Symbol Indexing Service (DEVOS-023)

File: `data/data-code/src/main/kotlin/com/devos/ai/data/code/indexer/SymbolIndexer.kt`

Runs within `RepositoryIndexingWorker` (DEVOS-015). For each `.kt` / `.java` file:

```kotlin
class SymbolIndexer @Inject constructor() {

    fun extractSymbols(fileContent: String, filePath: String, repoId: String): List<SymbolEntity> {
        val symbols = mutableListOf<SymbolEntity>()

        // Kotlin: regex for class, object, interface, fun, val, var at top level and class members
        val kotlinPatterns = mapOf(
            "CLASS"     to Regex("""^(public|private|internal|)?\s*(data\s+)?class\s+(\w+)""", MULTILINE),
            "OBJECT"    to Regex("""^(public|private|internal|)?\s*object\s+(\w+)""", MULTILINE),
            "INTERFACE" to Regex("""^(public|private|internal|)?\s*interface\s+(\w+)""", MULTILINE),
            "FUNCTION"  to Regex("""^(public|private|internal|protected|)?\s*(suspend\s+)?fun\s+(\w+)\s*\(""", MULTILINE),
        )
        // Apply patterns, extract line numbers, create SymbolEntity
        // For reference resolution: cross-reference import statements with known symbols
        return symbols
    }
}
```

For incremental updates: only re-index files where `contentHash` changed (compare against stored hash).

### 5. DependencyGraphScreen (DEVOS-024, FIGMA-14)

Route: `DEP_GRAPH/{repoId}`

UiState:
```kotlin
data class GraphNode(val id: String, val label: String, val kind: String, val isCycle: Boolean)
data class GraphEdge(val from: String, val to: String)

sealed interface DependencyGraphUiState {
    data object Loading : DependencyGraphUiState
    data class Success(
        val nodes: List<GraphNode>,
        val edges: List<GraphEdge>,
        val selectedNode: GraphNode?,
        val filter: GraphFilter,
    ) : DependencyGraphUiState
    data object Empty : DependencyGraphUiState
    data class Error(val message: String, val retryable: Boolean) : DependencyGraphUiState
}
```

Layout:
```kotlin
Scaffold(topBar = { DevOSTopBar(title = "Dependency Graph") }) { padding ->
    Box(Modifier.padding(padding)) {
        // Graph canvas
        val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale = (scale * zoomChange).coerceIn(0.2f, 5f)
            offset += offsetChange
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState)
                .graphicsLayer(scaleX = scale, scaleY = scale,
                    translationX = offset.x, translationY = offset.y)
        ) {
            // Draw edges first
            edges.forEach { edge -> drawEdge(nodePositions, edge) }
            // Draw nodes on top
            nodes.forEach { node ->
                drawNode(
                    position = nodePositions[node.id] ?: Offset.Zero,
                    label = node.label,
                    color = if (node.isCycle) cycleColor else nodeColor,
                    isSelected = node == selectedNode,
                )
            }
        }

        // Filter chips overlay
        Row(Modifier.align(Alignment.TopStart).padding(MaterialTheme.spacing.base)) {
            FilterChip(selected = filter == MODULES, onClick = { onFilterChange(MODULES) },
                label = { Text("Modules") })
            FilterChip(selected = filter == CLASSES, onClick = { onFilterChange(CLASSES) },
                label = { Text("Classes") })
        }

        // Selected node detail bottom sheet
        selectedNode?.let { node ->
            ModalBottomSheet(onDismissRequest = { onDeselectNode() }) {
                NodeDetailSheet(node, onNavigateToSymbol = { onNavigateToSymbol(node.id) })
            }
        }
    }
}
```

Node position layout: force-directed algorithm (simple spring model) computed off-main-thread in a `LaunchedEffect`. Cycle detection: DFS; mark edges and nodes in cycles with error color.

### 6. ArchitectureOverviewScreen (DEVOS-025, FIGMA-15)

Route: `ARCH_OVERVIEW/{repoId}`

UiState:
```kotlin
sealed interface ArchitectureUiState {
    data object Loading : ArchitectureUiState
    data class Success(
        val detectedStyle: ArchitectureStyle, // CLEAN, MVVM, MVC, MVP, UNKNOWN
        val summary: String,
        val isStreamingSummary: Boolean,
        val layers: List<ArchitectureLayer>,
        val hotspots: List<Hotspot>,
        val aiInsights: List<String>,
    ) : ArchitectureUiState
    data object Empty : ArchitectureUiState
    data class Error(val message: String, val retryable: Boolean) : ArchitectureUiState
}
```

Layout:
```kotlin
LazyColumn(Modifier.padding(MaterialTheme.spacing.base)) {
    item {
        // Architecture style banner
        DevOSCard {
            Row(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
                Icon(Icons.Outlined.AccountTree, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(MaterialTheme.spacing.sm))
                Column {
                    Text("Architecture Style", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(detectedStyle.label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    item {
        SectionHeader("Summary")
        DevOSAIMessage(
            content = summary,
            isStreaming = isStreamingSummary,
        )
    }
    item { SectionHeader("Layer Breakdown") }
    items(layers) { layer -> ArchitectureLayerCard(layer) }
    item { SectionHeader("Hotspots") }
    items(hotspots) { hotspot ->
        HotspotItem(hotspot,
            onClick = { onNavigateToFile(hotspot.filePath) })
    }
    item {
        FloatingActionButton(
            onClick = onAskAI,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.SmartToy, contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.spacing.sm))
                Text("Ask AI about architecture")
            }
        }
    }
}
```

AI summary streamed via `GetArchitectureSummaryUseCase` which calls `AIRepository.chat()` with a structured prompt about the repo's symbol structure. Updates `DevOSAIMessage` incrementally.

**Tests:**
- `FileExplorerViewModelTest`: tree loads; expand/collapse toggles correctly; breadcrumb updates on navigation
- `CodeSearchViewModelTest`: query debounced; full-text returns results; semantic toggle changes search mode; empty state on no results
- `SymbolDetailsViewModelTest`: symbol loads; references populated; AI explanation streaming
- `SymbolIndexerTest`: Kotlin class extracted; function signatures extracted; incremental update skips unchanged files
- `DependencyGraphViewModelTest`: nodes and edges computed; cycle detection marks cyclic nodes; filter changes graph content
- `ArchitectureViewModelTest`: style detected; AI summary streams; hotspots identified

**Acceptance Criteria:**
- AC1 (DEVOS-018): Directory tree renders with expand/collapse on directory nodes
- AC2 (DEVOS-018): File type icons shown per file extension (Kotlin, Java, XML, etc.)
- AC3 (DEVOS-018): Breadcrumb row updates as user navigates into subdirectories
- AC4 (DEVOS-018): Tapping a file navigates to `CodeViewerScreen`
- AC5 (DEVOS-021): Full-text search returns results within 2 seconds
- AC6 (DEVOS-021): Semantic search toggle is available and changes result ranking
- AC7 (DEVOS-021): Filters for file type, scope, and regex mode available via filter chips
- AC8 (DEVOS-021): Results show file path, line number, and match highlighted in context
- AC9 (DEVOS-021): Empty state shown when no results; error state on search failure
- AC10 (DEVOS-022): Symbol name, kind badge, and visibility badge shown
- AC11 (DEVOS-022): File path and line number link to `CodeViewerScreen`
- AC12 (DEVOS-022): KDoc/Javadoc rendered via `DevOSMarkdownText`
- AC13 (DEVOS-022): References list navigable; dependencies list shows symbol chips
- AC14 (DEVOS-022): "Explain with AI" opens AI Chat with symbol context pre-filled
- AC15 (DEVOS-023): Kotlin classes, functions, interfaces, and objects extracted
- AC16 (DEVOS-023): Symbols stored in Room and queryable by name, kind, and file path
- AC17 (DEVOS-023): Incremental sync only re-indexes files with changed content hash
- AC18 (DEVOS-024): Dependency graph renders nodes and edges for the repository
- AC19 (DEVOS-024): Zoom and pan gestures work smoothly within 16ms frame budget for ≤100 nodes
- AC20 (DEVOS-024): Cyclic dependencies highlighted in error color
- AC21 (DEVOS-024): Node tap shows detail bottom sheet; filter chips change graph scope
- AC22 (DEVOS-025): Architecture style detected and shown (Clean/MVVM/MVC/MVP)
- AC23 (DEVOS-025): Layer diagram displayed with module/package groupings
- AC24 (DEVOS-025): Hotspot analysis shows most-imported files
- AC25 (DEVOS-025): AI insights streamed via `DevOSAIMessage`; "Ask AI" FAB pre-fills chat
