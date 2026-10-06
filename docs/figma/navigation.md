# DevOS AI — Navigation Specification

**Version:** 1.0  
**Date:** 2026-10-07  
**Platform:** Android (Jetpack Compose + Navigation 3)

---

## 1. Navigation Architecture

DevOS AI uses a **three-tier navigation hierarchy**:

```
Tier 1: Bottom Navigation (always visible)
    Home | Projects | AI | Learn | More

Tier 2: Screen-level tabs (context-dependent)
    e.g., inside Repository: Overview | Files | Search | Symbols | Graph | RAG | Git | AI

Tier 3: Detail screens (pushed on back stack)
    e.g., File → Code Viewer → Symbol Details
```

---

## 2. Primary Navigation — Bottom Bar

| Tab | Icon | Destination |
|-----|------|-------------|
| Home | `home` | `HomeScreen` |
| Projects | `folder_open` | `ProjectListScreen` |
| AI | `auto_awesome` | `AIChatScreen` |
| Learn | `school` | `LearningDashboardScreen` |
| More | `menu` | `MoreDrawerScreen` |

### Behavior
- State saved per tab (back stack per root)
- Deep links supported to any primary destination
- "More" opens a modal bottom sheet with secondary destinations
- AI tab badge shows active agent count

---

## 3. Secondary Navigation — More Sheet

Destinations accessible from the More sheet:

| Label | Icon | Route |
|-------|------|-------|
| Search | `search` | `SearchScreen` |
| Git | `account_tree` | `GitScreen` |
| Agents | `smart_toy` | `AgentsScreen` |
| MCP Tools | `build` | `MCPToolsScreen` |
| Memory | `psychology` | `DeveloperMemoryScreen` |
| Notifications | `notifications` | `NotificationsScreen` |
| Settings | `settings` | `SettingsScreen` |
| Profile | `account_circle` | `ProfileScreen` |

---

## 4. Project Navigation (Inside a Project)

```
ProjectOverviewScreen
├── [Tab: Overview]   → ProjectOverviewTab
├── [Tab: Repos]      → RepositoryListTab
├── [Tab: Issues]     → IssueListScreen
├── [Tab: PRs]        → PullRequestListScreen
├── [Tab: Arch]       → ArchitectureOverviewScreen
├── [Tab: Security]   → SecurityFindingsScreen
└── [Tab: Tests]      → TestIntelligenceScreen
```

Quick Actions from Project:
- `Ask AI` → `AIChatScreen` (pre-loaded project context)
- `Explore Code` → `FileExplorerScreen`
- `Architecture` → `ArchitectureOverviewScreen`

---

## 5. Repository Navigation (Inside a Repository)

```
RepositoryOverviewScreen
├── [Tab: Overview]  → RepositoryOverviewTab
├── [Tab: Files]     → FileExplorerScreen
├── [Tab: Search]    → CodeSearchScreen
├── [Tab: Symbols]   → SymbolDetailsScreen (list)
├── [Tab: Graph]     → DependencyGraphScreen
├── [Tab: RAG]       → RAGScreen
├── [Tab: Git]       → GitHistoryScreen
└── [Tab: AI]        → AIChatScreen (repo context)
```

---

## 6. AI Chat Navigation

```
AIChatScreen
├── Context Selector (bottom sheet)
│   ├── Global workspace
│   ├── Current project
│   ├── Current repository
│   ├── Current file
│   └── Current symbol
├── [Tap answer] → AIAnswerDetailScreen
│   ├── [Tap source] → AISourceEvidenceScreen
│   └── [Tap file ref] → CodeViewerScreen
└── [Tap agent execution] → AgentRunScreen
    └── [Tap tool step] → AgentToolExecutionScreen
```

---

## 7. Learning Navigation

```
LearningDashboardScreen
├── [Tap course]  → CourseDetailsScreen
│   └── [Tap lesson] → LessonScreen
│       └── [End of lesson] → QuizScreen
└── [Tap topic]   → CourseDetailsScreen
```

---

## 8. Settings Navigation

```
SettingsScreen
├── AISettingsScreen
│   └── ProviderSettingsScreen
├── ProjectSettingsScreen
├── ProfileScreen
└── NotificationsScreen
```

