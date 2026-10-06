# DevOS AI — Component Inventory

**Version:** 1.0  
**Date:** 2026-10-07  
**Package:** `com.devos.ai.designsystem.components`

---

## Component Index

| # | Component | File | Usage |
|---|-----------|------|-------|
| 01 | `DevOSButton` | `DevOSButton.kt` | Primary actions everywhere |
| 02 | `DevOSCard` | `DevOSCard.kt` | Content containers |
| 03 | `DevOSTopBar` | `DevOSTopBar.kt` | Screen top bars |
| 04 | `DevOSBottomBar` | `DevOSBottomBar.kt` | Primary navigation |
| 05 | `DevOSSearchBar` | `DevOSSearchBar.kt` | Search surfaces |
| 06 | `DevOSCodeBlock` | `DevOSCodeBlock.kt` | Code rendering |
| 07 | `DevOSSourceReference` | `DevOSSourceReference.kt` | AI source evidence |
| 08 | `DevOSStatusBadge` | `DevOSStatusBadge.kt` | Status indicators |
| 09 | `DevOSHealthIndicator` | `DevOSHealthIndicator.kt` | Project/repo health |
| 10 | `DevOSProjectCard` | `DevOSProjectCard.kt` | Project list items |
| 11 | `DevOSRepositoryCard` | `DevOSRepositoryCard.kt` | Repository list items |
| 12 | `DevOSAIMessage` | `DevOSAIMessage.kt` | AI chat messages |
| 13 | `DevOSUserMessage` | `DevOSUserMessage.kt` | User chat messages |
| 14 | `DevOSToolExecution` | `DevOSToolExecution.kt` | Agent tool steps |
| 15 | `DevOSLoadingState` | `DevOSLoadingState.kt` | Loading placeholders |
| 16 | `DevOSEmptyState` | `DevOSEmptyState.kt` | Empty state screens |
| 17 | `DevOSErrorState` | `DevOSErrorState.kt` | Error state screens |
| 18 | `DevOSAgentStep` | `DevOSAgentStep.kt` | Agent execution steps |
| 19 | `DevOSFileItem` | `DevOSFileItem.kt` | File tree items |
| 20 | `DevOSChip` | `DevOSChip.kt` | Filter chips |
| 21 | `DevOSTabRow` | `DevOSTabRow.kt` | Screen-level tabs |
| 22 | `DevOSSectionHeader` | `DevOSSectionHeader.kt` | Section dividers |
| 23 | `DevOSLanguageBar` | `DevOSLanguageBar.kt` | Language composition bars |
| 24 | `DevOSCommitItem` | `DevOSCommitItem.kt` | Git commit list items |
| 25 | `DevOSIssueItem` | `DevOSIssueItem.kt` | Issue list items |
| 26 | `DevOSPRItem` | `DevOSPRItem.kt` | PR list items |
| 27 | `DevOSSecurityFinding` | `DevOSSecurityFinding.kt` | Security finding items |
| 28 | `DevOSProgressRing` | `DevOSProgressRing.kt` | Circular progress |
| 29 | `DevOSChatInput` | `DevOSChatInput.kt` | AI chat input bar |
| 30 | `DevOSContextSelector` | `DevOSContextSelector.kt` | AI context picker |
| 31 | `DevOSMarkdownText` | `DevOSMarkdownText.kt` | Markdown renderer |
| 32 | `DevOSProviderIcon` | `DevOSProviderIcon.kt` | AI provider logos |
| 33 | `DevOSBranchChip` | `DevOSBranchChip.kt` | Git branch selector |
| 34 | `DevOSLessonCard` | `DevOSLessonCard.kt` | Learning lesson cards |
| 35 | `DevOSQuizOption` | `DevOSQuizOption.kt` | Quiz answer options |
| 36 | `DevOSMemoryItem` | `DevOSMemoryItem.kt` | Developer memory entries |
| 37 | `DevOSMCPTool` | `DevOSMCPTool.kt` | MCP tool list items |
| 38 | `DevOSNotificationItem` | `DevOSNotificationItem.kt` | Notification list items |
| 39 | `DevOSActionChip` | `DevOSActionChip.kt` | AI suggested actions |
| 40 | `DevOSSyntaxHighlight` | `DevOSSyntaxHighlight.kt` | Syntax annotation |

