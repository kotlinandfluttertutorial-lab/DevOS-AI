# DevOS AI — Figma → Jira → Kiro Mapping

**Version:** 1.0  
**Date:** 2026-10-07

---

## Full Mapping Table

| Figma Ref | Screen Name | Jira Ticket | Jira Summary | Kiro Spec Path | Compose Screen | ViewModel | AI-SDLC Phase |
|-----------|-------------|-------------|-------------|----------------|----------------|-----------|---------------|
| FIGMA-01 | Splash | DEVOS-101 / DEVOS-009 | Splash screen | `.kiro/specs/android-ui/splash.md` | `SplashScreen.kt` | `SplashViewModel.kt` | IMPLEMENT |
| FIGMA-02 | Onboarding | DEVOS-102 / DEVOS-010 | Onboarding carousel | `.kiro/specs/android-ui/onboarding.md` | `OnboardingScreen.kt` | `OnboardingViewModel.kt` | IMPLEMENT |
| FIGMA-03 | Login | DEVOS-103 / DEVOS-011 | Login OAuth | `.kiro/specs/foundation/auth.md` | `LoginScreen.kt` | `AuthViewModel.kt` | IMPLEMENT |
| FIGMA-04 | Home Dashboard | DEVOS-104 / DEVOS-057 | Home Dashboard | `.kiro/specs/command-center/home.md` | `HomeScreen.kt` | `HomeViewModel.kt` | IMPLEMENT |
| FIGMA-05 | Project List | DEVOS-105 / DEVOS-017 | Project list | `.kiro/specs/repository-intelligence/list.md` | `ProjectListScreen.kt` | `ProjectListViewModel.kt` | IMPLEMENT |
| FIGMA-06 | Project Overview | DEVOS-106 / DEVOS-016 | Project overview tabs | `.kiro/specs/repository-intelligence/overview.md` | `ProjectOverviewScreen.kt` | `ProjectViewModel.kt` | IMPLEMENT |
| FIGMA-07 | Repository Import | DEVOS-107 / DEVOS-013 | Repository import | `.kiro/specs/repository-intelligence/import.md` | `RepositoryImportScreen.kt` | `ImportViewModel.kt` | IMPLEMENT |
| FIGMA-08 | Repository Sync | DEVOS-108 / DEVOS-014 | Repository sync progress | `.kiro/specs/repository-intelligence/sync.md` | `RepositorySyncScreen.kt` | `SyncViewModel.kt` | IMPLEMENT |
| FIGMA-09 | Repository Overview | DEVOS-109 / DEVOS-016 | Repository overview | `.kiro/specs/repository-intelligence/overview.md` | `RepositoryOverviewScreen.kt` | `RepositoryViewModel.kt` | IMPLEMENT |
| FIGMA-10 | File Explorer | DEVOS-110 / DEVOS-018 | File tree browser | `.kiro/specs/code-intelligence/file-explorer.md` | `FileExplorerScreen.kt` | `FileExplorerViewModel.kt` | IMPLEMENT |
| FIGMA-11 | Code Viewer | DEVOS-111 / DEVOS-019 | Code viewer with syntax | `.kiro/specs/code-intelligence/code-viewer.md` | `CodeViewerScreen.kt` | `CodeViewerViewModel.kt` | IMPLEMENT |
| FIGMA-12 | Code Search | DEVOS-112 / DEVOS-021 | Code search | `.kiro/specs/code-intelligence/search.md` | `CodeSearchScreen.kt` | `CodeSearchViewModel.kt` | IMPLEMENT |
| FIGMA-13 | Symbol Details | DEVOS-113 / DEVOS-022 | Symbol details | `.kiro/specs/code-intelligence/symbols.md` | `SymbolDetailsScreen.kt` | `SymbolViewModel.kt` | IMPLEMENT |
| FIGMA-14 | Dependency Graph | DEVOS-114 / DEVOS-024 | Dependency graph | `.kiro/specs/code-intelligence/graph.md` | `DependencyGraphScreen.kt` | `GraphViewModel.kt` | IMPLEMENT |
| FIGMA-15 | Architecture | DEVOS-115 / DEVOS-025 | Architecture overview | `.kiro/specs/code-intelligence/architecture.md` | `ArchitectureScreen.kt` | `ArchitectureViewModel.kt` | IMPLEMENT |
| FIGMA-16 | AI Chat | DEVOS-116 / DEVOS-026 | AI Chat primary | `.kiro/specs/ai-platform/chat.md` | `AIChatScreen.kt` | `AIChatViewModel.kt` | IMPLEMENT |
| FIGMA-17 | AI Answer Detail | DEVOS-117 / DEVOS-029 | AI answer evidence | `.kiro/specs/ai-platform/answers.md` | `AIAnswerDetailScreen.kt` | `AnswerViewModel.kt` | IMPLEMENT |
| FIGMA-18 | AI Source Evidence | DEVOS-118 / DEVOS-030 | Source evidence | `.kiro/specs/ai-platform/answers.md` | `AISourceEvidenceScreen.kt` | `EvidenceViewModel.kt` | IMPLEMENT |
| FIGMA-19 | Agent Run | DEVOS-119 / DEVOS-035 | Agent execution view | `.kiro/specs/agents-mcp/agent-run.md` | `AgentRunScreen.kt` | `AgentRunViewModel.kt` | IMPLEMENT |
| FIGMA-20 | Agent Tool Exec | DEVOS-120 / DEVOS-036 | Tool execution detail | `.kiro/specs/agents-mcp/tool-exec.md` | `AgentToolExecutionScreen.kt` | `ToolExecViewModel.kt` | IMPLEMENT |
| FIGMA-21 | Git History | DEVOS-121 / DEVOS-040 | Git history | `.kiro/specs/developer-intelligence/git.md` | `GitHistoryScreen.kt` | `GitHistoryViewModel.kt` | IMPLEMENT |
| FIGMA-22 | Issues | DEVOS-122 / DEVOS-042 | Issue list | `.kiro/specs/developer-intelligence/issues.md` | `IssueListScreen.kt` | `IssueListViewModel.kt` | IMPLEMENT |
| FIGMA-23 | Issue Detail | DEVOS-123 / DEVOS-043 | Issue detail | `.kiro/specs/developer-intelligence/issues.md` | `IssueDetailScreen.kt` | `IssueDetailViewModel.kt` | IMPLEMENT |
| FIGMA-24 | Pull Requests | DEVOS-124 / DEVOS-044 | PR list | `.kiro/specs/developer-intelligence/prs.md` | `PullRequestListScreen.kt` | `PRListViewModel.kt` | IMPLEMENT |
| FIGMA-25 | PR AI Review | DEVOS-125 / DEVOS-045 | PR AI review | `.kiro/specs/developer-intelligence/prs.md` | `PRReviewScreen.kt` | `PRReviewViewModel.kt` | IMPLEMENT |
| FIGMA-26 | Security | DEVOS-126 / DEVOS-046 | Security findings | `.kiro/specs/security-intelligence/findings.md` | `SecurityFindingsScreen.kt` | `SecurityViewModel.kt` | IMPLEMENT |
| FIGMA-27 | Test Intelligence | DEVOS-127 / DEVOS-048 | Test intelligence | `.kiro/specs/testing-intelligence/dashboard.md` | `TestIntelligenceScreen.kt` | `TestViewModel.kt` | IMPLEMENT |
| FIGMA-28 | Learning Dashboard | DEVOS-128 / DEVOS-050 | Learning dashboard | `.kiro/specs/learning/dashboard.md` | `LearningDashboardScreen.kt` | `LearningViewModel.kt` | IMPLEMENT |
| FIGMA-29 | Course Details | DEVOS-129 / DEVOS-051 | Course details | `.kiro/specs/learning/course.md` | `CourseDetailsScreen.kt` | `CourseViewModel.kt` | IMPLEMENT |
| FIGMA-30 | Lesson | DEVOS-130 / DEVOS-052 | Lesson screen | `.kiro/specs/learning/lesson.md` | `LessonScreen.kt` | `LessonViewModel.kt` | IMPLEMENT |
| FIGMA-31 | Quiz | DEVOS-131 / DEVOS-053 | Quiz screen | `.kiro/specs/learning/quiz.md` | `QuizScreen.kt` | `QuizViewModel.kt` | IMPLEMENT |
| FIGMA-32 | Developer Memory | DEVOS-132 / DEVOS-055 | Developer memory | `.kiro/specs/developer-memory/screen.md` | `DeveloperMemoryScreen.kt` | `MemoryViewModel.kt` | IMPLEMENT |
| FIGMA-33 | MCP Tools | DEVOS-133 / DEVOS-039 | MCP tools | `.kiro/specs/agents-mcp/mcp.md` | `MCPToolsScreen.kt` | `MCPViewModel.kt` | IMPLEMENT |
| FIGMA-34 | AI Settings | DEVOS-134 / DEVOS-033 | AI settings | `.kiro/specs/ai-platform/settings.md` | `AISettingsScreen.kt` | `AISettingsViewModel.kt` | IMPLEMENT |
| FIGMA-35 | Provider Settings | DEVOS-135 / DEVOS-034 | Provider settings | `.kiro/specs/ai-platform/settings.md` | `ProviderSettingsScreen.kt` | `ProviderViewModel.kt` | IMPLEMENT |
| FIGMA-36 | Project Settings | DEVOS-136 / DEVOS-063 | Project settings | `.kiro/specs/android-ui/settings.md` | `ProjectSettingsScreen.kt` | `ProjectSettingsViewModel.kt` | IMPLEMENT |
| FIGMA-37 | Notifications | DEVOS-137 / DEVOS-059 | Notifications | `.kiro/specs/command-center/notifications.md` | `NotificationsScreen.kt` | `NotificationViewModel.kt` | IMPLEMENT |
| FIGMA-38 | Profile | DEVOS-138 / DEVOS-061 | Profile | `.kiro/specs/command-center/profile.md` | `ProfileScreen.kt` | `ProfileViewModel.kt` | IMPLEMENT |
| FIGMA-39 | Search | DEVOS-139 / DEVOS-060 | Global search | `.kiro/specs/command-center/search.md` | `SearchScreen.kt` | `SearchViewModel.kt` | IMPLEMENT |
| FIGMA-40 | Settings | DEVOS-140 / DEVOS-062 | Settings root | `.kiro/specs/android-ui/settings.md` | `SettingsScreen.kt` | `SettingsViewModel.kt` | IMPLEMENT |

