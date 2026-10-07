package com.devos.ai.feature.auth.usecase

import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.feature.auth.model.OAuthProvider
import com.devos.ai.feature.auth.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Exchanges an OAuth authorization code for an access token and persists it securely.
 *
 * On success, the token is stored via [SecureTokenRepository] and Unit is returned.
 * On failure, the original error is propagated as [Result.failure].
 *
 * The [dispatcher] parameter defaults to [Dispatchers.IO] for production use. Pass
 * a test dispatcher in unit tests by constructing directly (not via Hilt injection).
 *
 * @param authRepository Performs the network token exchange.
 * @param tokenRepository Persists the token in EncryptedSharedPreferences.
 * @param dispatcher IO dispatcher — defaults to [Dispatchers.IO].
 */
class ExchangeCodeForTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenRepository: SecureTokenRepository,
) {
    // Dispatcher is not injected to avoid requiring a Hilt binding for CoroutineDispatcher.
    // Tests override it via the secondary constructor.
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO

    /** Secondary constructor for test injection of a controlled dispatcher. */
    constructor(
        authRepository: AuthRepository,
        tokenRepository: SecureTokenRepository,
        dispatcher: CoroutineDispatcher,
    ) : this(authRepository, tokenRepository) {
        this.dispatcher = dispatcher
    }

    suspend operator fun invoke(
        code: String,
        provider: OAuthProvider,
    ): Result<Unit> = withContext(dispatcher) {
        authRepository
            .exchangeCodeForToken(code, provider)
            .onSuccess { token -> tokenRepository.saveToken(provider, token) }
            .map { }
    }
}
