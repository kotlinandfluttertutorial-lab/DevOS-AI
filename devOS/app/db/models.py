"""SQLAlchemy ORM models — DA-127."""
import uuid
from datetime import datetime, timezone

from sqlalchemy import Boolean, Column, DateTime, Integer, String
from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    pass


class RepositoryORM(Base):
    """Persisted repository record.

    Mirrors :class:`app.models.repository.RepositoryResponse`.  Migration is
    not run yet — in-memory stubs are used for now.  The ORM model is defined
    here so Alembic can generate migrations in a later ticket.
    """

    __tablename__ = "repositories"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    name = Column(String, nullable=False)
    full_name = Column(String, nullable=False, unique=True)
    clone_url = Column(String, nullable=False)
    source = Column(String, nullable=False)  # github / gitlab / bitbucket / local
    description = Column(String, nullable=True)
    default_branch = Column(String, default="main")
    is_private = Column(Boolean, default=False)
    index_status = Column(String, default="pending")
    file_count = Column(Integer, default=0)
    size_bytes = Column(Integer, default=0)
    last_indexed_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
    )
    updated_at = Column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )
