package com.devos.ai.feature.auth.repository

import com.devos.ai.feature.auth.model.OAuthProvider

/**
 * Contract for exchanging an OAuth authorization code for an access token.
 *
 * Real implementation requires a backend token-exchange endpoint (DEVOS-041).
 * The current stub returns a failure so the UI can demonstrate the error state.
 */
interface AuthRepository {

    /**
     * Exchange an OAuth [code] (received via deep link callback) for an access token.
     *
     * @param code Authorization code from the OAuth provider redirect.
     * @param provider Which OAuth provider issued the code.
     * @return [Result.success] with the access token string, or [Result.failure] on any error.
     */
    suspend fun exchangeCodeForToken(code: String, provider: OAuthProvider): Result<String>
}
