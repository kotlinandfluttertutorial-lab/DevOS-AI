package com.devos.ai.core.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding [SecureTokenRepository] to its EncryptedSharedPreferences
 * implementation.
 *
 * Installed in [SingletonComponent] — one repository instance per app process.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindSecureTokenRepository(
        impl: SecureTokenRepositoryImpl,
    ): SecureTokenRepository
}
