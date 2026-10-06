# DevOS AI — Screen Inventory

**Total Screens:** 40  
**Version:** 1.0  
**Date:** 2026-10-07

---

## Screen Index

| # | Screen Name | Route | Figma Ref | Jira Ticket | Component |
|---|-------------|-------|-----------|-------------|-----------|
| 01 | Splash | `splash` | FIGMA-01 | DEVOS-101 | `SplashScreen` |
| 02 | Onboarding | `onboarding` | FIGMA-02 | DEVOS-102 | `OnboardingScreen` |
| 03 | Login | `login` | FIGMA-03 | DEVOS-103 | `LoginScreen` |
| 04 | Home Dashboard | `home` | FIGMA-04 | DEVOS-104 | `HomeScreen` |
| 05 | Project List | `project_list` | FIGMA-05 | DEVOS-105 | `ProjectListScreen` |
| 06 | Project Overview | `project/{id}` | FIGMA-06 | DEVOS-106 | `ProjectOverviewScreen` |
| 07 | Repository Import | `repository/import` | FIGMA-07 | DEVOS-107 | `RepositoryImportScreen` |
| 08 | Repository Sync | `repository/{id}/sync` | FIGMA-08 | DEVOS-108 | `RepositorySyncScreen` |
| 09 | Repository Overview | `repository/{id}` | FIGMA-09 | DEVOS-109 | `RepositoryOverviewScreen` |
| 10 | File Explorer | `repository/{id}/files` | FIGMA-10 | DEVOS-110 | `FileExplorerScreen` |
| 11 | Code Viewer | `repository/{id}/file` | FIGMA-11 | DEVOS-111 | `CodeViewerScreen` |
| 12 | Code Search | `repository/{id}/search` | FIGMA-12 | DEVOS-112 | `CodeSearchScreen` |
| 13 | Symbol Details | `repository/{id}/symbol/{sid}` | FIGMA-13 | DEVOS-113 | `SymbolDetailsScreen` |
| 14 | Dependency Graph | `repository/{id}/graph` | FIGMA-14 | DEVOS-114 | `DependencyGraphScreen` |
| 15 | Architecture Overview | `repository/{id}/architecture` | FIGMA-15 | DEVOS-115 | `ArchitectureOverviewScreen` |
| 16 | AI Chat | `ai_chat` | FIGMA-16 | DEVOS-116 | `AIChatScreen` |
| 17 | AI Answer Details | `ai/answer/{id}` | FIGMA-17 | DEVOS-117 | `AIAnswerDetailScreen` |
| 18 | AI Source Evidence | `ai/evidence/{id}` | FIGMA-18 | DEVOS-118 | `AISourceEvidenceScreen` |
| 19 | Agent Run | `agent/run/{id}` | FIGMA-19 | DEVOS-119 | `AgentRunScreen` |
| 20 | Agent Tool Execution | `agent/run/{id}/tool/{tid}` | FIGMA-20 | DEVOS-120 | `AgentToolExecutionScreen` |
| 21 | Git History | `repository/{id}/git` | FIGMA-21 | DEVOS-121 | `GitHistoryScreen` |
| 22 | Issues | `project/{id}/issues` | FIGMA-22 | DEVOS-122 | `IssueListScreen` |
| 23 | Issue Details | `project/{id}/issues/{iid}` | FIGMA-23 | DEVOS-123 | `IssueDetailScreen` |
| 24 | Pull Requests | `project/{id}/prs` | FIGMA-24 | DEVOS-124 | `PullRequestListScreen` |
| 25 | PR AI Review | `project/{id}/prs/{pid}/review` | FIGMA-25 | DEVOS-125 | `PRReviewScreen` |
| 26 | Security Findings | `project/{id}/security` | FIGMA-26 | DEVOS-126 | `SecurityFindingsScreen` |
| 27 | Test Intelligence | `project/{id}/tests` | FIGMA-27 | DEVOS-127 | `TestIntelligenceScreen` |
| 28 | Learning Dashboard | `learning_dashboard` | FIGMA-28 | DEVOS-128 | `LearningDashboardScreen` |
| 29 | Course Details | `learn/course/{id}` | FIGMA-29 | DEVOS-129 | `CourseDetailsScreen` |
| 30 | Lesson | `learn/course/{id}/lesson/{lid}` | FIGMA-30 | DEVOS-130 | `LessonScreen` |
| 31 | Quiz | `learn/course/{id}/quiz/{qid}` | FIGMA-31 | DEVOS-131 | `QuizScreen` |
| 32 | Developer Memory | `memory` | FIGMA-32 | DEVOS-132 | `DeveloperMemoryScreen` |
| 33 | MCP Tools | `mcp` | FIGMA-33 | DEVOS-133 | `MCPToolsScreen` |
| 34 | AI Settings | `settings/ai` | FIGMA-34 | DEVOS-134 | `AISettingsScreen` |
| 35 | Provider Settings | `settings/ai/providers` | FIGMA-35 | DEVOS-135 | `ProviderSettingsScreen` |
| 36 | Project Settings | `settings/project/{id}` | FIGMA-36 | DEVOS-136 | `ProjectSettingsScreen` |
| 37 | Notifications | `notifications` | FIGMA-37 | DEVOS-137 | `NotificationsScreen` |
| 38 | Profile | `profile` | FIGMA-38 | DEVOS-138 | `ProfileScreen` |
| 39 | Search | `search` | FIGMA-39 | DEVOS-139 | `SearchScreen` |
| 40 | Settings | `settings` | FIGMA-40 | DEVOS-140 | `SettingsScreen` |

