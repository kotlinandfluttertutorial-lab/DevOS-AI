# DevOS AI — GCP Cloud Run Infrastructure

This directory contains all infrastructure configuration for deploying the DevOS AI backend API to Google Cloud Platform using Cloud Run.

---

## Directory Structure

```
infra/
├── cloudbuild/
│   ├── cloudbuild-staging.yaml   # Cloud Build pipeline → staging (auto on main push)
│   └── cloudbuild-prod.yaml      # Cloud Build pipeline → production (manual only)
├── cloudrun/
│   ├── staging.yaml              # Cloud Run service manifest (staging)
│   └── prod.yaml                 # Cloud Run service manifest (production)
├── terraform/
│   ├── main.tf                   # Core GCP resources (Artifact Registry, SA, Secrets)
│   ├── variables.tf              # Input variables
│   ├── outputs.tf                # Output values
│   └── environments/
│       ├── staging.tfvars        # Staging variable values
│       └── prod.tfvars           # Production variable values
├── scripts/
│   ├── setup-gcp.sh              # One-time GCP project setup
│   ├── deploy-staging.sh         # Manual staging deployment
│   └── deploy-prod.sh            # Manual production deployment (confirmation required)
└── README.md                     # This file
```

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| gcloud CLI | Latest | https://cloud.google.com/sdk/docs/install |
| Terraform | >= 1.6 | https://developer.hashicorp.com/terraform/install |
| Docker | Latest | https://docs.docker.com/get-docker/ |

Authenticate gcloud before running any scripts:

```bash
gcloud auth login
gcloud auth application-default login
```

---

## Environments

| Environment | GCP Project ID | URL |
|-------------|---------------|-----|
| Staging | `devos-ai-staging` | https://api-staging.devos.ai |
| Production | `devos-ai-prod` | https://api.devos.ai |

---

## First-Time Setup

Run the setup script once per GCP project. It enables required APIs, creates the Artifact Registry repository, service account, and empty Secret Manager secrets.

```bash
# Staging project setup
PROJECT_ID=devos-ai-staging ./infra/scripts/setup-gcp.sh

# Production project setup
PROJECT_ID=devos-ai-prod ./infra/scripts/setup-gcp.sh
```

### What setup-gcp.sh does

1. Enables APIs: `run`, `artifactregistry`, `secretmanager`, `cloudbuild`, `sqladmin`, `iam`
2. Creates Artifact Registry repository: `devos-ai` (Docker format)
3. Creates service account: `devos-ai-sa@PROJECT_ID.iam.gserviceaccount.com`
4. Grants IAM roles: `run.invoker`, `secretmanager.secretAccessor`, `cloudsql.client`, `artifactregistry.reader`
5. Creates empty Secret Manager secrets (see next section)

---

## Setting Secret Values

**Secrets are NEVER stored in code, committed to git, or set by automation.**
Set them manually using the gcloud CLI after running setup:

```bash
# Set staging database URL
echo -n 'postgresql://user:password@host:5432/devos_staging' | \
  gcloud secrets versions add devos-staging-db-url --data-file=- --project=devos-ai-staging

# Set staging Redis URL
echo -n 'redis://host:6379' | \
  gcloud secrets versions add devos-staging-redis-url --data-file=- --project=devos-ai-staging

# Set production database URL
echo -n 'postgresql://user:password@host:5432/devos_prod' | \
  gcloud secrets versions add devos-prod-db-url --data-file=- --project=devos-ai-prod

# Set production Redis URL
echo -n 'redis://host:6379' | \
  gcloud secrets versions add devos-prod-redis-url --data-file=- --project=devos-ai-prod

# Set OpenAI API key (shared — set in both projects or use one project)
echo -n 'sk-...' | \
  gcloud secrets versions add devos-openai-api-key --data-file=- --project=devos-ai-staging

# Set Anthropic API key
echo -n 'sk-ant-...' | \
  gcloud secrets versions add devos-anthropic-api-key --data-file=- --project=devos-ai-staging
```

### Secrets managed

| Secret Name | Description | Environments |
|-------------|-------------|-------------|
| `devos-staging-db-url` | PostgreSQL connection string (staging) | Staging |
| `devos-staging-redis-url` | Redis connection string (staging) | Staging |
| `devos-prod-db-url` | PostgreSQL connection string (production) | Production |
| `devos-prod-redis-url` | Redis connection string (production) | Production |
| `devos-openai-api-key` | OpenAI API key | Both |
| `devos-anthropic-api-key` | Anthropic API key | Both |

