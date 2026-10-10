"""Repository CRUD router — stub responses."""
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException, status

from app.models.common import PaginatedResponse
from app.models.repository import RepositoryCreate, RepositoryResponse

router = APIRouter()

# In-memory stub store — replace with DB-backed service in a later ticket
_STUB_REPOS: dict[str, RepositoryResponse] = {}


@router.get("/repositories", response_model=PaginatedResponse[RepositoryResponse], tags=["repositories"])
async def list_repositories(page: int = 1, page_size: int = 20) -> PaginatedResponse[RepositoryResponse]:
    items = list(_STUB_REPOS.values())
    start = (page - 1) * page_size
    end = start + page_size
    page_items = items[start:end]
    return PaginatedResponse(
        items=page_items,
        total=len(items),
        page=page,
        page_size=page_size,
        has_next=end < len(items),
    )


@router.post("/repositories", response_model=RepositoryResponse, status_code=status.HTTP_201_CREATED, tags=["repositories"])
async def create_repository(body: RepositoryCreate) -> RepositoryResponse:
    now = datetime.now(tz=timezone.utc)
    repo = RepositoryResponse(
        id=str(uuid.uuid4()),
        name=body.name,
        url=body.url,
        description=body.description,
        created_at=now,
        updated_at=now,
    )
    _STUB_REPOS[repo.id] = repo
    return repo


@router.get("/repositories/{repo_id}", response_model=RepositoryResponse, tags=["repositories"])
async def get_repository(repo_id: str) -> RepositoryResponse:
    repo = _STUB_REPOS.get(repo_id)
    if not repo:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Repository not found")
    return repo


@router.delete("/repositories/{repo_id}", status_code=status.HTTP_204_NO_CONTENT, tags=["repositories"])
async def delete_repository(repo_id: str) -> None:
    if repo_id not in _STUB_REPOS:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Repository not found")
    del _STUB_REPOS[repo_id]
