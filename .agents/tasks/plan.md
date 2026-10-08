# Implementation Plan — DEVOS-057: Home Dashboard Screen
**Jira:** DA-69 | **Assignee:** Firoj Mohammad | **Module:** `feature/feature-home`

---

## Pre-flight: Codebase findings

- `feature/feature-home/` has empty `src/main/kotlin/com/devos/ai/feature/` and matching empty test dirs — no Kotlin files exist yet.
- `build.gradle.kts` already has all needed deps: `hilt`, `navigation-compose`, `lifecycle-compose`, `designsystem`, `core-common`, `core-ui`.
- **Missing dep:** `datastore-preferences` for dismissal persistence — must be added.
- `DevOSNavGraph.kt` has `composable(route = DevOSRoutes.HOME) { PlaceholderScreen(...) }` — this must be replaced with `homeNavigation(navController)`.
- Design tokens: all accessed via `MaterialTheme.colorScheme.*` and `MaterialTheme.spacing.*` (via `DevOSSpacing` extension on `MaterialTheme`).
- `DevOSSearchBar` is non-clickable (text input only). For the home screen, the search bar should be non-editable and navigate on tap — use a `Box` overlay over a disabled `DevOSSearchBar`.
- The `#s-home` mockup defines a **health grid** using raw value + label cells (not the ring `DevOSHealthIndicator`). Use a 2-column `LazyVerticalGrid` of `HealthCell` composables with color-coded big values.
- The mockup does **not** show a `RepositorySummary` section — the spec adds it but the mockup stops at Recent AI Sessions. Implement both per the spec, since the spec is authoritative for completeness.
- `DevOSSectionHeader` action slot takes `@Composable () -> Unit` — use a `TextButton` for "See all" links.
- `DevOSStatusBadge` takes `(status: DevOSBadgeStatus, label: String)` — map `RecommendationType` → `DevOSBadgeStatus`.
- No existing domain model for `ProjectSummary`, `AIRecommendation`, etc. — define all models in the feature's `model/` package (stub domain, no backend yet; real domain wiring is DEVOS-058).
- Auth test pattern: JUnit5 (`@Test` from `org.junit.jupiter.api`), MockK, `StandardTestDispatcher`, `assertk`.
- `DevOSSpacing.iconSizeSm` does not exist — use `DevOSSpacing.iconSizeSmall` (20dp) per `Spacing.kt`.

---

## Files to create

```
feature/feature-home/src/main/kotlin/com/devos/ai/feature/home/
  model/
    ProjectSummary.kt
    AIRecommendation.kt
    ProjectHealth.kt
    ChatSessionSummary.kt
    RepositorySummary.kt       ← needed by HomeUiState.Success
  dashboard/
    HomeUiState.kt
    HomeNavEvent.kt
    HomeViewModel.kt
    HomeScreen.kt
    components/
      ProjectCard.kt            ← DevOSProjectCard (horizontal scroll card)
      RecommendationCard.kt
      HealthCell.kt             ← 2-col grid cell matching mockup
      ChatSessionItem.kt
      RepositoryItem.kt
  navigation/
    HomeNavigation.kt

feature/feature-home/src/test/kotlin/com/devos/ai/feature/home/
  HomeViewModelTest.kt
```

## Files to modify

```
feature/feature-home/build.gradle.kts
  → add: implementation(libs.datastore.preferences)

app/src/main/kotlin/com/devos/ai/navigation/DevOSNavGraph.kt
  → replace: composable(route = DevOSRoutes.HOME) { PlaceholderScreen(...) }
  → with:    homeNavigation(navController)
  → add import: com.devos.ai.feature.home.navigation.homeNavigation
```

---

## Step-by-step implementation

---

- [ ] 1. **Add DataStore dependency to `feature/feature-home/build.gradle.kts`**

  DataStore is needed for persisting dismissed recommendation IDs. Insert one line in the `dependencies` block.

  File: `feature/feature-home/build.gradle.kts`

  Add inside `dependencies { ... }`:
  ```kotlin
  implementation(libs.datastore.preferences)
  ```

  Verify: `./gradlew :feature:feature-home:assembleDebug` — should compile (will fail on missing sources until later steps, but the dep resolution step itself must not fail).

---

- [ ] 2. **Create domain model stubs in `feature/feature-home/.../model/`**

  These are pure Kotlin data classes — no Android imports. They are local stubs until DEVOS-058 provides real domain interfaces.

  Files to create:

  **`model/ProjectSummary.kt`**
  ```kotlin
  package com.devos.ai.feature.home.model

  /** Summary of a project shown on the Home dashboard. */
  data class ProjectSummary(
      val id: String,
      val name: String,
      val language: String,
      val platform: String,           // e.g. "Android · Kotlin"
      val healthStatus: HealthStatus,
      val healthLabel: String,        // "Healthy" / "2 Warnings" / "1 Critical"
      val lastModifiedMs: Long = System.currentTimeMillis(),
  )

  /** Matches the color-coded dot in the mockup project cards. */
  enum class HealthStatus { HEALTHY, WARNING, CRITICAL }
  ```

  **`model/AIRecommendation.kt`**
  ```kotlin
  package com.devos.ai.feature.home.model

  import com.devos.ai.designsystem.components.DevOSBadgeStatus

  data class AIRecommendation(
      val id: String,
      val type: RecommendationType,
      val title: String,
      val description: String,
  )

  enum class RecommendationType(
      val label: String,
      val badgeStatus: DevOSBadgeStatus,
  ) {
      SECURITY("Security", DevOSBadgeStatus.ERROR),
      TESTING("Testing",   DevOSBadgeStatus.WARNING),
      LEARNING("Learning", DevOSBadgeStatus.INFO),
      PERFORMANCE("Performance", DevOSBadgeStatus.WARNING),
      REFACTOR("Refactor", DevOSBadgeStatus.PENDING),
  }
  ```

  **`model/ProjectHealth.kt`**
  ```kotlin
  package com.devos.ai.feature.home.model

  /** Four-quadrant health snapshot shown in the 2×2 grid. */
  data class ProjectHealth(
      val securityLabel: String,      // e.g. "2"
      val securitySub: String,        // e.g. "Critical findings"
      val securityStatus: HealthStatus,
      val testLabel: String,          // e.g. "67%"
      val testSub: String,            // e.g. "Coverage"
      val testStatus: HealthStatus,
      val archLabel: String,          // e.g. "A"
      val archSub: String,            // e.g. "Clean MVVM"
      val archStatus: HealthStatus,
      val depsLabel: String,          // e.g. "3"
      val depsSub: String,            // e.g. "Updates available"
      val depsStatus: HealthStatus,
  )
  ```

  **`model/ChatSessionSummary.kt`**
  ```kotlin
  package com.devos.ai.feature.home.model

  data class ChatSessionSummary(
      val id: String,
      val title: String,
      val relativeTime: String,       // "2 hours ago"
      val projectName: String,
  )
  ```

  **`model/RepositorySummary.kt`**
  ```kotlin
  package com.devos.ai.feature.home.model

  data class RepositorySummary(
      val id: String,
      val name: String,
      val language: String,
      val lastSyncedMs: Long,
  )
  ```

  Verify: these are pure Kotlin — no verification command needed at this step, but they compile in the next step.