---

## Design System Component → Figma → Code Mapping

| Design Token / Component | Figma Usage | Kotlin File | Package |
|--------------------------|-------------|-------------|---------|
| `DevOSDarkColorScheme` | All dark screens | `Color.kt` | `designsystem.theme` |
| `DevOSLightColorScheme` | All light screens | `Color.kt` | `designsystem.theme` |
| `DevOSTypography` | All text | `Typography.kt` | `designsystem.theme` |
| `DevOSShapes` | All rounded elements | `Shape.kt` | `designsystem.theme` |
| `DevOSSpacing` | All padding/margin | `Spacing.kt` | `designsystem.theme` |
| `DevOSButton` | All CTAs | `DevOSButton.kt` | `designsystem.components` |
| `DevOSCard` | All cards | `DevOSCard.kt` | `designsystem.components` |
| `DevOSTopBar` | All screens | `DevOSTopBar.kt` | `designsystem.components` |
| `DevOSBottomBar` | Primary nav | `DevOSBottomBar.kt` | `designsystem.components` |
| `DevOSSearchBar` | Home, Search | `DevOSSearchBar.kt` | `designsystem.components` |
| `DevOSCodeBlock` | Code Viewer, Chat, Lesson | `DevOSCodeBlock.kt` | `designsystem.components` |
| `DevOSAIMessage` | AI Chat | `DevOSAIMessage.kt` | `designsystem.components` |
| `DevOSAgentStep` | Agent Run | `DevOSAgentStep.kt` | `designsystem.components` |
| `DevOSStatusBadge` | Security, Tests, Issues | `DevOSStatusBadge.kt` | `designsystem.components` |
| `DevOSHealthIndicator` | Home, Project, Repo | `DevOSHealthIndicator.kt` | `designsystem.components` |
| `DevOSLoadingState` | All screens | `DevOSLoadingState.kt` | `designsystem.components` |
| `DevOSEmptyState` | All empty states | `DevOSEmptyState.kt` | `designsystem.components` |
| `DevOSErrorState` | All error states | `DevOSErrorState.kt` | `designsystem.components` |
| `DevOSMarkdownText` | AI Chat, Issues, Lessons | `DevOSMarkdownText.kt` | `designsystem.components` |
| `DevOSChatInput` | AI Chat | `DevOSChatInput.kt` | `designsystem.components` |
