# Kiro Prompt — DEVOS-002: Design System Tokens, Code Components, Navigation

**Jira:** DEVOS-002 / DEVOS-003 / DEVOS-004 / DEVOS-006 / DEVOS-007 / DEVOS-008  
**Epic:** DEVOS-E01  
**Figma:** — (design system; no dedicated screen)  
**Kiro Spec:** `.kiro/specs/foundation/design-system.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the complete DevOS AI design system token layer, code rendering components, bottom navigation, and navigation graph. This is foundational work in `:designsystem` and `app/` — every other feature module depends on it.

**Existing files to read first:**
- `docs/architecture/design-system-spec.md` — token values and component specs
- `docs/figma/component-inventory.md` — component catalogue
- `docs/figma/navigation.md` — full route list and navigation structure
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/` — existing theme files (read before editing)

**Modules:** `:designsystem` + `app/src/main/kotlin/com/devos/ai/navigation/`  
**Package:** `com.devos.ai.designsystem`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- All color, typography, shape, and spacing values live ONLY in `:designsystem` — feature modules never define their own.
- No hardcoded dp, sp, or color literals in any component — always reference tokens.
- `DevOSCodeBlock` code background is always `SyntaxColors.background = #1E1E2E` regardless of light/dark theme.
- All components must pass WCAG AA contrast (4.5:1 text, 3:1 large/UI elements).
- Every interactive component must have minimum 48×48dp touch target.

**What to implement:**

### 1. Color Tokens (DEVOS-002)

File: `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Color.kt`

```kotlin
// Dark scheme
val DevOSDarkColorScheme = darkColorScheme(
    primary          = Color(0xFF6C63FF),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3D35CC),
    secondary        = Color(0xFF03DAC6),
    onSecondary      = Color(0xFF000000),
    tertiary         = Color(0xFFCF6679),
    background       = Color(0xFF141420),
    onBackground     = Color(0xFFE6E1E5),
    surface          = Color(0xFF1E1E2E),
    onSurface        = Color(0xFFE6E1E5),
    surfaceVariant   = Color(0xFF2A2A3E),
    onSurfaceVariant = Color(0xFFAEA9B4),
    outline          = Color(0xFF49454F),
    error            = Color(0xFFCF6679),
    onError          = Color(0xFFFFFFFF),
)

// Light scheme
val DevOSLightColorScheme = lightColorScheme(
    primary          = Color(0xFF6C63FF),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8E6FF),
    secondary        = Color(0xFF018786),
    onSecondary      = Color(0xFFFFFFFF),
    tertiary         = Color(0xFF7D5260),
    background       = Color(0xFFF0F0F0),
    onBackground     = Color(0xFF1C1B1F),
    surface          = Color(0xFFFAFAFA),
    onSurface        = Color(0xFF1C1B1F),
    surfaceVariant   = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline          = Color(0xFF79747E),
    error            = Color(0xFFB00020),
    onError          = Color(0xFFFFFFFF),
)

// Syntax highlighting — always dark regardless of theme
object SyntaxColors {
    val background = Color(0xFF1E1E2E)
    val keyword    = Color(0xFFC792EA)
    val string     = Color(0xFFC3E88D)
    val comment    = Color(0xFF546E7A)
    val number     = Color(0xFFF78C6C)
    val function_  = Color(0xFF82AAFF)
    val type       = Color(0xFFFFCB6B)
    val operator   = Color(0xFF89DDFF)
    val plain      = Color(0xFFD4D4D4)
}
```

### 2. Typography Scale (DEVOS-003)

File: `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Type.kt`

```kotlin
val DevOSTypography = Typography(
    displayLarge  = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Normal),
    displaySmall  = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal),
    headlineLarge  = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal),
    headlineSmall  = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal),
    titleLarge  = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall  = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge  = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall  = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge  = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall  = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

// Code font — JetBrains Mono, always used for DevOSCodeBlock
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

val DevOSCodeTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontSize = 13.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Normal,
)
```

Font file placement: `designsystem/src/main/res/font/jetbrains_mono_regular.ttf` etc. (download from fonts.google.com or JetBrains GitHub).
Dynamic text scaling: all sizes use `sp` — never `dp` for text.

### 3. Shape and Spacing Tokens (DEVOS-004)

File: `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Shape.kt`

```kotlin
val DevOSShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),   // buttons, inputs, chips
    medium     = RoundedCornerShape(12.dp),  // cards
    large      = RoundedCornerShape(16.dp),  // dialogs
    extraLarge = RoundedCornerShape(24.dp),  // bottom sheets
)
```

File: `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Spacing.kt`

```kotlin
data class DevOSSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val base: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val cardPadding: Dp = 16.dp,
    val sectionSpacing: Dp = 24.dp,
    val screenPadding: Dp = 16.dp,
)

val LocalDevOSSpacing = compositionLocalOf { DevOSSpacing() }

// Extension for easy access
val MaterialTheme.spacing: DevOSSpacing
    @Composable get() = LocalDevOSSpacing.current
```