---

- [ ] 3. **Create `HomeUiState` sealed interface and `HomeNavEvent` sealed class**

  Files to create:

  **`dashboard/HomeUiState.kt`**
  ```kotlin
  package com.devos.ai.feature.home.dashboard

  import com.devos.ai.feature.home.model.AIRecommendation
  import com.devos.ai.feature.home.model.ChatSessionSummary
  import com.devos.ai.feature.home.model.ProjectHealth
  import com.devos.ai.feature.home.model.ProjectSummary
  import com.devos.ai.feature.home.model.RepositorySummary

  sealed interface HomeUiState {
      data object Loading : HomeUiState

      data class Success(
          val greetingName: String,
          val recentProjects: List<ProjectSummary>,
          val recommendations: List<AIRecommendation>,
          val projectHealth: ProjectHealth?,
          val recentSessions: List<ChatSessionSummary>,
          val recentRepositories: List<RepositorySummary>,
      ) : HomeUiState

      data object Empty : HomeUiState     // new user, no projects

      data class Error(
          val message: String,
          val retryable: Boolean,
      ) : HomeUiState
  }
  ```

  **`dashboard/HomeNavEvent.kt`**
  ```kotlin
  package com.devos.ai.feature.home.dashboard

  sealed class HomeNavEvent {
      data object NavigateToSearch          : HomeNavEvent()
      data class  NavigateToProject(val projectId: String) : HomeNavEvent()
      data class  NavigateToRepository(val repoId: String) : HomeNavEvent()
      data class  NavigateToChat(val sessionId: String? = null) : HomeNavEvent()
      data object NavigateToNotifications   : HomeNavEvent()
      data object NavigateToProfile         : HomeNavEvent()
      data object NavigateToImport          : HomeNavEvent()
      data object NavigateToProjectList     : HomeNavEvent()
  }
  ```

  Verify: `./gradlew :feature:feature-home:assembleDebug` — these compile without errors.

---

- [ ] 4. **Create `HomeViewModel`**

  File: `feature/feature-home/src/main/kotlin/com/devos/ai/feature/home/dashboard/HomeViewModel.kt`

  Key design decisions:
  - Uses stub data since DEVOS-058 (real domain use case) is not yet implemented. Annotate with `// TODO(DEVOS-058): replace with GetDashboardUseCase`.
  - `dismissRecommendation` persists dismissed IDs to `DataStore<Preferences>` using the key `"dismissed_recommendations"` (comma-separated IDs).
  - `navEvent` is a `SharedFlow<HomeNavEvent>` with replay=0, so navigation only fires once.
  - `ioDispatcher` is injected for testability using `@IoDispatcher` qualifier from `core-common`.

  ```kotlin
  package com.devos.ai.feature.home.dashboard

  import androidx.datastore.core.DataStore
  import androidx.datastore.preferences.core.Preferences
  import androidx.datastore.preferences.core.edit
  import androidx.datastore.preferences.core.stringPreferencesKey
  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.devos.ai.core.common.di.IoDispatcher
  import com.devos.ai.feature.home.model.AIRecommendation
  import com.devos.ai.feature.home.model.ChatSessionSummary
  import com.devos.ai.feature.home.model.HealthStatus
  import com.devos.ai.feature.home.model.ProjectHealth
  import com.devos.ai.feature.home.model.ProjectSummary
  import com.devos.ai.feature.home.model.RepositorySummary
  import com.devos.ai.feature.home.model.RecommendationType
  import dagger.hilt.android.lifecycle.HiltViewModel
  import kotlinx.coroutines.CoroutineDispatcher
  import kotlinx.coroutines.flow.MutableSharedFlow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.SharedFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asSharedFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.map
  import kotlinx.coroutines.flow.first
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.withContext
  import javax.inject.Inject

  @HiltViewModel
  class HomeViewModel @Inject constructor(
      private val dataStore: DataStore<Preferences>,
      @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  ) : ViewModel() {

      companion object {
          private val DISMISSED_KEY = stringPreferencesKey("dismissed_recommendations")
      }

      private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
      val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

      private val _navEvent = MutableSharedFlow<HomeNavEvent>()
      val navEvent: SharedFlow<HomeNavEvent> = _navEvent.asSharedFlow()

      init {
          loadDashboard()
      }

      fun loadDashboard() {
          viewModelScope.launch {
              _uiState.value = HomeUiState.Loading
              try {
                  val dismissed = withContext(ioDispatcher) {
                      dataStore.data.map { prefs ->
                          prefs[DISMISSED_KEY]?.split(",")?.toSet() ?: emptySet()
                      }.first()
                  }
                  // TODO(DEVOS-058): replace stub data with GetDashboardUseCase
                  val projects = stubProjects()
                  val allRecommendations = stubRecommendations()
                  val filteredRecs = allRecommendations.filter { it.id !in dismissed }

                  if (projects.isEmpty()) {
                      _uiState.value = HomeUiState.Empty
                  } else {
                      _uiState.value = HomeUiState.Success(
                          greetingName = "Dev",
                          recentProjects = projects,
                          recommendations = filteredRecs,
                          projectHealth = stubProjectHealth(),
                          recentSessions = stubSessions(),
                          recentRepositories = stubRepositories(),
                      )
                  }
              } catch (e: Exception) {
                  _uiState.value = HomeUiState.Error(
                      message = e.message ?: "Failed to load dashboard",
                      retryable = true,
                  )
              }
          }
      }

      fun dismissRecommendation(id: String) {
          viewModelScope.launch {
              withContext(ioDispatcher) {
                  dataStore.edit { prefs ->
                      val current = prefs[DISMISSED_KEY]?.split(",")?.toMutableSet() ?: mutableSetOf()
                      current.add(id)
                      prefs[DISMISSED_KEY] = current.joinToString(",")
                  }
              }
              // Update state immediately without a full reload
              val current = _uiState.value
              if (current is HomeUiState.Success) {
                  _uiState.value = current.copy(
                      recommendations = current.recommendations.filter { it.id != id }
                  )
              }
          }
      }

      fun onSearchTap()           = emit(HomeNavEvent.NavigateToSearch)
      fun onProjectTap(id: String) = emit(HomeNavEvent.NavigateToProject(id))
      fun onRepoTap(id: String)   = emit(HomeNavEvent.NavigateToRepository(id))
      fun onChatTap(id: String?)  = emit(HomeNavEvent.NavigateToChat(id))
      fun onNotificationsTap()    = emit(HomeNavEvent.NavigateToNotifications)
      fun onProfileTap()          = emit(HomeNavEvent.NavigateToProfile)
      fun onImportTap()           = emit(HomeNavEvent.NavigateToImport)
      fun onProjectListTap()      = emit(HomeNavEvent.NavigateToProjectList)

      private fun emit(event: HomeNavEvent) {
          viewModelScope.launch { _navEvent.emit(event) }
      }

      // ── Stub data — remove after DEVOS-058 ──────────────────────────────────

      private fun stubProjects() = listOf(
          ProjectSummary("p1", "DevOS AI",    "Kotlin", "Android · Kotlin",   HealthStatus.HEALTHY,  "Healthy"),
          ProjectSummary("p2", "Compose Lib", "Kotlin", "Android · Kotlin",   HealthStatus.WARNING,  "2 Warnings"),
          ProjectSummary("p3", "SDK Tools",   "Kotlin", "Kotlin Multiplatform",HealthStatus.CRITICAL, "1 Critical"),
      )

      private fun stubRecommendations() = listOf(
          AIRecommendation("r1", RecommendationType.SECURITY, "Security: SQL Injection Risk",
              "Found in DatabaseHelper.kt · High severity"),
          AIRecommendation("r2", RecommendationType.TESTING,  "Add tests for AuthViewModel",
              "Coverage dropped to 34% · 3 untested functions"),
          AIRecommendation("r3", RecommendationType.LEARNING, "Learn: Kotlin Coroutines",
              "Based on recent code patterns · 4 lessons"),
      )

      private fun stubProjectHealth() = ProjectHealth(
          securityLabel = "2",   securitySub = "Critical findings",  securityStatus = HealthStatus.CRITICAL,
          testLabel     = "67%", testSub     = "Coverage",           testStatus     = HealthStatus.WARNING,
          archLabel     = "A",   archSub     = "Clean MVVM",         archStatus     = HealthStatus.HEALTHY,
          depsLabel     = "3",   depsSub     = "Updates available",  depsStatus     = HealthStatus.WARNING,
      )

      private fun stubSessions() = listOf(
          ChatSessionSummary("s1", "Explain RepositoryViewModel", "2 hours ago", "DevOS AI"),
          ChatSessionSummary("s2", "Debug navigation back stack issue", "Yesterday", "Compose Lib"),
      )

      private fun stubRepositories() = listOf(
          RepositorySummary("repo1", "DevOS-AI",    "Kotlin",   System.currentTimeMillis()),
          RepositorySummary("repo2", "ComposeLib",  "Kotlin",   System.currentTimeMillis()),
      )
  }
  ```

  Verify: `./gradlew :feature:feature-home:assembleDebug` — ViewModel and models compile without errors.

