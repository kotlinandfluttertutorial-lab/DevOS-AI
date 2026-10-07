# DevOS AI — Jira Traceability Matrix

**Version:** 1.0  
**Date:** 2026-10-07  
**Format:** Requirement → Jira → Figma → Kiro Spec → Implementation → Tests → Acceptance Criteria

---

## Epic Traceability

| Epic | ID | Jira Tickets | Figma Screens | Kiro Specs | Status |
|------|----|-------------|---------------|------------|--------|
| Foundation | DEVOS-E01 | 001–012, 101–103 | FIGMA-01,02,03 | foundation/* | 🔵 Planned |
| Repository Intelligence | DEVOS-E02 | 013–017, 105–109 | FIGMA-05,06,07,08,09 | repository-intelligence/* | 🔵 Planned |
| Code Intelligence | DEVOS-E03 | 018–025, 110–115 | FIGMA-10,11,12,13,14,15 | code-intelligence/* | 🔵 Planned |
| AI Platform | DEVOS-E04 | 026–034, 116–118, 134–135 | FIGMA-16,17,18,34,35 | ai-platform/* | 🔵 Planned |
| Agents & MCP | DEVOS-E05 | 035–039, 119–120, 133 | FIGMA-19,20,33 | agents-mcp/* | 🔵 Planned |
| Developer Intelligence | DEVOS-E06 | 040–045, 121–125 | FIGMA-21,22,23,24,25 | developer-intelligence/* | 🔵 Planned |
| Quality Intelligence | DEVOS-E07 | 046–049, 126–127 | FIGMA-26,27 | quality-intelligence/* | 🔵 Planned |
| Learning | DEVOS-E08 | 050–054, 128–131 | FIGMA-28,29,30,31 | learning/* | 🔵 Planned |
| Developer Memory | DEVOS-E09 | 055–056, 132 | FIGMA-32 | developer-memory/* | 🔵 Planned |
| Developer Command Center | DEVOS-E10 | 057–060, 104, 137–139 | FIGMA-04,37,38,39 | command-center/* | 🔵 Planned |
| Android UI/UX | DEVOS-E11 | 061–066, 101–140 | All 40 | android-ui/* | 🔵 Planned |
| AI-SDLC & Platform | DEVOS-E12 | 067–070 | — | — | 🔵 Planned |

---

## Full Traceability Table

| Jira | Epic | Summary | Figma | Kiro Spec | Android Component | ViewModel | UseCase | Repository | Tests |
|------|------|---------|-------|-----------|-------------------|-----------|---------|------------|-------|
| DEVOS-001 | E01 | Android project setup | — | foundation/android-setup | — | — | — | — | BuildTest |
| DEVOS-002 | E01 | Color tokens | — | foundation/design-system | `Color.kt` | — | — | — | ColorContrastTest |
| DEVOS-003 | E01 | Typography | — | foundation/design-system | `Typography.kt` | — | — | — | TypographyTest |
| DEVOS-004 | E01 | Shape/spacing | — | foundation/design-system | `Shape.kt`, `Spacing.kt` | — | — | — | ComponentTest |
| DEVOS-005 | E01 | Core component library | — | foundation/design-system | `DevOSButton`, `DevOSCard`, etc. | — | — | — | ComponentTests |
| DEVOS-006 | E01 | Code rendering | — | foundation/design-system | `DevOSCodeBlock` | — | — | — | CodeBlockTest |
| DEVOS-007 | E01 | Bottom navigation | — | foundation/navigation | `DevOSBottomBar` | `NavViewModel` | — | — | NavTest |
| DEVOS-008 | E01 | Navigation graph | — | foundation/navigation | `DevOSNavGraph` | — | — | — | NavGraphTest |
| DEVOS-009 | E01 | Splash screen | FIGMA-01 | android-ui/splash | `SplashScreen` | `SplashViewModel` | — | — | SplashTest |
| DEVOS-010 | E01 | Onboarding | FIGMA-02 | android-ui/onboarding | `OnboardingScreen` | `OnboardingViewModel` | — | — | OnboardingTest |
| DEVOS-011 | E01 | Login | FIGMA-03 | foundation/auth | `LoginScreen` | `AuthViewModel` | `LoginUseCase` | `AuthRepository` | AuthTest |
| DEVOS-012 | E01 | Secure token storage | — | foundation/auth | — | — | — | `TokenRepository` | TokenStorageTest |
| DEVOS-013 | E02 | Repository import | FIGMA-07 | repo-intel/import | `RepositoryImportScreen` | `ImportViewModel` | `ImportRepositoryUseCase` | `RepositoryRepository` | ImportTest |
| DEVOS-014 | E02 | Repository sync | FIGMA-08 | repo-intel/sync | `RepositorySyncScreen` | `SyncViewModel` | `SyncRepositoryUseCase` | `SyncRepository` | SyncTest |
| DEVOS-015 | E02 | Clone/indexing service | — | repo-intel/core | — | — | `IndexRepositoryUseCase` | `GitRepository` | IndexingTest |
| DEVOS-016 | E02 | Repository overview | FIGMA-09 | repo-intel/overview | `RepositoryOverviewScreen` | `RepositoryViewModel` | `GetRepositoryUseCase` | `RepositoryRepository` | RepositoryOverviewTest |
| DEVOS-017 | E02 | Repository list | FIGMA-05 | repo-intel/list | `ProjectListScreen` | `ProjectListViewModel` | `GetProjectsUseCase` | `ProjectRepository` | ProjectListTest |
| DEVOS-018 | E03 | File explorer | FIGMA-10 | code-intel/file-explorer | `FileExplorerScreen` | `FileExplorerViewModel` | `GetFilesUseCase` | `FileRepository` | FileExplorerTest |
| DEVOS-019 | E03 | Code viewer syntax | FIGMA-11 | code-intel/code-viewer | `CodeViewerScreen` | `CodeViewerViewModel` | `GetFileContentUseCase` | `CodeRepository` | CodeViewerTest |
| DEVOS-020 | E03 | Code viewer AI bar | FIGMA-11 | code-intel/code-viewer | `CodeViewerScreen` | `CodeViewerViewModel` | `AskAIUseCase` | `AIRepository` | AIActionTest |
| DEVOS-021 | E03 | Code search | FIGMA-12 | code-intel/search | `CodeSearchScreen` | `CodeSearchViewModel` | `SearchCodeUseCase` | `SearchRepository` | CodeSearchTest |
| DEVOS-022 | E03 | Symbol details | FIGMA-13 | code-intel/symbols | `SymbolDetailsScreen` | `SymbolViewModel` | `GetSymbolUseCase` | `SymbolRepository` | SymbolTest |
| DEVOS-023 | E03 | Symbol indexing | — | code-intel/indexer | — | — | `IndexSymbolsUseCase` | `SymbolRepository` | SymbolIndexTest |
| DEVOS-024 | E03 | Dependency graph | FIGMA-14 | code-intel/graph | `DependencyGraphScreen` | `GraphViewModel` | `GetDependencyGraphUseCase` | `GraphRepository` | GraphTest |
| DEVOS-025 | E03 | Architecture overview | FIGMA-15 | code-intel/architecture | `ArchitectureScreen` | `ArchitectureViewModel` | `AnalyzeArchitectureUseCase` | `ArchitectureRepository` | ArchitectureTest |
| DEVOS-026 | E04 | AI Chat core | FIGMA-16 | ai-platform/chat | `AIChatScreen` | `AIChatViewModel` | `SendMessageUseCase` | `AIRepository` | AIChatTest |
| DEVOS-027 | E04 | AI context selector | FIGMA-16 | ai-platform/chat | `DevOSContextSelector` | `AIChatViewModel` | `SetContextUseCase` | `AIRepository` | ContextTest |
| DEVOS-028 | E04 | AI suggested actions | FIGMA-16 | ai-platform/chat | `DevOSActionChip` | `AIChatViewModel` | — | — | ActionChipTest |
| DEVOS-029 | E04 | AI answer detail | FIGMA-17 | ai-platform/answers | `AIAnswerDetailScreen` | `AnswerViewModel` | `GetAnswerDetailUseCase` | `AIRepository` | AnswerDetailTest |
| DEVOS-030 | E04 | AI source evidence | FIGMA-18 | ai-platform/answers | `AISourceEvidenceScreen` | `EvidenceViewModel` | `GetEvidenceUseCase` | `AIRepository` | EvidenceTest |
| DEVOS-031 | E04 | RAG pipeline | — | ai-platform/rag | — | — | `RetrieveChunksUseCase` | `RAGRepository` | RAGTest |
| DEVOS-032 | E04 | AI provider abstraction | — | ai-platform/providers | — | — | — | `AIProviderRepository` | ProviderTest |
| DEVOS-033 | E04 | AI settings | FIGMA-34 | ai-platform/settings | `AISettingsScreen` | `AISettingsViewModel` | `UpdateAISettingsUseCase` | `SettingsRepository` | AISettingsTest |
| DEVOS-034 | E04 | Provider settings | FIGMA-35 | ai-platform/settings | `ProviderSettingsScreen` | `ProviderViewModel` | `SaveProviderUseCase` | `ProviderRepository` | ProviderTest |
| DEVOS-035 | E05 | Agent run screen | FIGMA-19 | agents-mcp/agent-run | `AgentRunScreen` | `AgentRunViewModel` | `GetAgentRunUseCase` | `AgentRepository` | AgentRunTest |
| DEVOS-036 | E05 | Agent tool exec | FIGMA-20 | agents-mcp/tool-exec | `AgentToolExecutionScreen` | `ToolExecViewModel` | `GetToolExecUseCase` | `AgentRepository` | ToolExecTest |
| DEVOS-037 | E05 | Agent engine | — | agents-mcp/engine | — | — | `RunAgentUseCase` | `AgentRepository` | AgentEngineTest |
| DEVOS-038 | E05 | MCP integration | — | agents-mcp/mcp | — | — | `InvokeMCPToolUseCase` | `MCPRepository` | MCPTest |
| DEVOS-039 | E05 | MCP tools screen | FIGMA-33 | agents-mcp/mcp | `MCPToolsScreen` | `MCPViewModel` | `GetMCPToolsUseCase` | `MCPRepository` | MCPScreenTest |
| DEVOS-040 | E06 | Git history | FIGMA-21 | dev-intel/git | `GitHistoryScreen` | `GitHistoryViewModel` | `GetCommitsUseCase` | `GitRepository` | GitHistoryTest |
| DEVOS-041 | E06 | GitHub/GitLab API | — | dev-intel/vcs | — | — | `SyncIssuesUseCase` | `VCSRepository` | VCSApiTest |
| DEVOS-042 | E06 | Issue list | FIGMA-22 | dev-intel/issues | `IssueListScreen` | `IssueListViewModel` | `GetIssuesUseCase` | `IssueRepository` | IssueListTest |
| DEVOS-043 | E06 | Issue detail | FIGMA-23 | dev-intel/issues | `IssueDetailScreen` | `IssueDetailViewModel` | `GetIssueUseCase` | `IssueRepository` | IssueDetailTest |
| DEVOS-044 | E06 | PR list | FIGMA-24 | dev-intel/prs | `PullRequestListScreen` | `PRListViewModel` | `GetPRsUseCase` | `PRRepository` | PRListTest |
| DEVOS-045 | E06 | PR AI review | FIGMA-25 | dev-intel/prs | `PRReviewScreen` | `PRReviewViewModel` | `ReviewPRUseCase` | `PRRepository` | PRReviewTest |
| DEVOS-046 | E07 | Security findings | FIGMA-26 | security-intel/findings | `SecurityFindingsScreen` | `SecurityViewModel` | `GetFindingsUseCase` | `SecurityRepository` | SecurityTest |
| DEVOS-047 | E07 | Security scanner | — | security-intel/scanner | — | — | `ScanRepositoryUseCase` | `SecurityRepository` | ScannerTest |
| DEVOS-048 | E07 | Test intelligence | FIGMA-27 | testing-intel/dashboard | `TestIntelligenceScreen` | `TestViewModel` | `GetTestCoverageUseCase` | `TestRepository` | TestIntelTest |
| DEVOS-049 | E07 | Test coverage service | — | testing-intel/analysis | — | — | `AnalyzeCoverageUseCase` | `TestRepository` | CoverageTest |
| DEVOS-050 | E08 | Learning dashboard | FIGMA-28 | learning/dashboard | `LearningDashboardScreen` | `LearningViewModel` | `GetLearningProgressUseCase` | `LearningRepository` | LearningDashTest |
| DEVOS-051 | E08 | Course details | FIGMA-29 | learning/course | `CourseDetailsScreen` | `CourseViewModel` | `GetCourseUseCase` | `LearningRepository` | CourseTest |
| DEVOS-052 | E08 | Lesson | FIGMA-30 | learning/lesson | `LessonScreen` | `LessonViewModel` | `GetLessonUseCase` | `LearningRepository` | LessonTest |
| DEVOS-053 | E08 | Quiz | FIGMA-31 | learning/quiz | `QuizScreen` | `QuizViewModel` | `SubmitAnswerUseCase` | `LearningRepository` | QuizTest |
| DEVOS-054 | E08 | Learning recommendations | — | learning/recommendations | — | `LearningViewModel` | `GetRecommendationsUseCase` | `LearningRepository` | RecommendationTest |
| DEVOS-055 | E09 | Developer memory screen | FIGMA-32 | dev-memory/screen | `DeveloperMemoryScreen` | `MemoryViewModel` | `GetMemoryUseCase` | `MemoryRepository` | MemoryScreenTest |
| DEVOS-056 | E09 | Developer memory service | — | dev-memory/service | — | — | `SaveMemoryUseCase` | `MemoryRepository` | MemoryServiceTest |
| DEVOS-057 | E10 | Home dashboard | FIGMA-04 | command-center/home | `HomeScreen` | `HomeViewModel` | `GetDashboardUseCase` | `DashboardRepository` | HomeTest |
| DEVOS-058 | E10 | AI recommendations engine | — | command-center/recs | — | `HomeViewModel` | `GetRecommendationsUseCase` | `AIRepository` | RecsTest |
| DEVOS-059 | E10 | Notifications screen | FIGMA-37 | command-center/notifications | `NotificationsScreen` | `NotificationViewModel` | `GetNotificationsUseCase` | `NotificationRepository` | NotifTest |
| DEVOS-060 | E10 | Global search | FIGMA-39 | command-center/search | `SearchScreen` | `SearchViewModel` | `GlobalSearchUseCase` | `SearchRepository` | SearchTest |
| DEVOS-061 | E10 | Profile | FIGMA-38 | command-center/profile | `ProfileScreen` | `ProfileViewModel` | `GetProfileUseCase` | `UserRepository` | ProfileTest |
| DEVOS-062 | E11 | Settings root | FIGMA-40 | android-ui/settings | `SettingsScreen` | `SettingsViewModel` | — | — | SettingsTest |
| DEVOS-063 | E11 | Project settings | FIGMA-36 | android-ui/settings | `ProjectSettingsScreen` | `ProjectSettingsViewModel` | `UpdateProjectUseCase` | `ProjectRepository` | ProjectSettingsTest |
| DEVOS-064 | E11 | Dark mode | — | android-ui/themes | `Theme.kt` | — | — | — | DarkModeTest |
| DEVOS-065 | E11 | Accessibility audit | — | android-ui/accessibility | All screens | — | — | — | A11yTest |
| DEVOS-066 | E11 | Responsive/tablet | — | android-ui/responsive | All screens | — | — | — | ResponsiveTest |
| DEVOS-067 | E12 | CI/CD | — | — | — | — | — | — | CIPipelineTest |
| DEVOS-068 | E12 | Observability | — | — | — | — | — | — | LoggingTest |
| DEVOS-069 | E12 | AI evaluation | — | — | — | — | `EvaluateAIUseCase` | `EvalRepository` | EvalFrameworkTest |
| DEVOS-070 | E12 | Performance | — | — | All screens | — | — | — | PerformanceTest |

---

## AI-SDLC Phase Coverage

| Phase | Tickets | Count |
|-------|---------|-------|
| DISCOVER | — | — |
| SPECIFY | E01-E12 epics | 12 |
| DESIGN | 002–006 | 5 |
| PLAN | All tickets created | 70+ |
| IMPLEMENT | 001–070, 101–140 | 110 |
| VERIFY | 065, 069 | 2 |
| REVIEW | — | ongoing |
| DEPLOY | 067 | 1 |
| OBSERVE | 068, 069 | 2 |
| LEARN | 054, 058 | 2 |
| IMPROVE | 070 | 1 |

---

## Kiro Prompt Coverage Map

*Updated: 2026-10-07 — 20 prompt files covering all 70 feature tickets (100% coverage)*

| Prompt File | Tickets Covered | Epic(s) |
|-------------|----------------|---------|
| `DEVOS-001-android-setup.md` | DEVOS-001 | E01 |
| `DEVOS-002-design-system-tokens.md` | DEVOS-002, 003, 004, 006, 007, 008 | E01 |
| `DEVOS-005-design-system-components.md` | DEVOS-005 | E01 |
| `DEVOS-009-onboarding-auth.md` | DEVOS-009, 010, 011, 012 | E01 |
| `DEVOS-013-repository-intelligence.md` | DEVOS-013, 014, 015, 016, 017 | E02 |
| `DEVOS-018-code-intelligence.md` | DEVOS-018, 021, 022, 023, 024, 025 | E03 |
| `DEVOS-019-code-viewer.md` | DEVOS-019, 020 | E03 |
| `DEVOS-026-ai-chat-screen.md` | DEVOS-026, 027, 028 | E04 |
| `DEVOS-029-ai-platform-extended.md` | DEVOS-029, 030, 032, 033, 034 | E04 |
| `DEVOS-031-rag-pipeline.md` | DEVOS-031 | E04 |
| `DEVOS-037-agent-engine.md` | DEVOS-035, 036, 037 | E05 |
| `DEVOS-038-mcp-integration.md` | DEVOS-038, 039 | E05 |
| `DEVOS-040-developer-intelligence.md` | DEVOS-040, 041, 042, 043, 044, 045 | E06 |
| `DEVOS-046-quality-intelligence.md` | DEVOS-046, 047, 048, 049 | E07 |
| `DEVOS-050-learning-dashboard.md` | DEVOS-050, 051, 052, 053, 054 | E08 |
| `DEVOS-055-developer-memory.md` | DEVOS-055, 056 | E09 |
| `DEVOS-057-home-dashboard.md` | DEVOS-057, 058 | E10 |
| `DEVOS-059-command-center-screens.md` | DEVOS-059, 060, 061 | E10 |
| `DEVOS-062-settings-and-ui-polish.md` | DEVOS-062, 063, 064, 065, 066 | E11 |
| `DEVOS-067-platform-ai-sdlc.md` | DEVOS-067, 068, 069, 070 | E12 |

### Coverage by Epic

| Epic | Feature Tickets | Prompt Files | Coverage |
|------|----------------|--------------|----------|
| E01 Foundation | 12 | 4 files (001, 002, 005, 009) | ✅ 100% |
| E02 Repository Intelligence | 5 | 1 file (013) | ✅ 100% |
| E03 Code Intelligence | 8 | 2 files (018, 019) | ✅ 100% |
| E04 AI Platform | 9 | 3 files (026, 029, 031) | ✅ 100% |
| E05 Agents & MCP | 5 | 2 files (037, 038) | ✅ 100% |
| E06 Developer Intelligence | 6 | 1 file (040) | ✅ 100% |
| E07 Quality Intelligence | 4 | 1 file (046) | ✅ 100% |
| E08 Learning | 5 | 1 file (050) | ✅ 100% |
| E09 Developer Memory | 2 | 1 file (055) | ✅ 100% |
| E10 Command Center | 5 | 2 files (057, 059) | ✅ 100% |
| E11 Android UI/UX | 5 | 1 file (062) | ✅ 100% |
| E12 Platform | 4 | 1 file (067) | ✅ 100% |
| **Total** | **70** | **20 files** | **✅ 100%** |