---

## Detailed Screen Specifications

---

### 01 · Splash Screen
**Route:** `splash`  
**Description:** App launch screen with animated logo and initialization.

**States:** Loading  
**Layout:**
- Full-screen background: `background` color
- Centered DevOS AI logo (animated pulse)
- App name: `displayMedium` typography
- Tagline: "Understand your code. Learn faster. Build smarter."
- Progress indicator (linear, bottom)

**Transitions:** Fade → Onboarding (first launch) | Fade → Home (returning user)

---

### 02 · Onboarding Screen
**Route:** `onboarding`  
**Description:** 4-step onboarding carousel introducing core features.

**States:** Loading, Active, Complete  
**Steps:**
1. AI Developer Command Center
2. Repository Intelligence
3. AI Chat & Agents
4. Learning System

**Layout:**
- Pager with dot indicators
- Illustration + headline + body
- Skip button (top right)
- Next / Get Started button (bottom)

---

### 03 · Login Screen
**Route:** `login`  
**Description:** Authentication via GitHub, GitLab, or email.

**States:** Idle, Loading, Error  
**Layout:**
- DevOS AI logo
- "Sign in to your workspace"
- GitHub OAuth button
- GitLab OAuth button
- Email / password fallback
- Terms & Privacy links

---

### 04 · Home Dashboard
**Route:** `home`  
**Description:** AI developer command center — the primary landing screen.

**States:** Loading, Success, Empty, Error  
**Sections:**
- Global AI search bar (pinned top)
- Recent Projects (horizontal scroll cards)
- AI Recommendations (vertical list, dismissible)
  - Open PRs needing review
  - Missing tests
  - Architecture hotspots
  - Security alerts
  - Learning suggestions
- Project Health summary (grid: Security, Tests, Architecture, Dependencies)
- Recent AI Sessions (list)
- Recent Repositories (list)

**Actions:**
- Tap search → `SearchScreen`
- Tap project card → `ProjectOverviewScreen`
- Tap recommendation → relevant screen
- Tap AI session → `AIChatScreen`

---

### 05 · Project List
**Route:** `project_list`  
**Description:** List of all projects with health indicators.

**States:** Loading, Success (list), Empty, Error  
**Layout:**
- Top bar with "Projects" title + add button
- Search/filter bar
- Project cards (sorted by last activity)
  - Name, language icons, health badge, last sync
- FAB: Import / Create project

---

### 06 · Project Overview
**Route:** `project/{projectId}`  
**Description:** Project dashboard with tabs for all sub-features.

**States:** Loading, Success, Error  
**Header:**
- Project name, repository, language chips, health score
**Tabs:** Overview | Repos | Issues | PRs | Architecture | Security | Tests  
**Quick Actions:** Ask AI, Explore Code, Architecture, Graph, Git, Issues, PRs, Security, Tests

---

