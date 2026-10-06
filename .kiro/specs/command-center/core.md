# Spec: Developer Command Center — Core

**Jira:** DEVOS-057 / DEVOS-058 / DEVOS-059 / DEVOS-060 / DEVOS-061  
**Epic:** DEVOS-E10  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement the Home Dashboard, AI recommendations engine, notifications, global search, and profile.

## Home Dashboard

### Screen: HomeScreen (FIGMA-04)
**Route:** `home`  
**States:** Loading | Success | Empty (new user) | Error

**Sections (in order):**

1. **Global AI Search Bar** (pinned, DevOSSearchBar)
   - Placeholder: "Ask DevOS anything..."
   - Tap → SearchScreen
   - Voice input button

2. **Recent Projects** (horizontal LazyRow of DevOSProjectCard)
   - Max 5 cards
   - Empty: "No projects yet → Import your first repository"

3. **AI Recommendations** (vertical LazyColumn, dismissible)
   Each recommendation is a `DevOSCard` with:
   - Icon + type badge (PR | Security | Test | Learning | Architecture)
   - Title + description
   - Primary action button
   - Dismiss (X) button — persisted dismissal

   Recommendation types:
   - "Review PR: [title]" → PRReviewScreen
   - "Missing tests: [file]" → TestIntelligenceScreen
   - "Security: [finding]" → SecurityFindingsScreen
   - "Architecture hotspot: [module]" → ArchitectureScreen
   - "Continue learning: [course]" → LearningDashboardScreen
   - "Stale repository: [repo]" → RepositorySyncScreen

4. **Project Health Grid** (2×2 grid of DevOSHealthIndicator)
   - Security | Tests | Architecture | Dependencies
   - Color-coded rings with scores
   - Tap → relevant screen

5. **Recent AI Sessions** (LazyColumn list)
   - Session title (first message truncated)
   - Timestamp + context (repo/project)
   - Tap → AIChatScreen with session restored

6. **Recent Repositories** (LazyColumn list)
   - Repo name + owner + last sync + language bar
   - Tap → RepositoryOverviewScreen

## Domain Models

```kotlin
data class DashboardData(
    val recentProjects: List<ProjectSummary>,
    val recommendations: List<AIRecommendation>,
    val projectHealth: ProjectHealth?,
    val recentSessions: List<ChatSessionSummary>,
    val recentRepositories: List<RepositorySummary>,
)

data class AIRecommendation(
    val id: String,
    val type: RecommendationType,
    val title: String,
    val description: String,
    val actionRoute: String,
    val priority: RecommendationPriority,
    val dismissed: Boolean,
    val createdAt: Long,
)

enum class RecommendationType { PR_REVIEW, MISSING_TESTS, SECURITY, ARCHITECTURE, LEARNING, STALE_REPO }
```

## Recommendations Engine

```kotlin
class GetRecommendationsUseCase @Inject constructor(
    private val prRepository: PRRepository,
    private val securityRepository: SecurityRepository,
    private val testRepository: TestRepository,
    private val learningRepository: LearningRepository,
    private val repoRepository: RepositoryRepository,
) {
    operator fun invoke(): Flow<List<AIRecommendation>> = combine(
        prRepository.getOpenPRsNeedingReview(),
        securityRepository.getCriticalFindings(),
        testRepository.getLowCoverageFiles(),
        learningRepository.getNextRecommendation(),
        repoRepository.getStaleSyncRepos(),
    ) { prs, findings, files, learning, stale ->
        buildRecommendations(prs, findings, files, learning, stale)
            .sortedByDescending { it.priority }
            .take(8)
    }
}
```

## Screen: NotificationsScreen (FIGMA-37)
- Filter chips: All | AI | PRs | Issues | Security | Tests
- NotificationItem: icon + title + description + timestamp + read indicator
- Swipe-to-dismiss
- Tap → navigate to relevant screen
- Mark all read button

## Screen: SearchScreen (FIGMA-39)
- Global search input (auto-focused on open)
- Scope chips: All | Code | Issues | PRs | Learning
- Recent searches (idle state)
- Results grouped:
  - Code results (file + line + snippet)
  - Issue results (title + number)
  - PR results (title + branch)
  - Learning results (course + lesson)
- AI semantic results section (top of results when query is question-like)

## Screen: ProfileScreen (FIGMA-38)
- Avatar + name + email
- Connected accounts (GitHub chip, GitLab chip)
- Usage stats (tokens used, repos indexed, AI queries)
- Plan indicator
- Sign out button (confirmation dialog)

## Acceptance Criteria
- [ ] Home loads within 2 seconds on first render
- [ ] All 6 sections render with correct data
- [ ] Recommendation dismissal persists across restarts
- [ ] Project health grid shows correct scores
- [ ] Search returns results across all scopes within 2s
- [ ] Notifications filter correctly
- [ ] Profile shows connected accounts
- [ ] Sign out clears all local session data
- [ ] Empty state shows for new users with call to action