---

## Staging Deployment

### Automatic (Cloud Build — on push to main)

Every push to the `main` branch triggers `cloudbuild-staging.yaml` automatically:
1. Runs Python tests (`pytest`)
2. Builds the Docker image
3. Pushes to Artifact Registry
4. Deploys to `devos-ai-api-staging` on Cloud Run

Set up the Cloud Build trigger once:

```bash
gcloud builds triggers create github \
  --repo-name=devos-ai \
  --repo-owner=YOUR_GITHUB_ORG \
  --branch-pattern=^main$ \
  --build-config=infra/cloudbuild/cloudbuild-staging.yaml \
  --project=devos-ai-staging
```

### Manual staging deployment

```bash
PROJECT_ID=devos-ai-staging ./infra/scripts/deploy-staging.sh
```

Optional overrides:
```bash
PROJECT_ID=devos-ai-staging REGION=us-east1 IMAGE_TAG=abc1234 ./infra/scripts/deploy-staging.sh
```

---

## Production Deployment

**Production deployments are ALWAYS manual.** There is no automatic trigger for production.

```bash
PROJECT_ID=devos-ai-prod ./infra/scripts/deploy-prod.sh
```

The script prompts for explicit confirmation before deploying:

```
Deploy to PRODUCTION? (yes/no): yes
```

Any answer other than `yes` aborts the deployment.

### Via Cloud Build (manual trigger)

```bash
gcloud builds submit \
  --config=infra/cloudbuild/cloudbuild-prod.yaml \
  --substitutions=SHORT_SHA=$(git rev-parse --short HEAD) \
  --project=devos-ai-prod \
  .
```

---

## Terraform (Infrastructure as Code)

Terraform manages GCP resources: Artifact Registry, service accounts, IAM bindings, and Secret Manager secrets (structure only — not values).

### Initialize

```bash
cd infra/terraform

# Staging
terraform init
terraform plan -var-file=environments/staging.tfvars
terraform apply -var-file=environments/staging.tfvars

# Production
terraform plan -var-file=environments/prod.tfvars
terraform apply -var-file=environments/prod.tfvars
```

### Terraform state

State is stored in a GCS bucket: `devos-ai-terraform-state`. Create this bucket before initializing:

```bash
gsutil mb -p devos-ai-staging -l us-central1 gs://devos-ai-terraform-state
gsutil versioning set on gs://devos-ai-terraform-state
```

---

## Cloud Run Service Configuration

### Staging

| Setting | Value |
|---------|-------|
| Min instances | 0 (scale to zero) |
| Max instances | 10 |
| CPU | 1 vCPU |
| Memory | 512 MiB |
| CPU throttling | Yes |
| Timeout | 300s |

### Production

| Setting | Value |
|---------|-------|
| Min instances | 1 (always warm) |
| Max instances | 20 |
| CPU | 2 vCPU |
| Memory | 1 GiB |
| CPU throttling | No |
| Timeout | 300s |

---

## Security Notes

- All sensitive values are in **Secret Manager** — never in environment variables, code, or git.
- The Cloud Run service account has **least-privilege** IAM roles only.
- Deploy scripts require `PROJECT_ID` to be explicitly set — no default to prevent accidental cross-environment deployments.
- Production deployment script requires typed confirmation (`yes`) before proceeding.
- Never commit `.env` files or any file containing real credentials.

---

## Troubleshooting

### View Cloud Run logs

```bash
gcloud run services logs read devos-ai-api-staging \
  --region=us-central1 --project=devos-ai-staging --limit=50

gcloud run services logs read devos-ai-api-prod \
  --region=us-central1 --project=devos-ai-prod --limit=50
```

### Check service status

```bash
gcloud run services describe devos-ai-api-staging \
  --region=us-central1 --project=devos-ai-staging

gcloud run services describe devos-ai-api-prod \
  --region=us-central1 --project=devos-ai-prod
```

### Rollback

Cloud Run keeps previous revisions. Roll back to the last known-good revision:

```bash
# List revisions
gcloud run revisions list --service=devos-ai-api-prod \
  --region=us-central1 --project=devos-ai-prod

# Migrate traffic to a specific revision
gcloud run services update-traffic devos-ai-api-prod \
  --to-revisions=devos-ai-api-prod-REVISION=100 \
  --region=us-central1 --project=devos-ai-prod
```