---

- [ ] 5. **Wire DataStore in Hilt — add `HomeModule`**

  The ViewModel requires `DataStore<Preferences>` injection. Check if a DataStore binding already exists in the app's `AppModule`; if not, create a `HomeModule` in feature-home that provides a named `DataStore<Preferences>` scoped to the feature.

  Check first: `app/src/main/kotlin/com/devos/ai/di/AppModule.kt` — look for `DataStore` bindings.

  If not present, create:

  File: `feature/feature-home/src/main/kotlin/com/devos/ai/feature/home/di/HomeModule.kt`

  ```kotlin
  package com.devos.ai.feature.home.di

  import android.content.Context
  import androidx.datastore.core.DataStore
  import androidx.datastore.preferences.core.Preferences
  import androidx.datastore.preferences.preferencesDataStore
  import dagger.Module
  import dagger.Provides
  import dagger.hilt.InstallIn
  import dagger.hilt.android.qualifiers.ApplicationContext
  import dagger.hilt.components.SingletonComponent
  import javax.inject.Singleton

  private val Context.homeDataStore: DataStore<Preferences> by preferencesDataStore(
      name = "home_prefs"
  )

  @Module
  @InstallIn(SingletonComponent::class)
  object HomeModule {

      @Provides
      @Singleton
      fun provideHomeDataStore(
          @ApplicationContext context: Context,
      ): DataStore<Preferences> = context.homeDataStore
  }
  ```

  > **Note:** If an app-level DataStore binding already exists and conflicts, use a `@Named("home")` qualifier. Check `AppModule.kt` before deciding.

  Verify: `./gradlew :feature:feature-home:assembleDebug` — Hilt graph resolves without "missing binding" errors.

---

