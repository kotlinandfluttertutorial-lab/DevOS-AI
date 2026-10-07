package com.devos.ai.feature.auth.model

import com.devos.ai.core.security.TokenKey

/**
 * Secure storage keys for OAuth client IDs.
 *
 * Client IDs are treated like secrets: stored in EncryptedSharedPreferences via
 * [com.devos.ai.core.security.SecureTokenRepository] and never hardcoded in source.
 *
 * A UI (e.g. Settings → Developer Credentials) or a provisioning step must call
 * [com.devos.ai.core.security.SecureTokenRepository.saveToken] with the appropriate
 * key before the OAuth flow is initiated. Until then [SecureTokenRepository.getToken]
 * returns null and the OAuth URL will not be opened.
 *
 * @see OAuthProvider — the companion key set for storing access tokens.
 */
enum class OAuthClientIdKey(
    override val prefKey: String,
) : TokenKey {

    GITHUB(prefKey = "github_client_id"),
    GITLAB(prefKey = "gitlab_client_id"),
}