---

## Detailed Component Specifications

---

### DevOSButton

```kotlin
@Composable
fun DevOSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DevOSButtonStyle = DevOSButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
)

enum class DevOSButtonStyle {
    Primary,      // Filled — main CTA
    Secondary,    // Outlined — secondary action
    Tertiary,     // Text — low-emphasis
    Destructive,  // Filled error color — delete/danger
    Ghost,        // No border, subtle hover — toolbar actions
}
```

**Spec:**
- Height: 48dp (standard), 40dp (compact), 56dp (large)
- Corner radius: 8dp
- Icon: 20dp, 8dp gap from text
- Loading: replaces text with `CircularProgressIndicator(16dp)`
- Disabled: 38% opacity

---

### DevOSCard

```kotlin
@Composable
fun DevOSCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: DevOSCardElevation = DevOSCardElevation.Default,
    content: @Composable ColumnScope.() -> Unit,
)

enum class DevOSCardElevation { Flat, Default, Raised }
```

**Spec:**
- Corner radius: 12dp
- Border: 1dp `outline` (dark) / none (light)
- Padding: 16dp
- Elevation: 0dp (flat), 1dp (default), 3dp (raised)
- Ripple on click when `onClick != null`

---

### DevOSTopBar

```kotlin
@Composable
fun DevOSTopBar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    subtitle: String? = null,
)
```

**Spec:**
- Height: 64dp (compact), 112dp (medium with subtitle)
- Title: `titleLarge` typography
- Subtitle: `bodySmall`, `onSurfaceVariant`
- Background: transparent (merges with screen content on scroll)

---

### DevOSBottomBar

```kotlin
@Composable
fun DevOSBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
)
```

**Spec:**
- Height: 80dp + WindowInsets.navigationBars
- Items: Home, Projects, AI, Learn, More
- Active item: pill indicator behind icon + label
- AI item: badge for active agents
- Background: `surfaceVariant` + elevation 2

---

### DevOSSearchBar

```kotlin
@Composable
fun DevOSSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Ask DevOS anything...",
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
)
```

**Spec:**
- Collapsed: pill shape, 56dp height, full-width minus 32dp margin
- Expanded: full-screen overlay with results
- Background: `surfaceVariant` + elevation 2
- Leading: search icon or AI spark icon
- Trailing: mic icon + clear button

---

### DevOSCodeBlock

```kotlin
@Composable
fun DevOSCodeBlock(
    code: String,
    language: String = "kotlin",
    modifier: Modifier = Modifier,
    showLineNumbers: Boolean = true,
    startLine: Int = 1,
    highlightLines: Set<Int> = emptySet(),
    onCopy: (() -> Unit)? = null,
    onAskAI: (() -> Unit)? = null,
)
```

**Spec:**
- Background: `Color(0xFF1E1E2E)` (both themes)
- Font: JetBrains Mono 13sp
- Line numbers: `onSurfaceVariant` color, right-padded
- Syntax highlighting: per-language token colors
- Header: language chip + copy button
- Highlighted lines: `primaryContainer` background tint
- Horizontal scroll for long lines
- Max height: 400dp (scrollable)

---

### DevOSSourceReference

```kotlin
@Composable
fun DevOSSourceReference(
    filePath: String,
    lineStart: Int,
    lineEnd: Int,
    snippet: String,
    relevanceScore: Float? = null,
    onClick: () -> Unit,
)
```

**Spec:**
- File path: `labelMedium`, monospace, `primary` color
- Line range: `labelSmall`, `onSurfaceVariant`
- Snippet: code block mini (max 3 lines)
- Relevance: percentage badge if provided
- Tappable → `CodeViewerScreen`

---

### DevOSStatusBadge

```kotlin
@Composable
fun DevOSStatusBadge(
    text: String,
    status: DevOSStatus,
    modifier: Modifier = Modifier,
)

enum class DevOSStatus {
    Success, Warning, Error, Info, Neutral, Running, Pending
}
```

**Spec:**
- Shape: pill (4dp radius)
- Height: 24dp, horizontal padding: 8dp
- Font: `labelSmall`
- Colors: semantic color tokens per status