Wire into `DevOSTheme`:
```kotlin
@Composable
fun DevOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DevOSDarkColorScheme else DevOSLightColorScheme
    CompositionLocalProvider(LocalDevOSSpacing provides DevOSSpacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DevOSTypography,
            shapes = DevOSShapes,
            content = content,
        )
    }
}
```

### 4. DevOSCodeBlock (DEVOS-006)

File: `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/CodeBlock.kt`

```kotlin
@Composable
fun DevOSCodeBlock(
    code: String,
    language: String = "kotlin",
    modifier: Modifier = Modifier,
    showLineNumbers: Boolean = true,
    highlightLine: Int? = null,
    onCopy: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    val annotated = remember(code, language) { syntaxHighlight(code, language) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(SyntaxColors.background, MaterialTheme.shapes.medium)
                .horizontalScroll(scrollState)
                .padding(MaterialTheme.spacing.base),
        ) {
            if (showLineNumbers) {
                // Gutter
                Column(horizontalAlignment = Alignment.End) {
                    annotated.lines().forEachIndexed { index, _ ->
                        Text(
                            text = "${index + 1}",
                            style = DevOSCodeTextStyle,
                            color = SyntaxColors.comment,
                            modifier = Modifier
                                .background(
                                    if (highlightLine == index + 1)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .padding(end = MaterialTheme.spacing.sm),
                        )
                    }
                }
                Spacer(Modifier.width(MaterialTheme.spacing.sm))
            }
            // Code body
            Text(
                text = annotated,
                style = DevOSCodeTextStyle,
                softWrap = false,
            )
        }

        // Header: language badge + copy button
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(MaterialTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = language,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs, vertical = 2.dp),
                )
            }
            if (onCopy != null) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = "Copy code",
                        modifier = Modifier.size(16.dp),
                        tint = SyntaxColors.plain,
                    )
                }
            }
        }
    }
}

// Basic syntax highlighter using AnnotatedString
private fun syntaxHighlight(code: String, language: String): AnnotatedString {
    // Implement per-language keyword/string/comment regex highlighting
    // Return AnnotatedString with SpanStyle applied per token type
    // Use SyntaxColors.keyword, .string, .comment, .number, .function_, .type, .operator, .plain
    return buildAnnotatedString { append(code) } // placeholder — implement per language
}
```

### 5. DevOSBottomBar (DEVOS-007)

File: `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/BottomBar.kt`

```kotlin
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val badgeCount: Int = 0,
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME,     "Home",     Icons.Outlined.Home,       "Home"),
    BottomNavItem(Routes.PROJECTS, "Projects", Icons.Outlined.FolderOpen, "Projects"),
    BottomNavItem(Routes.AI_CHAT,  "AI",       Icons.Outlined.SmartToy,   "AI Chat"),
    BottomNavItem(Routes.LEARN,    "Learn",    Icons.Outlined.School,      "Learn"),
    BottomNavItem(Routes.MORE,     "More",     Icons.Outlined.Menu,        "More"),
)

@Composable
fun DevOSBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    agentBadgeCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        bottomNavItems.forEach { item ->
            val isActive = currentRoute.startsWith(item.route)
            NavigationBarItem(
                selected = isActive,
                onClick = { onNavigate(item.route) },
                icon = {
                    BadgedBox(badge = {
                        if (item.route == Routes.AI_CHAT && agentBadgeCount > 0) {
                            Badge { Text("$agentBadgeCount") }
                        }
                    }) {
                        Icon(item.icon, contentDescription = item.contentDescription)
                    }
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}
```

### 6. Navigation Graph — All 40 Routes (DEVOS-008)

File: `app/src/main/kotlin/com/devos/ai/navigation/Routes.kt`