- [ ] 6. **Create sub-composables: `ProjectCard`, `RecommendationCard`, `HealthCell`, `ChatSessionItem`, `RepositoryItem`**

  These are private-ish composables in the `dashboard/components/` package. Keep them `internal` — they are not part of the public API of the feature module.

  **`components/ProjectCard.kt`** — matches the `min-width:160dp` horizontal-scroll card in the `#s-home` mockup.

  ```kotlin
  package com.devos.ai.feature.home.dashboard.components

  import androidx.compose.foundation.background
  import androidx.compose.foundation.border
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.semantics.contentDescription
  import androidx.compose.ui.semantics.semantics
  import androidx.compose.ui.unit.dp
  import com.devos.ai.designsystem.components.DevOSCard
  import com.devos.ai.designsystem.theme.DevOSSpacing
  import com.devos.ai.feature.home.model.HealthStatus
  import com.devos.ai.feature.home.model.ProjectSummary

  @Composable
  internal fun ProjectCard(
      project: ProjectSummary,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
  ) {
      DevOSCard(
          onClick = onClick,
          modifier = modifier
              .width(160.dp)
              .semantics { contentDescription = "${project.name} project, ${project.healthLabel}" },
      ) {
          Column(
              modifier = Modifier.padding(DevOSSpacing.base),
              verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
          ) {
              // Platform icon placeholder — use first letter in a colored circle
              // until real project icons are available (DEVOS-016)
              Box(
                  modifier = Modifier
                      .size(32.dp)
                      .background(
                          color = MaterialTheme.colorScheme.primaryContainer,
                          shape = MaterialTheme.shapes.small,
                      ),
                  contentAlignment = Alignment.Center,
              ) {
                  Text(
                      text = project.name.first().uppercase(),
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
              }
              Text(
                  text = project.name,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
              )
              Text(
                  text = project.platform,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
              )
              // Health dot + label row
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
              ) {
                  Box(
                      modifier = Modifier
                          .size(6.dp)
                          .background(
                              color = project.healthStatus.dotColor(),
                              shape = CircleShape,
                          ),
                  )
                  Text(
                      text = project.healthLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = project.healthStatus.dotColor(),
                  )
              }
          }
      }
  }

  @Composable
  private fun HealthStatus.dotColor(): Color = when (this) {
      HealthStatus.HEALTHY  -> MaterialTheme.colorScheme.tertiary   // #C3E88D
      HealthStatus.WARNING  -> MaterialTheme.colorScheme.secondary  // #FFCB6B (warning mapped to secondary in M3 extended)
      HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error      // #FF5370
  }
  ```

  > **Design note:** The mockup uses hardcoded `#C3E88D` (healthy green), `#FFCB6B` (warning amber), `#FF5370` (critical red). These map to `tertiary`, `warning`-via-secondary (check actual Color.kt), and `error` respectively. After reading `Color.kt` in step 6, verify these mappings are correct and adjust if `warning` is mapped differently in the theme.

  **`components/RecommendationCard.kt`** — matches the `border-left:3px solid <color>` cards in the mockup.

  ```kotlin
  package com.devos.ai.feature.home.dashboard.components

  import androidx.compose.foundation.layout.*
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.outlined.BugReport
  import androidx.compose.material.icons.outlined.Close
  import androidx.compose.material.icons.outlined.Lock
  import androidx.compose.material.icons.outlined.MenuBook
  import androidx.compose.material.icons.outlined.Science
  import androidx.compose.material.icons.outlined.Speed
  import androidx.compose.material3.Icon
  import androidx.compose.material3.IconButton
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.semantics.contentDescription
  import androidx.compose.ui.semantics.semantics
  import com.devos.ai.designsystem.components.DevOSCard
  import com.devos.ai.designsystem.components.DevOSStatusBadge
  import com.devos.ai.designsystem.theme.DevOSSpacing
  import com.devos.ai.feature.home.model.AIRecommendation
  import com.devos.ai.feature.home.model.RecommendationType

  @Composable
  internal fun RecommendationCard(
      recommendation: AIRecommendation,
      onDismiss: (String) -> Unit,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
  ) {
      DevOSCard(
          onClick = onClick,
          modifier = modifier
              .fillMaxWidth()
              .semantics { contentDescription = recommendation.title },
      ) {
          Row(
              modifier = Modifier.padding(DevOSSpacing.base),
              verticalAlignment = Alignment.Top,
          ) {
              Icon(
                  imageVector = recommendation.type.icon(),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(DevOSSpacing.iconSize),
              )
              Spacer(modifier = Modifier.width(DevOSSpacing.sm))
              Column(modifier = Modifier.weight(1f)) {
                  DevOSStatusBadge(
                      status = recommendation.type.badgeStatus,
                      label  = recommendation.type.label,
                  )
                  Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                  Text(
                      text  = recommendation.title,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                  Text(
                      text  = recommendation.description,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
              }
              IconButton(
                  onClick = { onDismiss(recommendation.id) },
                  modifier = Modifier.size(DevOSSpacing.touchTarget),
              ) {
                  Icon(
                      imageVector = Icons.Outlined.Close,
                      contentDescription = "Dismiss ${recommendation.title}",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
              }
          }
      }
  }

  private fun RecommendationType.icon() = when (this) {
      RecommendationType.SECURITY    -> Icons.Outlined.Lock
      RecommendationType.TESTING     -> Icons.Outlined.Science
      RecommendationType.LEARNING    -> Icons.Outlined.MenuBook
      RecommendationType.PERFORMANCE -> Icons.Outlined.Speed
      RecommendationType.REFACTOR    -> Icons.Outlined.BugReport
  }
  ```

  **`components/HealthCell.kt`** — matches the `health-cell` grid items in the mockup (large colored value + sub-label, NOT a ring indicator).

  ```kotlin
  package com.devos.ai.feature.home.dashboard.components

  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.semantics.contentDescription
  import androidx.compose.ui.semantics.semantics
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.unit.sp
  import com.devos.ai.designsystem.components.DevOSCard
  import com.devos.ai.designsystem.theme.DevOSSpacing
  import com.devos.ai.feature.home.model.HealthStatus

  @Composable
  internal fun HealthCell(
      label: String,          // e.g. "Security"
      value: String,          // e.g. "2", "67%", "A"
      subLabel: String,       // e.g. "Critical findings"
      status: HealthStatus,
      modifier: Modifier = Modifier,
  ) {
      DevOSCard(
          modifier = modifier
              .fillMaxWidth()
              .semantics { contentDescription = "$label: $value. $subLabel" },
      ) {
          Column(
              modifier = Modifier.padding(DevOSSpacing.base),
              horizontalAlignment = Alignment.Start,
              verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
          ) {
              Text(
                  text  = label,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                  text  = value,
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Bold,
                  color = status.valueColor(),
              )
              Text(
                  text  = subLabel,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
          }
      }
  }

  @Composable
  private fun HealthStatus.valueColor(): Color = when (this) {
      HealthStatus.HEALTHY  -> MaterialTheme.colorScheme.tertiary
      HealthStatus.WARNING  -> MaterialTheme.colorScheme.secondary
      HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error
  }
  ```

  > **Warning color:** The mockup shows `#FFCB6B` for warnings. Check `Color.kt` to confirm which M3 role maps to that value. If `warning` color is mapped to `secondaryContainer` or is a custom extension, adjust the mapping accordingly.

  **`components/ChatSessionItem.kt`** — matches `list-item` rows in the mockup.

  ```kotlin
  package com.devos.ai.feature.home.dashboard.components

  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.*
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.outlined.AutoAwesome
  import androidx.compose.material.icons.outlined.ChevronRight
  import androidx.compose.material3.HorizontalDivider
  import androidx.compose.material3.Icon
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.semantics.Role
  import androidx.compose.ui.semantics.contentDescription
  import androidx.compose.ui.semantics.role
  import androidx.compose.ui.semantics.semantics
  import com.devos.ai.designsystem.theme.DevOSSpacing

  @Composable
  internal fun ChatSessionItem(
      title: String,
      relativeTime: String,
      projectName: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
      showDivider: Boolean = false,
  ) {
      Column(modifier = modifier) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .clickable(onClick = onClick)
                  .padding(
                      horizontal = DevOSSpacing.base,
                      vertical   = DevOSSpacing.sm,
                  )
                  .semantics {
                      contentDescription = "$title, $relativeTime, $projectName"
                      role = Role.Button
                  },
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
          ) {
              Icon(
                  imageVector = Icons.Outlined.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(DevOSSpacing.iconSize),
              )
              Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text  = title,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                  )
                  Text(
                      text  = "$relativeTime · $projectName",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
              }
              Icon(
                  imageVector = Icons.Outlined.ChevronRight,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
          }
          if (showDivider) {
              HorizontalDivider(
                  thickness = DevOSSpacing.xxs,
                  color = MaterialTheme.colorScheme.outline,
              )
          }
      }
  }
  ```

  **`components/RepositoryItem.kt`** — simple list row for recent repositories.

  ```kotlin
  package com.devos.ai.feature.home.dashboard.components

  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.*
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.outlined.ChevronRight
  import androidx.compose.material.icons.outlined.FolderOpen
  import androidx.compose.material3.Icon
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.semantics.Role
  import androidx.compose.ui.semantics.contentDescription
  import androidx.compose.ui.semantics.role
  import androidx.compose.ui.semantics.semantics
  import com.devos.ai.designsystem.theme.DevOSSpacing
  import com.devos.ai.feature.home.model.RepositorySummary

  @Composable
  internal fun RepositoryItem(
      repo: RepositorySummary,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
  ) {
      Row(
          modifier = modifier
              .fillMaxWidth()
              .clickable(onClick = onClick)
              .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm)
              .semantics {
                  contentDescription = "${repo.name} repository, ${repo.language}"
                  role = Role.Button
              },
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
      ) {
          Icon(
              imageVector = Icons.Outlined.FolderOpen,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(DevOSSpacing.iconSize),
          )
          Column(modifier = Modifier.weight(1f)) {
              Text(
                  text  = repo.name,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                  text  = repo.language,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
          }
          Icon(
              imageVector = Icons.Outlined.ChevronRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
      }
  }
  ```

  Verify: `./gradlew :feature:feature-home:assembleDebug` — all sub-composables compile.

