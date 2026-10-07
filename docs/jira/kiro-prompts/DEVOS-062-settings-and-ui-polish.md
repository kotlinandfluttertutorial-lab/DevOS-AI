# Kiro Prompt — DEVOS-062: Settings, Dark Mode, Accessibility & Responsive Layout

**Jira:** DEVOS-062 / DEVOS-063 / DEVOS-064 / DEVOS-065 / DEVOS-066  
**Epic:** DEVOS-E11  
**Figma:** FIGMA-36 / FIGMA-40  
**Kiro Spec:** `.kiro/specs/android-ui/polish.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the Settings screens, first-class dark mode support, accessibility audit, and responsive tablet layout for DevOS AI.

**Existing files to read first:**
- `docs/figma/screen-inventory.md` — Screens FIGMA-36, FIGMA-40
- `docs/figma/component-inventory.md` — all DevOS components
- `docs/architecture/design-system-spec.md` — theme tokens
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/` — existing theme
- `app/src/main/kotlin/com/devos/ai/MainActivity.kt` — entry point for window size class

**Modules:**
- `feature/feature-settings/` — SettingsScreen, ProjectSettingsScreen
- `designsystem/` — dark mode + responsive layout tokens
- `app/` — window size class integration

**Package:** `com.devos.ai.feature.settings`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- All colors MUST use `MaterialTheme.colorScheme` tokens — never hardcoded hex values in screen code.
- All interactive elements MUST have `contentDescription` and meet 48×48dp minimum touch target.
- Responsive layout uses `WindowSizeClass` — never hardcode breakpoints as raw `dp` values in screens.
- Accessibility: every icon-only button needs `contentDescription`; every card clickable via keyboard and TalkBack.

**What to implement:**

### 1. SettingsScreen — Root (DEVOS-062, FIGMA-40)

Route: `SETTINGS`

UiState:
```kotlin
data class SettingsUiState(
    val appVersion: String,
    val buildNumber: String,
    val theme: AppTheme,            // SYSTEM, LIGHT, DARK
)

enum class AppTheme { SYSTEM, LIGHT, DARK }
```

Layout:
```kotlin
Scaffold(topBar = { DevOSTopBar(title = "Settings") }) { padding ->
    LazyColumn(Modifier.padding(padding)) {
        item { SettingsGroupHeader("Appearance") }
        item {
            ThemeSelector(
                current = theme,
                onSelect = viewModel::setTheme,
            )
        }
        item { SettingsGroupHeader("AI") }
        item {
            SettingsNavigationItem(
                icon = Icons.Outlined.SmartToy,
                title = "AI Settings",
                subtitle = "Models, RAG, agents",
                onClick = { onNavigateTo(Routes.AI_SETTINGS) },
            )
        }
        item {
            SettingsNavigationItem(
                icon = Icons.Outlined.Key,
                title = "API Providers",
                subtitle = "Configure API keys",
                onClick = { onNavigateTo(Routes.PROVIDER_SETTINGS) },
            )
        }
        item { SettingsGroupHeader("Projects") }
        item {
            SettingsNavigationItem(
                icon = Icons.Outlined.FolderOpen,
                title = "Project Settings",
                subtitle = "Per-project configuration",
                onClick = { onNavigateTo(Routes.PROJECT_SETTINGS) },
            )
        }
        item { SettingsGroupHeader("About") }
        item {
            SettingsInfoItem(title = "Version", value = appVersion)
        }
        item {
            SettingsInfoItem(title = "Build", value = buildNumber)
        }
        item {
            SettingsNavigationItem(
                icon = Icons.Outlined.Feedback,
                title = "Send Feedback",
                onClick = onSendFeedback,
            )
        }
    }
}
```

Theme selector persisted via DataStore. Theme applied in `MainActivity` by observing `AppThemeRepository.observeTheme()` and passing to `DevOSTheme(darkTheme = ...)`.

### 2. ProjectSettingsScreen (DEVOS-063, FIGMA-36)

Route: `PROJECT_SETTINGS/{projectId}`

UiState:
```kotlin
data class ProjectSettingsUiState(
    val project: Project,
    val name: String,
    val description: String,
    val aiContextMode: AIContextMode,
    val notificationsEnabled: Boolean,
    val hasUnsavedChanges: Boolean,
)

enum class AIContextMode { REPO_ONLY, GLOBAL, CUSTOM }
```

