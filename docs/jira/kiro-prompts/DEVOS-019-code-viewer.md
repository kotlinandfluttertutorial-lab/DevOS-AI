# Kiro Prompt — DEVOS-019: Code Viewer Screen

**Jira:** DEVOS-019 / DEVOS-020  
**Epic:** DEVOS-E03  
**Figma:** FIGMA-11  
**Kiro Spec:** `.kiro/specs/code-intelligence/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the Code Viewer screen — one of the most important screens in DevOS AI.

**Existing files to read first:**
- `.kiro/specs/code-intelligence/core.md`
- `docs/figma/screen-inventory.md` — Screen 11
- `docs/figma/component-inventory.md` — DevOSCodeBlock, DevOSSyntaxHighlight

**Module:** `feature/feature-code/`  
**Package:** `com.devos.ai.feature.code.viewer`

**Route:** `repository/{repoId}/file?path={path}&line={line}`

**What to implement:**

### 1. UiState
```
CodeViewerUiState:
  Loading
  Success(file, lines, highlightLines, currentLine, symbols)
  Error(message, retryable)
```

### 2. CodeViewerViewModel
- Load file content from `GetFileContentUseCase(repoId, path)`
- Scroll to line from navigation argument
- Symbol tap: navigate via `NavigateToSymbol(symbolId)` event
- AI actions: open chat with correct context

### 3. CodeViewerScreen Layout
```
DevOSTopBar(
  title = fileName (not full path),
  navigationIcon = BackButton,
  actions = [SearchInFile, CopyFile, ShareFile, MoreMenu]
)
Subtitle: full file path in DevOSCodeTextStyle

Content:
  LazyColumn(state = listState) {
    itemsIndexed(lines) { index, line ->
      CodeLineRow(
        lineNumber = index + 1,
        tokens = syntaxHighlight(line, language),
        highlighted = (index + 1) in highlightLines,
        onClick = { onSymbolTap(line, index) }
      )
    }
  }

Bottom action bar (persistent):
  [Explain] [Debug] [Find Usages] [Generate Tests] [Ask AI]
  Each opens AIChatScreen with file + selection context
```

### 4. Syntax Highlighting
Support at minimum: Kotlin, Java, Swift, Python, JavaScript, TypeScript

```kotlin
fun syntaxHighlight(line: String, language: CodeLanguage): List<SyntaxToken>
```

Use `SyntaxColors.*` for token colors:
- Keywords → `SyntaxColors.keyword` (#C792EA)
- Strings → `SyntaxColors.string` (#C3E88D)  
- Numbers → `SyntaxColors.number` (#F78C6C)
- Comments → `SyntaxColors.comment` (#546E7A)
- Types → `SyntaxColors.type` (#FFCB6B)
- Functions → `SyntaxColors.function` (#82AAFF)
- Background → `SyntaxColors.background` (#1E1E2E) — ALWAYS, both themes

### 5. Viewport-Only Rendering
For large files (>500 lines), only highlight visible lines + 10 buffer:
```kotlin
val visibleRange by remember {
    derivedStateOf {
        val first = listState.firstVisibleItemIndex
        val last = min(first + listState.layoutInfo.visibleItemsInfo.size + 10, lines.size)
        first..last
    }
}
```

### 6. Deep Link to Line
```kotlin
LaunchedEffect(targetLine) {
    if (targetLine > 0) listState.scrollToItem(targetLine - 1)
}
```

### 7. AI Action Bar
```kotlin
@Composable
fun CodeViewerAIActionBar(
    onExplain: () -> Unit,
    onDebug: () -> Unit,
    onFindUsages: () -> Unit,
    onGenerateTests: () -> Unit,
    onAskAI: () -> Unit,
)
```
Each action opens `AIChatScreen` with:
- `context = AIContext.File(repoId, path)`
- Pre-filled message: "Explain this code" / "Debug this" / etc.

**Tests to write:**
- `CodeViewerViewModelTest`: loads file, scrolls to line, handles missing file error
- `CodeViewerScreenTest`: renders with content, scroll to line, AI action bar taps
- `SyntaxHighlightTest`: verifies keyword detection for Kotlin and Java

**Acceptance Criteria:**
- [ ] File content loads and displays with correct line numbers
- [ ] Kotlin code has correct syntax highlighting
- [ ] Java code has correct syntax highlighting
- [ ] Deep link `devos://repo/{id}/file?path=X&line=42` scrolls to line 42
- [ ] Horizontal scroll works for long lines
- [ ] AI action bar opens chat with file context
- [ ] "Generate Tests" sends file to AI with test generation prompt
- [ ] Loading state shows while file is loading
- [ ] Error state shows if file not found or access denied
- [ ] Code background is always #1E1E2E (dark for code regardless of theme)
