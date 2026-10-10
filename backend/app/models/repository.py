"""Repository request/response schemas — DA-127."""
from datetime import datetime
from enum import Enum
from typing import Optional

from pydantic import BaseModel


class RepositorySource(str, Enum):
    GITHUB = "github"
    GITLAB = "gitlab"
    BITBUCKET = "bitbucket"
    LOCAL = "local"


class IndexStatus(str, Enum):
    PENDING = "pending"
    INDEXING = "indexing"
    INDEXED = "indexed"
    FAILED = "failed"


class RepositoryBase(BaseModel):
    name: str
    full_name: str  # e.g. "owner/repo"
    clone_url: str
    source: RepositorySource
    description: Optional[str] = None
    default_branch: str = "main"
    is_private: bool = False


class RepositoryCreate(RepositoryBase):
    pass


class RepositoryUpdate(BaseModel):
    description: Optional[str] = None
    default_branch: Optional[str] = None


class RepositoryResponse(RepositoryBase):
    id: str
    index_status: IndexStatus = IndexStatus.PENDING
    file_count: int = 0
    size_bytes: int = 0
    last_indexed_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class RepositoryListResponse(BaseModel):
    items: list[RepositoryResponse]
    total: int
    page: int
    page_size: int


class IndexRequest(BaseModel):
    force_reindex: bool = False


class IndexStatusResponse(BaseModel):
    repository_id: str
    status: IndexStatus
    progress_percent: int = 0
    files_processed: int = 0
    total_files: int = 0
    error_message: Optional[str] = None
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
