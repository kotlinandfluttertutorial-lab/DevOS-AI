# DevOS AI Backend

Python FastAPI service powering the DevOS AI Android application.

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 4.x+
- Python 3.12 (only needed for local development outside Docker)
- [Poetry](https://python-poetry.org/) (only needed for local development outside Docker)

---

## Local Development (Docker — recommended)

```bash
# 1. Copy the environment template
cp .env.example .env

# 2. Fill in your AI provider keys in .env
#    OPENAI_API_KEY, ANTHROPIC_API_KEY, GEMINI_API_KEY

# 3. Start all services (API + PostgreSQL + Redis) with hot-reload
docker-compose up --build
```

The override file (`docker-compose.override.yml`) is automatically applied,
mounting the source directory into the container and enabling `--reload`.

---

## Service URLs

| Service   | URL                          |
|-----------|------------------------------|
| API       | http://localhost:8000        |
| API Docs  | http://localhost:8000/docs   |
| ReDoc     | http://localhost:8000/redoc  |
| PostgreSQL| localhost:5432               |
| Redis     | localhost:6379               |

---

## Health Check

```bash
curl http://localhost:8000/v1/health
```

Expected response:
```json
{"status": "ok", "version": "0.1.0", "environment": "local"}
```

---

## Android Emulator Access

The Android emulator routes `10.0.2.2` to the host machine's `localhost`.
Configure the app's `local` product flavor to use:

```
BASE_URL = http://10.0.2.2:8000/v1/
```

---

## Running Tests (outside Docker)

```bash
poetry install
poetry run pytest
```

---

## Project Structure

```
backend/
├── app/
│   ├── main.py           # FastAPI app, middleware, router registration
│   ├── config.py         # Pydantic Settings (env-driven configuration)
│   ├── dependencies.py   # Shared FastAPI DI (auth token extraction)
│   ├── models/           # Pydantic request/response schemas
│   ├── routers/          # Route handlers grouped by domain
│   ├── services/         # Business logic / AI provider adapters
│   └── db/               # SQLAlchemy session factory
├── tests/                # pytest test suite
├── Dockerfile            # Multi-stage production build
├── docker-compose.yml    # Base services definition
├── docker-compose.override.yml  # Local dev hot-reload config
├── pyproject.toml        # Poetry dependency manifest
└── .env.example          # Environment variable template (no secrets)
```

---

## Environment Variables

| Variable             | Description                              | Default                                              |
|----------------------|------------------------------------------|------------------------------------------------------|
| `ENVIRONMENT`        | Runtime environment                      | `local`                                              |
| `DEBUG`              | Enable debug mode + OpenAPI docs         | `true`                                               |
| `DATABASE_URL`       | Async PostgreSQL connection string       | `postgresql+asyncpg://devos:devos@localhost:5432/devos` |
| `REDIS_URL`          | Redis connection string                  | `redis://localhost:6379`                             |
| `OPENAI_API_KEY`     | OpenAI API key                           | *(empty — set in .env)*                              |
| `ANTHROPIC_API_KEY`  | Anthropic API key                        | *(empty — set in .env)*                              |
| `GEMINI_API_KEY`     | Google Gemini API key                    | *(empty — set in .env)*                              |
| `CORS_ORIGINS`       | Allowed CORS origins (JSON array)        | `["*"]`                                              |

> **Security:** Never commit `.env` to version control. It is listed in `.gitignore`.

---

## GCP Deployment (Staging / Production)

GCP environment setup (Cloud Run + Cloud SQL + Memorystore) is tracked in tickets
**DEVOS-B03** (staging) and **DEVOS-B04** (production). Environment variables are
injected via GCP Secret Manager — no secrets in source code or Docker images.
