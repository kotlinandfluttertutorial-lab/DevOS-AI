"""VCS proxy service — DA-129.

Proxies GitHub / GitLab APIs using a caller-supplied token.  The token is
used only for the outbound HTTP request and is never stored or logged.

When the token is absent, empty, or the upstream API returns 401 / 403 the
methods fall back to deterministic stub data so the Android app can operate
in demo mode without real credentials.
"""
import logging
from datetime import datetime, timedelta, timezone

import httpx

from app.models.git import (
    GitAuthor,
    GitBranch,
    GitCommit,
    GitDiff,
    GitStats,
    Issue,
    IssueCreateRequest,
    IssueLabel,
    IssueListResponse,
    IssueState,
    PRComment,
    PRCreateRequest,
    PRListResponse,
    PRReview,
    PRReviewState,
    PRState,
    PullRequest,
    VCSProvider,
)

logger = logging.getLogger("devos.vcs")

# ---------------------------------------------------------------------------
# Stub data factory helpers — deterministic, owner/repo-scoped
# ---------------------------------------------------------------------------

_NOW = datetime(2024, 6, 1, 12, 0, 0, tzinfo=timezone.utc)
_STUB_AUTHOR = GitAuthor(name="Demo Dev", email="demo@devos.ai", date=_NOW)


def _stub_commit(n: int, owner: str, repo: str) -> GitCommit:
    return GitCommit(
        sha=f"a{n:040x}",
        message=f"feat: stub commit #{n} for {owner}/{repo}",
        author=_STUB_AUTHOR,
        committer=_STUB_AUTHOR,
        url=f"https://github.com/{owner}/{repo}/commit/a{n:040x}",
        stats={"additions": n * 5, "deletions": n * 2, "total": n * 7},
    )


def _stub_branch(name: str, default: bool = False) -> GitBranch:
    return GitBranch(
        name=name,
        sha="b" + "0" * 39,
        is_default=default,
        is_protected=default,
    )


def _stub_issue(n: int, owner: str, repo: str) -> Issue:
    return Issue(
        id=n,
        number=n,
        title=f"Stub issue #{n}: sample bug report",
        body="This is a sample issue body for demo mode.",
        state=IssueState.OPEN,
        labels=[IssueLabel(name="bug", color="d73a4a")],
        assignees=[],
        author="demo-user",
        created_at=_NOW - timedelta(days=n),
        updated_at=_NOW - timedelta(hours=n),
        comments_count=n % 5,
        url=f"https://github.com/{owner}/{repo}/issues/{n}",
    )


def _stub_pr(n: int, owner: str, repo: str) -> PullRequest:
    return PullRequest(
        id=n,
        number=n,
        title=f"Stub PR #{n}: sample feature branch",
        body="Sample PR description for demo mode.",
        state=PRState.OPEN,
        base_branch="main",
        head_branch=f"feature/stub-{n}",
        author="demo-user",
        reviewers=["reviewer-1"],
        reviews=[
            PRReview(
                reviewer="reviewer-1",
                state=PRReviewState.PENDING,
                submitted_at=_NOW,
            )
        ],
        labels=[IssueLabel(name="enhancement", color="a2eeef")],
        created_at=_NOW - timedelta(days=n),
        updated_at=_NOW - timedelta(hours=n),
        additions=n * 10,
        deletions=n * 3,
        changed_files=n,
        url=f"https://github.com/{owner}/{repo}/pull/{n}",
    )


def _stub_pr_comment(n: int) -> PRComment:
    return PRComment(
        id=n,
        author="reviewer-1",
        body=f"Stub comment #{n}: looks good.",
        created_at=_NOW,
        updated_at=_NOW,
    )


# ---------------------------------------------------------------------------
# VCSService
# ---------------------------------------------------------------------------


