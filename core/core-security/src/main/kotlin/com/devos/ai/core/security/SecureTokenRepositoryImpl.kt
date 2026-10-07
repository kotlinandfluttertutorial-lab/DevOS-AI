package com.devos.ai.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EncryptedSharedPreferences-backed implementation of [SecureTokenRepository].
 *
 * Security guarantees:
 * - Keys encrypted with AES256-SIV
 * - Values encrypted with AES256-GCM via Android Keystore master key
 * - Raw token values are NEVER written to Logcat — only `token.take(4)+"****"`
 */
@Singleton
class SecureTokenRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SecureTokenRepository {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "devos_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun saveToken(key: TokenKey, token: String) {
        prefs.edit().putString(key.prefKey, token).apply()
        // SECURITY: only log the masked form — never the real token value
        Timber.d("Token saved for ${key.name}: ${token.take(4)}****")
    }

    override suspend fun getToken(key: TokenKey): String? =
        prefs.getString(key.prefKey, null)

    override suspend fun clearToken(key: TokenKey) {
        prefs.edit().remove(key.prefKey).apply()
    }

    override suspend fun hasToken(key: TokenKey): Boolean =
        getToken(key) != null
}
