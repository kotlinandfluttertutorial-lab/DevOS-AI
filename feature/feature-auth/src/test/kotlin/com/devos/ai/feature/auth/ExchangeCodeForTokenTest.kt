package com.devos.ai.feature.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.feature.auth.model.OAuthProvider
import com.devos.ai.feature.auth.repository.AuthRepository
import com.devos.ai.feature.auth.usecase.ExchangeCodeForTokenUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ExchangeCodeForTokenUseCase].
 *
 * AC18 (DEVOS-011):
 * - Successful exchange calls [SecureTokenRepository.saveToken].
 * - Failed exchange returns [Result.failure] without calling saveToken.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeCodeForTokenTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockAuthRepo: AuthRepository = mockk()
    private val mockTokenRepo: SecureTokenRepository = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun useCase() = ExchangeCodeForTokenUseCase(
        authRepository = mockAuthRepo,
        tokenRepository = mockTokenRepo,
        dispatcher = testDispatcher,
    )

    @Test
    fun `successful exchange saves token to repository`() = runTest {
        coEvery { mockAuthRepo.exchangeCodeForToken("code", OAuthProvider.GITHUB) } returns
            Result.success("token_abc123")
        coJustRun { mockTokenRepo.saveToken(OAuthProvider.GITHUB, "token_abc123") }

        val result = useCase()("code", OAuthProvider.GITHUB)

        assertThat(result.isSuccess).isEqualTo(true)
        coVerify(exactly = 1) { mockTokenRepo.saveToken(OAuthProvider.GITHUB, "token_abc123") }
    }

    @Test
    fun `failed exchange returns failure without calling saveToken`() = runTest {
        val error = RuntimeException("Server error")
        coEvery { mockAuthRepo.exchangeCodeForToken("bad_code", OAuthProvider.GITLAB) } returns
            Result.failure(error)

        val result = useCase()("bad_code", OAuthProvider.GITLAB)

        assertThat(result.isFailure).isEqualTo(true)
        coVerify(exactly = 0) { mockTokenRepo.saveToken(any(), any()) }
    }

    @Test
    fun `failed exchange propagates original exception`() = runTest {
        val error = RuntimeException("Unauthorized")
        coEvery { mockAuthRepo.exchangeCodeForToken(any(), any()) } returns
            Result.failure(error)

        val result = useCase()("code", OAuthProvider.GITHUB)

        assertThat(result.isFailure).isEqualTo(true)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Unauthorized")
    }

    @Test
    fun `successful exchange for GitLab provider stores gitlab token`() = runTest {
        coEvery { mockAuthRepo.exchangeCodeForToken("gl_code", OAuthProvider.GITLAB) } returns
            Result.success("gl_token_xyz")
        coJustRun { mockTokenRepo.saveToken(OAuthProvider.GITLAB, "gl_token_xyz") }

        val result = useCase()("gl_code", OAuthProvider.GITLAB)

        assertThat(result.isSuccess).isEqualTo(true)
        coVerify(exactly = 1) { mockTokenRepo.saveToken(OAuthProvider.GITLAB, "gl_token_xyz") }
    }
}
