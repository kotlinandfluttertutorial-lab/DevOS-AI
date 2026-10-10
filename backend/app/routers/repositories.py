"""Repository CRUD + indexing router — DA-127.

All storage is currently in-memory (module-level dicts).  A later ticket
(DA-128 / Alembic migrations) will swap these for the SQLAlchemy ORM.

Route map
---------
GET  /v1/repositories                          — paginated list
POST /v1/repositories                          — create
GET  /v1/repositories/{repo_id}               — single record
PUT  /v1/repositories/{repo_id}               — update metadata
DEL  /v1/repositories/{repo_id}               — delete (204)
POST /v1/repositories/{repo_id}/index         — trigger async indexing (202)
GET  /v1/repositories/{repo_id}/index/status  — indexing progress
GET  /v1/repositories/{repo_id}/files         — file list (paginated)
GET  /v1/repositories/{repo_id}/files/content — file content
"""
import uuid
from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, HTTPException, Query, status
from fastapi.responses import JSONResponse

from app.models.common import ErrorResponse, PaginatedResponse
from app.models.repository import (
    IndexRequest,
    IndexStatus,
    IndexStatusResponse,
    RepositoryCreate,
    RepositoryResponse,
    RepositoryUpdate,
)

router = APIRouter()

# ---------------------------------------------------------------------------
# In-memory stores — replaced by DB-backed service in a later ticket
# ---------------------------------------------------------------------------
_repositories: dict[str, dict] = {}
_index_status: dict[str, dict] = {}

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

_NOT_FOUND = HTTPException(
    status_code=status.HTTP_404_NOT_FOUND,
    detail="Repository not found",
)


def _repo_to_response(data: dict) -> RepositoryResponse:
    return RepositoryResponse(**data)


# ---------------------------------------------------------------------------
# Stub file data returned by /files and /files/content
# ---------------------------------------------------------------------------
_STUB_FILES = [
    {"path": "README.md", "name": "README.md", "type": "file", "size": 1024},
    {"path": "src/main/kotlin/com/devos/Main.kt", "name": "Main.kt", "type": "file", "size": 512},
    {"path": "src/main/kotlin/com/devos/ui/HomeScreen.kt", "name": "HomeScreen.kt", "type": "file", "size": 2048},
    {"path": "src/test/kotlin/com/devos/MainTest.kt", "name": "MainTest.kt", "type": "file", "size": 256},
    {"path": "build.gradle.kts", "name": "build.gradle.kts", "type": "file", "size": 768},
]

_STUB_FILE_CONTENT = """\
// Stub file content — real content will be served from the indexed repository.
package com.devos

fun main() {
    println("DevOS AI — placeholder")
}
"""

# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------


@router.get(
    "/repositories",
    response_model=PaginatedResponse[RepositoryResponse],
    tags=["repositories"],
    summary="List repositories",
)
async def list_repositories(
    page: int = Query(default=1, ge=1, description="1-based page number"),
    page_size: int = Query(default=20, ge=1, le=100, description="Items per page"),
    source: Optional[str] = Query(default=None, description="Filter by source (github/gitlab/bitbucket/local)"),
) -> PaginatedResponse[RepositoryResponse]:
    """Return a paginated list of repositories, optionally filtered by source."""
    items = list(_repositories.values())
    if source:
        items = [r for r in items if r.get("source") == source]
    total = len(items)
    start = (page - 1) * page_size
    page_items = [_repo_to_response(r) for r in items[start : start + page_size]]
    return PaginatedResponse(
        items=page_items,
        total=total,
        page=page,
        page_size=page_size,
        has_next=(start + page_size) < total,
    )


@router.post(
    "/repositories",
    response_model=RepositoryResponse,
    status_code=status.HTTP_201_CREATED,
    tags=["repositories"],
    summary="Create repository",
)
async def create_repository(body: RepositoryCreate) -> RepositoryResponse:
    """Register a new repository record.  Indexing is not triggered automatically."""
    # Guard against duplicate full_name
    for existing in _repositories.values():
        if existing["full_name"] == body.full_name:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Repository '{body.full_name}' already exists",
            )

    now = datetime.now(tz=timezone.utc)
    repo_id = str(uuid.uuid4())
    data = {
        "id": repo_id,
        "name": body.name,
        "full_name": body.full_name,
        "clone_url": body.clone_url,
        "source": body.source,
        "description": body.description,
        "default_branch": body.default_branch,
        "is_private": body.is_private,
        "index_status": IndexStatus.PENDING,
        "file_count": 0,
        "size_bytes": 0,
        "last_indexed_at": None,
        "created_at": now,
        "updated_at": now,
    }
    _repositories[repo_id] = data
    return _repo_to_response(data)


