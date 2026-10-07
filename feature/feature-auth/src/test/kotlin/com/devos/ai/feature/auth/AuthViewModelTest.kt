package com.devos.ai.feature.auth

import android.content.Context
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.feature.auth.login.AuthViewModel
import com.devos.ai.feature.auth.login.LoginUiState
import com.devos.ai.feature.auth.model.OAuthProvider
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

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

    private fun createViewModel(): AuthViewModel = AuthViewModel(
        secureTokenRepository = mockTokenRepository,
        context = mockContext,
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `initial uiState is Idle`() = runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value).isEqualTo(LoginUiState.Idle)
    }

    /**
     * loginWithGitHub/GitLab set Loading synchronously before attempting
     * Chrome Custom Tab launch. CCT uses Android framework which is not available
     * in JVM unit tests, so we wrap the call and verify state was set.
     */
    @Test
    fun `loginWithGitHub sets uiState to Loading before CCT launch`() = runTest {
        val viewModel = createViewModel()
        runCatching { viewModel.loginWithGitHub() }
        assertThat(viewModel.uiState.value).isEqualTo(LoginUiState.Loading)
    }

    @Test
    fun `loginWithGitLab sets uiState to Loading before CCT launch`() = runTest {
        val viewModel = createViewModel()
        runCatching { viewModel.loginWithGitLab() }
        assertThat(viewModel.uiState.value).isEqualTo(LoginUiState.Loading)
    }

    @Test
    fun `handleAuthCallback network failure sets Error state`() = runTest {
        coJustRun { mockTokenRepository.saveToken(any(), any()) }
        val viewModel = createViewModel()
        viewModel.handleAuthCallback("some_code", OAuthProvider.GITHUB)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(LoginUiState.Error::class)
    }

    @Test
    fun `handleAuthCallback with empty code results in Error state`() = runTest {
        coJustRun { mockTokenRepository.saveToken(any(), any()) }
        val viewModel = createViewModel()
        viewModel.handleAuthCallback("", OAuthProvider.GITHUB)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(LoginUiState.Error::class)
    }
}