### 07 · Repository Import
**Route:** `repository/import`  
**Description:** Import a repository from GitHub, GitLab, or local path.

**States:** Idle, Validating, Loading, Success, Error  
**Layout:**
- Source selector: GitHub | GitLab | Local
- URL / path input
- Repository info preview (after validation)
- Import options: branch, depth, index options
- Import button

---

### 08 · Repository Sync
**Route:** `repository/{repoId}/sync`  
**Description:** Sync/re-index a repository with progress.

**States:** Idle, Syncing (with progress), Success, Error  
**Layout:**
- Repository name and current status
- Sync steps with progress indicators:
  - Fetch remote
  - Index files
  - Build symbol table
  - Build code graph
  - Update RAG embeddings
- Cancel button
- Done / Retry on completion

---

### 09 · Repository Overview
**Route:** `repository/{repoId}`  
**Description:** Repository home with tabbed navigation.

**States:** Loading, Success, Stale, Error  
**Header:** Repo name, owner, branch selector, last sync, language bars  
**Tabs:** Overview | Files | Search | Symbols | Graph | RAG | Git | AI  
**Health panel:** Stars, issues, PR count, CI status, coverage

---

### 10 · File Explorer
**Route:** `repository/{repoId}/files?path={path}`  
**Description:** Tree-based file browser with quick actions.

**States:** Loading, Success, Empty (dir), Error  
**Layout:**
- Breadcrumb path bar
- Collapsible directory tree
- File list with: icon, name, last modified, size
- File type filter chips
- Search within files

**Actions per file:**
- Open in Code Viewer
- Ask AI about this file
- View Git history

---

### 11 · Code Viewer
**Route:** `repository/{repoId}/file?path={path}&line={line}`  
**Description:** Full-featured code viewer with AI integration.

**States:** Loading, Success, Error  
**Layout:**
- Top bar: file path, actions (search, copy, share, AI)
- Line numbers + syntax-highlighted code
- Scroll-linked line highlight
- Symbol click → `SymbolDetailsScreen`
- Bottom AI action bar: Explain | Debug | Find Usages | Generate Tests | Ask AI

---

### 12 · Code Search
**Route:** `repository/{repoId}/search?q={query}`  
**Description:** Full-text and semantic code search across repository.

**States:** Idle, Searching, Results, Empty, Error  
**Layout:**
- Search input (auto-focused)
- Filters: file type, scope, case-sensitive, regex
- Results list: file path, line preview, match highlight
- Semantic search toggle (AI-powered)

---

### 13 · Symbol Details
**Route:** `repository/{repoId}/symbol/{symbolId}`  
**Description:** Detailed view of a code symbol (class, function, etc).

**States:** Loading, Success, Error  
**Sections:**
- Symbol name, type badge, file location
- Signature / declaration
- Documentation / KDoc
- References (where used)
- Dependencies (what it uses)
- AI explanation
- Code snippet

---

### 14 · Dependency Graph
**Route:** `repository/{repoId}/graph`  
**Description:** Interactive dependency/call graph visualization.

**States:** Loading, Success, Empty, Error  
**Layout:**
- Zoomable/pannable canvas
- Node: class/module with color by type
- Edge: dependency arrows
- Detail panel (tap node): symbol info
- Filter: show only cycles, show only hotspots
- Export / share graph

---

### 15 · Architecture Overview
**Route:** `repository/{repoId}/architecture`  
**Description:** AI-generated architecture summary and layer analysis.

**States:** Loading, Success, Stale, Error  
**Sections:**
- Architecture style detected (e.g., MVVM, Clean)
- Layer diagram
- Module breakdown
- Hotspot analysis (complexity, coupling)
- AI architecture insights
- Ask AI about architecture

---

### 16 · AI Chat
**Route:** `ai_chat`  
**Description:** Primary AI interaction surface.

**States:** Idle, Thinking, Streaming, Error  
**Layout:**
- Top bar: "DevOS AI" + context chip + clear button
- Context selector bar (project/repo/file/symbol)
- Chat message list
  - AI messages: markdown, code blocks, source references
  - User messages: right-aligned bubbles
  - Agent execution inline steps
- Suggested action chips (Explain | Find | Debug | Analyze | Review | Learn)
- Input bar: text field + send + attach + voice

---

### 17 · AI Answer Details
**Route:** `ai/answer/{answerId}`  
**Description:** Expanded view of an AI answer with full evidence.

**States:** Loading, Success  
**Layout:**
- Full AI response (markdown rendered)
- Evidence section: source files referenced
- Call chain / data flow visualization
- Actions: Open source | Explain more | Show dependencies | Ask follow-up

---

### 18 · AI Source Evidence
**Route:** `ai/evidence/{answerId}`  
**Description:** Grounding evidence for an AI answer — file/line references.

**States:** Loading, Success, Empty  
**Layout:**
- Evidence list: file path + line range + snippet
- Relevance score per source
- Open in Code Viewer action
- Tap to highlight in code

---

### 19 · Agent Run
**Route:** `agent/run/{runId}`  
**Description:** Transparent view of an agent execution in progress.

**States:** Running, Completed, Failed  
**Layout:**
- Agent name + goal description
- Step-by-step execution log:
  - ✓ Completed steps
  - ⟳ In-progress step (animated)
  - ○ Pending steps
- Tool call indicators (expandable)
- Final answer panel (when complete)
- Cancel button (when running)

---

### 20 · Agent Tool Execution
**Route:** `agent/run/{runId}/tool/{toolId}`  
**Description:** Detail view of a single tool invocation.

**States:** Pending, Running, Success, Error  
**Layout:**
- Tool name + type badge
- Input parameters (formatted)
- Output / result
- Duration + token usage
- Expandable raw JSON

---

### 21 · Git History
**Route:** `repository/{repoId}/git`  
**Description:** Commit history with AI-powered insights.

**States:** Loading, Success, Empty, Error  
**Layout:**
- Branch selector
- Commit list: hash, message, author, date, stats
- Tap commit → commit detail with diff
- AI summary button: "Summarize recent changes"
- Filter: author, date range, file path

---

### 22 · Issues
**Route:** `project/{projectId}/issues`  
**Description:** Issue list from GitHub/GitLab with AI triage.

**States:** Loading, Success, Empty, Error  
**Layout:**
- Filter chips: Open | Closed | Assigned to me | Labels
- Issue list: number, title, labels, assignee, comments, updated
- AI triage badge (priority suggestion)
- FAB: Create issue

---

### 23 · Issue Details
**Route:** `project/{projectId}/issues/{issueId}`  
**Description:** Full issue view with AI-powered context.

**States:** Loading, Success, Error  
**Layout:**
- Title, labels, assignee, milestone, created date
- Body (markdown rendered)
- AI summary + suggested fix
- Related code references
- Comments thread
- Actions: Close | Assign | Label | AI Fix Suggestion

---

### 24 · Pull Requests
**Route:** `project/{projectId}/prs`  
**Description:** PR list with AI review status.

**States:** Loading, Success, Empty, Error  
**Layout:**
- Filter: Open | Draft | Needs Review | My PRs
- PR card: title, author, branch, CI status, review status, AI score
- AI review badge

---

### 25 · PR AI Review
**Route:** `project/{projectId}/prs/{prId}/review`  
**Description:** AI-powered code review for a pull request.

**States:** Loading, Analyzing, Success, Error  
**Layout:**
- PR header: title, branch, author, stats
- AI review summary
- File-by-file review (expandable)
- Inline code comments with AI suggestions
- Overall verdict: Approved | Changes Requested | Neutral
- Actions: Copy review | Post to GitHub/GitLab

---

### 26 · Security Findings
**Route:** `project/{projectId}/security`  
**Description:** Security vulnerability dashboard.

**States:** Loading, Success, Empty, Scanning, Error  
**Layout:**
- Summary: Critical / High / Medium / Low counts
- Finding list: severity badge, file, description, line
- AI explanation per finding
- Filters: severity, type (OWASP), status
- Actions: Mark fixed | Dismiss | Ask AI

---

### 27 · Test Intelligence
**Route:** `project/{projectId}/tests`  
**Description:** Test coverage and quality analysis.

**States:** Loading, Success, Empty, Error  
**Sections:**
- Coverage summary (percentage, trend chart)
- Uncovered files list
- Flaky test list
- AI suggestions: "Add tests for AuthViewModel"
- Recent test runs

---

