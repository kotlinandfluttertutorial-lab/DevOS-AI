# Spec: Android UI — Core Screens & Themes

**Jira:** DEVOS-064 / DEVOS-065 / DEVOS-066 / DEVOS-101 / DEVOS-102 / DEVOS-103  
**Epic:** DEVOS-E11  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement splash, onboarding, login, dark mode, accessibility compliance, and responsive tablet layouts.

## SplashScreen (FIGMA-01)

```kotlin
@Composable
fun SplashScreen(onComplete: () -> Unit) {
    // Use Android 12+ SplashScreen API for instant display
    // Animate DevOS AI logo with pulse effect
    // Navigate to Onboarding (first launch) or Home (returning)

    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            DevOSLogo(Modifier.size(96.dp).scale(scale))
            Spacer(Modifier.height(MaterialTheme.spacing.base))
            Text("DevOS AI", style = MaterialTheme.typography.displaySmall)
            Text("Understand your code. Learn faster. Build smarter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}
```

## OnboardingScreen (FIGMA-02)

4 pages:
1. **AI Command Center** — "Your entire codebase, understood by AI"
2. **Repository Intelligence** — "Import any GitHub or GitLab repository"
3. **AI Chat & Agents** — "Ask anything, get grounded answers with sources"
4. **Learn as You Build** — "Connect learning directly to your code"

```kotlin
val pages = listOf(
    OnboardingPage(R.drawable.onboarding_ai, "AI Command Center", "Your entire codebase..."),
    OnboardingPage(R.drawable.onboarding_repo, "Repository Intelligence", "Import any..."),
    OnboardingPage(R.drawable.onboarding_chat, "AI Chat & Agents", "Ask anything..."),
    OnboardingPage(R.drawable.onboarding_learn, "Learn as You Build", "Connect learning..."),
)
```

- HorizontalPager + PagerState
- Dot indicator row
- Skip button (top right, navigates to Login immediately)
- "Next" / "Get Started" button
- Stored as seen: `dataStore.edit { it[ONBOARDING_SEEN] = true }`

## LoginScreen (FIGMA-03)

```kotlin
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onGitHubLogin: () -> Unit,
    onGitLabLogin: () -> Unit,
    onEmailLogin: (String, String) -> Unit,
) {
    // Logo
    // "Sign in to DevOS AI" headline
    // GitHub button (filled with GitHub icon)
    // GitLab button (outlined with GitLab icon)
    // Divider "or"
    // Email + Password fields
    // Sign In button
    // "Forgot password?" link
    // Terms + Privacy links at bottom
}

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
    data object Success : LoginUiState
}
```

OAuth flow via `CustomTabsIntent` — never a WebView.

## Dark Mode

All screens automatically use `DevOSTheme` which wraps `MaterialTheme` with correct color scheme.

Code blocks always use `SyntaxColors.background = Color(0xFF1E1E2E)` — independent of theme.

Dark mode verification checklist per screen:
```
[ ] Background is DevOSNavy950 (#0F111A)
[ ] Cards use DevOSNavy900 (#1A1C2E) with 1dp border
[ ] Code visible (not black on black)
[ ] AI messages distinguishable from background
[ ] Status badges readable (sufficient contrast)
[ ] Input fields have visible border/outline
```

## Responsive Layout

### WindowSizeClass-based layout:
```kotlin
@Composable
fun DevOSAdaptiveLayout(windowSizeClass: WindowSizeClass) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact  -> DevOSPhoneLayout()    // <600dp
        WindowWidthSizeClass.Medium   -> DevOSTabletLayout()   // 600–840dp
        WindowWidthSizeClass.Expanded -> DevOSLargeTabletLayout() // >840dp
    }
}
```

### Phone Layout: Bottom navigation bar
### Tablet (Medium): Navigation rail (left) + content
### Large Tablet: Navigation rail + list pane + detail pane

Navigation rail (tablet):
```kotlin
NavigationRail {
    NavItem(Icons.Filled.Home, "Home", selected = current == Routes.HOME)
    NavItem(Icons.Filled.FolderOpen, "Projects", ...)
    NavItem(Icons.Filled.AutoAwesome, "AI", ...)
    NavItem(Icons.Filled.School, "Learn", ...)
}
```

## Accessibility Requirements

Every screen must pass:
- `composeTestRule.onAllNodes(hasContentDescription("")).assertCountEquals(0)` — no empty descriptions
- All interactive elements have `contentDescription` or `semantics { role }`
- Touch targets ≥48dp (use `Modifier.minimumInteractiveComponentSize()`)
- TalkBack traversal order logical (left-to-right, top-to-bottom)
- Dynamic font scaling: test at 1.0× and 2.0× — no text clipping

## Acceptance Criteria
- [ ] Splash transitions to onboarding on first launch
- [ ] Splash transitions to home on subsequent launches
- [ ] Onboarding shown only once
- [ ] Skip navigates to login immediately
- [ ] Login shows loading state during OAuth
- [ ] Login shows error on failed auth
- [ ] All 40 screens verified in dark mode
- [ ] Accessibility audit passes (no missing descriptions)
- [ ] Tablet navigation rail renders at 600dp
- [ ] Three-pane layout at 840dp