Layout: `LazyColumn` with:
- Name `OutlinedTextField`
- Description `OutlinedTextField` (multi-line)
- Repository connections list with add/remove
- AI context mode selector (SegmentedButton)
- Notifications toggle
- **Delete Project** button — red, requires `AlertDialog` confirmation with project name typed to confirm

```kotlin
// Delete project confirmation — user must type project name
AlertDialog(
    title = { Text("Delete Project?") },
    text = {
        Column {
            Text("Type \"${project.name}\" to confirm deletion. This cannot be undone.")
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            OutlinedTextField(value = confirmName, onValueChange = { confirmName = it },
                label = { Text("Project name") })
        }
    },
    confirmButton = {
        DevOSButton("Delete",
            enabled = confirmName == project.name,
            onClick = { onDeleteProject(project.id) },
            containerColor = MaterialTheme.colorScheme.error)
    },
    dismissButton = {
        DevOSButton("Cancel", onClick = onDismiss, style = DevOSButtonStyle.Secondary)
    },
    onDismissRequest = onDismiss,
)
```

### 3. Dark Mode — Full Implementation (DEVOS-064)

Dark mode is already supported by `DevOSTheme` token architecture. This ticket ensures:

**Verification checklist (automated + manual):**
```kotlin
// DesignSystemDarkModeTest — screenshot tests for all components in dark mode
@Test fun devosButton_darkMode() { /* verify primary/secondary/tertiary variants */ }
@Test fun devosCard_darkMode() { /* verify surface and onSurface contrast */ }
@Test fun devosCodeBlock_darkMode() { /* verify background is always #1E1E2E */ }
@Test fun devosAIMessage_darkMode() { /* verify AI message bubble readable */ }
@Test fun devosStatusBadge_darkMode() { /* verify all severity colors visible */ }
```

