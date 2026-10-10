"""Common FastAPI dependencies (DI wiring)."""
from fastapi import Header, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db  # re-export for convenience


async def get_current_token(authorization: str = Header(default="")) -> str:
    """
    Extract and validate the Bearer token from the Authorization header.
    Returns the raw token string for downstream use.
    Raises 401 if the header is missing or malformed.
    """
    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid Authorization header",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return authorization.removeprefix("Bearer ").strip()