---

## 9. Complete Route Map

```kotlin
// NavGraph routes
object DevOSRoutes {
    // Primary
    const val HOME                = "home"
    const val PROJECT_LIST        = "project_list"
    const val AI_CHAT             = "ai_chat"
    const val LEARNING_DASHBOARD  = "learning_dashboard"
    const val MORE                = "more"

    // Projects
    const val PROJECT_OVERVIEW    = "project/{projectId}"
    const val REPOSITORY_IMPORT   = "repository/import"
    const val REPOSITORY_SYNC     = "repository/{repoId}/sync"

    // Repository
    const val REPOSITORY_OVERVIEW = "repository/{repoId}"
    const val FILE_EXPLORER       = "repository/{repoId}/files?path={path}"
    const val CODE_VIEWER         = "repository/{repoId}/file?path={path}&line={line}"
    const val CODE_SEARCH         = "repository/{repoId}/search?q={query}"
    const val SYMBOL_DETAILS      = "repository/{repoId}/symbol/{symbolId}"
    const val DEPENDENCY_GRAPH    = "repository/{repoId}/graph"
    const val ARCHITECTURE        = "repository/{repoId}/architecture"

    // AI
    const val AI_ANSWER_DETAIL    = "ai/answer/{answerId}"
    const val AI_SOURCE_EVIDENCE  = "ai/evidence/{answerId}"
    const val AGENT_RUN           = "agent/run/{runId}"
    const val AGENT_TOOL_EXEC     = "agent/run/{runId}/tool/{toolId}"

    // Git
    const val GIT_HISTORY         = "repository/{repoId}/git"

    // Issues / PRs
    const val ISSUE_LIST          = "project/{projectId}/issues"
    const val ISSUE_DETAIL        = "project/{projectId}/issues/{issueId}"
    const val PR_LIST             = "project/{projectId}/prs"
    const val PR_AI_REVIEW        = "project/{projectId}/prs/{prId}/review"

    // Quality
    const val SECURITY_FINDINGS   = "project/{projectId}/security"
    const val TEST_INTELLIGENCE   = "project/{projectId}/tests"

    // Learning
    const val COURSE_DETAILS      = "learn/course/{courseId}"
    const val LESSON              = "learn/course/{courseId}/lesson/{lessonId}"
    const val QUIZ                = "learn/course/{courseId}/quiz/{quizId}"

    // Developer
    const val DEVELOPER_MEMORY    = "memory"
    const val MCP_TOOLS           = "mcp"
    const val NOTIFICATIONS       = "notifications"
    const val PROFILE             = "profile"
    const val SEARCH              = "search?q={query}"

    // Settings
    const val SETTINGS            = "settings"
    const val AI_SETTINGS         = "settings/ai"
    const val PROVIDER_SETTINGS   = "settings/ai/providers"
    const val PROJECT_SETTINGS    = "settings/project/{projectId}"

    // Onboarding
    const val SPLASH              = "splash"
    const val ONBOARDING          = "onboarding"
    const val LOGIN               = "login"
}
```

---

## 10. Back Stack Strategy

| Navigation Action | Back Behavior |
|------------------|---------------|
| Bottom tab switch | Restore tab back stack |
| Push detail screen | Pop to parent |
| Modal sheet | Dismiss sheet |
| Deep link into repo | Back to project list |
| AI context switch | Preserve chat history |

---

## 11. Tablet Adaptive Navigation

On tablets (≥600dp):

```
NavigationRail (left) + Content (right)
├── Narrow tablet (<840dp): 2-pane (list + detail)
└── Large tablet (≥840dp): 3-pane (nav + list + detail)

Example (Repository, large tablet):
┌──────┬─────────────────┬──────────────────┐
│ Rail │  File Explorer  │   Code Viewer    │
│      │                 │   + AI Panel     │
└──────┴─────────────────┴──────────────────┘
```

---

## 12. Deep Link Scheme

```
devos://home
devos://project/{projectId}
devos://repo/{repoId}
devos://repo/{repoId}/file?path={path}&line={line}
devos://ai/chat?context=repo&repoId={repoId}
devos://learn/course/{courseId}
devos://notification/{notificationId}
```
