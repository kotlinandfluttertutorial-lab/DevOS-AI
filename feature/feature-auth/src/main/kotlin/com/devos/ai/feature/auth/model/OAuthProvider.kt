package com.devos.ai.feature.auth.model

import com.devos.ai.core.security.TokenKey

/**
 * Supported OAuth providers for DevOS AI repository authentication.
 *
 * Implements [TokenKey] so [OAuthProvider] can be passed directly to
 * [com.devos.ai.core.security.SecureTokenRepository] without coupling
 * core-security to feature-auth.
 *
 * [prefKey] is the EncryptedSharedPreferences key used to store the access token.
 * [displayName] is the human-readable name shown in the UI.
 */
enum class OAuthProvider(
    override val prefKey: String,
    val displayName: String,
) : TokenKey {

    GITHUB(prefKey = "github_token", displayName = "GitHub"),
    GITLAB(prefKey = "gitlab_token", displayName = "GitLab"),
}
