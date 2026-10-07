# Kiro Prompt — DEVOS-040: Developer Intelligence (Git, Issues, PRs)

**Jira:** DEVOS-040 / DEVOS-041 / DEVOS-042 / DEVOS-043 / DEVOS-044 / DEVOS-045  
**Epic:** DEVOS-E06  
**Figma:** FIGMA-21 / FIGMA-22 / FIGMA-23 / FIGMA-24 / FIGMA-25  
**Kiro Spec:** `.kiro/specs/developer-intelligence/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the Developer Intelligence module for DevOS AI: git history screen, GitHub/GitLab API client, issue list and detail screens, pull request list and AI review screens.

**Existing files to read first:**
- `.kiro/specs/developer-intelligence/core.md`
- `docs/figma/screen-inventory.md` — Screens FIGMA-21 through FIGMA-25
- `docs/figma/component-inventory.md` — DevOSCard, DevOSStatusBadge, DevOSAIMessage
- `core/core-security/` — SecureTokenRepository (tokens for GitHub/GitLab)
- `domain/domain-ai/` — AIRepository interface (for AI PR review streaming)

**Modules:**
- `feature/feature-git/` — GitHistoryScreen
- `feature/feature-issues/` — IssueListScreen, IssueDetailScreen
- `feature/feature-prs/` — PRListScreen, PRAIReviewScreen
- `domain/domain-git/` — repository interfaces + domain models
- `data/data-git/` — GitHub/GitLab API clients + Room DAOs
- `core/core-network/` — Retrofit API service interfaces

**Package:** `com.devos.ai.feature`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- GitHub/GitLab tokens loaded from `SecureTokenRepository` — never hardcoded.
- Retrofit DTOs must be mapped to domain models in the Repository layer — DTOs never leave `data-git`.
- AI PR review streams via `Flow<StreamChunk>` — never block UI waiting for full review.
- Background sync (issues, PRs, commits) uses WorkManager with 15-minute minimum poll interval.
- Never store raw OAuth tokens in Room — only reference user ID; tokens stay in EncryptedSharedPreferences.

**What to implement:**

### 1. GitHub/GitLab API Clients (DEVOS-041)

File: `core/core-network/src/main/kotlin/com/devos/ai/network/github/GitHubApiService.kt`

```kotlin
interface GitHubApiService {
    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30,
        @Header("Authorization") authHeader: String,
    ): List<GitHubIssueDto>

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Header("Authorization") authHeader: String,
    ): List<GitHubPRDto>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Header("Authorization") authHeader: String,
    ): List<GitHubCommitDto>

    @GET("repos/{owner}/{repo}/pulls/{number}/files")
    suspend fun getPRFiles(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Header("Authorization") authHeader: String,
    ): List<GitHubPRFileDto>
}
```

Token injection pattern (never hardcode):
```kotlin
// In DeveloperIntelligenceRepositoryImpl:
val token = secureTokenRepository.getToken(OAuthProvider.GITHUB) ?: return Result.failure(...)
val authHeader = "Bearer $token"
```

GitLab equivalent: `GitLabApiService` using `/projects/{id}/issues`, `/projects/{id}/merge_requests`, `/projects/{id}/repository/commits`.

Room entities: `IssueEntity`, `PullRequestEntity`, `CommitEntity` in `core-database`. Sync via `DeveloperIntelligenceSyncWorker` (WorkManager).

### 2. Domain Models

```kotlin
data class Issue(
    val id: String,
    val number: Int,
    val title: String,
    val body: String,
    val state: IssueState,
    val labels: List<Label>,
    val assignees: List<User>,
    val commentCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val aiTriagePriority: TriagePriority?,
)

enum class IssueState { OPEN, CLOSED }
enum class TriagePriority { HIGH, MEDIUM, LOW }

data class PullRequest(
    val id: String,
    val number: Int,
    val title: String,
    val body: String,
    val state: PRState,
    val author: User,
    val baseBranch: String,
    val headBranch: String,
    val ciStatus: CIStatus,
    val aiReviewScore: Int?,        // 0–100
    val isDraft: Boolean,
    val createdAt: Instant,
)