---

- [ ] 7. **Create `HomeScreen` composable**

  This is the main screen. It must match the `#s-home` mockup exactly.

  Mockup structure (top to bottom):
  1. Top bar: date string (11sp), greeting "Good morning, Dev 👋" (20sp/700w), notification bell (with red dot badge), avatar circle.
  2. AI search bar — non-editable tap target that navigates to SearchScreen.
  3. Section "Recent Projects" + "See all" → horizontal `LazyRow` of `ProjectCard`.
  4. Section "AI Recommendations" + "3 items" count → `LazyColumn` items of `RecommendationCard`.
  5. Section "Project Health" → fixed 2×2 `FlowRow` (or `LazyVerticalGrid`) of `HealthCell`.
  6. Section "Recent AI Sessions" + "See all" → `ChatSessionItem` list.
  7. Section "Recent Repositories" → `RepositoryItem` list.

  **`dashboard/HomeScreen.kt`**

  ```kotlin
  package com.devos.ai.feature.home.dashboard

  import androidx.compose.foundation.background
  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.lazy.LazyRow
  import androidx.compose.foundation.lazy.items
  import androidx.compose.foundation.lazy.itemsIndexed
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.outlined.NotificationsNone
  import androidx.compose.material.icons.outlined.RocketLaunch
  import androidx.compose.material3.Badge
  import androidx.compose.material3.BadgedBox
  import androidx.compose.material3.ExperimentalMaterial3Api
  import androidx.compose.material3.Icon
  import androidx.compose.material3.IconButton
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Scaffold
  import androidx.compose.material3.Text
  import androidx.compose.material3.TextButton
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.LaunchedEffect
  import androidx.compose.runtime.getValue
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.semantics.contentDescription
  import androidx.compose.ui.semantics.semantics
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import androidx.lifecycle.compose.collectAsStateWithLifecycle
  import com.devos.ai.designsystem.components.DevOSButton
  import com.devos.ai.designsystem.components.DevOSEmptyState
  import com.devos.ai.designsystem.components.DevOSErrorState
  import com.devos.ai.designsystem.components.DevOSLoadingState
  import com.devos.ai.designsystem.components.DevOSSearchBar
  import com.devos.ai.designsystem.components.DevOSSectionHeader
  import com.devos.ai.designsystem.theme.DevOSSpacing
  import com.devos.ai.feature.home.dashboard.components.ChatSessionItem
  import com.devos.ai.feature.home.dashboard.components.HealthCell
  import com.devos.ai.feature.home.dashboard.components.ProjectCard
  import com.devos.ai.feature.home.dashboard.components.RecommendationCard
  import com.devos.ai.feature.home.dashboard.components.RepositoryItem
  import com.devos.ai.feature.home.model.ProjectHealth
  import kotlinx.coroutines.flow.SharedFlow

  /**
   * Home Dashboard — primary landing screen and AI Developer Command Center.
   *
   * Stateless composable: all state and events come from the ViewModel via
   * [HomeNavigation] which passes [uiState] and callbacks.
   *
   * Handles 4 UI states: Loading | Success | Empty | Error
   */
  @Composable
  fun HomeScreen(
      uiState: HomeUiState,
      navEvent: SharedFlow<HomeNavEvent>,
      onSearchTap: () -> Unit,
      onProjectTap: (String) -> Unit,
      onRepoTap: (String) -> Unit,
      onChatTap: (String?) -> Unit,
      onNotificationsTap: () -> Unit,
      onProfileTap: () -> Unit,
      onImportTap: () -> Unit,
      onProjectListTap: () -> Unit,
      onDismissRecommendation: (String) -> Unit,
      onRetry: () -> Unit,
      modifier: Modifier = Modifier,
  ) {
      // Navigation side-effect collection is handled by HomeNavigation — NOT here.
      // HomeScreen is a pure UI composable; nav events flow out via the ViewModel.

      Scaffold(
          modifier = modifier,
          containerColor = MaterialTheme.colorScheme.background,
      ) { innerPadding ->
          when (val state = uiState) {
              is HomeUiState.Loading -> DevOSLoadingState(
                  modifier = Modifier.padding(innerPadding),
              )

              is HomeUiState.Error -> DevOSErrorState(
                  description = state.message,
                  modifier    = Modifier.padding(innerPadding),
                  onRetry     = if (state.retryable) onRetry else null,
              )

              is HomeUiState.Empty -> DevOSEmptyState(
                  icon        = Icons.Outlined.RocketLaunch,
                  title       = "Welcome to DevOS AI",
                  description = "Import your first repository to get started",
                  modifier    = Modifier.padding(innerPadding),
                  action      = {
                      DevOSButton(
                          text    = "Import Repository",
                          onClick = onImportTap,
                      )
                  },
              )

              is HomeUiState.Success -> HomeDashboardContent(
                  state                  = state,
                  onSearchTap            = onSearchTap,
                  onProjectTap           = onProjectTap,
                  onRepoTap              = onRepoTap,
                  onChatTap              = onChatTap,
                  onNotificationsTap     = onNotificationsTap,
                  onProfileTap           = onProfileTap,
                  onImportTap            = onImportTap,
                  onProjectListTap       = onProjectListTap,
                  onDismissRecommendation = onDismissRecommendation,
                  modifier               = Modifier.padding(innerPadding),
              )
          }
      }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun HomeDashboardContent(
      state: HomeUiState.Success,
      onSearchTap: () -> Unit,
      onProjectTap: (String) -> Unit,
      onRepoTap: (String) -> Unit,
      onChatTap: (String?) -> Unit,
      onNotificationsTap: () -> Unit,
      onProfileTap: () -> Unit,
      onImportTap: () -> Unit,
      onProjectListTap: () -> Unit,
      onDismissRecommendation: (String) -> Unit,
      modifier: Modifier = Modifier,
  ) {
      LazyColumn(
          modifier = modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = DevOSSpacing.xl),
      ) {

          // ── Custom top bar (not DevOSTopBar — mockup shows date + greeting + avatar) ──
          item {
              HomeTopBar(
                  greetingName       = state.greetingName,
                  onNotificationsTap = onNotificationsTap,
                  onProfileTap       = onProfileTap,
              )
          }

          // ── AI Search bar (tap-to-navigate; non-editable overlay) ─────────────
          item {
              Box(
                  modifier = Modifier
                      .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm)
                      .clickable(onClick = onSearchTap)
                      .semantics { contentDescription = "Search bar, tap to search" },
              ) {
                  DevOSSearchBar(
                      query         = "",
                      onQueryChange = {},   // non-editable; tap navigates to SearchScreen
                      placeholder   = "Ask AI anything about your code…",
                      modifier      = Modifier.fillMaxWidth(),
                  )
                  // Transparent overlay to intercept tap before BasicTextField
                  Box(modifier = Modifier.matchParentSize())
              }
          }

          // ── Recent Projects ───────────────────────────────────────────────────
          item {
              DevOSSectionHeader(
                  title = "Recent Projects",
                  modifier = Modifier.padding(
                      horizontal = DevOSSpacing.base,
                      vertical   = DevOSSpacing.sm,
                  ),
                  action = {
                      TextButton(onClick = onProjectListTap) {
                          Text(
                              text  = "See all",
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.primary,
                          )
                      }
                  },
              )
          }

          item {
              LazyRow(
                  contentPadding   = PaddingValues(horizontal = DevOSSpacing.base),
                  horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
              ) {
                  items(items = state.recentProjects, key = { it.id }) { project ->
                      ProjectCard(
                          project  = project,
                          onClick  = { onProjectTap(project.id) },
                      )
                  }
              }
          }

          // ── AI Recommendations ────────────────────────────────────────────────
          item {
              DevOSSectionHeader(
                  title    = "AI Recommendations",
                  modifier = Modifier.padding(
                      horizontal = DevOSSpacing.base,
                      vertical   = DevOSSpacing.sm,
                  ),
                  action = {
                      Text(
                          text  = "${state.recommendations.size} items",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                  },
              )
          }

          items(items = state.recommendations, key = { it.id }) { recommendation ->
              RecommendationCard(
                  recommendation = recommendation,
                  onDismiss      = onDismissRecommendation,
                  onClick        = { /* navigate to recommendation detail — DEVOS-058 */ },
                  modifier       = Modifier.padding(
                      horizontal = DevOSSpacing.base,
                      vertical   = DevOSSpacing.xs,
                  ),
              )
          }

          // ── Project Health ────────────────────────────────────────────────────
          state.projectHealth?.let { health ->
              item {
                  DevOSSectionHeader(
                      title    = "Project Health",
                      modifier = Modifier.padding(
                          horizontal = DevOSSpacing.base,
                          vertical   = DevOSSpacing.sm,
                      ),
                  )
              }
              item {
                  ProjectHealthGrid(
                      health   = health,
                      modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                  )
              }
          }

          // ── Recent AI Sessions ────────────────────────────────────────────────
          item {
              DevOSSectionHeader(
                  title    = "Recent AI Sessions",
                  modifier = Modifier.padding(
                      horizontal = DevOSSpacing.base,
                      vertical   = DevOSSpacing.sm,
                  ),
                  action = {
                      TextButton(onClick = { onChatTap(null) }) {
                          Text(
                              text  = "See all",
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.primary,
                          )
                      }
                  },
              )
          }

          itemsIndexed(items = state.recentSessions, key = { _, s -> s.id }) { index, session ->
              ChatSessionItem(
                  title        = session.title,
                  relativeTime = session.relativeTime,
                  projectName  = session.projectName,
                  onClick      = { onChatTap(session.id) },
                  showDivider  = index < state.recentSessions.lastIndex,
              )
          }

          // ── Recent Repositories ───────────────────────────────────────────────
          if (state.recentRepositories.isNotEmpty()) {
              item {
                  DevOSSectionHeader(
                      title    = "Recent Repositories",
                      modifier = Modifier.padding(
                          horizontal = DevOSSpacing.base,
                          vertical   = DevOSSpacing.sm,
                      ),
                  )
              }

              items(items = state.recentRepositories, key = { it.id }) { repo ->
                  RepositoryItem(
                      repo    = repo,
                      onClick = { onRepoTap(repo.id) },
                  )
              }
          }
      }
  }

  /**
   * Custom top bar matching the #s-home mockup:
   * - Date string (11sp, onSurfaceVariant)
   * - Greeting "Good morning, Dev 👋" (20sp bold, onBackground)
   * - Notification bell with red badge dot
   * - Avatar circle with initial letter
   */
  @Composable
  private fun HomeTopBar(
      greetingName: String,
      onNotificationsTap: () -> Unit,
      onProfileTap: () -> Unit,
      modifier: Modifier = Modifier,
  ) {
      Row(
          modifier = modifier
              .fillMaxWidth()
              .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
          verticalAlignment = Alignment.CenterVertically,
      ) {
          Column(modifier = Modifier.weight(1f)) {
              Text(
                  text  = "DevOS AI",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                  text     = "Good morning, $greetingName 👋",
                  fontSize = 20.sp,
                  color    = MaterialTheme.colorScheme.onBackground,
              )
          }

          // Notification bell with badge
          BadgedBox(
              badge = {
                  Badge(containerColor = MaterialTheme.colorScheme.error)
              },
          ) {
              IconButton(
                  onClick   = onNotificationsTap,
                  modifier  = Modifier.size(DevOSSpacing.touchTarget),
              ) {
                  Icon(
                      imageVector    = Icons.Outlined.NotificationsNone,
                      contentDescription = "Notifications",
                      tint           = MaterialTheme.colorScheme.onSurface,
                  )
              }
          }

          Spacer(modifier = Modifier.width(DevOSSpacing.xs))

          // Avatar circle
          Box(
              modifier = Modifier
                  .size(DevOSSpacing.iconSizeLarge)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer)
                  .clickable(onClick = onProfileTap)
                  .semantics { contentDescription = "Profile, ${greetingName.first()}" },
              contentAlignment = Alignment.Center,
          ) {
              Text(
                  text  = greetingName.first().uppercase(),
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
          }
      }
  }

  /**
   * 2×2 health grid matching the #s-home mockup health-grid section.
   *
   * Uses a simple Column+Row layout instead of LazyVerticalGrid to avoid
   * nested scrollable container issues within LazyColumn.
   */
  @Composable
  private fun ProjectHealthGrid(
      health: ProjectHealth,
      modifier: Modifier = Modifier,
  ) {
      Column(
          modifier  = modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
      ) {
          Row(
              horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
          ) {
              HealthCell(
                  label    = "Security",
                  value    = health.securityLabel,
                  subLabel = health.securitySub,
                  status   = health.securityStatus,
                  modifier = Modifier.weight(1f),
              )
              HealthCell(
                  label    = "Tests",
                  value    = health.testLabel,
                  subLabel = health.testSub,
                  status   = health.testStatus,
                  modifier = Modifier.weight(1f),
              )
          }
          Row(
              horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
          ) {
              HealthCell(
                  label    = "Architecture",
                  value    = health.archLabel,
                  subLabel = health.archSub,
                  status   = health.archStatus,
                  modifier = Modifier.weight(1f),
              )
              HealthCell(
                  label    = "Dependencies",
                  value    = health.depsLabel,
                  subLabel = health.depsSub,
                  status   = health.depsStatus,
                  modifier = Modifier.weight(1f),
              )
          }
      }
  }
  ```

  > **Nested scroll note:** Using `LazyVerticalGrid` inside `LazyColumn` is not supported. The plan uses a fixed `Column+Row` pattern for the 2×2 health grid — this avoids the nested scroll conflict and matches the mockup exactly (4 fixed items).

  Verify: `./gradlew :feature:feature-home:assembleDebug` — screen composables compile.