---

### DevOSHealthIndicator

```kotlin
@Composable
fun DevOSHealthIndicator(
    score: Float, // 0.0–1.0
    label: String,
    modifier: Modifier = Modifier,
    size: DevOSHealthSize = DevOSHealthSize.Medium,
)
```

**Spec:**
- Circular arc indicator
- Color: green (>0.8) / yellow (0.5-0.8) / red (<0.5)
- Score displayed as percentage in center
- Label below

---

### DevOSProjectCard

```kotlin
@Composable
fun DevOSProjectCard(
    project: ProjectSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

**Spec:**
- Width: 200dp (horizontal scroll) or full-width (list)
- Content: name, repository, language icons (max 4), health score, last active
- Bookmark indicator (top right)

---

### DevOSAIMessage

```kotlin
@Composable
fun DevOSAIMessage(
    message: AIMessage,
    onSourceClick: (SourceReference) -> Unit,
    onFollowUp: (String) -> Unit,
    onExpand: () -> Unit,
)
```

**Spec:**
- Bubble: left-aligned, `surfaceVariant` background
- AI avatar: DevOS spark icon (24dp)
- Content: `DevOSMarkdownText` renderer
- Source references: inline chips → `DevOSSourceReference`
- Agent steps: inline `DevOSAgentStep` list
- Actions row: Expand | Copy | Follow up | Rate

---

### DevOSToolExecution

```kotlin
@Composable
fun DevOSToolExecution(
    step: AgentToolStep,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
)
```

**Spec:**
- Collapsed: icon + tool name + status badge + duration
- Expanded: input params + output + raw JSON toggle
- Status icons: ✓ (success), ✗ (error), ⟳ (running/animated), ○ (pending)

---

### DevOSLoadingState

```kotlin
@Composable
fun DevOSLoadingState(
    message: String? = null,
    modifier: Modifier = Modifier,
    type: DevOSLoadingType = DevOSLoadingType.Shimmer,
)

enum class DevOSLoadingType { Shimmer, Spinner, Skeleton }
```

**Spec:**
- Shimmer: skeleton placeholders matching content layout
- Spinner: centered `CircularProgressIndicator` + optional message
- Skeleton: gray rectangles at content proportions

---

### DevOSEmptyState

```kotlin
@Composable
fun DevOSEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

**Spec:**
- Centered content
- Icon: 64dp, `onSurfaceVariant`
- Title: `titleMedium`
- Description: `bodyMedium`, `onSurfaceVariant`
- Action: optional `DevOSButton`

---

### DevOSErrorState

```kotlin
@Composable
fun DevOSErrorState(
    title: String = "Something went wrong",
    description: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

**Spec:**
- Error icon: `error_outline`, 64dp, `error` color
- Retry button: secondary style
- Description can include error code

---

### DevOSMarkdownText

```kotlin
@Composable
fun DevOSMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    onLinkClick: (String) -> Unit = {},
    onCodeClick: ((String, String) -> Unit)? = null, // (code, language)
)
```

**Spec:**
- Renders: headings, bold, italic, code (inline + block), lists, tables, links
- Code blocks → `DevOSCodeBlock`
- Links: primary color, underlined
- Tables: bordered, `surfaceVariant` header row

---

### DevOSChatInput

```kotlin
@Composable
fun DevOSChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Ask DevOS anything...",
)
```

**Spec:**
- Outlined text field, 8dp radius, auto-expand up to 4 lines
- Leading: context icon button
- Trailing: send button (enabled when text non-empty)
- Background: `surfaceVariant`
- Safe area padding

---

### DevOSContextSelector

```kotlin
@Composable
fun DevOSContextSelector(
    currentContext: AIContext,
    onContextChange: (AIContext) -> Unit,
)

sealed class AIContext {
    object Global : AIContext()
    data class Project(val projectId: String, val name: String) : AIContext()
    data class Repository(val repoId: String, val name: String) : AIContext()
    data class File(val repoId: String, val path: String) : AIContext()
    data class Symbol(val repoId: String, val symbolId: String, val name: String) : AIContext()
}
```

**Spec:**
- Horizontal scrolling chip row
- Active chip: filled, primary color
- Tap to open selection bottom sheet
