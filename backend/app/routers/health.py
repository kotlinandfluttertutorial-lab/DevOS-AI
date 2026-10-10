"""Health check router."""
from fastapi import APIRouter

from app.config import settings
from app.models.health import HealthResponse

router = APIRouter()

VERSION = "0.1.0"


@router.get("/health", response_model=HealthResponse, tags=["meta"])
async def health() -> HealthResponse:
    """Returns service health, version, and active environment."""
    return HealthResponse(
        status="ok",
        version=VERSION,
        environment=settings.environment,
    )
