package com.devos.ai.feature.auth

import android.content.Context
import assertk.assertThat
import assertk.assertions.isNull
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.feature.auth.login.AuthViewModel
import com.devos.ai.feature.auth.model.OAuthProvider
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Tests for AuthViewModel.exchangeCodeForToken.
 *
 * In unit test environments real network calls fail, so we verify the function
 * handles failures gracefully and returns null rather than throwing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeCodeForTokenTest {

    private val mockContext: Context = mockk(relaxed = true)
    private val mockTokenRepository: SecureTokenRepository = mockk()

    @Test
    fun `returns null when network call fails`() = runTest {
        val viewModel = AuthViewModel(
            secureTokenRepository = mockTokenRepository,
            context = mockContext,
        )
        // Real network is unavailable in unit tests — exchangeCodeForToken should return null
        val result = viewModel.exchangeCodeForToken("test_code", OAuthProvider.GITHUB)
        assertThat(result).isNull()
    }

    @Test
    fun `returns null for GitLab when network call fails`() = runTest {
        val viewModel = AuthViewModel(
            secureTokenRepository = mockTokenRepository,
            context = mockContext,
        )
        val result = viewModel.exchangeCodeForToken("test_code", OAuthProvider.GITLAB)
        assertThat(result).isNull()
    }
}
