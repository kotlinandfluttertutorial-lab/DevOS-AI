# Spec: Code Intelligence — Core

**Jira:** DEVOS-018 / DEVOS-019 / DEVOS-020 / DEVOS-021 / DEVOS-022 / DEVOS-023 / DEVOS-024 / DEVOS-025  
**Epic:** DEVOS-E03  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement file explorer, code viewer with syntax highlighting, code search, symbol details, dependency graph, and architecture overview.

## Domain Models

```kotlin
data class CodeFile(
    val repoId: String,
    val path: String,
    val name: String,
    val extension: String,
    val size: Long,
    val language: CodeLanguage,
    val lineCount: Int,
    val lastModified: Long,
)

data class Symbol(
    val id: String,
    val repoId: String,
    val filePath: String,
    val name: String,
    val kind: SymbolKind,       // CLASS, FUNCTION, INTERFACE, ENUM, PROPERTY
    val lineStart: Int,
    val lineEnd: Int,
    val signature: String,
    val documentation: String?,
    val references: List<SymbolReference>,
    val dependencies: List<String>,
)

data class DependencyNode(
    val id: String,
    val name: String,
    val kind: NodeKind,        // MODULE, CLASS, PACKAGE
    val filePath: String?,
    val dependencies: List<String>,   // node IDs this node depends on
    val dependents: List<String>,     // node IDs that depend on this
    val isHotspot: Boolean,           // high coupling/complexity
)
```

## Screens

### FileExplorerScreen (FIGMA-10)
- Lazy tree with expand-on-demand
- Breadcrumb bar updating on navigation
- File type icons
- File quick actions: Open | Ask AI | Git history

### CodeViewerScreen (FIGMA-11)
- Syntax-highlighted via `DevOSCodeBlock`
- Line numbers, scroll to line via deep link
- Symbol tap → navigate to SymbolDetailsScreen
- Bottom AI action bar: Explain | Debug | Find Usages | Generate Tests | Ask AI
- Languages: Kotlin, Java, Swift, Python, JavaScript, TypeScript, Go, Rust, C++

### CodeSearchScreen (FIGMA-12)
- Full-text search via indexed trigrams
- Semantic search toggle (vector similarity)
- Filters: file type, scope, case-sensitive, regex
- Result: file path + line + context snippet + match highlight

### SymbolDetailsScreen (FIGMA-13)
- Symbol kind badge (class/fun/interface/etc.)
- File location + tap to open
- KDoc/Javadoc rendered as markdown
- References list (where used)
- Dependencies list (what it calls)
- AI explanation section

### DependencyGraphScreen (FIGMA-14)
- Canvas-based graph (Compose `Canvas` or Jetpack Graph library)
- Zoom + pan (pinch + drag)
- Tap node → detail panel slides up
- Cycle detection: highlight circular dependencies in error color
- Filter: show only cycles | show hotspots | by module

### ArchitectureOverviewScreen (FIGMA-15)
- AI-detected architecture pattern label (MVVM, MVC, Clean, etc.)
- Layer diagram (simplified box diagram)
- Module breakdown list with coupling scores
- Hotspot list (high cyclomatic complexity)
- Ask AI action → opens chat with architecture context

## Syntax Highlighting Tokens
```kotlin
enum class SyntaxToken {
    KEYWORD, STRING, NUMBER, COMMENT, TYPE, FUNCTION,
    VARIABLE, OPERATOR, ANNOTATION, IMPORT, PUNCTUATION, PLAIN
}
```

## Acceptance Criteria
- [ ] File explorer loads and navigates nested directories
- [ ] Code viewer shows correct syntax for Kotlin and Java
- [ ] Symbol click navigates to symbol details
- [ ] AI action bar sends correct context to AI chat
- [ ] Code search returns results within 2 seconds
- [ ] Semantic search returns relevant results
- [ ] Symbol details show references and dependencies
- [ ] Dependency graph renders 100-node project within 3 seconds
- [ ] Architecture overview shows AI-generated summary
- [ ] All screens: Loading/Success/Empty/Error states
