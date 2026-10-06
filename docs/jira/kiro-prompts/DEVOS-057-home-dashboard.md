# Kiro Prompt — DEVOS-057: Home Dashboard

**Jira:** DEVOS-057 / DEVOS-058  
**Epic:** DEVOS-E10  
**Figma:** FIGMA-04  
**Kiro Spec:** `.kiro/specs/command-center/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the Home Dashboard — the primary landing screen and AI Developer Command Center.

**Existing files to read first:**
- `.kiro/specs/command-center/core.md`
- `docs/figma/screen-inventory.md` — Screen 04
- `docs/figma/component-inventory.md` — DevOSProjectCard, DevOSRepositoryCard, DevOSHealthIndicator

**Module:** `feature/feature-home/`  
**Package:** `com.devos.ai.feature.home`

**What to implement:**

### 1. UiState
```
HomeUiState:
  Loading
  Success(
    recentProjects: List<ProjectSummary>,
    recommendations: List<AIRecommendation>,
    projectHealth: ProjectHealth?,
    recentSessions: List<ChatSessionSummary>,
    recentRepositories: List<RepositorySummary>,
  )
  Empty  (new user, no projects)
  Error(message, retryable)
```

### 2. HomeViewModel
- Load all data via `GetDashboardUseCase` (parallel coroutines via `async`)
- `dismissRecommendation(id: String)` — persists dismissal
- Recommendations refresh every 15 minutes via Flow

### 3. HomeScreen Layout
```
Scaffold(topBar = DevOSTopBar("DevOS AI", actions = [NotificationBell, Profile])) {
  LazyColumn {
    // 1. Global AI search bar (sticky)
    stickyHeader { DevOSSearchBar("Ask DevOS anything...", onExpand = navToSearch) }

    // 2. Recent projects horizontal row
    item { SectionHeader("Recent Projects", action = { navToProjectList }) }
    item { LazyRow { items(projects) { DevOSProjectCard(it, onClick = navToProject) } } }

    // 3. AI Recommendations
    item { SectionHeader("AI Recommendations") }
    items(recommendations, key = { it.id }) { recommendation ->
      RecommendationCard(recommendation, onDismiss = viewModel::dismissRecommendation)
    }

    // 4. Project Health grid
    item { SectionHeader("Project Health") }
    item {
      LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        item { DevOSHealthIndicator(securityScore, "Security") }
        item { DevOSHealthIndicator(testScore, "Tests") }
        item { DevOSHealthIndicator(archScore, "Architecture") }
        item { DevOSHealthIndicator(depScore, "Dependencies") }
      }
    }

    // 5. Recent AI Sessions
    item { SectionHeader("Recent AI Sessions") }
    items(recentSessions) { ChatSessionItem(it, onClick = navToChat) }

    // 6. Recent Repositories
    item { SectionHeader("Recent Repositories") }
    items(recentRepos) { DevOSRepositoryCard(it, onClick = navToRepo) }
  }
}
```

### 4. RecommendationCard
```kotlin
@Composable
fun RecommendationCard(
    recommendation: AIRecommendation,
    onDismiss: (String) -> Unit,
    onClick: () -> Unit,
) {
    DevOSCard(onClick = onClick) {
        Row(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
            RecommendationIcon(recommendation.type)
            Spacer(Modifier.width(MaterialTheme.spacing.sm))
            Column(Modifier.weight(1f)) {
                DevOSStatusBadge(recommendation.type.label, recommendation.type.status)
                Text(recommendation.title, style = MaterialTheme.typography.titleSmall)
                Text(recommendation.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onDismiss(recommendation.id) }) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss recommendation")
            }
        }
    }
}
```

### 5. Empty State (New User)
```kotlin
DevOSEmptyState(
    icon = Icons.Outlined.RocketLaunch,
    title = "Welcome to DevOS AI",
    description = "Import your first repository to get started",
    action = { DevOSButton("Import Repository", onClick = navToImport) }
)
```

### 6. Navigation Events (SharedFlow)
- `NavigateToSearch`
- `NavigateToProject(projectId)`
- `NavigateToRepository(repoId)`
- `NavigateToChat(sessionId?)`
- `NavigateToNotifications`
- `NavigateToProfile`
- `NavigateToImport`

**Performance:**
- Use `derivedStateOf` for computed values
- Lazy list with `key =` on all items
- Health data cached — don't re-fetch on every recomposition

**Tests:**
- `HomeViewModelTest`: loads dashboard, dismisses recommendation, handles error
- `HomeScreenTest`: all sections render; recommendation dismiss X fires callback; empty state shows for new user

**Acceptance Criteria:**
- [ ] Dashboard loads within 2 seconds
- [ ] All 6 sections render with correct data
- [ ] Recommendation dismiss persists across app restarts
- [ ] Health grid shows 4 indicators with correct colors
- [ ] Empty state shown for users with no projects
- [ ] Search bar tap navigates to SearchScreen
- [ ] Notification bell navigates to NotificationsScreen