```kotlin
object Routes {
    // Foundation
    const val SPLASH         = "splash"
    const val ONBOARDING     = "onboarding"
    const val LOGIN          = "login"

    // Home / Command Center
    const val HOME           = "home"
    const val NOTIFICATIONS  = "notifications"
    const val SEARCH         = "search"
    const val PROFILE        = "profile"

    // Projects / Repository
    const val PROJECT_LIST   = "projects"
    const val PROJECT_OVERVIEW = "projects/{projectId}"
    const val REPO_IMPORT    = "repo/import"
    const val REPO_SYNC      = "repo/sync/{repoId}"
    const val REPO_OVERVIEW  = "repo/{repoId}"

    // Code Intelligence
    const val FILE_EXPLORER  = "repo/{repoId}/files"
    const val CODE_VIEWER    = "repo/{repoId}/file?path={path}&line={line}"
    const val CODE_SEARCH    = "repo/{repoId}/search"
    const val SYMBOL_DETAILS = "repo/{repoId}/symbol/{symbolId}"
    const val DEP_GRAPH      = "repo/{repoId}/graph"
    const val ARCH_OVERVIEW  = "repo/{repoId}/architecture"

    // AI Platform
    const val AI_CHAT        = "ai/chat"
    const val AI_CHAT_SESSION = "ai/chat/{sessionId}"
    const val AI_ANSWER_DETAIL = "ai/answer/{answerId}"
    const val AI_SOURCE_EVIDENCE = "ai/answer/{answerId}/source/{sourceId}"
    const val AI_SETTINGS    = "ai/settings"
    const val PROVIDER_SETTINGS = "ai/settings/providers"

    // Agents & MCP
    const val AGENT_RUN      = "agents/run/{runId}"
    const val AGENT_TOOL_EXEC = "agents/run/{runId}/tool/{stepId}"
    const val MCP_TOOLS      = "mcp/tools"

    // Developer Intelligence
    const val GIT_HISTORY    = "repo/{repoId}/git"
    const val ISSUE_LIST     = "repo/{repoId}/issues"
    const val ISSUE_DETAIL   = "repo/{repoId}/issues/{issueId}"
    const val PR_LIST        = "repo/{repoId}/prs"
    const val PR_REVIEW      = "repo/{repoId}/prs/{prId}/review"

    // Quality Intelligence
    const val SECURITY_FINDINGS = "repo/{repoId}/security"
    const val TEST_INTELLIGENCE = "repo/{repoId}/tests"

    // Learning
    const val LEARN          = "learn"
    const val COURSE_DETAILS = "learn/course/{courseId}"
    const val LESSON         = "learn/course/{courseId}/lesson/{lessonId}"
    const val QUIZ           = "learn/course/{courseId}/quiz/{quizId}"

    // Developer Memory
    const val MEMORY         = "memory"

    // Settings
    const val SETTINGS       = "settings"
    const val PROJECT_SETTINGS = "settings/project/{projectId}"
}
```

Deep links to register in `AndroidManifest.xml`:
- `devos://auth/callback` → `Routes.LOGIN`
- `devos://repo/{repoId}/file?path={path}&line={line}` → `Routes.CODE_VIEWER`

Navigation rules:
- All arguments typed via `SavedStateHandle` in ViewModels — never raw string parsing in composables.
- Back stack per tab preserved with `NavBackStackEntry` per tab route.
- All 40 routes must be defined in `NavGraphBuilder` extensions, one extension per feature module.

**Tests:**
- `DesignSystemTest`: DevOSTheme wraps correctly; spacing values correct; dark/light switch applies correct color scheme
- `DevOSCodeBlockTest`: renders with line numbers; copy action fires; language badge shows; background is always SyntaxColors.background
- `DevOSBottomBarTest`: 5 items render; active item highlighted; agent badge shows count; all items accessible (contentDescription present)
- `NavigationTest`: all 40 routes navigable; back stack restored per tab; deep link `devos://auth/callback` opens Login screen

**Acceptance Criteria:**
- AC1 (DEVOS-002): `DevOSDarkColorScheme` and `DevOSLightColorScheme` defined with all M3 color roles
- AC2 (DEVOS-002): Semantic tokens documented in `design-system-spec.md` and match implementation
- AC3 (DEVOS-002): WCAG AA contrast verified for primary text (4.5:1) and large text/UI (3:1) in both themes
- AC4 (DEVOS-003): All 13 M3 type roles defined with correct `sp` sizes
- AC5 (DEVOS-003): JetBrains Mono integrated and applied as `DevOSCodeTextStyle`
- AC6 (DEVOS-003): Text renders correctly at 100% and 200% font scale (no clipping)
- AC7 (DEVOS-004): `DevOSShapes` defines all 5 corner radius levels (4/8/12/16/24dp)
- AC8 (DEVOS-004): `DevOSSpacing` defines xs/sm/base/lg/xl/cardPadding/sectionSpacing accessible via `MaterialTheme.spacing`
- AC9 (DEVOS-004): No hardcoded `dp` or `sp` values in any component — all use tokens
- AC10 (DEVOS-006): `DevOSCodeBlock` renders Kotlin, Java, Swift, Python, JS, TS with correct syntax colors
- AC11 (DEVOS-006): Code background is always `#1E1E2E` in both dark and light themes
- AC12 (DEVOS-006): Line numbers shown in gutter; copy action copies code to clipboard; language badge chip shows language name
- AC13 (DEVOS-006): Horizontal scroll works for long lines without wrapping
- AC14 (DEVOS-007): 5 bottom nav tabs render with correct icons and labels
- AC15 (DEVOS-007): Active tab icon and label use `primary` color; inactive use `onSurfaceVariant`
- AC16 (DEVOS-007): AI tab shows agent badge with count when agents are running
- AC17 (DEVOS-007): All tabs have accessibility `contentDescription`; active state announced to TalkBack
- AC18 (DEVOS-007): Back stack per tab restored when switching between tabs
- AC19 (DEVOS-008): All 40 route constants defined in `Routes` object
- AC20 (DEVOS-008): All routes use typed arguments — no raw string parsing
- AC21 (DEVOS-008): Deep links `devos://auth/callback` and code viewer deep link registered
- AC22 (DEVOS-008): Back navigation correct on all routes (up button returns to parent screen)