---

- [ ] 8. **Create `HomeNavigation` NavGraphBuilder extension**

  Follows the exact same pattern as `AuthNavigation.kt`: `hiltViewModel()`, `collectAsStateWithLifecycle()`, `LaunchedEffect` on `navEvent`, callbacks passed to screen.

  File: `feature/feature-home/src/main/kotlin/com/devos/ai/feature/home/navigation/HomeNavigation.kt`

  ```kotlin
  package com.devos.ai.feature.home.navigation

  import androidx.compose.runtime.LaunchedEffect
  import androidx.compose.runtime.getValue
  import androidx.hilt.navigation.compose.hiltViewModel
  import androidx.lifecycle.compose.collectAsStateWithLifecycle
  import androidx.navigation.NavController
  import androidx.navigation.NavGraphBuilder
  import androidx.navigation.compose.composable
  import com.devos.ai.feature.home.dashboard.HomeNavEvent
  import com.devos.ai.feature.home.dashboard.HomeScreen
  import com.devos.ai.feature.home.dashboard.HomeViewModel

  private const val ROUTE_HOME = "home"

  /**
   * Adds the Home Dashboard screen to the NavGraph.
   *
   * Replaces `composable(DevOSRoutes.HOME) { PlaceholderScreen(...) }` in DevOSNavGraph.
   */
  fun NavGraphBuilder.homeNavigation(navController: NavController) {
      composable(route = ROUTE_HOME) {
          val viewModel: HomeViewModel = hiltViewModel()
          val uiState by viewModel.uiState.collectAsStateWithLifecycle()

          LaunchedEffect(Unit) {
              viewModel.navEvent.collect { event ->
                  when (event) {
                      HomeNavEvent.NavigateToSearch         ->
                          navController.navigate("search?q=")

                      is HomeNavEvent.NavigateToProject     ->
                          navController.navigate("project/${event.projectId}")

                      is HomeNavEvent.NavigateToRepository  ->
                          navController.navigate("repository/${event.repoId}")

                      is HomeNavEvent.NavigateToChat        ->
                          navController.navigate("ai_chat")

                      HomeNavEvent.NavigateToNotifications  ->
                          navController.navigate("notifications")

                      HomeNavEvent.NavigateToProfile        ->
                          navController.navigate("profile")

                      HomeNavEvent.NavigateToImport         ->
                          navController.navigate("repository/import")

                      HomeNavEvent.NavigateToProjectList    ->
                          navController.navigate("project_list")
                  }
              }
          }

          HomeScreen(
              uiState                 = uiState,
              navEvent                = viewModel.navEvent,
              onSearchTap             = viewModel::onSearchTap,
              onProjectTap            = viewModel::onProjectTap,
              onRepoTap               = viewModel::onRepoTap,
              onChatTap               = viewModel::onChatTap,
              onNotificationsTap      = viewModel::onNotificationsTap,
              onProfileTap            = viewModel::onProfileTap,
              onImportTap             = viewModel::onImportTap,
              onProjectListTap        = viewModel::onProjectListTap,
              onDismissRecommendation = viewModel::dismissRecommendation,
              onRetry                 = viewModel::loadDashboard,
          )
      }
  }
  ```

  Verify: `./gradlew :feature:feature-home:assembleDebug` — navigation extension compiles.