**Critical dark mode rules to enforce:**
- `DevOSCodeBlock` background is ALWAYS `SyntaxColors.background = #1E1E2E` regardless of theme — never `MaterialTheme.colorScheme.surface`
- Status/severity colors (Critical #FF4444, High #FF8800) must maintain 3:1 contrast against dark background
- Loading skeletons use `surfaceVariant` with shimmer animation — not white flashes
- All text must have minimum 4.5:1 contrast ratio in dark theme

**Force dark mode for screenshot tests:**
```kotlin
@Composable
fun DarkModePreview(content: @Composable () -> Unit) {
    DevOSTheme(darkTheme = true) { Surface { content() } }
}
```

### 4. Accessibility Audit — All Screens (DEVOS-065)

This ticket is a verification + fix pass across all 40 screens. Key patterns to enforce:

**Pattern 1 — Icon-only buttons:**
```kotlin
// CORRECT
IconButton(onClick = onBack) {
    Icon(Icons.AutoMirrored.Outlined.ArrowBack,
        contentDescription = "Go back")  // MUST be non-null
}

// WRONG — missing contentDescription
Icon(Icons.Outlined.Close, contentDescription = null)  // if clickable, this is inaccessible
```

**Pattern 2 — Clickable non-Button elements:**
```kotlin
// CORRECT
Box(modifier = Modifier
    .clickable { onClick() }
    .semantics {
        role = Role.Button
        contentDescription = "Open repository ${repo.name}"
    }
)
```

**Pattern 3 — Minimum touch targets:**
```kotlin
// CORRECT — minimum 48×48dp
IconButton(modifier = Modifier.size(48.dp)) { ... }

// WRONG — too small
IconButton(modifier = Modifier.size(24.dp)) { ... }
```

**Pattern 4 — Dynamic text scaling:**
```kotlin
// CORRECT — uses sp, wraps text, avoids fixed height
Text(text, style = MaterialTheme.typography.bodyMedium)

// WRONG — truncates at large text scale
Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis,
    modifier = Modifier.height(20.dp))
```

**Accessibility audit checklist (verify for ALL 40 screens):**
- [ ] All `Icon` composables: `contentDescription` non-null when icon is interactive; null only when purely decorative
- [ ] All `IconButton` composables: minimum `size(48.dp)` via modifier or default NavigationBarItem sizing
- [ ] All `Image` composables (avatars, logos): `contentDescription` set
- [ ] All `Card`/`Box` with `clickable`: `Modifier.semantics { role = Role.Button }` + `contentDescription`
- [ ] Text contrast: run `AccessibilityChecker` or Espresso accessibility test in CI
- [ ] TalkBack: manually verify Home, AI Chat, Repository List, Code Viewer, and Settings screens
- [ ] Dynamic text: test at 200% font scale — no text clipped or hidden

### 5. Responsive Layout — Tablet Support (DEVOS-066)

Use `WindowSizeClass` from `androidx.compose.material3.windowsizeclass`:

```kotlin
// In MainActivity
val windowSizeClass = calculateWindowSizeClass(this)

// Pass to NavHost root composable
DevOSApp(windowSizeClass = windowSizeClass)

// In DevOSApp composable
@Composable
fun DevOSApp(windowSizeClass: WindowSizeClass) {
    val isCompact  = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact     // phone
    val isMedium   = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium      // tablet 600–840dp
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded    // large tablet 840dp+

    Scaffold(
        bottomBar = { if (isCompact) DevOSBottomBar(...) },
        // Navigation rail for medium+
    ) { padding ->
        Row {
            if (!isCompact) {
                NavigationRail {
                    bottomNavItems.forEach { item ->
                        NavigationRailItem(selected = ..., onClick = ...,
                            icon = { Icon(item.icon, contentDescription = item.contentDescription) },
                            label = { Text(item.label) })
                    }
                }
            }
            // Content
            NavHost(...)
        }
    }
}
```

**Split-pane layouts:**

Repository + Code Viewer (medium and expanded):
```kotlin
if (isMedium || isExpanded) {
    Row(Modifier.fillMaxSize()) {
        // File explorer — fixed width panel
        FileExplorerPanel(Modifier.width(280.dp))
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
        // Code viewer — remaining width
        CodeViewerPanel(Modifier.weight(1f))
    }
} else {
    // Phone: navigate to CodeViewerScreen
}
```

Issues + Issue Detail (expanded only, 3-pane):
```kotlin
if (isExpanded) {
    Row {
        IssueListPanel(Modifier.width(320.dp))
        Divider(...)
        IssueDetailPanel(Modifier.weight(1f))
    }
}
```

**No horizontal overflow rule:** every screen must be tested at 600dp width with no horizontal scroll except inside `DevOSCodeBlock` (intentional) and horizontal `LazyRow` components.

**Tests:**
- `SettingsViewModelTest`: theme change persisted; navigation items present
- `ProjectSettingsViewModelTest`: name/description edits tracked as unsaved; delete requires name confirmation
- `DarkModeScreenshotTest`: all major components rendered in dark theme; code block background always #1E1E2E
- `AccessibilityTest` (Espresso): `AccessibilityChecks.enable()` run on Home, AI Chat, and Repository screens
- `ResponsiveLayoutTest`: bottom bar shown at Compact; navigation rail shown at Medium+; split pane at Medium+ for repo/code

**Acceptance Criteria:**
- AC1 (DEVOS-062): Settings root groups render (Appearance/AI/Projects/About)
- AC2 (DEVOS-062): Navigation to all sub-settings screens works
- AC3 (DEVOS-062): App version and build number shown
- AC4 (DEVOS-062): Theme selector (System/Light/Dark) persists and applies immediately
- AC5 (DEVOS-063): Name and description editable and saved
- AC6 (DEVOS-063): Repository connections manageable (add/remove)
- AC7 (DEVOS-063): AI context mode selector persists
- AC8 (DEVOS-063): Delete project requires typing project name to confirm
- AC9 (DEVOS-064): All 40 screens verified in dark mode — no readability issues
- AC10 (DEVOS-064): Code blocks always use #1E1E2E background in both themes
- AC11 (DEVOS-064): AI message bubbles readable with ≥4.5:1 contrast in dark mode
- AC12 (DEVOS-064): Status severity colors (Critical/High/Medium/Low) distinguishable in dark mode
- AC13 (DEVOS-065): All icon-only elements have non-null `contentDescription`
- AC14 (DEVOS-065): All touch targets ≥48×48dp
- AC15 (DEVOS-065): TalkBack verified on Home, AI Chat, Repository List, Code Viewer, and Settings
- AC16 (DEVOS-065): Contrast ratio ≥4.5:1 for all body text; ≥3:1 for large text and UI elements
- AC17 (DEVOS-065): Dynamic text scaling: no clipping at 200% font scale
- AC18 (DEVOS-066): Navigation rail shown on tablets (600dp+); bottom bar on phones
- AC19 (DEVOS-066): Split-pane repo/code viewer layout on medium+ screens
- AC20 (DEVOS-066): No horizontal overflow on any screen at 600dp width
