"""
DevOS AI — FastAPI application entry point.

Startup/shutdown lifecycle is managed via the lifespan context manager.
All routes are versioned under /v1.
"""
import logging
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.routers import agents, ai_chat, files, health, rag, repositories

logger = logging.getLogger("devos.api")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup and shutdown lifecycle."""
    logger.info("DevOS AI backend starting — environment: %s", settings.environment)
    # Future: initialise DB connection pool, load ML models, warm caches
    yield
    # Future: close DB pool, flush metrics
    logger.info("DevOS AI backend shutting down")


def create_app() -> FastAPI:
    app = FastAPI(
        title="DevOS AI API",
        version="0.1.0",
        description="Backend service for the DevOS AI Android developer command centre.",
        docs_url="/docs" if settings.debug else None,
        redoc_url="/redoc" if settings.debug else None,
        lifespan=lifespan,
    )

    # CORS — origins are configured per-environment via settings
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Request logging middleware
    @app.middleware("http")
    async def log_requests(request: Request, call_next):
        start = time.perf_counter()
        response = await call_next(request)
        duration_ms = (time.perf_counter() - start) * 1000
        logger.info(
            "%s %s %d %.1fms",
            request.method,
            request.url.path,
            response.status_code,
            duration_ms,
        )
        return response

    # Routers — all mounted under /v1
    app.include_router(health.router, prefix="/v1")
    app.include_router(repositories.router, prefix="/v1")
    app.include_router(files.router, prefix="/v1", tags=["files"])
    app.include_router(ai_chat.router, prefix="/v1")
    app.include_router(rag.router, prefix="/v1")
    app.include_router(agents.router, prefix="/v1")

    return app


app = create_app()
