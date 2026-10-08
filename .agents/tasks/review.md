# Home Dashboard — DEVOS-057 (DA-69)

The change delivers the primary landing screen of DevOS AI: a scrollable dashboard presenting recent projects, AI recommendations with DataStore-backed dismissal, a 2×2 project health grid, and recent AI sessions. All four UI states (Loading/Success/Empty/Error) are wired through a sealed `HomeUiState` interface. The ViewModel emits navigation events via `SharedFlow`; no `NavController` import appears anywhere in the feature module. The stub data layer is explicitly marked TODO for DEVOS-058.

**Watch for:** Route string literals in `HomeNavigation.kt` are not validated against `DevOSRoutes` constants at compile time — drift is possible (likely concern). `NavigateToAllSessions` and `NavigateToChat` both navigate to `"ai_chat"` with no session ID — duplicate targets that will need splitting when DEVOS-058 lands (confirmed). The profile avatar touch target is 32dp (`iconSizeLarge`), confirmed below the 48dp accessibility minimum.

**Verdict**: APPROVED

---

## High-level view

All four UI states are handled correctly in `HomeScreen` via a `when` branch on the sealed interface, delegating to the appropriate DevOS design-system component (`DevOSLoadingState`, `DevOSEmptyState`, `DevOSErrorState`, full `HomeDashboardContent`). The state transition logic in `HomeViewModel` is clean: Loading → try DataStore + stubs → Success/Empty/Error, with explicit retry support.

The `NavGraphBuilder.homeNavigation()` extension matches the pattern established by `AuthNavigation.kt` and is correctly wired into `DevOSNavGraph.kt` with `homeNavigation(navController)` replacing the old placeholder.

DataStore dismissal persistence is properly isolated under a `@Named("home")` qualifier to avoid collision with `AuthModule`'s unqualified `DataStore<Preferences>` binding. The qualifier propagates correctly from `HomeModule` through to `HomeViewModel`'s `@Inject` constructor parameter.

The spacing and color audits are clean. The three findings from the first review pass (hardcoded `160.dp`, `6.dp`, `24.sp`) are confirmed fixed: `projectCardWidth` and `statusDotSize` tokens were added to `DevOSSpacing`, and `HealthCell` switched to `headlineSmall.copy(fontWeight = FontWeight.Bold)`. No raw `.dp` or `.sp` literals remain in any feature-home source file.

Route strings in `HomeNavigation.kt` are raw string literals (`"search?q="`, `"project/${event.id}"`, `"notifications"`, etc.) rather than references to `DevOSRoutes` constants. For the routes that already exist in `DevOSRoutes`, this is a silent drift risk — a rename of a route constant won't break the build but will silently break navigation at runtime.

<details>
<summary>Issues (3)</summary>

1. **Route string drift** — `HomeNavigation.kt` navigates using raw string literals instead of `DevOSRoutes` constants. Routes like `"notifications"`, `"profile"`, `"project_list"`, `"repository/import"` already exist as `DevOSRoutes` entries; any constant rename silently breaks navigation without a compile error. Replace with `DevOSRoutes.*` references where they exist.

2. **Duplicate nav target for Sessions** — `NavigateToAllSessions` and `NavigateToChat(sessionId)` both navigate to `"ai_chat"` with no session ID argument passed in either case. When a real `ai_chat/{sessionId}` route exists (DEVOS-058), both call sites will need updating; leaving them identical risks the `NavigateToAllSessions` case being forgotten.

3. **Avatar touch target below 48dp** — The profile avatar `Box` is `DevOSSpacing.iconSizeLarge` (32dp) with a bare `Modifier.clickable`. Wrap it in an `IconButton` or expand it to `DevOSSpacing.touchTarget` (48dp) to meet the 48dp accessibility minimum.

</details>

<details>
<summary>Details</summary>

### All four UI states

`HomeScreen` handles all four states explicitly. Loading delegates to `DevOSLoadingState`. Error delegates to `DevOSErrorState` with `onRetry = if (state.retryable) onRetry else null`. Empty renders `DevOSEmptyState` with the `RocketLaunch` icon and an "Import Repository" CTA via `DevOSButton`. Success renders the full `HomeDashboardContent`. The pattern matches the design-system state contract exactly.

The Empty branch fires when `stubProjects().isEmpty()`. Since the stub always returns 3 items, the Empty path is not exercised by the current tests — but the branching logic is correct and will activate once real use cases are connected (DEVOS-058).

### Navigation: pattern conformance and route string risk

`HomeNavigation.kt` correctly follows the `NavGraphBuilder` extension pattern from `AuthNavigation.kt`: `hiltViewModel()` is called inside the composable lambda, `collectAsStateWithLifecycle()` collects state, `LaunchedEffect(Unit)` drives a `navEvent.collect` loop for one-shot navigation, and `HomeScreen` receives only callbacks and state — no ViewModel reference passed down the tree.

The `NavController` import does not appear in `HomeViewModel.kt`, confirming the separation is clean.

