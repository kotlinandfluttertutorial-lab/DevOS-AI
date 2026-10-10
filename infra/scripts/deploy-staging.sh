#!/bin/bash
# =============================================================================
# deploy-staging.sh — Build and deploy the DevOS AI API to staging (Cloud Run)
#
# Usage:
#   PROJECT_ID=your-project-id ./infra/scripts/deploy-staging.sh
#
# Optional overrides:
#   REGION=us-central1         (default: us-central1)
#   IMAGE_TAG=$(git short SHA)  (default: current git HEAD short SHA)
#
# Prerequisites:
#   - gcloud CLI installed and authenticated
#   - Docker installed and running
#   - PROJECT_ID environment variable set
#   - ./infra/scripts/setup-gcp.sh run at least once
# =============================================================================
set -euo pipefail

# ── Validate required environment variables ───────────────────────────────────
: "${PROJECT_ID:?ERROR: Set PROJECT_ID environment variable.}"

REGION="${REGION:-us-central1}"
REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/devos-ai/api"
SHA="${IMAGE_TAG:-$(git rev-parse --short HEAD)}"
IMAGE="${REGISTRY}:${SHA}"
IMAGE_LATEST="${REGISTRY}:staging-latest"
SERVICE_NAME="devos-ai-api-staging"
SA_EMAIL="devos-ai-sa@${PROJECT_ID}.iam.gserviceaccount.com"

echo "==================================================="
echo " DevOS AI — Deploy to STAGING"
echo "==================================================="
echo " Project   : ${PROJECT_ID}"
echo " Region    : ${REGION}"
echo " Image     : ${IMAGE}"
echo " Service   : ${SERVICE_NAME}"
echo "==================================================="
echo ""

# ── Build Docker image ────────────────────────────────────────────────────────
echo ">>> Building Docker image..."
docker build \
  -t "${IMAGE}" \
  -t "${IMAGE_LATEST}" \
  ./backend

echo "    Build complete."

# ── Push to Artifact Registry ─────────────────────────────────────────────────
echo ""
echo ">>> Pushing image to Artifact Registry..."
docker push "${IMAGE}"
docker push "${IMAGE_LATEST}"
echo "    Push complete."

# ── Deploy to Cloud Run (staging) ─────────────────────────────────────────────
echo ""
echo ">>> Deploying to Cloud Run (staging)..."
gcloud run deploy "${SERVICE_NAME}" \
  --image="${IMAGE}" \
  --region="${REGION}" \
  --platform=managed \
  --service-account="${SA_EMAIL}" \
  --allow-unauthenticated \
  --port=8000 \
  --memory=512Mi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=10 \
  --concurrency=80 \
  --timeout=300 \
  --set-env-vars=ENVIRONMENT=staging \
  --update-secrets="DATABASE_URL=devos-staging-db-url:latest,REDIS_URL=devos-staging-redis-url:latest,OPENAI_API_KEY=devos-openai-api-key:latest,ANTHROPIC_API_KEY=devos-anthropic-api-key:latest" \
  --project="${PROJECT_ID}"

echo ""
echo "==================================================="
echo " Deployed to staging: ${IMAGE}"
STAGING_URL=$(gcloud run services describe "${SERVICE_NAME}" \
  --region="${REGION}" --project="${PROJECT_ID}" \
  --format="value(status.url)" 2>/dev/null || echo "(run 'gcloud run services describe ${SERVICE_NAME}' to get URL)")
echo " URL: ${STAGING_URL}"
echo "==================================================="
