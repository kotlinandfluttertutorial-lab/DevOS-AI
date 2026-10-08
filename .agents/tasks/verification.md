# DEVOS-057 Verification — Home Dashboard Screen

**Date:** 2025-01-09 (updated after review fixes)
**Assignee:** Firoj Mohammad
**Jira:** DA-69

---

## Iteration 2 — Review Fixes Applied

Three review findings from `.agents/tasks/review.json` were resolved:

| Finding | File | Fix Applied |
|---------|------|-------------|
| Hardcoded card width `.width(160.dp)` | `ProjectCard.kt` | Added `DevOSSpacing.projectCardWidth = 160.dp` token; replaced literal |
| Hardcoded dot size `.size(6.dp)` | `ProjectCard.kt` | Added `DevOSSpacing.statusDotSize = 6.dp` token; replaced literal |
| Hardcoded font size `fontSize = 24.sp` | `HealthCell.kt` | Replaced with `MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)` |

---

## Build Results

### 1. Feature module build (post-fix)
Command: `.\gradlew :feature:feature-home:assembleDebug`
**Result: BUILD SUCCESSFUL** (40s)

### 2. Full app build (post-fix)
Command: `.\gradlew assembleDebug`
**Result: BUILD SUCCESSFUL** (1m 10s)

### 3. Unit tests (post-fix)
Command: `.\gradlew :feature:feature-home:testDebugUnitTest`
**Result: BUILD SUCCESSFUL** — 9 tests, 0 failures, 0 skipped (3.602s)

Tests executed:
- `initial state is Loading` ✅
- `loadDashboard emits Success with stub data` ✅
- `loadDashboard Success state contains 3 stub projects` ✅
- `loadDashboard Success state contains 3 stub recommendations` ✅
- `dismissRecommendation removes item from uiState` ✅
- `dismissRecommendation with pre-dismissed IDs in DataStore filters them out` ✅
- `loadDashboard transitions to Error state when DataStore throws` ✅
- `error state is retryable` ✅
- `retry after error recovers to Success state` ✅

---

## Acceptance Criteria

| AC | Description | Status | Evidence |
|----|-------------|--------|---------|
| AC1 | Dashboard loads within 2 seconds | ✅ PASS | Stub data loads in `init` synchronously; no network call |
| AC2 | All sections render with correct data | ✅ PASS | `HomeDashboardContent` renders all sections: Projects, Recommendations, Health, Sessions |
| AC3 | Recommendation dismiss persists across restarts | ✅ PASS | `dismissRecommendation` calls `dataStore.edit` and filters on load; test verifies pre-dismissed IDs |
| AC4 | Health grid shows 4 indicators with correct colors | ✅ PASS | `ProjectHealthGrid` renders 2×2 `HealthCell` grid with `HealthStatus`-mapped colors |
| AC5 | Empty state shown for users with no projects | ✅ PASS | `HomeUiState.Empty` renders `DevOSEmptyState` with RocketLaunch icon |
| AC6 | Search bar tap navigates to SearchScreen | ✅ PASS | Transparent overlay in search bar section fires `onSearchTap` → `HomeNavEvent.NavigateToSearch` |
| AC7 | Notification bell navigates to NotificationsScreen | ✅ PASS | `onNotificationTap` → `HomeNavEvent.NavigateToNotifications` → `navController.navigate("notifications")` |
| AC8 | Dark mode: all colors from `MaterialTheme.colorScheme.*` | ✅ PASS | Zero hardcoded hex values; all colors use design system tokens |
| AC9 | All interactive elements have contentDescription | ✅ PASS | Notification bell, profile avatar, project cards, recommendation cards, session items all have semantics |
| AC10 | No hardcoded dimension literals | ✅ PASS | All dp/sp values in feature-home sourced from `DevOSSpacing.*` or `MaterialTheme.typography.*` |

---

## Design System Changes

`designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Spacing.kt` — added:
```kotlin
// Home Dashboard
val projectCardWidth: Dp = 160.dp
val statusDotSize:    Dp = 6.dp
```

---

## Files Created

```
feature/feature-home/src/main/kotlin/com/devos/ai/feature/home/
  model/
    HealthStatus.kt
    RecommendationType.kt
    ProjectSummary.kt
    AIRecommendation.kt
    ProjectHealth.kt
    ChatSessionSummary.kt
  dashboard/
    HomeUiState.kt
    HomeNavEvent.kt
    HomeViewModel.kt
    HomeScreen.kt
    components/
      ProjectCard.kt
      RecommendationCard.kt
      HealthCell.kt
      ChatSessionItem.kt
  di/
    HomeModule.kt
  navigation/
    HomeNavigation.kt

feature/feature-home/src/test/kotlin/com/devos/ai/feature/home/
  HomeViewModelTest.kt
```

## Files Modified

```
designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Spacing.kt
  - Added: projectCardWidth = 160.dp
  - Added: statusDotSize = 6.dp

feature/feature-home/src/main/kotlin/.../components/ProjectCard.kt
  - Replaced: .width(160.dp) → .width(DevOSSpacing.projectCardWidth)
  - Replaced: .size(6.dp)   → .size(DevOSSpacing.statusDotSize)
  - Removed:  unused import androidx.compose.ui.unit.dp

feature/feature-home/src/main/kotlin/.../components/HealthCell.kt
  - Replaced: fontSize = 24.sp, fontWeight = FontWeight.Bold
             → style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
  - Removed:  unused import androidx.compose.ui.unit.sp

feature/feature-home/build.gradle.kts
  - Added: implementation(libs.androidx.datastore)
  - Added: testRuntimeOnly(libs.test.junit5.launcher)
  - Added: testOptions { unitTests.all { it.useJUnitPlatform() } }

app/src/main/kotlin/com/devos/ai/navigation/DevOSNavGraph.kt
  - Replaced PlaceholderScreen for HOME route with homeNavigation(navController)
  - Added import: com.devos.ai.feature.home.navigation.homeNavigation
```

---

## Architecture Notes

- `HomeModule` provides `@Named("home") DataStore<Preferences>` to avoid conflict with `AuthModule`'s unqualified `DataStore<Preferences>`.
- Health grid uses Column+Row (not LazyVerticalGrid) to avoid nested scrollable container conflict inside LazyColumn.
- Search bar uses a transparent `Box` overlay to intercept taps before `BasicTextField` captures focus.
- Warning color maps to `MaterialTheme.colorScheme.secondary` (DevOSCyan300/#89DDFF in dark). No dedicated M3 warning role exists in the theme. TODO(DEVOS-060): add `ExtendedColors.warning = DevOSAmber300` if design requires it.
