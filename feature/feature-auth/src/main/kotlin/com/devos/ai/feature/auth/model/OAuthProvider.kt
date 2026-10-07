package com.devos.ai.feature.auth.model

import com.devos.ai.core.security.TokenKey

/**
 * Supported OAuth providers for DevOS AI repository authentication.
 *
 * Implements [TokenKey] so instances can be passed directly to [SecureTokenRepository].
 *
 * [prefKey] is the EncryptedSharedPreferences key used to store the access token.
 * [name] is the human-readable name used in logs (masked) and UI.
 */
enum class OAuthProvider(
    override val prefKey: String,
    override val name: String,
) : TokenKey {
    GITHUB(prefKey = "github_token", name = "GitHub"),
    GITLAB(prefKey = "gitlab_token", name = "GitLab"),
}
