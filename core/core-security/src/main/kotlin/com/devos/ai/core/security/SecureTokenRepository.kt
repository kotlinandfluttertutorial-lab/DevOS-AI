package com.devos.ai.core.security

/**
 * Repository contract for secure OAuth token storage.
 *
 * Implementations must back storage with Android Keystore-encrypted preferences.
 * Tokens are NEVER written to plaintext storage, BuildConfig, or logs.
 */
interface SecureTokenRepository {

    /**
     * Persists [token] for [key].
     *
     * The raw token is never logged; only the masked form `XXXX****` appears in logs.
     */
    suspend fun saveToken(key: TokenKey, token: String)

    /** Returns the stored token, or null if absent. */
    suspend fun getToken(key: TokenKey): String?

    /** Removes any token stored under [key]. */
    suspend fun clearToken(key: TokenKey)

    /** True if a non-null token is currently stored for [key]. */
    suspend fun hasToken(key: TokenKey): Boolean
}
