"""
Application configuration using Pydantic Settings.

Environment file resolution order (first match wins):
  1. ENV_FILE env var — e.g. ENV_FILE=.env.staging
  2. .env.<ENVIRONMENT> — derived from the ENVIRONMENT var
  3. .env — fallback for plain docker-compose usage

All secrets are loaded from environment variables — never hardcoded.
"""
import os
from pydantic_settings import BaseSettings


def _resolve_env_file() -> str:
    """Pick the right .env file based on ENV_FILE or ENVIRONMENT."""
    explicit = os.getenv("ENV_FILE")
    if explicit:
        return explicit
    environment = os.getenv("ENVIRONMENT", "local")
    derived = f".env.{environment}"
    if os.path.exists(derived):
        return derived
    return ".env"


class Settings(BaseSettings):
    # ── Core ──────────────────────────────────────────────────────────────
    environment: str = "local"          # local | staging | production
    debug: bool = True
    docs_enabled: bool = True           # set False in staging/prod

    # ── Network ───────────────────────────────────────────────────────────
    cors_origins: list[str] = ["*"]

    # ── Database ──────────────────────────────────────────────────────────
    database_url: str = "postgresql+asyncpg://devos:devos@localhost:5432/devos"

    # ── Redis ─────────────────────────────────────────────────────────────
    redis_url: str = "redis://localhost:6379"

    # ── AI providers — loaded from env, never hardcoded ───────────────────
    openai_api_key: str = ""
    anthropic_api_key: str = ""
    gemini_api_key: str = ""
    ollama_base_url: str = "http://localhost:11434"

    # ── GCP (staging / production only) ───────────────────────────────────
    gcp_project_id: str = ""
    gcp_region: str = "us-central1"

    # ── Observability ─────────────────────────────────────────────────────
    sentry_dsn: str = ""

    # ── Rate limiting ─────────────────────────────────────────────────────
    rate_limit_rpm: int = 60            # requests per minute per client IP

    # ── Local dev convenience ─────────────────────────────────────────────
    local_dev_token: str = ""

    model_config = {"env_file": _resolve_env_file(), "env_file_encoding": "utf-8"}

    @property
    def is_local(self) -> bool:
        return self.environment == "local"

    @property
    def is_staging(self) -> bool:
        return self.environment == "staging"

    @property
    def is_production(self) -> bool:
        return self.environment == "production"


settings = Settings()
