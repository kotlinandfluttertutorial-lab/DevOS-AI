terraform {
  required_version = ">= 1.6"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }

  backend "gcs" {
    bucket = "devos-ai-terraform-state"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# ─────────────────────────────────────────
# Artifact Registry — Docker repository
# ─────────────────────────────────────────
resource "google_artifact_registry_repository" "api" {
  location      = var.region
  repository_id = "devos-ai"
  description   = "DevOS AI API Docker images"
  format        = "DOCKER"
}

# ─────────────────────────────────────────
# Service Account for Cloud Run
# ─────────────────────────────────────────
resource "google_service_account" "api" {
  account_id   = "devos-ai-sa"
  display_name = "DevOS AI API Service Account"
  description  = "Runtime identity for the DevOS AI Cloud Run service"
}

# IAM: allow service account to access Secret Manager secrets
resource "google_project_iam_member" "api_secret_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.api.email}"
}

# IAM: allow service account to connect to Cloud SQL
resource "google_project_iam_member" "api_cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.api.email}"
}

# IAM: allow unauthenticated invocation for the Cloud Run service
resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  project  = var.project_id
  location = var.region
  name     = "devos-ai-api-${var.environment}"
  role     = "roles/run.invoker"
  member   = "allUsers"

  depends_on = [
    google_artifact_registry_repository.api
  ]
}

# ─────────────────────────────────────────
# Secret Manager — secrets (values set manually, never in Terraform)
# ─────────────────────────────────────────
resource "google_secret_manager_secret" "db_url_staging" {
  secret_id = "devos-staging-db-url"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret" "db_url_prod" {
  secret_id = "devos-prod-db-url"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret" "redis_url_staging" {
  secret_id = "devos-staging-redis-url"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret" "redis_url_prod" {
  secret_id = "devos-prod-redis-url"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret" "openai_api_key" {
  secret_id = "devos-openai-api-key"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret" "anthropic_api_key" {
  secret_id = "devos-anthropic-api-key"
  replication {
    auto {}
  }
}
