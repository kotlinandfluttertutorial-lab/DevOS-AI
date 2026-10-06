# Spec: Developer Intelligence — Core

**Jira:** DEVOS-040 / DEVOS-041 / DEVOS-042 / DEVOS-043 / DEVOS-044 / DEVOS-045  
**Epic:** DEVOS-E06  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement Git history, GitHub/GitLab sync, issue management, and AI-powered PR review.

## GitHub/GitLab API Integration

```kotlin
interface VCSRepository {
    // Authentication
    suspend fun authenticate(token: String, provider: VCSProvider): Boolean

    // Issues
    fun getIssues(projectId: String, filter: IssueFilter): Flow<PagingData<Issue>>
    suspend fun getIssue(projectId: String, issueId: String): Issue
    suspend fun createIssue(projectId: String, issue: IssueCreate): Issue
    suspend fun closeIssue(projectId: String, issueId: String)

    // Pull Requests
    fun getPullRequests(projectId: String, filter: PRFilter): Flow<PagingData<PullRequest>>
    suspend fun getPullRequest(projectId: String, prId: String): PullRequest
    suspend fun getPRDiff(projectId: String, prId: String): PRDiff

    // Git
    fun getCommits(repoId: String, filter: CommitFilter): Flow<PagingData<Commit>>
    suspend fun getCommitDetail(repoId: String, sha: String): CommitDetail
}
```

## Domain Models

```kotlin
data class Issue(
    val id: String,
    val number: Int,
    val title: String,
    val body: String,
    val state: IssueState,
    val labels: List<Label>,
    val assignees: List<User>,
    val commentsCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val aiTriagePriority: AIPriority?,   // AI-suggested severity
    val aiSummary: String?,
)

data class PullRequest(
    val id: String,
    val number: Int,
    val title: String,
    val author: User,
    val sourceBranch: String,
    val targetBranch: String,
    val state: PRState,
    val ciStatus: CIStatus,
    val reviewStatus: ReviewStatus,
    val aiReviewScore: Float?,        // 0.0–1.0 AI quality score
    val filesChanged: Int,
    val additions: Int,
    val deletions: Int,
)

data class PRReview(
    val summary: String,
    val verdict: ReviewVerdict,       // APPROVED, CHANGES_REQUESTED, NEUTRAL
    val fileFeedback: List<FileReview>,
    val overallScore: Float,
)

data class Commit(
    val sha: String,
    val message: String,
    val author: CommitAuthor,
    val additions: Int,
    val deletions: Int,
    val filesChanged: Int,
    val timestamp: Long,
)
```

## Screens

### GitHistoryScreen (FIGMA-21)
- Branch selector chip
- Commit list (LazyColumn, paginated)
- Per commit: short sha, message, author avatar + name, date, ±stats
- Tap → commit detail + diff viewer
- AI "Summarize recent changes" action
- Filter: author | date range | file path

### IssueListScreen (FIGMA-22)
- Filter chips: Open | Closed | Assigned to me | Labels
- Issue cards: number, title, labels (color chips), assignee, comment count, updated
- AI triage badge on issues (severity suggestion)
- FAB: Create issue (confirmation dialog)
- Paginated via Pager + PagingSource

### IssueDetailScreen (FIGMA-23)
- Title, labels, assignee, milestone, created date
- Body rendered via DevOSMarkdownText
- AI summary section
- AI fix suggestion (code-grounded)
- Related code references (SourceReference chips)
- Comments thread
- Actions: Close | Assign | Label | AI Fix

### PullRequestListScreen (FIGMA-24)
- Filter: Open | Draft | Needs Review | My PRs
- PR card: title, author, branch chips, CI badge, review status, AI score ring
- Pagination

### PRReviewScreen (FIGMA-25)
- PR header with stats (+add/-del files)
- AI review summary section
- File-by-file accordion (expandable per file)
- Inline code comments with AI suggestion chips
- Verdict badge: Approved ✓ | Changes Requested ✗ | Neutral ○
- Copy review | Post to GitHub/GitLab action (with confirmation)

## AI PR Review Pipeline
```
PRDiff (file diffs)
    ↓ per-file chunk analysis
    ↓ RAGRepository.retrieve(file context)
    ↓ ReviewPrompt(diff + code context + standards)
    ↓ AIProvider.streamChat()
    ↓ parse inline comments + verdict
    ↓ PRReview model
```

## Acceptance Criteria
- [ ] Issues sync from GitHub within 30s of creation
- [ ] Issues sync from GitLab within 30s of creation
- [ ] AI triage badge shows on issue list
- [ ] Commit history paginated and correct
- [ ] PR list shows correct CI and review status
- [ ] AI PR review generates per-file comments
- [ ] Posting review requires confirmation
- [ ] All screens: Loading/Success/Empty/Error states
- [ ] All lists paginated (no full load in memory)
