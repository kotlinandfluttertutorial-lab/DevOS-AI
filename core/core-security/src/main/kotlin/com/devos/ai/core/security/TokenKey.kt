package com.devos.ai.core.security

/**
 * Minimal key abstraction for token storage.
 *
 * Implemented by [com.devos.ai.feature.auth.model.OAuthProvider] so that
 * core-security remains a pure Kotlin module with no dependency on feature-auth.
 */
interface TokenKey {
    /** The key used to store/retrieve the token in EncryptedSharedPreferences. */
    val prefKey: String

    /** Human-readable provider name — used in masked log output only. */
    val name: String
}
