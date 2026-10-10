output "artifact_registry_url" {
  description = "Full URL of the Artifact Registry Docker repository."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.api.repository_id}"
}

output "service_account_email" {
  description = "Email of the Cloud Run service account."
  value       = google_service_account.api.email
}

output "secret_ids" {
  description = "Secret Manager secret resource IDs created by Terraform."
  value = {
    db_url_staging      = google_secret_manager_secret.db_url_staging.secret_id
    db_url_prod         = google_secret_manager_secret.db_url_prod.secret_id
    redis_url_staging   = google_secret_manager_secret.redis_url_staging.secret_id
    redis_url_prod      = google_secret_manager_secret.redis_url_prod.secret_id
    openai_api_key      = google_secret_manager_secret.openai_api_key.secret_id
    anthropic_api_key   = google_secret_manager_secret.anthropic_api_key.secret_id
  }
}
