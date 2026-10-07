package com.devos.ai.feature.auth.model

import com.devos.ai.core.security.TokenKey

/**
 * Supported OAuth providers for DevOS AI repository authentication.
 *
 * Implements [TokenKey] so instances can be passed directly to [SecureTokenRepository].
 *
 * [prefKey]      EncryptedSharedPreferences key used to store the access token.
 * [displayName]  Human-readable name shown in the UI.
 *
 * Note: [TokenKey.name] is satisfied by the enum's built-in [Enum.name] property
 * ("GITHUB", "GITLAB"), which is used for log masking. [displayName] is used in UI.
 */
enum class OAuthProvider(
    override val prefKey: String,
    val displayName: String,
) : TokenKey {
    GITHUB(prefKey = "github_token", displayName = "GitHub"),
    GITLAB(prefKey = "gitlab_token", displayName = "GitLab"),
}