---

- [ ] 9. **Wire `homeNavigation` in `DevOSNavGraph.kt`**

  Replace the placeholder `composable(route = DevOSRoutes.HOME)` block with `homeNavigation(navController)` and add the import.

  File: `app/src/main/kotlin/com/devos/ai/navigation/DevOSNavGraph.kt`

  Replace:
  ```kotlin
  composable(route = DevOSRoutes.HOME) {
      PlaceholderScreen(route = DevOSRoutes.HOME)
  }
  ```

  With:
  ```kotlin
  homeNavigation(navController)
  ```

  Add import at the top:
  ```kotlin
  import com.devos.ai.feature.home.navigation.homeNavigation
  ```

  Also add `feature-home` to the `:app` module's `build.gradle.kts` dependencies if it is not already present.

  Check: `app/build.gradle.kts` — look for `implementation(project(":feature:feature-home"))`.
  If missing, add it to the `dependencies` block.

  Verify: `./gradlew assembleDebug` — full project compiles and links.

---

- [ ] 10. **Create `HomeViewModelTest`**

  Follows the `AuthViewModelTest` pattern: JUnit5, MockK, `StandardTestDispatcher`, assertk.

  File: `feature/feature-home/src/test/kotlin/com/devos/ai/feature/home/HomeViewModelTest.kt`

  ```kotlin
  package com.devos.ai.feature.home

  import androidx.datastore.core.DataStore
  import androidx.datastore.preferences.core.MutablePreferences
  import androidx.datastore.preferences.core.Preferences
  import androidx.datastore.preferences.core.emptyPreferences
  import assertk.assertThat
  import assertk.assertions.isInstanceOf
  import assertk.assertions.isEqualTo
  import com.devos.ai.feature.home.dashboard.HomeUiState
  import com.devos.ai.feature.home.dashboard.HomeViewModel
  import io.mockk.coEvery
  import io.mockk.every
  import io.mockk.mockk
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.flow.flow
  import kotlinx.coroutines.flow.flowOf
  import kotlinx.coroutines.test.StandardTestDispatcher
  import kotlinx.coroutines.test.advanceUntilIdle
  import kotlinx.coroutines.test.resetMain
  import kotlinx.coroutines.test.runTest
  import kotlinx.coroutines.test.setMain
  import org.junit.jupiter.api.AfterEach
  import org.junit.jupiter.api.BeforeEach
  import org.junit.jupiter.api.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class HomeViewModelTest {

      private val testDispatcher = StandardTestDispatcher()
      private val mockDataStore: DataStore<Preferences> = mockk()
      private val mockPrefs: Preferences = mockk()

      @BeforeEach
      fun setUp() {
          Dispatchers.setMain(testDispatcher)
          // Default: no dismissed recommendations
          every { mockDataStore.data } returns flowOf(emptyPreferences())
      }

      @AfterEach
      fun tearDown() {
          Dispatchers.resetMain()
      }

      private fun createViewModel() = HomeViewModel(
          dataStore    = mockDataStore,
          ioDispatcher = testDispatcher,
      )

      @Test
      fun `initial state is Loading`() {
          val viewModel = createViewModel()
          assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Loading::class)
      }

      @Test
      fun `loadDashboard emits Success with stub data`() = runTest {
          val viewModel = createViewModel()
          advanceUntilIdle()
          assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Success::class)
      }

      @Test
      fun `loadDashboard Success state contains 3 stub projects`() = runTest {
          val viewModel = createViewModel()
          advanceUntilIdle()
          val state = viewModel.uiState.value as HomeUiState.Success
          assertThat(state.recentProjects.size).isEqualTo(3)
      }

      @Test
      fun `loadDashboard Success state contains 3 stub recommendations`() = runTest {
          val viewModel = createViewModel()
          advanceUntilIdle()
          val state = viewModel.uiState.value as HomeUiState.Success
          assertThat(state.recommendations.size).isEqualTo(3)
      }

      @Test
      fun `dismissRecommendation removes item from uiState`() = runTest {
          // Arrange: dataStore.edit does nothing
          coEvery { mockDataStore.edit(any()) } returns mockPrefs
          val viewModel = createViewModel()
          advanceUntilIdle()
          val before = (viewModel.uiState.value as HomeUiState.Success).recommendations

          // Act
          viewModel.dismissRecommendation(before.first().id)
          advanceUntilIdle()

          // Assert
          val after = (viewModel.uiState.value as HomeUiState.Success).recommendations
          assertThat(after.size).isEqualTo(before.size - 1)
      }

      @Test
      fun `dismissRecommendation with pre-dismissed IDs in DataStore filters them out`() = runTest {
          // Arrange: DataStore returns "r1" as already dismissed
          val prefsWithDismissed: Preferences = mockk()
          every { prefsWithDismissed[any<Preferences.Key<String>>()] } returns "r1"
          every { mockDataStore.data } returns flowOf(prefsWithDismissed)

          val viewModel = createViewModel()
          advanceUntilIdle()

          val state = viewModel.uiState.value as HomeUiState.Success
          assertThat(state.recommendations.none { it.id == "r1" }).isEqualTo(true)
      }

      @Test
      fun `loadDashboard Error state when DataStore throws`() = runTest {
          every { mockDataStore.data } returns flow { throw RuntimeException("DataStore unavailable") }
          val viewModel = createViewModel()
          advanceUntilIdle()
          assertThat(viewModel.uiState.value).isInstanceOf(HomeUiState.Error::class)
      }

      @Test
      fun `loadDashboard Error is retryable`() = runTest {
          every { mockDataStore.data } returns flow { throw RuntimeException("Network error") }
          val viewModel = createViewModel()
          advanceUntilIdle()
          val error = viewModel.uiState.value as HomeUiState.Error
          assertThat(error.retryable).isEqualTo(true)
      }
  }
  ```

  Verify: `./gradlew :feature:feature-home:testDebugUnitTest` — all 7 tests pass.

