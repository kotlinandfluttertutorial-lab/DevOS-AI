# Kiro Prompt — DEVOS-059: Command Center Screens (Notifications, Search, Profile)

**Jira:** DEVOS-059 / DEVOS-060 / DEVOS-061  
**Epic:** DEVOS-E10  
**Figma:** FIGMA-37 / FIGMA-38 / FIGMA-39  
**Kiro Spec:** `.kiro/specs/command-center/screens.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing three command center screens for DevOS AI: the notifications screen, the global search screen, and the profile screen.

**Existing files to read first:**
- `docs/figma/screen-inventory.md` — Screens FIGMA-37, FIGMA-38, FIGMA-39
- `docs/figma/component-inventory.md` — DevOSCard, DevOSSearchBar, DevOSTopBar
- `feature/feature-home/` — existing Home Dashboard for navigation patterns
- `core/core-security/` — SecureTokenRepository (for sign-out)

**Modules:**
- `feature/feature-settings/` — ProfileScreen
- `feature/feature-home/` — NotificationsScreen, SearchScreen (extend existing feature)
- `domain/domain-ai/` — notification + search domain models
- `core/core-database/` — NotificationEntity, SearchHistoryEntity

**Package:** `com.devos.ai.feature`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- Sign-out MUST clear all EncryptedSharedPreferences tokens and Room user data before navigating to Login.
- Search debounce: 300ms minimum before firing query.
- Recent searches persisted in Room — max 20 entries; LRU eviction.
- Notification tap must navigate to the source screen (deep link behavior via Routes).

**What to implement:**

### 1. NotificationsScreen (DEVOS-059, FIGMA-37)

Route: `NOTIFICATIONS`

Domain models:
```kotlin
data class DevOSNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val sourceRoute: String?,       // route to navigate on tap
    val isRead: Boolean,
    val createdAt: Instant,
)

