#!/bin/bash
# =============================================================================
# setup-gcp.sh — One-time GCP project setup for DevOS AI
#
# Usage:
#   PROJECT_ID=your-gcp-project-id ./infra/scripts/setup-gcp.sh
#
# Prerequisites:
#   - gcloud CLI installed and authenticated (gcloud auth login)
#   - Sufficient IAM permissions on the target project (Owner or Editor + specific roles)
#
# What this script does:
#   1. Enables required GCP APIs
#   2. Creates the Artifact Registry Docker repository
#   3. Creates the Cloud Run service account
#   4. Grants required IAM roles to the service account
#   5. Creates empty Secret Manager secrets (values must be set manually)
#
# What this script does NOT do:
#   - Never populates secret values — secrets are set manually for security
#   - Never stores credentials anywhere
# =============================================================================
set -euo pipefail

# ── Validate required environment variables ──────────────────────────────────
: "${PROJECT_ID:?ERROR: Set the PROJECT_ID environment variable before running this script.}"

REGION="${REGION:-us-central1}"
SA_NAME="devos-ai-sa"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
REPO_NAME="devos-ai"

echo "==================================================="
echo " DevOS AI — GCP Project Setup"
echo "==================================================="
echo " Project ID : ${PROJECT_ID}"
echo " Region     : ${REGION}"
echo " SA Email   : ${SA_EMAIL}"
echo "==================================================="
echo ""
read -p "Proceed with setup? (yes/no): " confirm
[[ "$confirm" == "yes" ]] || { echo "Aborted."; exit 0; }

# ── Set default project ───────────────────────────────────────────────────────
echo ""
echo ">>> Setting active project to ${PROJECT_ID}..."
gcloud config set project "${PROJECT_ID}"

# ── Enable required APIs ──────────────────────────────────────────────────────
echo ""
echo ">>> Enabling required GCP APIs (this may take a few minutes)..."
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  cloudbuild.googleapis.com \
  sqladmin.googleapis.com \
  iam.googleapis.com \
  cloudresourcemanager.googleapis.com

echo "    APIs enabled successfully."

# ── Create Artifact Registry repository ──────────────────────────────────────
echo ""
echo ">>> Creating Artifact Registry repository '${REPO_NAME}'..."
if gcloud artifacts repositories describe "${REPO_NAME}" \
    --location="${REGION}" --project="${PROJECT_ID}" &>/dev/null; then
  echo "    Repository '${REPO_NAME}' already exists — skipping."
else
  gcloud artifacts repositories create "${REPO_NAME}" \
    --repository-format=docker \
    --location="${REGION}" \
    --description="DevOS AI API Docker images"
  echo "    Repository created: ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}"
fi

# ── Create service account ────────────────────────────────────────────────────
echo ""
echo ">>> Creating service account '${SA_NAME}'..."
if gcloud iam service-accounts describe "${SA_EMAIL}" \
    --project="${PROJECT_ID}" &>/dev/null; then
  echo "    Service account '${SA_EMAIL}' already exists — skipping."
else
  gcloud iam service-accounts create "${SA_NAME}" \
    --display-name="DevOS AI API Service Account" \
    --description="Runtime identity for the DevOS AI Cloud Run service" \
    --project="${PROJECT_ID}"
  echo "    Service account created: ${SA_EMAIL}"
fi

# ── Grant IAM roles ───────────────────────────────────────────────────────────
echo ""
echo ">>> Granting IAM roles to ${SA_EMAIL}..."

ROLES=(
  "roles/run.invoker"
  "roles/secretmanager.secretAccessor"
  "roles/cloudsql.client"
  "roles/artifactregistry.reader"
)

for ROLE in "${ROLES[@]}"; do
  echo "    Binding role: ${ROLE}"
  gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${SA_EMAIL}" \
    --role="${ROLE}" \
    --quiet
done

echo "    IAM roles granted."

# ── Create Secret Manager secrets (empty — values set manually) ───────────────
echo ""
echo ">>> Creating Secret Manager secrets (values NOT set — see instructions below)..."

SECRETS=(
  "devos-staging-db-url"
  "devos-staging-redis-url"
  "devos-prod-db-url"
  "devos-prod-redis-url"
  "devos-openai-api-key"
  "devos-anthropic-api-key"
)

for SECRET in "${SECRETS[@]}"; do
  if gcloud secrets describe "${SECRET}" \
      --project="${PROJECT_ID}" &>/dev/null; then
    echo "    Secret '${SECRET}' already exists — skipping."
  else
    gcloud secrets create "${SECRET}" \
      --replication-policy=automatic \
      --project="${PROJECT_ID}"
    echo "    Created secret: ${SECRET}"
  fi
done

# ── Configure Docker auth for Artifact Registry ───────────────────────────────
echo ""
echo ">>> Configuring Docker authentication for Artifact Registry..."
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet
echo "    Docker configured."

# ── Print next steps ──────────────────────────────────────────────────────────
echo ""
echo "==================================================="
echo " Setup complete! Next steps:"
echo "==================================================="
echo ""
echo " 1. Set secret values manually (NEVER commit these):"
echo ""
echo "    # Staging database URL"
echo "    echo -n 'postgresql://user:password@host:5432/db' | \\"
echo "      gcloud secrets versions add devos-staging-db-url --data-file=-"
echo ""
echo "    # Staging Redis URL"
echo "    echo -n 'redis://host:6379' | \\"
echo "      gcloud secrets versions add devos-staging-redis-url --data-file=-"
echo ""
echo "    # Production database URL"
echo "    echo -n 'postgresql://user:password@host:5432/db' | \\"
echo "      gcloud secrets versions add devos-prod-db-url --data-file=-"
echo ""
echo "    # Production Redis URL"
echo "    echo -n 'redis://host:6379' | \\"
echo "      gcloud secrets versions add devos-prod-redis-url --data-file=-"
echo ""
echo "    # OpenAI API key (shared across envs)"
echo "    echo -n 'sk-...' | \\"
echo "      gcloud secrets versions add devos-openai-api-key --data-file=-"
echo ""
echo "    # Anthropic API key (shared across envs)"
echo "    echo -n 'sk-ant-...' | \\"
echo "      gcloud secrets versions add devos-anthropic-api-key --data-file=-"
echo ""
echo " 2. Deploy to staging:"
echo "    PROJECT_ID=${PROJECT_ID} ./infra/scripts/deploy-staging.sh"
echo ""
echo " 3. Deploy to production (manual confirmation required):"
echo "    PROJECT_ID=${PROJECT_ID} ./infra/scripts/deploy-prod.sh"
echo ""
echo "==================================================="
