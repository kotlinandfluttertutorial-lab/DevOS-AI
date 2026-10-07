package com.devos.ai.feature.auth.repository

import com.devos.ai.feature.auth.model.OAuthProvider
import javax.inject.Inject

/**
 * Stub implementation of [AuthRepository].
 *
 * The real token exchange requires a backend endpoint (DEVOS-041). This stub
 * returns a failure so the app compiles and the LoginScreen error state is
 * exercisable without a live server.
 */
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    override suspend fun exchangeCodeForToken(
        code: String,
        provider: OAuthProvider,
    ): Result<String> = Result.failure(
        NotImplementedError(
            "Token exchange endpoint not yet implemented — awaiting DEVOS-041.",
        ),
    )
}
