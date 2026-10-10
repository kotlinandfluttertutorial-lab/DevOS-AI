"""Developer Intelligence API tests — DA-129.

Tests use the synchronous TestClient.  All VCS service calls are patched with
pytest-mock so the tests never make real network requests.

Conventions
-----------
- Stub mode means: a non-empty token that causes the service to return stub
  data (simulated by patching VCSService methods directly).
- 401 mode means: the X-VCS-Token header is absent.
"""
import pytest
from fastapi.testclient import TestClient
from unittest.mock import AsyncMock, patch

from app.main import app
from app.models.git import (
    GitBranch,
    GitCommit,
    GitAuthor,
    GitDiff,
    GitStats,
    Issue,
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
)
from datetime import datetime, timezone

client = TestClient(app)

# ---------------------------------------------------------------------------
# Shared fixture data
# ---------------------------------------------------------------------------

_NOW = datetime(2024, 6, 1, 12, 0, 0, tzinfo=timezone.utc)
_AUTHOR = {"name": "Dev", "email": "dev@test.com", "date": _NOW.isoformat()}
_STUB_TOKEN = "ghp_stub_token_for_tests"
_OWNER = "test-owner"
_REPO = "test-repo"

_STUB_COMMIT = GitCommit(
    sha="a" * 40,
    message="feat: stub",
    author=GitAuthor(**_AUTHOR),
    committer=GitAuthor(**_AUTHOR),
    url="https://github.com/test-owner/test-repo/commit/" + "a" * 40,
    stats={"additions": 5, "deletions": 2, "total": 7},
)

_STUB_BRANCH = GitBranch(name="main", sha="b" * 40, is_default=True)

_STUB_ISSUE = Issue(
    id=1,
    number=1,
    title="Stub issue",
    body="body",
    state=IssueState.OPEN,
    labels=[IssueLabel(name="bug", color="d73a4a")],
    assignees=[],
    author="demo-user",
    created_at=_NOW,
    updated_at=_NOW,
    comments_count=0,
    url="https://github.com/test-owner/test-repo/issues/1",
)

_STUB_PR = PullRequest(
    id=1,
    number=1,
    title="Stub PR",
    body="body",
    state=PRState.OPEN,
    base_branch="main",
    head_branch="feature/stub",
    author="demo-user",
    reviewers=[],
    reviews=[],
    labels=[],
    created_at=_NOW,
    updated_at=_NOW,
    additions=10,
    deletions=3,
    changed_files=2,
    url="https://github.com/test-owner/test-repo/pull/1",
)

_STUB_DIFF = GitDiff(
    base="main",
    head="feature/stub",
    files_changed=2,
    additions=42,
    deletions=10,
    patches=[],
)


# ---------------------------------------------------------------------------
# Git — commits
# ---------------------------------------------------------------------------


def test_list_commits_no_token_returns_401():
    """Missing X-VCS-Token must return 401."""
    resp = client.get(f"/v1/git/{_OWNER}/{_REPO}/commits")
    assert resp.status_code == 401
    assert "X-VCS-Token" in resp.json()["detail"]


