"""Git, Issues, and Pull Request domain models — DA-129."""
from enum import Enum
from typing import Optional
from datetime import datetime

from pydantic import BaseModel


# ---------------------------------------------------------------------------
# VCS provider
# ---------------------------------------------------------------------------


class VCSProvider(str, Enum):
    GITHUB = "github"
    GITLAB = "gitlab"
    BITBUCKET = "bitbucket"


# ---------------------------------------------------------------------------
# Git objects
# ---------------------------------------------------------------------------


class GitAuthor(BaseModel):
    name: str
    email: str
    date: datetime


class GitCommit(BaseModel):
    sha: str
    message: str
    author: GitAuthor
    committer: GitAuthor
    url: Optional[str] = None
    stats: Optional[dict] = None  # {additions, deletions, total}


class GitBranch(BaseModel):
    name: str
    sha: str
    is_default: bool = False
    is_protected: bool = False
    ahead_by: int = 0
    behind_by: int = 0


class GitDiff(BaseModel):
    base: str
    head: str
    files_changed: int
    additions: int
    deletions: int
    patches: list[dict]  # [{filename, status, additions, deletions, patch}]


class GitStats(BaseModel):
    """Aggregated repository statistics."""
    total_commits: int = 0
    contributors_count: int = 0
    # Commit frequency keyed by YYYY-MM-DD (last 30 days)
    commit_frequency: dict[str, int] = {}
    # Top languages keyed by language name, value = percentage (0-100)
    languages: dict[str, float] = {}


# ---------------------------------------------------------------------------
# Issues
# ---------------------------------------------------------------------------


class IssueState(str, Enum):
    OPEN = "open"
    CLOSED = "closed"
    ALL = "all"


class IssueLabel(BaseModel):
    name: str
    color: str
    description: Optional[str] = None


class Issue(BaseModel):
    id: int
    number: int
    title: str
    body: Optional[str] = None
    state: IssueState
    labels: list[IssueLabel] = []
    assignees: list[str] = []
    author: str
    created_at: datetime
    updated_at: datetime
    closed_at: Optional[datetime] = None
    comments_count: int = 0
    url: str


class IssueListResponse(BaseModel):
    items: list[Issue]
    total: int
    page: int
    page_size: int


# Issues — create request
class IssueCreateRequest(BaseModel):
    title: str
    body: str = ""
    labels: list[str] = []
    assignees: list[str] = []


# ---------------------------------------------------------------------------
# Pull Requests
# ---------------------------------------------------------------------------


class PRState(str, Enum):
    OPEN = "open"
    CLOSED = "closed"
    MERGED = "merged"
    ALL = "all"


class PRReviewState(str, Enum):
    PENDING = "pending"
    APPROVED = "approved"
    CHANGES_REQUESTED = "changes_requested"
    COMMENTED = "commented"


class PRReview(BaseModel):
    reviewer: str
    state: PRReviewState
    body: Optional[str] = None
    submitted_at: Optional[datetime] = None


class PullRequest(BaseModel):
    id: int
    number: int
    title: str
    body: Optional[str] = None
    state: PRState
    base_branch: str
    head_branch: str
    author: str
    reviewers: list[str] = []
    reviews: list[PRReview] = []
    labels: list[IssueLabel] = []
    created_at: datetime
    updated_at: datetime
    merged_at: Optional[datetime] = None
    additions: int = 0
    deletions: int = 0
    changed_files: int = 0
    url: str
    is_draft: bool = False


class PRListResponse(BaseModel):
    items: list[PullRequest]
    total: int
    page: int
    page_size: int


class PRCreateRequest(BaseModel):
    title: str
    body: str
    head_branch: str
    base_branch: str = "main"
    draft: bool = False
    reviewers: list[str] = []


class PRComment(BaseModel):
    id: int
    author: str
    body: str
    created_at: datetime
    updated_at: datetime
    path: Optional[str] = None   # inline review comment: file path
    line: Optional[int] = None   # inline review comment: line number