@router.get(
    "/repositories/{repo_id}",
    response_model=RepositoryResponse,
    tags=["repositories"],
    summary="Get repository",
)
async def get_repository(repo_id: str) -> RepositoryResponse:
    """Return a single repository by ID."""
    data = _repositories.get(repo_id)
    if not data:
        raise _NOT_FOUND
    return _repo_to_response(data)


@router.put(
    "/repositories/{repo_id}",
    response_model=RepositoryResponse,
    tags=["repositories"],
    summary="Update repository metadata",
)
async def update_repository(repo_id: str, body: RepositoryUpdate) -> RepositoryResponse:
    """Update mutable repository fields (description, default_branch)."""
    data = _repositories.get(repo_id)
    if not data:
        raise _NOT_FOUND
    if body.description is not None:
        data["description"] = body.description
    if body.default_branch is not None:
        data["default_branch"] = body.default_branch
    data["updated_at"] = datetime.now(tz=timezone.utc)
    return _repo_to_response(data)


@router.delete(
    "/repositories/{repo_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    tags=["repositories"],
    summary="Delete repository",
)
async def delete_repository(repo_id: str) -> None:
    """Delete a repository record and its associated index status."""
    if repo_id not in _repositories:
        raise _NOT_FOUND
    del _repositories[repo_id]
    _index_status.pop(repo_id, None)


@router.post(
    "/repositories/{repo_id}/index",
    status_code=status.HTTP_202_ACCEPTED,
    tags=["repositories"],
    summary="Trigger repository indexing",
)
async def trigger_index(repo_id: str, body: IndexRequest = IndexRequest()) -> JSONResponse:
    """Enqueue an indexing job for the repository.

    Returns 202 Accepted with a ``status_url`` header pointing to the status
    endpoint so the Android client can poll progress.
    """
    data = _repositories.get(repo_id)
    if not data:
        raise _NOT_FOUND

    now = datetime.now(tz=timezone.utc)

    # Reset status if force_reindex or currently failed/indexed
    current = _index_status.get(repo_id, {})
    if body.force_reindex or current.get("status") in (None, IndexStatus.INDEXED, IndexStatus.FAILED):
        _index_status[repo_id] = {
            "repository_id": repo_id,
            "status": IndexStatus.INDEXING,
            "progress_percent": 0,
            "files_processed": 0,
            "total_files": len(_STUB_FILES),
            "error_message": None,
            "started_at": now,
            "completed_at": None,
        }
        data["index_status"] = IndexStatus.INDEXING
        data["updated_at"] = now

    status_url = f"/v1/repositories/{repo_id}/index/status"
    return JSONResponse(
        status_code=status.HTTP_202_ACCEPTED,
        content={"message": "Indexing started", "status_url": status_url},
        headers={"Location": status_url},
    )


@router.get(
    "/repositories/{repo_id}/index/status",
    response_model=IndexStatusResponse,
    tags=["repositories"],
    summary="Get indexing status",
)
async def get_index_status(repo_id: str) -> IndexStatusResponse:
    """Return the current indexing progress for a repository."""
    if repo_id not in _repositories:
        raise _NOT_FOUND

    index_data = _index_status.get(repo_id)
    if not index_data:
        # No indexing triggered yet — return PENDING
        return IndexStatusResponse(
            repository_id=repo_id,
            status=IndexStatus.PENDING,
        )
    return IndexStatusResponse(**index_data)


@router.get(
    "/repositories/{repo_id}/files",
    response_model=PaginatedResponse[dict],
    tags=["repositories"],
    summary="List repository files",
)
async def list_files(
    repo_id: str,
    path: str = Query(default="/", description="Directory path to list"),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=50, ge=1, le=200),
) -> PaginatedResponse[dict]:
    """Return a paginated list of files/directories at the given path.

    For now returns stub data; real implementation will read from the indexed
    file tree in a later ticket.
    """
    if repo_id not in _repositories:
        raise _NOT_FOUND

    # Stub: return all files regardless of path filter
    items = _STUB_FILES
    total = len(items)
    start = (page - 1) * page_size
    page_items = items[start : start + page_size]
    return PaginatedResponse(
        items=page_items,
        total=total,
        page=page,
        page_size=page_size,
        has_next=(start + page_size) < total,
    )


@router.get(
    "/repositories/{repo_id}/files/content",
    tags=["repositories"],
    summary="Get file content",
)
async def get_file_content(
    repo_id: str,
    path: str = Query(..., description="File path relative to repository root"),
) -> dict:
    """Return the raw content of a single file.

    For now returns stub content; real implementation will read from the
    indexed repository in a later ticket.
    """
    if repo_id not in _repositories:
        raise _NOT_FOUND

    return {
        "path": path,
        "content": _STUB_FILE_CONTENT,
        "encoding": "utf-8",
        "size": len(_STUB_FILE_CONTENT),
    }