### 28 · Learning Dashboard
**Route:** `learning_dashboard`  
**Description:** Personalized learning system connected to repositories.

**States:** Loading, Success, Empty  
**Sections:**
- Continue learning (current course card)
- Daily goal progress
- Streak indicator
- Recommended topics (based on active repository)
- Recent lessons
- Quiz scores
- Repository connection: "RAG found in your current project"

---

### 29 · Course Details
**Route:** `learn/course/{courseId}`  
**Description:** Course overview with lesson list.

**States:** Loading, Success  
**Layout:**
- Course title, description, difficulty, duration
- Progress bar
- Lesson list with completion indicators
- Repository connection (if applicable)
- Start / Continue button

---

### 30 · Lesson
**Route:** `learn/course/{courseId}/lesson/{lessonId}`  
**Description:** Interactive lesson with content and code examples.

**States:** Loading, Active, Complete  
**Layout:**
- Progress indicator
- Lesson content (markdown + code blocks)
- Embedded DevOS code examples (from actual repositories)
- Next / Previous navigation
- "Try in your repo" action

---

### 31 · Quiz
**Route:** `learn/course/{courseId}/quiz/{quizId}`  
**Description:** Knowledge check quiz.

**States:** Active, Reviewing, Complete  
**Layout:**
- Question counter
- Question text
- Multiple choice options
- Submit answer
- Result feedback (correct/incorrect + explanation)
- Final score screen with recommendations

---

### 32 · Developer Memory
**Route:** `memory`  
**Description:** AI-powered developer context and memory management.

**States:** Loading, Success, Empty  
**Sections:**
- Recent decisions made
- Code preferences learned
- Repository-specific memory entries
- Global preferences
- Search memory
- Clear / manage entries

---

### 33 · MCP Tools
**Route:** `mcp`  
**Description:** Browse and invoke MCP tools.

**States:** Loading, Success, Empty, Error  
**Layout:**
- Connected MCP servers list
- Available tools per server
- Tool detail: name, description, parameters
- Execute tool (with confirmation for destructive actions)
- Execution history

---

### 34 · AI Settings
**Route:** `settings/ai`  
**Description:** Configure AI behavior, models, and capabilities.

**States:** Loading, Success  
**Sections:**
- Default model selector
- RAG settings (chunk size, top-k)
- Agent settings (max steps, auto-approve)
- Memory settings
- Response format preferences
- Token usage stats

---

### 35 · Provider Settings
**Route:** `settings/ai/providers`  
**Description:** Configure AI provider API keys and endpoints.

**States:** Loading, Success, Error  
**Layout:**
- Provider list: OpenAI, Anthropic, Gemini, Ollama, Custom
- Per-provider: API key (masked), endpoint, model selection, test connection
- Warning: API keys stored in encrypted keystore

---

### 36 · Project Settings
**Route:** `settings/project/{projectId}`  
**Description:** Configure project-specific settings.

**States:** Loading, Success  
**Sections:**
- Name, description
- Repository connections
- Default branch
- AI context settings
- Notification preferences
- Danger zone: Delete project

---

### 37 · Notifications
**Route:** `notifications`  
**Description:** Notification center for AI events, PRs, issues.

**States:** Loading, Success, Empty  
**Layout:**
- Filter: All | AI | PRs | Issues | Security | Tests
- Notification list with read/unread states
- Swipe to dismiss
- Tap to navigate to relevant screen
- Mark all read button

---

### 38 · Profile
**Route:** `profile`  
**Description:** User profile and account management.

**States:** Loading, Success  
**Sections:**
- Avatar, name, email, GitHub/GitLab username
- Connected accounts
- Usage statistics
- Subscription / plan
- Sign out

---

### 39 · Search
**Route:** `search?q={query}`  
**Description:** Global search across projects, repos, code, and AI history.

**States:** Idle, Searching, Results, Empty  
**Layout:**
- Search input (auto-focused)
- Scope selector: All | Code | Issues | PRs | Learning
- Recent searches (idle state)
- Results grouped by type
- AI semantic search results

---

### 40 · Settings
**Route:** `settings`  
**Description:** App settings root.

**States:** Loaded  
**Layout:**
- Settings groups: AI, Appearance, Account, Projects, Notifications, About
- Each group → sub-screen
- Version, build number, feedback link
