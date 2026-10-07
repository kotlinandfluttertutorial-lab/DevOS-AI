package com.devos.ai.feature.auth

import android.content.Context
import assertk.assertThat
import assertk.assertions.isNull
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.feature.auth.login.AuthViewModel
import com.devos.ai.feature.auth.model.OAuthProvider
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for AuthViewModel.exchangeCodeForToken.
 *
 * In unit test environments real network calls fail, so we verify the function
 * handles failures gracefully and returns null rather than throwing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeCodeForTokenTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockContext: Context = mockk(relaxed = true)
    private val mockTokenRepository: SecureTokenRepository = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AuthViewModel(
        secureTokenRepository = mockTokenRepository,
        context = mockContext,
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `returns null when GitHub network call fails`() = runTest {
        val viewModel = createViewModel()
        // Real network is unavailable in unit tests — exchangeCodeForToken should return null
        val result = viewModel.exchangeCodeForToken("test_code", OAuthProvider.GITHUB)
        assertThat(result).isNull()
    }

    @Test
    fun `returns null for GitLab when network call fails`() = runTest {
        val viewModel = createViewModel()
        val result = viewModel.exchangeCodeForToken("test_code", OAuthProvider.GITLAB)
        assertThat(result).isNull()
    }
}
