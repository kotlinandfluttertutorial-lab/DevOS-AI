"""
Application configuration using Pydantic Settings.
All secrets are loaded from environment variables — never hardcoded.
"""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    environment: str = "local"  # local | staging | production
    debug: bool = True
    cors_origins: list[str] = ["*"]
    database_url: str = "postgresql+asyncpg://devos:devos@localhost:5432/devos"
    redis_url: str = "redis://localhost:6379"

    # AI providers — loaded from env, never hardcoded
    openai_api_key: str = ""
    anthropic_api_key: str = ""
    gemini_api_key: str = ""

    class Config:
        env_file = ".env"


settings = Settings()