---

- [ ] 11. **Verify Color.kt warning color mapping and fix HealthStatus.WARNING color**

  Read `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Color.kt` to confirm how `#FFCB6B` (warning amber) is mapped in the M3 color scheme. If it is mapped to `secondary` or a custom extension token (e.g. `warning`), update `HealthStatus.WARNING.dotColor()` and `HealthStatus.WARNING.valueColor()` in `ProjectCard.kt` and `HealthCell.kt` accordingly.

  File: `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Color.kt` — read, find mapping.

  If a `warning` extension exists (e.g. `MaterialTheme.colorScheme.warning` via a custom `ExtendedColors`), use it. Otherwise use `MaterialTheme.colorScheme.secondary` (the closest M3 mapping for `#FFCB6B`/`#89DDFF` → per the design-fidelity doc `secondary = #89DDFF`, `warning = #FFCB6B`).

  > **Decision:** Use `MaterialTheme.colorScheme.secondary` for WARNING until a dedicated `warning` extension is confirmed in the theme. If `Color.kt` defines a custom warning color extension, switch to that.

  Verify: `./gradlew :feature:feature-home:assembleDebug` — no compilation errors after adjustment.

---

- [ ] 12. **Final end-to-end verification**

  Run all three verification commands in order:

  ```bash
  # 1. Feature module debug build
  ./gradlew :feature:feature-home:assembleDebug

  # 2. Full project debug build (verifies NavGraph wiring and Hilt graph)
  ./gradlew assembleDebug

  # 3. Feature unit tests
  ./gradlew :feature:feature-home:testDebugUnitTest
  ```

  Expected outcomes:
  - Step 1: `BUILD SUCCESSFUL` — feature module compiles in isolation.
  - Step 2: `BUILD SUCCESSFUL` — full app builds with HomeScreen wired into NavGraph.
  - Step 3: `BUILD SUCCESSFUL`, all `HomeViewModelTest` tests pass (7 tests).

  After build passes:
  - Update `.kiro/implementation-status.md`: set DEVOS-057 to 🟢 Complete, note ACs passed.
  - Update Jira ticket DA-69 to Done.

---

## Acceptance Criteria checklist

| AC | Description | How verified |
|----|-------------|-------------|
| AC1 | Dashboard loads within 2 seconds | Stub data loads instantly; no async blocking in `init` |
| AC2 | All 6 sections render with correct data | `HomeDashboardContent` renders all 6 sections in order |
| AC3 | Recommendation dismiss persists across app restarts | DataStore `edit` called in `dismissRecommendation`; dismissed IDs filtered on `loadDashboard` |
| AC4 | Health grid shows 4 indicators with correct colors | `ProjectHealthGrid` renders 4 `HealthCell` items with `HealthStatus`-mapped colors |
| AC5 | Empty state shown for users with no projects | `HomeUiState.Empty` renders `DevOSEmptyState` with RocketLaunch icon |
| AC6 | Search bar tap navigates to SearchScreen | `onSearchTap` fires `HomeNavEvent.NavigateToSearch` → `NavController.navigate("search?q=")` |
| AC7 | Notification bell navigates to NotificationsScreen | `onNotificationsTap` fires `HomeNavEvent.NavigateToNotifications` → `NavController.navigate("notifications")` |
| AC8 | Dark mode: all colors from `MaterialTheme.colorScheme.*` | No hardcoded hex in any composable |
| AC9 | All interactive elements have contentDescription | `semantics { contentDescription = ... }` on every card, button, icon |

---

## Architecture decision record

**Decision 1 — No domain module for stub data.** The spec requires `GetDashboardUseCase` (DEVOS-058), but that ticket depends on this one. Stub data lives in `HomeViewModel` with `// TODO(DEVOS-058)` markers. The ViewModel's constructor injection of `DataStore<Preferences>` is already the real dependency; only the data-loading code is stubbed.

**Decision 2 — Column+Row for health grid, not LazyVerticalGrid.** Nested scrollable containers (LazyColumn containing LazyVerticalGrid) are not supported in Compose. The health grid always has exactly 4 cells, so a fixed 2×2 Column+Row layout is correct and avoids the restriction.

**Decision 3 — SearchBar non-editable overlay.** `DevOSSearchBar` wraps a `BasicTextField` which captures input focus. On the home screen the bar is decoration; tapping it should navigate to the full SearchScreen. A transparent `Box` overlay over the `DevOSSearchBar` intercepts the click before `BasicTextField` can steal focus.

**Decision 4 — `HomeModule` DataStore named `"home_prefs"`.** DataStore instances must be named uniquely per app; `"home_prefs"` is distinct from any other DataStore that might exist. If the app already defines a global DataStore, a `@Named("home")` qualifier will be needed — check `AppModule.kt` in step 5.

**Decision 5 — Warning color = `MaterialTheme.colorScheme.secondary`.** The design token doc maps `secondary = #89DDFF` (cyan/code) and `warning = #FFCB6B`, but the M3 `ColorScheme` interface has no `warning` slot. Until a custom `ExtendedColors` extension is confirmed in `Color.kt`, `secondary` is the fallback. Step 11 makes this a verified adjustment rather than a guess.