enum class NotificationType { AI, PR, ISSUE, SECURITY, TEST, SYSTEM }
```

UiState:
```kotlin
sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Success(
        val notifications: List<DevOSNotification>,
        val activeFilter: NotificationType?,
        val unreadCount: Int,
    ) : NotificationsUiState
    data object Empty : NotificationsUiState
    data class Error(val message: String, val retryable: Boolean) : NotificationsUiState
}
```

Layout:
```kotlin
Scaffold(
    topBar = {
        DevOSTopBar(
            title = "Notifications",
            actions = {
                TextButton(onClick = onMarkAllRead) {
                    Text("Mark all read", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
) { padding ->
    Column(Modifier.padding(padding)) {
        // Filter chips
        LazyRow(Modifier.padding(horizontal = MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            item {
                FilterChip(selected = activeFilter == null,
                    onClick = { onFilterChange(null) }, label = { Text("All") })
            }
            items(NotificationType.entries) { type ->
                FilterChip(selected = activeFilter == type,
                    onClick = { onFilterChange(type) },
                    label = { Text(type.displayName) })
            }
        }
        when (val s = uiState) {
            is Loading -> DevOSLoadingState()
            is Empty   -> DevOSEmptyState(
                icon = Icons.Outlined.NotificationsNone,
                title = "All caught up",
                description = "No notifications right now",
            )
            is Error   -> DevOSErrorState(description = s.message,
                onRetry = if (s.retryable) viewModel::retry else null)
            is Success -> LazyColumn {
                items(s.notifications, key = { it.id }) { notification ->
                    val dismissState = rememberDismissState(
                        confirmStateChange = {
                            if (it == DismissValue.DismissedToEnd ||
                                it == DismissValue.DismissedToStart) {
                                onDismissNotification(notification.id)
                            }
                            true
                        }
                    )
                    SwipeToDismiss(
                        state = dismissState,
                        background = { DismissBackground(dismissState) },
                        dismissContent = {
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    onMarkRead(notification.id)
                                    notification.sourceRoute?.let { onNavigateTo(it) }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: DevOSNotification, onClick: () -> Unit) {
    DevOSCard(
        onClick = onClick,
        modifier = Modifier.background(
            if (!notification.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else Color.Transparent
        )
    ) {
        Row(Modifier.padding(MaterialTheme.spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            NotificationTypeIcon(notification.type)
            Column(Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(notification.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal)
                Text(notification.body, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Text(notification.createdAt.toRelativeTimeString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!notification.isRead) {
                Box(Modifier.size(8.dp).background(
                    MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}
```

### 2. SearchScreen (DEVOS-060, FIGMA-38)

Route: `SEARCH`

Domain models:
```kotlin
data class SearchResult(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val subtitle: String,
    val route: String,              // navigate here on tap
    val relevanceScore: Float,
)

enum class SearchResultType { CODE, ISSUE, PR, MEMORY, AI_SESSION, FILE, SYMBOL }

data class SearchScope(val id: String, val label: String)
val searchScopes = listOf(
    SearchScope("all", "All"),
    SearchScope("code", "Code"),
    SearchScope("issues", "Issues"),
    SearchScope("prs", "PRs"),
    SearchScope("memory", "Memory"),
)
```

UiState:
```kotlin
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Searching : SearchUiState
    data class Success(
        val query: String,
        val scope: String,
        val results: Map<SearchResultType, List<SearchResult>>,
        val aiResults: List<SearchResult>,
    ) : SearchUiState
    data object Empty : SearchUiState
    data class Error(val message: String, val retryable: Boolean) : SearchUiState
}
```

Layout:
```kotlin
Scaffold { padding ->
    Column(Modifier.padding(padding)) {
        // Always-focused search bar on entry
        DevOSSearchBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            placeholder = "Search everything…",
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.base),
            autoFocus = true,
        )
        // Scope selector
        LazyRow(Modifier.padding(horizontal = MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            items(searchScopes) { scope ->
                FilterChip(selected = activeScope == scope.id,
                    onClick = { onScopeChange(scope.id) },
                    label = { Text(scope.label) })
            }
        }
        when (val s = uiState) {
            is Idle -> RecentSearchesSection(recentSearches, onSelectRecent = { onQueryChange(it) })
            is Searching -> DevOSLoadingState()
            is Empty -> DevOSEmptyState(icon = Icons.Outlined.SearchOff,
                title = "No results", description = "Try different keywords or scope")
            is Error -> DevOSErrorState(description = s.message,
                onRetry = if (s.retryable) viewModel::retry else null)
            is Success -> LazyColumn {
                // AI semantic results section
                if (s.aiResults.isNotEmpty()) {
                    item { SectionHeader("AI Results") }
                    items(s.aiResults) { result ->
                        SearchResultItem(result, onClick = { onNavigateTo(result.route) })
                    }
                }
                // Grouped results by type
                s.results.forEach { (type, results) ->
                    if (results.isNotEmpty()) {
                        item { SectionHeader(type.displayName) }
                        items(results) { result ->
                            SearchResultItem(result, onClick = { onNavigateTo(result.route) })
                        }
                    }
                }
            }
        }
    }
}
```

Recent searches: stored in Room `SearchHistoryEntity` (max 20, LRU eviction). Debounced 300ms.

### 3. ProfileScreen (DEVOS-061, FIGMA-39)

Route: `PROFILE`

UiState:
```kotlin
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val displayName: String,
        val email: String,
        val avatarUrl: String?,
        val connectedAccounts: List<ConnectedAccount>,
        val usageStats: UsageStats,
        val plan: DevOSPlan,
    ) : ProfileUiState
    data object Empty : ProfileUiState
    data class Error(val message: String, val retryable: Boolean) : ProfileUiState
}

data class ConnectedAccount(
    val provider: OAuthProvider,
    val username: String,
    val isConnected: Boolean,
)

data class UsageStats(
    val totalAISessions: Int,
    val totalMessagesThisMonth: Int,
    val repositoriesIndexed: Int,
    val lessonsCompleted: Int,
)

enum class DevOSPlan { FREE, PRO, TEAM }
```

Layout:
```kotlin
Scaffold(topBar = { DevOSTopBar(title = "Profile") }) { padding ->
    LazyColumn(Modifier.padding(padding)) {
        when (val s = uiState) {
            is Loading -> item { DevOSLoadingState() }
            is Error   -> item { DevOSErrorState(description = s.message, onRetry = viewModel::retry) }
            is Success -> {
                item {
                    // Avatar + name + email
                    Column(Modifier.padding(MaterialTheme.spacing.base)
                        .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = s.avatarUrl, contentDescription = "Profile avatar",
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            placeholder = painterResource(R.drawable.ic_default_avatar))
                        Spacer(Modifier.height(MaterialTheme.spacing.sm))
                        Text(s.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(s.email, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        DevOSStatusBadge(s.plan.name, MaterialTheme.colorScheme.primary)
                    }
                }
                item {
                    SectionHeader("Connected Accounts")
                    s.connectedAccounts.forEach { account ->
                        ConnectedAccountItem(account)
                    }
                }
                item {
                    SectionHeader("Usage This Month")
                    UsageStatsCard(s.usageStats)
                }
                item {
                    Spacer(Modifier.height(MaterialTheme.spacing.lg))
                    DevOSButton(
                        text = "Sign Out",
                        onClick = {
                            // Show confirmation dialog before sign-out
                            showSignOutDialog = true
                        },
                        style = DevOSButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.base),
                    )
                }
            }
            else -> {}
        }
    }
}
```

Sign-out flow:
1. Show `AlertDialog` "Sign out of DevOS AI? You will need to sign in again."
2. On confirm: `SignOutUseCase` — clears all tokens from EncryptedSharedPreferences, clears user Room data
3. Navigate to `SPLASH` (not `LOGIN` directly — SplashViewModel re-evaluates auth state)

**Tests:**
- `NotificationsViewModelTest`: notifications load; filter changes list; swipe-dismiss removes; mark-all-read updates all
- `NotificationsScreenTest`: filter chips render; swipe-to-dismiss works; tap navigates to source route; unread dot visible
- `SearchViewModelTest`: debounce fires after 300ms; scope change re-runs search; recent searches saved; empty state on no results
- `SearchScreenTest`: search bar auto-focused; recent searches shown on idle; results grouped by type; AI results section shown
- `ProfileViewModelTest`: profile loads; connected accounts shown; sign-out clears tokens
- `ProfileScreenTest`: avatar rendered; usage stats shown; sign-out shows confirmation dialog; confirmed sign-out navigates to splash

**Acceptance Criteria:**
- AC1 (DEVOS-059): Filter chips (All/AI/PRs/Issues/Security/Tests) render and narrow notification list
- AC2 (DEVOS-059): Unread notifications shown with distinct background and unread dot
- AC3 (DEVOS-059): Swipe-to-dismiss removes notification from list
- AC4 (DEVOS-059): Tap navigates to source screen via notification's `sourceRoute`
- AC5 (DEVOS-059): "Mark all read" clears all unread indicators
- AC6 (DEVOS-060): Search bar auto-focused on screen entry
- AC7 (DEVOS-060): Scope selector (All/Code/Issues/PRs/Memory) filters results
- AC8 (DEVOS-060): Recent searches shown when query is empty; tap pre-fills search bar
- AC9 (DEVOS-060): Results grouped by type with section headers
- AC10 (DEVOS-060): AI semantic results section shown separately from keyword results
- AC11 (DEVOS-060): Empty state on no results; error state on search failure
- AC12 (DEVOS-061): Avatar, display name, email, and plan badge rendered
- AC13 (DEVOS-061): Connected GitHub/GitLab accounts shown with username
- AC14 (DEVOS-061): Usage stats (sessions, messages, repos, lessons) displayed
- AC15 (DEVOS-061): Sign-out shows confirmation dialog; confirmed sign-out clears all tokens and navigates to splash
