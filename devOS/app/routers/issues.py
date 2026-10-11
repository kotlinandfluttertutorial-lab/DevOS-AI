"""Issues router — DA-129.

Proxy endpoints for repository issues (GitHub / GitLab).

All endpoints require X-VCS-Token and X-VCS-Provider headers.

Route map
---------
GET  /v1/issues/{owner}/{repo}                  ?state=open&page=1&page_size=20
GET  /v1/issues/{owner}/{repo}/{issue_number}
POST /v1/issues/{owner}/{repo}
"""
import logging

from fastapi import APIRouter, Header, HTTPException, Query, status

from app.models.git import (
    Issue,
    IssueCreateRequest,
    IssueListResponse,
    IssueState,
    VCSProvider,
)
from app.services.vcs_service import VCSService

logger = logging.getLogger("devos.routers.issues")

router = APIRouter(tags=["issues"])


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _get_vcs_service(vcs_token: str, vcs_provider: str) -> VCSService:
    """Validate headers and return a configured VCSService."""
    if not vcs_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="X-VCS-Token header is required",
        )
    try:
        provider = VCSProvider(vcs_provider.lower())
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unknown VCS provider '{vcs_provider}'. Supported: github, gitlab, bitbucket",
        )
    logger.debug("VCS request: provider=%s has_token=%s", provider, bool(vcs_token))
    return VCSService(provider=provider, token=vcs_token)


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------


@router.get(
    "/issues/{owner}/{repo}",
    response_model=IssueListResponse,
    summary="List repository issues",
)
async def list_issues(
    owner: str,
    repo: str,
    state: IssueState = Query(default=IssueState.OPEN, description="Filter by issue state"),
    page: int = Query(default=1, ge=1, description="1-based page number"),
    page_size: int = Query(default=20, ge=1, le=100, description="Items per page"),
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> IssueListResponse:
    """
    Return a paginated list of issues.  Supports filtering by state (open/closed/all).
    Falls back to stub data in demo mode (no token / auth failure).
    """
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.list_issues(owner, repo, state=state, page=page, per_page=page_size)


@router.get(
    "/issues/{owner}/{repo}/{issue_number}",
    response_model=Issue,
    summary="Get a single issue",
)
async def get_issue(
    owner: str,
    repo: str,
    issue_number: int,
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> Issue:
    """Return a single issue by number."""
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.get_issue(owner, repo, issue_number)


@router.post(
    "/issues/{owner}/{repo}",
    response_model=Issue,
    status_code=status.HTTP_201_CREATED,
    summary="Create an issue",
)
async def create_issue(
    owner: str,
    repo: str,
    body: IssueCreateRequest,
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> Issue:
    """
    Create a new issue.

    NOTE: This endpoint is a stub implementation — it validates the request
    and returns a synthetic issue with number 0.  Full proxy implementation
    will be added in a follow-up ticket once write-scope VCS token handling
    is designed.
    """
    _get_vcs_service(x_vcs_token, x_vcs_provider)  # validate headers

    from datetime import datetime, timezone
    from app.models.git import IssueLabel

    now = datetime.now(tz=timezone.utc)
    stub_labels = [
        IssueLabel(name=lbl, color="cccccc") for lbl in body.labels
    ]
    return Issue(
        id=0,
        number=0,
        title=body.title,
        body=body.body,
        state=IssueState.OPEN,
        labels=stub_labels,
        assignees=body.assignees,
        author="devos-user",
        created_at=now,
        updated_at=now,
        comments_count=0,
        url=f"https://github.com/{owner}/{repo}/issues/0",
    )