data class Commit(
    val sha: String,
    val shortSha: String,
    val message: String,
    val author: User,
    val committedAt: Instant,
    val additions: Int,
    val deletions: Int,
    val filesChanged: Int,
)
```

### 3. GitHistoryScreen (DEVOS-040, FIGMA-21)

Route: `GIT_HISTORY/{repoId}`

UiState:
```kotlin
sealed interface GitHistoryUiState {
    data object Loading : GitHistoryUiState
    data class Success(
        val commits: List<Commit>,
        val branches: List<String>,
        val selectedBranch: String,
        val aiSummary: String?,
        val isLoadingAiSummary: Boolean,
    ) : GitHistoryUiState
    data object Empty : GitHistoryUiState
    data class Error(val message: String, val retryable: Boolean) : GitHistoryUiState
}
```

Layout:
```kotlin
Scaffold(topBar = { DevOSTopBar(title = "Git History") }) { padding ->
    Column(Modifier.padding(padding)) {
        // Branch selector
        ExposedDropdownMenuBox(...) { /* branch list */ }
        // AI summary card (expandable)
        AnimatedVisibility(visible = aiSummary != null || isLoadingAiSummary) {
            DevOSAIMessage(content = aiSummary ?: "", isStreaming = isLoadingAiSummary)
        }
        // Commit list
        LazyColumn {
            items(commits, key = { it.sha }) { commit ->
                CommitCard(commit = commit,
                    onClick = { onNavigateToCommitDetail(commit.sha) })
            }
        }
    }
}