class VCSService:
    """
    Thin async proxy over GitHub / GitLab REST APIs.

    The caller-supplied token is forwarded in the outbound request header and
    is NEVER persisted, logged, or returned in any response.
    """

    def __init__(self, provider: VCSProvider, token: str) -> None:
        self.provider = provider
        # SECURITY: store token as private attribute, never expose in repr/logs
        self._token = token
        self._base_url = self._get_base_url()

    def __repr__(self) -> str:  # pragma: no cover
        return f"VCSService(provider={self.provider}, has_token={bool(self._token)})"

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _get_base_url(self) -> str:
        if self.provider == VCSProvider.GITLAB:
            return "https://gitlab.com/api/v4"
        if self.provider == VCSProvider.BITBUCKET:
            return "https://api.bitbucket.org/2.0"
        return "https://api.github.com"  # default / GITHUB

    def _get_headers(self) -> dict:
        # SECURITY: token value goes into headers only — not logged
        if self.provider == VCSProvider.GITLAB:
            return {"PRIVATE-TOKEN": self._token}
        # GitHub + Bitbucket use Bearer
        return {
            "Authorization": f"Bearer {self._token}",
            "Accept": "application/vnd.github.v3+json",
            "X-GitHub-Api-Version": "2022-11-28",
        }

    def _is_auth_error(self, status_code: int) -> bool:
        return status_code in (401, 403)

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    async def list_commits(
        self,
        owner: str,
        repo: str,
        branch: str = "main",
        page: int = 1,
        per_page: int = 20,
    ) -> list[GitCommit]:
        """List commits for *branch*.  Falls back to stub data on auth failure."""
        if not self._token:
            logger.debug("list_commits: no token — returning stub data (has_token=False)")
            return [_stub_commit(i, owner, repo) for i in range(1, per_page + 1)]

        url = f"{self._base_url}/repos/{owner}/{repo}/commits"
        params = {"sha": branch, "page": page, "per_page": per_page}

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(url, headers=self._get_headers(), params=params)
        except httpx.RequestError as exc:
            logger.warning("list_commits: network error — %s — returning stub data", exc)
            return [_stub_commit(i, owner, repo) for i in range(1, per_page + 1)]

        if self._is_auth_error(resp.status_code):
            logger.info(
                "list_commits: upstream auth error %d (has_token=%s) — returning stub data",
                resp.status_code,
                bool(self._token),  # SECURITY: log boolean, not the token value
            )
            return [_stub_commit(i, owner, repo) for i in range(1, per_page + 1)]

        resp.raise_for_status()
        commits = []
        for raw in resp.json():
            commit_data = raw.get("commit", {})
            author_data = commit_data.get("author") or {}
            committer_data = commit_data.get("committer") or {}

            def _parse_author(d: dict) -> GitAuthor:
                return GitAuthor(
                    name=d.get("name", "unknown"),
                    email=d.get("email", ""),
                    date=d.get("date", _NOW.isoformat()),
                )

            stats = raw.get("stats")
            commits.append(
                GitCommit(
                    sha=raw.get("sha", ""),
                    message=commit_data.get("message", ""),
                    author=_parse_author(author_data),
                    committer=_parse_author(committer_data),
                    url=raw.get("html_url"),
                    stats=stats,
                )
            )
        return commits

    async def get_diff(self, owner: str, repo: str, base: str, head: str) -> GitDiff:
        """Return diff between *base* and *head*.  Falls back to stub on failure."""
        if not self._token:
            return GitDiff(
                base=base,
                head=head,
                files_changed=2,
                additions=42,
                deletions=10,
                patches=[
                    {
                        "filename": "README.md",
                        "status": "modified",
                        "additions": 20,
                        "deletions": 5,
                        "patch": "@@ -1,5 +1,20 @@\n-old line\n+new line",
                    },
                    {
                        "filename": "app/main.py",
                        "status": "modified",
                        "additions": 22,
                        "deletions": 5,
                        "patch": "@@ -10,5 +10,22 @@\n-old code\n+new code",
                    },
                ],
            )

        url = f"{self._base_url}/repos/{owner}/{repo}/compare/{base}...{head}"
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.get(url, headers=self._get_headers())
        except httpx.RequestError as exc:
            logger.warning("get_diff: network error — %s", exc)
            return GitDiff(base=base, head=head, files_changed=0, additions=0, deletions=0, patches=[])

        if self._is_auth_error(resp.status_code):
            logger.info("get_diff: upstream auth error %d (has_token=%s)", resp.status_code, bool(self._token))
            return GitDiff(base=base, head=head, files_changed=0, additions=0, deletions=0, patches=[])

        resp.raise_for_status()
        data = resp.json()
        files = data.get("files", [])
        patches = [
            {
                "filename": f.get("filename", ""),
                "status": f.get("status", ""),
                "additions": f.get("additions", 0),
                "deletions": f.get("deletions", 0),
                "patch": f.get("patch", ""),
            }
            for f in files
        ]
        return GitDiff(
            base=base,
            head=head,
            files_changed=data.get("total_commits", len(files)),
            additions=sum(f.get("additions", 0) for f in files),
            deletions=sum(f.get("deletions", 0) for f in files),
            patches=patches,
        )

    async def list_branches(self, owner: str, repo: str) -> list[GitBranch]:
        """List branches.  Falls back to stub on failure."""
        if not self._token:
            return [
                _stub_branch("main", default=True),
                _stub_branch("develop"),
                _stub_branch("feature/stub-branch"),
            ]

        url = f"{self._base_url}/repos/{owner}/{repo}/branches"
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(url, headers=self._get_headers())
        except httpx.RequestError as exc:
            logger.warning("list_branches: network error — %s", exc)
            return [_stub_branch("main", default=True)]

        if self._is_auth_error(resp.status_code):
            logger.info("list_branches: upstream auth error %d (has_token=%s)", resp.status_code, bool(self._token))
            return [_stub_branch("main", default=True), _stub_branch("develop")]

        resp.raise_for_status()
        branches = []
        for raw in resp.json():
            branches.append(
                GitBranch(
                    name=raw.get("name", ""),
                    sha=raw.get("commit", {}).get("sha", "0" * 40),
                    is_protected=raw.get("protected", False),
                )
            )
        return branches

    async def get_stats(self, owner: str, repo: str) -> GitStats:
        """Return aggregated repo statistics (stub-only for now)."""
        # NOTE: GitHub stats endpoints are async (202 while computing).
        # Full implementation deferred; returning deterministic stub.
        freq: dict[str, int] = {}
        base_date = _NOW.date()
        for i in range(30):
            day = base_date - timedelta(days=i)
            freq[day.isoformat()] = (i % 5) + 1

        return GitStats(
            total_commits=342,
            contributors_count=4,
            commit_frequency=freq,
            languages={"Kotlin": 72.5, "Python": 18.3, "Shell": 5.2, "Other": 4.0},
        )

    async def list_issues(
        self,
        owner: str,
        repo: str,
        state: IssueState = IssueState.OPEN,
        page: int = 1,
        per_page: int = 20,
    ) -> IssueListResponse:
        """List issues.  Falls back to stub on auth failure."""
        if not self._token:
            items = [_stub_issue(i, owner, repo) for i in range(1, per_page + 1)]
            return IssueListResponse(items=items, total=per_page, page=page, page_size=per_page)

        # GitHub: exclude PRs by filtering via GitHub issues endpoint
        url = f"{self._base_url}/repos/{owner}/{repo}/issues"
        state_param = state.value if state != IssueState.ALL else "all"
        params = {"state": state_param, "page": page, "per_page": per_page, "filter": "all"}

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(url, headers=self._get_headers(), params=params)
        except httpx.RequestError as exc:
            logger.warning("list_issues: network error — %s", exc)
            items = [_stub_issue(i, owner, repo) for i in range(1, per_page + 1)]
            return IssueListResponse(items=items, total=per_page, page=page, page_size=per_page)

        if self._is_auth_error(resp.status_code):
            logger.info("list_issues: upstream auth error %d (has_token=%s)", resp.status_code, bool(self._token))
            items = [_stub_issue(i, owner, repo) for i in range(1, per_page + 1)]
            return IssueListResponse(items=items, total=per_page, page=page, page_size=per_page)

        resp.raise_for_status()
        raw_list = [r for r in resp.json() if "pull_request" not in r]  # exclude PRs
        items = []
        for raw in raw_list:
            labels = [
                IssueLabel(
                    name=lbl.get("name", ""),
                    color=lbl.get("color", "000000"),
                    description=lbl.get("description"),
                )
                for lbl in raw.get("labels", [])
            ]
            items.append(
                Issue(
                    id=raw["id"],
                    number=raw["number"],
                    title=raw.get("title", ""),
                    body=raw.get("body"),
                    state=IssueState(raw.get("state", "open")),
                    labels=labels,
                    assignees=[a["login"] for a in raw.get("assignees", [])],
                    author=raw.get("user", {}).get("login", ""),
                    created_at=raw["created_at"],
                    updated_at=raw["updated_at"],
                    closed_at=raw.get("closed_at"),
                    comments_count=raw.get("comments", 0),
                    url=raw.get("html_url", ""),
                )
            )
        return IssueListResponse(items=items, total=len(items), page=page, page_size=per_page)

    async def get_issue(self, owner: str, repo: str, issue_number: int) -> Issue:
        """Get a single issue.  Falls back to stub on failure."""
        if not self._token:
            return _stub_issue(issue_number, owner, repo)

        url = f"{self._base_url}/repos/{owner}/{repo}/issues/{issue_number}"
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(url, headers=self._get_headers())
        except httpx.RequestError as exc:
            logger.warning("get_issue: network error — %s", exc)
            return _stub_issue(issue_number, owner, repo)

        if self._is_auth_error(resp.status_code):
            logger.info("get_issue: upstream auth error %d (has_token=%s)", resp.status_code, bool(self._token))
            return _stub_issue(issue_number, owner, repo)

        resp.raise_for_status()
        raw = resp.json()
        labels = [
            IssueLabel(
                name=lbl.get("name", ""),
                color=lbl.get("color", "000000"),
                description=lbl.get("description"),
            )
            for lbl in raw.get("labels", [])
        ]
        return Issue(
            id=raw["id"],
            number=raw["number"],
            title=raw.get("title", ""),
            body=raw.get("body"),
            state=IssueState(raw.get("state", "open")),
            labels=labels,
            assignees=[a["login"] for a in raw.get("assignees", [])],
            author=raw.get("user", {}).get("login", ""),
            created_at=raw["created_at"],
            updated_at=raw["updated_at"],
            closed_at=raw.get("closed_at"),
            comments_count=raw.get("comments", 0),
            url=raw.get("html_url", ""),
        )

    async def list_prs(
        self,
        owner: str,
        repo: str,
        state: PRState = PRState.OPEN,
        page: int = 1,
        per_page: int = 20,
    ) -> PRListResponse:
        """List pull requests.  Falls back to stub on failure."""
        if not self._token:
            items = [_stub_pr(i, owner, repo) for i in range(1, per_page + 1)]
            return PRListResponse(items=items, total=per_page, page=page, page_size=per_page)

        # GitHub merged PRs require state=closed; filter client-side by merged_at
        gh_state = "closed" if state == PRState.MERGED else (state.value if state != PRState.ALL else "all")
        url = f"{self._base_url}/repos/{owner}/{repo}/pulls"
        params = {"state": gh_state, "page": page, "per_page": per_page}

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(url, headers=self._get_headers(), params=params)
        except httpx.RequestError as exc:
            logger.warning("list_prs: network error — %s", exc)
            items = [_stub_pr(i, owner, repo) for i in range(1, per_page + 1)]
            return PRListResponse(items=items, total=per_page, page=page, page_size=per_page)

        if self._is_auth_error(resp.status_code):
            logger.info("list_prs: upstream auth error %d (has_token=%s)", resp.status_code, bool(self._token))
            items = [_stub_pr(i, owner, repo) for i in range(1, per_page + 1)]
            return PRListResponse(items=items, total=per_page, page=page, page_size=per_page)

        resp.raise_for_status()
        raw_list = resp.json()
        if state == PRState.MERGED:
            raw_list = [r for r in raw_list if r.get("merged_at")]

        items = []
        for raw in raw_list:
            labels = [
                IssueLabel(
                    name=lbl.get("name", ""),
                    color=lbl.get("color", "000000"),
                    description=lbl.get("description"),
                )
                for lbl in raw.get("labels", [])
            ]
            merged_at = raw.get("merged_at")
            pr_state = PRState.MERGED if merged_at else PRState(raw.get("state", "open"))
            items.append(
                PullRequest(
                    id=raw["id"],
                    number=raw["number"],
                    title=raw.get("title", ""),
                    body=raw.get("body"),
                    state=pr_state,
                    base_branch=raw.get("base", {}).get("ref", "main"),
                    head_branch=raw.get("head", {}).get("ref", ""),
                    author=raw.get("user", {}).get("login", ""),
                    reviewers=[r["login"] for r in raw.get("requested_reviewers", [])],
                    labels=labels,
                    created_at=raw["created_at"],
                    updated_at=raw["updated_at"],
                    merged_at=merged_at,
                    additions=raw.get("additions", 0),
                    deletions=raw.get("deletions", 0),
                    changed_files=raw.get("changed_files", 0),
                    url=raw.get("html_url", ""),
                    is_draft=raw.get("draft", False),
                )
            )
        return PRListResponse(items=items, total=len(items), page=page, page_size=per_page)

    async def get_pr(self, owner: str, repo: str, pr_number: int) -> PullRequest:
        """Get a single pull request.  Falls back to stub on failure."""
        if not self._token:
            return _stub_pr(pr_number, owner, repo)

        url = f"{self._base_url}/repos/{owner}/{repo}/pulls/{pr_number}"
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(url, headers=self._get_headers())
        except httpx.RequestError as exc:
            logger.warning("get_pr: network error — %s", exc)
            return _stub_pr(pr_number, owner, repo)

        if self._is_auth_error(resp.status_code):
            logger.info("get_pr: upstream auth error %d (has_token=%s)", resp.status_code, bool(self._token))
            return _stub_pr(pr_number, owner, repo)

        resp.raise_for_status()
        raw = resp.json()
        labels = [
            IssueLabel(
                name=lbl.get("name", ""),
                color=lbl.get("color", "000000"),
                description=lbl.get("description"),
            )
            for lbl in raw.get("labels", [])
        ]
        merged_at = raw.get("merged_at")
        pr_state = PRState.MERGED if merged_at else PRState(raw.get("state", "open"))

        # Fetch reviews separately
        reviews: list[PRReview] = []
        try:
            reviews_url = f"{self._base_url}/repos/{owner}/{repo}/pulls/{pr_number}/reviews"
            async with httpx.AsyncClient(timeout=10.0) as client:
                reviews_resp = await client.get(reviews_url, headers=self._get_headers())
            if reviews_resp.status_code == 200:
                for rev in reviews_resp.json():
                    try:
                        reviews.append(
                            PRReview(
                                reviewer=rev.get("user", {}).get("login", ""),
                                state=PRReviewState(rev.get("state", "pending").lower()),
                                body=rev.get("body"),
                                submitted_at=rev.get("submitted_at"),
                            )
                        )
                    except ValueError:
                        pass  # skip unknown review states
        except httpx.RequestError:
            pass  # reviews are optional enrichment

        return PullRequest(
            id=raw["id"],
            number=raw["number"],
            title=raw.get("title", ""),
            body=raw.get("body"),
            state=pr_state,
            base_branch=raw.get("base", {}).get("ref", "main"),
            head_branch=raw.get("head", {}).get("ref", ""),
            author=raw.get("user", {}).get("login", ""),
            reviewers=[r["login"] for r in raw.get("requested_reviewers", [])],
            reviews=reviews,
            labels=labels,
            created_at=raw["created_at"],
            updated_at=raw["updated_at"],
            merged_at=merged_at,
            additions=raw.get("additions", 0),
            deletions=raw.get("deletions", 0),
            changed_files=raw.get("changed_files", 0),
            url=raw.get("html_url", ""),
            is_draft=raw.get("draft", False),
        )

    async def create_pr(self, owner: str, repo: str, request: PRCreateRequest) -> PullRequest:
        """Create a pull request.  Returns a stub PR in demo mode."""
        if not self._token:
            pr = _stub_pr(999, owner, repo)
            return pr.model_copy(
                update={
                    "title": request.title,
                    "body": request.body,
                    "head_branch": request.head_branch,
                    "base_branch": request.base_branch,
                    "is_draft": request.draft,
                    "reviewers": request.reviewers,
                }
            )

        url = f"{self._base_url}/repos/{owner}/{repo}/pulls"
        payload = {
            "title": request.title,
            "body": request.body,
            "head": request.head_branch,
            "base": request.base_branch,
            "draft": request.draft,
        }
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.post(url, headers=self._get_headers(), json=payload)
        except httpx.RequestError as exc:
            logger.warning("create_pr: network error — %s", exc)
            raise

        if self._is_auth_error(resp.status_code):
            logger.info("create_pr: upstream auth error %d (has_token=%s)", resp.status_code, bool(self._token))
            pr = _stub_pr(999, owner, repo)
            return pr.model_copy(update={"title": request.title, "head_branch": request.head_branch})

        resp.raise_for_status()
        raw = resp.json()
        merged_at = raw.get("merged_at")
        pr_state = PRState.MERGED if merged_at else PRState(raw.get("state", "open"))
        return PullRequest(
            id=raw["id"],
            number=raw["number"],
            title=raw.get("title", ""),
            body=raw.get("body"),
            state=pr_state,
            base_branch=raw.get("base", {}).get("ref", "main"),
            head_branch=raw.get("head", {}).get("ref", ""),
            author=raw.get("user", {}).get("login", ""),
            reviewers=[r["login"] for r in raw.get("requested_reviewers", [])],
            labels=[],
            created_at=raw["created_at"],
            updated_at=raw["updated_at"],
            merged_at=merged_at,
            additions=raw.get("additions", 0),
            deletions=raw.get("deletions", 0),
            changed_files=raw.get("changed_files", 0),
            url=raw.get("html_url", ""),
            is_draft=raw.get("draft", False),
        )

    async def list_pr_comments(self, owner: str, repo: str, pr_number: int) -> list[PRComment]:
        """List review comments on a PR.  Falls back to stub on failure."""
        if not self._token:
            return [_stub_pr_comment(i) for i in range(1, 4)]

        url = f"{self._base_url}/repos/{owner}/{repo}/pulls/{pr_number}/comments"
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(url, headers=self._get_headers())
        except httpx.RequestError as exc:
            logger.warning("list_pr_comments: network error — %s", exc)
            return [_stub_pr_comment(i) for i in range(1, 4)]

        if self._is_auth_error(resp.status_code):
            logger.info(
                "list_pr_comments: upstream auth error %d (has_token=%s)",
                resp.status_code,
                bool(self._token),
            )
            return [_stub_pr_comment(i) for i in range(1, 4)]

        resp.raise_for_status()
        comments = []
        for raw in resp.json():
            comments.append(
                PRComment(
                    id=raw["id"],
                    author=raw.get("user", {}).get("login", ""),
                    body=raw.get("body", ""),
                    created_at=raw["created_at"],
                    updated_at=raw["updated_at"],
                    path=raw.get("path"),
                    line=raw.get("line"),
                )
            )
        return comments
