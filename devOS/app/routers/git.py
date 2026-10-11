"""Git router — DA-129.

Proxy endpoints for git history, branches, diffs, and repo statistics.
All endpoints require:
  - X-VCS-Token  : caller's VCS access token (forwarded to GitHub/GitLab, never stored)
  - X-VCS-Provider : "github" | "gitlab" | "bitbucket"  (default: "github")

Route map
---------
GET  /v1/git/{owner}/{repo}/commits   ?branch=main&page=1&page_size=20
GET  /v1/git/{owner}/{repo}/branches
GET  /v1/git/{owner}/{repo}/diff      ?base=main&head=feature/xyz
GET  /v1/git/{owner}/{repo}/stats
"""
import logging

from fastapi import APIRouter, Header, HTTPException, Query, status

from app.models.git import (
    GitBranch,
    GitCommit,
    GitDiff,
    GitStats,
    VCSProvider,
)
from app.services.vcs_service import VCSService

logger = logging.getLogger("devos.routers.git")

router = APIRouter(tags=["git"])


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _get_vcs_service(vcs_token: str, vcs_provider: str) -> VCSService:
    """Validate headers and return a configured VCSService.

    SECURITY: token value is never logged — only its boolean presence is.
    """
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
    "/git/{owner}/{repo}/commits",
    response_model=list[GitCommit],
    summary="List repository commits",
)
async def list_commits(
    owner: str,
    repo: str,
    branch: str = Query(default="main", description="Branch name or commit SHA"),
    page: int = Query(default=1, ge=1, description="1-based page number"),
    page_size: int = Query(default=20, ge=1, le=100, description="Items per page"),
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> list[GitCommit]:
    """
    Return a paginated list of commits for *branch*.

    When no token is supplied or the upstream API rejects the token the
    endpoint returns deterministic stub data so the Android client can operate
    in demo mode.
    """
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.list_commits(owner, repo, branch=branch, page=page, per_page=page_size)


@router.get(
    "/git/{owner}/{repo}/branches",
    response_model=list[GitBranch],
    summary="List repository branches",
)
async def list_branches(
    owner: str,
    repo: str,
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> list[GitBranch]:
    """Return all branches for the repository."""
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.list_branches(owner, repo)


@router.get(
    "/git/{owner}/{repo}/diff",
    response_model=GitDiff,
    summary="Get diff between two refs",
)
async def get_diff(
    owner: str,
    repo: str,
    base: str = Query(..., description="Base ref (branch name, tag, or SHA)"),
    head: str = Query(..., description="Head ref to compare against base"),
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> GitDiff:
    """Return the diff (file patches, additions, deletions) between *base* and *head*."""
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.get_diff(owner, repo, base=base, head=head)


@router.get(
    "/git/{owner}/{repo}/stats",
    response_model=GitStats,
    summary="Repository statistics",
)
async def get_stats(
    owner: str,
    repo: str,
    x_vcs_token: str = Header(default="", alias="X-VCS-Token"),
    x_vcs_provider: str = Header(default="github", alias="X-VCS-Provider"),
) -> GitStats:
    """
    Return aggregated repository statistics: commit frequency, contributors,
    and language breakdown.
    """
    svc = _get_vcs_service(x_vcs_token, x_vcs_provider)
    return await svc.get_stats(owner, repo)