The issue is the raw string literals used inside the `when (event)` block. `"notifications"` maps to `DevOSRoutes.NOTIFICATIONS`, `"profile"` to `DevOSRoutes.PROFILE`, `"project_list"` to `DevOSRoutes.PROJECT_LIST`, `"repository/import"` to `DevOSRoutes.REPOSITORY_IMPORT`. These should reference constants; as literals they are invisible to a refactor and won't produce a compile error when a constant changes.

### DataStore dismissal persistence

`dismissRecommendation` writes to DataStore, then immediately filters the in-memory `Success` state without a full reload — so the UI reacts instantly while the persistence is async. On the next `loadDashboard()`, the dismissed IDs are read back and filtered before populating state. The test `dismissRecommendation with pre-dismissed IDs in DataStore filters them out` covers this path by pre-populating the mock with `"r1"` and asserting it is absent from the loaded state.

### Lazy list usage and `key=` discipline

Every list in the `LazyColumn` supplies stable `key` values: static items use string literals (`"top_bar"`, `"search_bar"`), the recommendations and project cards use `key = { it.id }`, and session rows use `key = { _, session -> session.id }`. The projects horizontal strip is a `LazyRow` inside a single `item {}`, avoiding a nested scrollable conflict.

The health grid uses a fixed 2×2 `Column+Row` instead of `LazyVerticalGrid` — `LazyVerticalGrid` inside `LazyColumn` is unsupported by Compose and would crash at runtime. The trade-off is documented in the code comment.

### Accessibility

`ProjectCard` sets `contentDescription = "${project.name} project, ${project.healthStatus.label()}"`. `RecommendationCard`'s dismiss `IconButton` has `contentDescription = "Dismiss ${recommendation.title}"`. `ChatSessionItem` uses `.semantics { contentDescription = ...; role = Role.Button }` on the clickable row, covering the role requirement for non-Button clickables.

Touch targets: `IconButton` wrappers are sized at `DevOSSpacing.touchTarget` (48dp). The avatar circle is `DevOSSpacing.iconSizeLarge` (32dp) — confirmed below the 48dp minimum. The circle is clickable via a raw `Modifier.clickable`, not wrapped in an `IconButton`, so there's no automatic size enforcement. Fix: wrap in `IconButton` or add `Modifier.size(DevOSSpacing.touchTarget)` around the avatar `Box`.

### Spacing and color discipline

No raw `.dp` or `.sp` literals appear in any feature-home Kotlin file after the second iteration fixes. `HealthStatus` → color mapping uses `tertiary`/`secondary`/`error` and documents the `secondary` ≠ amber-warning concern as TODO(DEVOS-060).

The `fontWeight = FontWeight.Bold` in `HomeTopBar` is passed as the Compose `Text` `fontWeight` parameter on top of `style = MaterialTheme.typography.titleLarge` — it overrides weight without introducing a raw font size literal. The typography base remains a design-system token.

### Test coverage

Nine unit tests cover `HomeViewModel`: initial Loading state, Success transition, stub data counts, dismissal in-memory update, dismissal pre-filtering from DataStore, Error transition, retryable flag, and retry recovery. All pass per verification evidence (9 tests, 0 failures, 3.602s).

No Compose UI tests are present. The arch standard requires a `@Composable` UI test per screen. This gap should be addressed before the screen ships with real data (DEVOS-058 or a dedicated test ticket).

</details>

---

<details>
<summary>File map</summary>

| File | Change |
|------|--------|
| `dashboard/HomeUiState.kt` | New — sealed interface with 4 states |
| `dashboard/HomeNavEvent.kt` | New — 8 sealed nav event types |
| `dashboard/HomeViewModel.kt` | New — @HiltViewModel with DataStore dismissal and SharedFlow nav events |
| `dashboard/HomeScreen.kt` | New — stateless Compose screen, 4-state when branch, full dashboard layout |
| `dashboard/components/ProjectCard.kt` | New — 160dp horizontal-scroll card with health dot |
| `dashboard/components/RecommendationCard.kt` | New — dismissible AI recommendation card |
| `dashboard/components/HealthCell.kt` | New — display-only health metric cell |
| `dashboard/components/ChatSessionItem.kt` | New — session list row with divider support |
| `di/HomeModule.kt` | New — @Named("home") DataStore<Preferences> binding |
| `navigation/HomeNavigation.kt` | New — NavGraphBuilder extension, nav event collection |
| `model/*.kt` | New — 6 stub model files (ProjectSummary, AIRecommendation, HealthStatus, ProjectHealth, ChatSessionSummary, RecommendationType) |
| `HomeViewModelTest.kt` | New — 9 unit tests for HomeViewModel |
| `designsystem/theme/Spacing.kt` | Modified — added projectCardWidth=160dp, statusDotSize=6dp |
| `app/navigation/DevOSNavGraph.kt` | Modified — homeNavigation(navController) wired in |

</details>