def test_list_commits_stub_mode():
    """With a stub token the endpoint returns a list of commit objects."""
    with patch(
        "app.services.vcs_service.VCSService.list_commits",
        new=AsyncMock(return_value=[_STUB_COMMIT]),
    ):
        resp = client.get(
            f"/v1/git/{_OWNER}/{_REPO}/commits",
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    data = resp.json()
    assert isinstance(data, list)
    assert len(data) == 1
    assert data[0]["sha"] == "a" * 40
    assert data[0]["message"] == "feat: stub"


def test_list_commits_invalid_provider_returns_400():
    """An unrecognised provider value must return 400."""
    resp = client.get(
        f"/v1/git/{_OWNER}/{_REPO}/commits",
        headers={"X-VCS-Token": _STUB_TOKEN, "X-VCS-Provider": "notaplatform"},
    )
    assert resp.status_code == 400


# ---------------------------------------------------------------------------
# Git — branches
# ---------------------------------------------------------------------------


def test_list_branches_stub_mode():
    """With a stub token the endpoint returns a list of branch objects."""
    with patch(
        "app.services.vcs_service.VCSService.list_branches",
        new=AsyncMock(return_value=[_STUB_BRANCH]),
    ):
        resp = client.get(
            f"/v1/git/{_OWNER}/{_REPO}/branches",
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    data = resp.json()
    assert isinstance(data, list)
    assert data[0]["name"] == "main"
    assert data[0]["is_default"] is True


def test_list_branches_no_token_returns_401():
    resp = client.get(f"/v1/git/{_OWNER}/{_REPO}/branches")
    assert resp.status_code == 401


# ---------------------------------------------------------------------------
# Git — diff
# ---------------------------------------------------------------------------


def test_get_diff_stub_mode():
    with patch(
        "app.services.vcs_service.VCSService.get_diff",
        new=AsyncMock(return_value=_STUB_DIFF),
    ):
        resp = client.get(
            f"/v1/git/{_OWNER}/{_REPO}/diff",
            params={"base": "main", "head": "feature/stub"},
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    data = resp.json()
    assert data["base"] == "main"
    assert data["head"] == "feature/stub"
    assert data["additions"] == 42


def test_get_diff_missing_params_returns_422():
    """Both base and head are required query params."""
    resp = client.get(
        f"/v1/git/{_OWNER}/{_REPO}/diff",
        headers={"X-VCS-Token": _STUB_TOKEN},
    )
    assert resp.status_code == 422


# ---------------------------------------------------------------------------
# Git — stats
# ---------------------------------------------------------------------------


def test_get_stats_stub_mode():
    stub_stats = GitStats(
        total_commits=100,
        contributors_count=3,
        commit_frequency={"2024-06-01": 2},
        languages={"Kotlin": 80.0},
    )
    with patch(
        "app.services.vcs_service.VCSService.get_stats",
        new=AsyncMock(return_value=stub_stats),
    ):
        resp = client.get(
            f"/v1/git/{_OWNER}/{_REPO}/stats",
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    data = resp.json()
    assert data["total_commits"] == 100
    assert "Kotlin" in data["languages"]


# ---------------------------------------------------------------------------
# Issues
# ---------------------------------------------------------------------------


def test_list_issues_no_token_returns_401():
    resp = client.get(f"/v1/issues/{_OWNER}/{_REPO}")
    assert resp.status_code == 401


def test_list_issues_stub_mode():
    stub_response = IssueListResponse(
        items=[_STUB_ISSUE], total=1, page=1, page_size=20
    )
    with patch(
        "app.services.vcs_service.VCSService.list_issues",
        new=AsyncMock(return_value=stub_response),
    ):
        resp = client.get(
            f"/v1/issues/{_OWNER}/{_REPO}",
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    data = resp.json()
    assert data["total"] == 1
    assert data["items"][0]["title"] == "Stub issue"
    assert data["items"][0]["state"] == "open"


def test_get_issue_stub_mode():
    with patch(
        "app.services.vcs_service.VCSService.get_issue",
        new=AsyncMock(return_value=_STUB_ISSUE),
    ):
        resp = client.get(
            f"/v1/issues/{_OWNER}/{_REPO}/1",
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    assert resp.json()["number"] == 1


def test_create_issue_stub_mode():
    """POST /issues returns 201 with a stub issue."""
    payload = {"title": "New issue", "body": "Details here", "labels": [], "assignees": []}
    resp = client.post(
        f"/v1/issues/{_OWNER}/{_REPO}",
        json=payload,
        headers={"X-VCS-Token": _STUB_TOKEN},
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data["title"] == "New issue"
    assert data["state"] == "open"


# ---------------------------------------------------------------------------
# Pull Requests
# ---------------------------------------------------------------------------


def test_list_prs_no_token_returns_401():
    resp = client.get(f"/v1/prs/{_OWNER}/{_REPO}")
    assert resp.status_code == 401


def test_list_prs_stub_mode():
    stub_response = PRListResponse(
        items=[_STUB_PR], total=1, page=1, page_size=20
    )
    with patch(
        "app.services.vcs_service.VCSService.list_prs",
        new=AsyncMock(return_value=stub_response),
    ):
        resp = client.get(
            f"/v1/prs/{_OWNER}/{_REPO}",
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    data = resp.json()
    assert data["total"] == 1
    assert data["items"][0]["title"] == "Stub PR"
    assert data["items"][0]["state"] == "open"


def test_get_pr_stub_mode():
    with patch(
        "app.services.vcs_service.VCSService.get_pr",
        new=AsyncMock(return_value=_STUB_PR),
    ):
        resp = client.get(
            f"/v1/prs/{_OWNER}/{_REPO}/1",
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    assert resp.json()["number"] == 1


def test_create_pr_stub_mode():
    """POST /prs returns 201 with the created PR."""
    created_pr = _STUB_PR.model_copy(
        update={"title": "New PR", "head_branch": "feature/new", "number": 999}
    )
    with patch(
        "app.services.vcs_service.VCSService.create_pr",
        new=AsyncMock(return_value=created_pr),
    ):
        payload = {
            "title": "New PR",
            "body": "PR body",
            "head_branch": "feature/new",
            "base_branch": "main",
            "draft": False,
            "reviewers": [],
        }
        resp = client.post(
            f"/v1/prs/{_OWNER}/{_REPO}",
            json=payload,
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 201
    data = resp.json()
    assert data["title"] == "New PR"
    assert data["number"] == 999


def test_list_pr_comments_stub_mode():
    stub_comment = PRComment(
        id=1,
        author="reviewer",
        body="Looks good!",
        created_at=_NOW,
        updated_at=_NOW,
    )
    with patch(
        "app.services.vcs_service.VCSService.list_pr_comments",
        new=AsyncMock(return_value=[stub_comment]),
    ):
        resp = client.get(
            f"/v1/prs/{_OWNER}/{_REPO}/1/comments",
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 1
    assert data[0]["body"] == "Looks good!"


def test_get_pr_diff_stub_mode():
    with patch(
        "app.services.vcs_service.VCSService.get_pr",
        new=AsyncMock(return_value=_STUB_PR),
    ), patch(
        "app.services.vcs_service.VCSService.get_diff",
        new=AsyncMock(return_value=_STUB_DIFF),
    ):
        resp = client.get(
            f"/v1/prs/{_OWNER}/{_REPO}/1/diff",
            headers={"X-VCS-Token": _STUB_TOKEN},
        )
    assert resp.status_code == 200
    data = resp.json()
    assert data["files_changed"] == 2
    assert data["additions"] == 42
