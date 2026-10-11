"""Pull Requests router — DA-129.

Proxy endpoints for repository pull requests (GitHub / GitLab).

All endpoints require X-VCS-Token and X-VCS-Provider headers.

Route map
---------
GET  /v1/prs/{owner}/{repo}                             ?state=open&page=1&page_size=20
GET  /v1/prs/{owner}/{repo}/{pr_number}
POST /v1/prs/{owner}/{repo}
GET  /v1/prs/{owner}/{repo}/{pr_number}/comments
GET  /v1/prs/{owner}/{repo}/{pr_number}/diff
"""
import logging

from fastapi import APIRouter, Header, HTTPException, Query, status

from app.models.git import (
    GitDiff,
    PRComment,
    PRCreateRequest,
    PRListResponse,
    PRState,
    PullRequest,
    VCSProvider,
)
from app.services.vcs_service import VCSService

logger = logging.getLogger("devos.routers.prs")

router = APIRouter(tags=["pull-requests"])


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
    "/prs/{owner}/{repo}",
    response_model=PRListResponse,
    summary="List pull requests",
)
async def list_prs(
    owner: str,
    repo: str,
    state: PRState = Query(default=PRState.OPEN, description="Filter by PR state"),
    page: int = Query(default=1, ge=1, description="1-based page number"),
    page_size: int = Query(default=20, ge=1, le=100, description="Items per page"),
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> PRListResponse:
    """
    Return a paginated list of pull requests.
    Supports state filtering: open / closed / merged / all.
    Falls back to stub data in demo mode.
    """
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.list_prs(owner, repo, state=state, page=page, per_page=page_size)


@router.get(
    "/prs/{owner}/{repo}/{pr_number}",
    response_model=PullRequest,
    summary="Get a single pull request",
)
async def get_pr(
    owner: str,
    repo: str,
    pr_number: int,
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> PullRequest:
    """Return a single pull request by number, including review details."""
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.get_pr(owner, repo, pr_number)


@router.post(
    "/prs/{owner}/{repo}",
    response_model=PullRequest,
    status_code=status.HTTP_201_CREATED,
    summary="Create a pull request",
)
async def create_pr(
    owner: str,
    repo: str,
    body: PRCreateRequest,
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> PullRequest:
    """
    Create a new pull request.

    In demo mode (no token / auth failure) returns a stub PR with the
    requested title and branch names.  Requires write-scope VCS token for
    real PR creation.
    """
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.create_pr(owner, repo, body)


@router.get(
    "/prs/{owner}/{repo}/{pr_number}/comments",
    response_model=list[PRComment],
    summary="List PR review comments",
)
async def list_pr_comments(
    owner: str,
    repo: str,
    pr_number: int,
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> list[PRComment]:
    """Return all inline review comments on a pull request."""
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.list_pr_comments(owner, repo, pr_number)


@router.get(
    "/prs/{owner}/{repo}/{pr_number}/diff",
    response_model=GitDiff,
    summary="Get PR file changes (diff)",
)
async def get_pr_diff(
    owner: str,
    repo: str,
    pr_number: int,
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> GitDiff:
    """
    Return the file-level diff for a pull request.

    Fetches the PR to obtain base/head SHAs then delegates to the compare
    endpoint.  Falls back to stub diff in demo mode.
    """
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    pr = await svc.get_pr(owner, repo, pr_number)
    return await svc.get_diff(owner, repo, base=pr.base_branch, head=pr.head_branch)
