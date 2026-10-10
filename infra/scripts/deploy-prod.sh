#!/bin/bash
# =============================================================================
# deploy-prod.sh — Build and deploy the DevOS AI API to production (Cloud Run)
#
# !! PRODUCTION DEPLOYMENT — REQUIRES EXPLICIT CONFIRMATION !!
#
# Usage:
#   PROJECT_ID=your-project-id ./infra/scripts/deploy-prod.sh
#
# Optional overrides:
#   REGION=us-central1         (default: us-central1)
#   IMAGE_TAG=$(git short SHA)  (default: current git HEAD short SHA)
#
# Prerequisites:
#   - gcloud CLI installed and authenticated
#   - Docker installed and running
#   - PROJECT_ID environment variable set
#   - Staging deployment verified first
# =============================================================================
set -euo pipefail

# ── Validate required environment variables ───────────────────────────────────
: "${PROJECT_ID:?ERROR: Set PROJECT_ID environment variable.}"

REGION="${REGION:-us-central1}"
REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/devos-ai/api"
SHA="${IMAGE_TAG:-$(git rev-parse --short HEAD)}"
IMAGE="${REGISTRY}:${SHA}"
IMAGE_LATEST="${REGISTRY}:prod-latest"
SERVICE_NAME="devos-ai-api-prod"
SA_EMAIL="devos-ai-sa@${PROJECT_ID}.iam.gserviceaccount.com"

echo "==================================================="
echo " DevOS AI — Deploy to PRODUCTION"
echo "==================================================="
echo " Project   : ${PROJECT_ID}"
echo " Region    : ${REGION}"
echo " Image     : ${IMAGE}"
echo " Service   : ${SERVICE_NAME}"
echo "==================================================="
echo ""
echo " WARNING: You are about to deploy to PRODUCTION."
echo " Make sure the staging deployment has been verified first."
echo ""

# ── Mandatory confirmation prompt ─────────────────────────────────────────────
read -p "Deploy to PRODUCTION? (yes/no): " confirm
[[ "$confirm" == "yes" ]] || { echo "Deployment cancelled."; exit 1; }

echo ""
echo ">>> Starting production deployment..."

# ── Build Docker image ────────────────────────────────────────────────────────
echo ""
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

# ── Deploy to Cloud Run (production) ─────────────────────────────────────────
echo ""
echo ">>> Deploying to Cloud Run (production)..."
gcloud run deploy "${SERVICE_NAME}" \
  --image="${IMAGE}" \
  --region="${REGION}" \
  --platform=managed \
  --service-account="${SA_EMAIL}" \
  --allow-unauthenticated \
  --port=8000 \
  --memory=1Gi \
  --cpu=2 \
  --min-instances=1 \
  --max-instances=20 \
  --concurrency=80 \
  --timeout=300 \
  --set-env-vars=ENVIRONMENT=production \
  --update-secrets="DATABASE_URL=devos-prod-db-url:latest,REDIS_URL=devos-prod-redis-url:latest,OPENAI_API_KEY=devos-openai-api-key:latest,ANTHROPIC_API_KEY=devos-anthropic-api-key:latest" \
  --project="${PROJECT_ID}"

echo ""
echo "==================================================="
echo " Deployed to production: ${IMAGE}"
PROD_URL=$(gcloud run services describe "${SERVICE_NAME}" \
  --region="${REGION}" --project="${PROJECT_ID}" \
  --format="value(status.url)" 2>/dev/null || echo "(run 'gcloud run services describe ${SERVICE_NAME}' to get URL)")
echo " URL: ${PROD_URL}"
echo "==================================================="
