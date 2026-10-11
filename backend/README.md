# DevOS AI Backend

Python FastAPI service powering the DevOS AI Android application.

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 4.x+
- Python 3.12 (only needed for local development outside Docker)
- [Poetry](https://python-poetry.org/) (only needed for local development outside Docker)

---

## Environment Files

Three environment files — one per deployment target. They are **gitignored** (no real secrets in VCS).

| File | Used by | Purpose |
|------|---------|---------|
| `.env.local` | `docker-compose up` on your dev machine | Local Postgres + Redis containers, debug on, AI provider dev keys |
| `.env.staging` | GCP Cloud Run — `devos-ai-api-staging` | Reference template; real secrets injected by Secret Manager |
| `.env.production` | GCP Cloud Run — `devos-ai-api-prod` | Reference template; real secrets injected by Secret Manager |

The app resolves the file automatically:
1. `ENV_FILE` env var (explicit override) — e.g. `ENV_FILE=.env.staging`
2. `.env.<ENVIRONMENT>` derived from the `ENVIRONMENT` var
3. `.env` fallback

---

## Local Development (Docker — recommended)

```bash
# 1. Copy the local template
cp .env.local .env.local   # already exists — just fill in your keys

# 2. Fill in your AI provider keys in .env.local
#    OPENAI_API_KEY, ANTHROPIC_API_KEY, GEMINI_API_KEY

# 3. Start all services (API + PostgreSQL + Redis) with hot-reload
docker-compose up --build
```

The override file (`docker-compose.override.yml`) is automatically applied,
mounting the source directory into the container and enabling `--reload`.

---

## Service URLs

| Service    | URL                         |
|------------|-----------------------------|
| API        | http://localhost:8000       |
| API Docs   | http://localhost:8000/docs  |
| ReDoc      | http://localhost:8000/redoc |
| PostgreSQL | localhost:5432              |
| Redis      | localhost:6379              |

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
The app's `local` product flavor points to:

```
BASE_URL = http://10.0.2.2:8000/v1/
```

The `.env.local` `CORS_ORIGINS` list already includes `http://10.0.2.2`.

---

## Staging Deployment

```bash
# Deploy from your machine
cd infra
PROJECT_ID=devos-ai-staging ./scripts/deploy-staging.sh
```

Or push to `main` — Cloud Build auto-deploys to staging.

Secrets are **not** in `.env.staging`. Set them once via Secret Manager:
```bash
echo -n "sk-..." | gcloud secrets versions add devos-openai-api-key \
  --project=devos-ai-staging --data-file=-
```

---

## Production Deployment

```bash
cd infra
PROJECT_ID=devos-ai-prod ./scripts/deploy-prod.sh   # requires typed "yes" confirmation
```

Production deploys are **manual only** — no auto-trigger on push.

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
│   ├── config.py         # Pydantic Settings (auto-resolves env file)
│   ├── dependencies.py   # Shared FastAPI DI (auth token extraction)
│   ├── models/           # Pydantic request/response schemas
│   ├── routers/          # Route handlers grouped by domain
│   ├── services/         # Business logic / AI provider adapters
│   └── db/               # SQLAlchemy session factory
├── tests/                # pytest test suite
├── Dockerfile            # Multi-stage production build
├── docker-compose.yml    # Base services (uses .env.local)
├── docker-compose.override.yml  # Local dev hot-reload config
├── pyproject.toml        # Poetry dependency manifest
├── .env.local            # Local dev config (gitignored — fill in keys)
├── .env.staging          # Staging reference template (gitignored)
└── .env.production       # Production reference template (gitignored)
```

---

## Environment Variables Reference

| Variable          | local default                                    | staging/prod          |
|-------------------|--------------------------------------------------|-----------------------|
| `ENVIRONMENT`     | `local`                                          | `staging` / `production` |
| `DEBUG`           | `true`                                           | `false`               |
| `DOCS_ENABLED`    | `true`                                           | `false`               |
| `DATABASE_URL`    | `postgresql+asyncpg://devos:devos@db:5432/devos` | Secret Manager        |
| `REDIS_URL`       | `redis://redis:6379`                             | Secret Manager        |
| `OPENAI_API_KEY`  | your dev key                                     | Secret Manager        |
| `ANTHROPIC_API_KEY` | your dev key                                   | Secret Manager        |
| `GEMINI_API_KEY`  | your dev key                                     | Secret Manager        |
| `OLLAMA_BASE_URL` | `http://host.docker.internal:11434`              | *(not used)*          |
| `CORS_ORIGINS`    | emulator + localhost                             | production domain only |
| `SENTRY_DSN`      | *(empty)*                                        | Secret Manager        |
| `RATE_LIMIT_RPM`  | *(not enforced)*                                 | `60`                  |

> **Security:** All `.env.*` files are gitignored. Never commit real credentials.
> In staging/prod, all secrets live exclusively in GCP Secret Manager.