@Composable
fun CommitCard(commit: Commit, onClick: () -> Unit) {
    DevOSCard(onClick = onClick) {
        Row(Modifier.padding(MaterialTheme.spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            Surface(shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(commit.shortSha, style = DevOSCodeTextStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs,
                        vertical = 2.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(commit.message.lines().first(),
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text("${commit.author.name} · ${commit.committedAt.toRelativeTimeString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("+${commit.additions} -${commit.deletions} · ${commit.filesChanged} files",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

### 4. IssueListScreen (DEVOS-042, FIGMA-22)

Route: `ISSUE_LIST/{repoId}`

UiState:
```kotlin
sealed interface IssueListUiState {
    data object Loading : IssueListUiState
    data class Success(
        val issues: List<Issue>,
        val filter: IssueFilter,
    ) : IssueListUiState
    data object Empty : IssueListUiState
    data class Error(val message: String, val retryable: Boolean) : IssueListUiState
}

data class IssueFilter(
    val state: IssueState = IssueState.OPEN,
    val assignedToMe: Boolean = false,
    val reportedByMe: Boolean = false,
)
```

Layout: Filter chips row (Open / Closed / Assigned to Me / My Reports). `LazyColumn` of `IssueCard` items showing: issue number, title, label chips, assignee avatar(s), comment count chip, AI triage priority badge. FAB opens issue URL in Custom Tab. All 4 UiStates.

### 5. IssueDetailScreen (DEVOS-043, FIGMA-23)

Route: `ISSUE_DETAIL/{repoId}/{issueId}`

Layout:
```kotlin
Scaffold(topBar = { DevOSTopBar(title = "#${issue.number}", onBack = onBack) }) { padding ->
    LazyColumn(Modifier.padding(padding).padding(MaterialTheme.spacing.base)) {
        item { Text(issue.title, style = MaterialTheme.typography.titleLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                DevOSStatusBadge(issue.state.name, if (issue.state == OPEN)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                issue.labels.forEach { label -> LabelChip(label) }
            }
        }
        item {
            SectionHeader("AI Summary")
            DevOSAIMessage(content = aiSummary, isStreaming = isLoadingAiSummary)
        }
        item { SectionHeader("Description") }
        item { DevOSMarkdownText(markdown = issue.body) }
        if (relatedCodeRefs.isNotEmpty()) {
            item { SectionHeader("Related Code") }
            items(relatedCodeRefs) { ref ->
                CodeRefChip(ref, onClick = { onNavigateToCode(ref.filePath, ref.lineNumber) })
            }
        }
        item { SectionHeader("Comments (${comments.size})") }
        items(comments) { comment -> IssueCommentItem(comment) }
        // Action bar
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                DevOSButton("Close", onClick = onClose, style = DevOSButtonStyle.Secondary)
                DevOSButton("AI Fix Suggestion", onClick = onAIFix)
            }
        }
    }
}
```

### 6. PRListScreen (DEVOS-044, FIGMA-24)

Route: `PR_LIST/{repoId}`

Filter tabs: Open / Draft / Needs Review / My PRs. `LazyColumn` of `PRCard` items showing: PR number, title, author avatar, `baseBranch ← headBranch`, CI status badge (Pass=green/Fail=red/Running=amber), AI review score chip. All 4 UiStates.

### 7. PRAIReviewScreen (DEVOS-045, FIGMA-25)

Route: `PR_REVIEW/{repoId}/{prId}`

UiState:
```kotlin
sealed interface PRAIReviewUiState {
    data object Loading : PRAIReviewUiState
    data class Success(
        val pr: PullRequest,
        val aiSummary: String,
        val isStreamingSummary: Boolean,
        val fileReviews: List<FileReview>,
        val verdict: ReviewVerdict,
    ) : PRAIReviewUiState
    data object Empty : PRAIReviewUiState
    data class Error(val message: String, val retryable: Boolean) : PRAIReviewUiState
}

data class FileReview(
    val filePath: String,
    val additions: Int,
    val deletions: Int,
    val diff: String,
    val suggestions: List<InlineSuggestion>,
)

data class InlineSuggestion(
    val lineNumber: Int,
    val comment: String,
    val severity: SuggestionSeverity,
)

enum class ReviewVerdict { APPROVED, NEEDS_WORK, CRITICAL }
```

Layout:
```kotlin
Scaffold(
    topBar = { DevOSTopBar(title = "AI Review: #${pr.number}", onBack = onBack) },
    bottomBar = {
        Row(Modifier.padding(MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            DevOSButton("Copy Review", onClick = onCopyReview,
                style = DevOSButtonStyle.Secondary, modifier = Modifier.weight(1f))
            DevOSButton("Post to ${pr.provider}", onClick = onPostReview,
                modifier = Modifier.weight(1f))
        }
    }
) { padding ->
    LazyColumn(Modifier.padding(padding)) {
        item {
            // PR header + verdict badge
            Row {
                Column(Modifier.weight(1f)) {
                    Text(pr.title, style = MaterialTheme.typography.titleMedium)
                    Text("${pr.author.name} · ${pr.baseBranch} ← ${pr.headBranch}",
                        style = MaterialTheme.typography.bodySmall)
                }
                VerdictBadge(verdict)
            }
        }
        item {
            SectionHeader("AI Summary")
            DevOSAIMessage(content = aiSummary, isStreaming = isStreamingSummary)
        }
        items(fileReviews) { fileReview ->
            FileReviewAccordion(fileReview = fileReview)
        }
    }
}
```

AI review is triggered via `RunAIPRReviewUseCase` which:
1. Fetches PR diff via `GitHubApiService.getPRFiles()`
2. Sends diff + repo context to `AIRepository.chat()` with a structured review prompt
3. Streams result back as `Flow<StreamChunk>` updating `aiSummary` incrementally
4. Parses final result for inline suggestions and verdict

"Post to GitHub/GitLab" opens the PR URL in a Chrome Custom Tab — it does not post programmatically (requires write scope; defer to future ticket).

**Tests:**
- `GitHubApiServiceTest`: mocked Retrofit; issues parsed; commits parsed; auth header sent
- `DeveloperIntelligenceRepositoryImplTest`: Room in-memory DB; issues synced; PR list correct; token from SecureTokenRepository
- `GitHistoryViewModelTest`: commits load; branch change reloads; AI summary streams
- `IssueListViewModelTest`: filter changes; empty state on no results; error handled
- `IssueDetailViewModelTest`: issue loads; AI summary streams; related code refs shown
- `PRListViewModelTest`: filter tabs work; AI score badge shown
- `PRAIReviewViewModelTest`: AI review streams; verdict parsed; copy action fires; post action navigates

**Acceptance Criteria:**
- AC1 (DEVOS-040): Branch selector renders; commit list shows SHA, message, author, stats
- AC2 (DEVOS-040): Tap commit navigates to commit detail with diff
- AC3 (DEVOS-040): AI summary of recent changes streams and renders in expandable card
- AC4 (DEVOS-040): Filter by author/date works
- AC5 (DEVOS-041): GitHub REST client fetches issues, PRs, and commits with auth header
- AC6 (DEVOS-041): GitLab API client fetches issues and merge requests
- AC7 (DEVOS-041): Issues and PRs synced to Room; visible in app within 30s of creation on VCS
- AC8 (DEVOS-041): Background sync uses WorkManager — not foreground coroutines
- AC9 (DEVOS-042): Filter chips (Open/Closed/Assigned to Me/My Reports) functional
- AC10 (DEVOS-042): Issue cards show labels, assignee avatars, comment count, and AI triage badge
- AC11 (DEVOS-042): FAB opens create-issue URL in Custom Tab
- AC12 (DEVOS-042): All 4 UiStates render correctly
- AC13 (DEVOS-043): Full issue body rendered via `DevOSMarkdownText`
- AC14 (DEVOS-043): AI summary present and streamed
- AC15 (DEVOS-043): Related code refs shown and navigable to `CodeViewerScreen`
- AC16 (DEVOS-043): Comments thread rendered; Close and AI Fix Suggestion actions work
- AC17 (DEVOS-044): Filter tabs (Open/Draft/Needs Review/My PRs) work
- AC18 (DEVOS-044): PR cards show CI status badge and AI review score
- AC19 (DEVOS-044): All 4 UiStates render
- AC20 (DEVOS-045): PR header with title, author, branch info, and verdict badge rendered
- AC21 (DEVOS-045): AI summary streams via `DevOSAIMessage`
- AC22 (DEVOS-045): File-by-file accordion with inline suggestion chips
- AC23 (DEVOS-045): Verdict badge: Approved/Needs Work/Critical shown
- AC24 (DEVOS-045): "Copy Review" copies formatted review to clipboard
- AC25 (DEVOS-045): "Post to GitHub/GitLab" opens PR URL in Custom Tab
