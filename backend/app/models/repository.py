"""Repository request/response schemas."""
from datetime import datetime
from pydantic import BaseModel


class RepositoryBase(BaseModel):
    name: str
    url: str
    description: str = ""


class RepositoryCreate(RepositoryBase):
    pass


class RepositoryResponse(RepositoryBase):
    id: str
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
