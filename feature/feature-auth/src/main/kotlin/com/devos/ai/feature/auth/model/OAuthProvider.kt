package com.devos.ai.feature.auth.model

/**
 * Supported OAuth providers for DevOS AI repository authentication.
 *
 * [prefKey] is the EncryptedSharedPreferences key used to store the access token.
 * [displayName] is the human-readable name shown in the UI.
 */
enum class OAuthProvider(val prefKey: String, val displayName: String) {
    GITHUB(prefKey = "github_token", displayName = "GitHub"),
    GITLAB(prefKey = "gitlab_token", displayName = "GitLab"),
}
