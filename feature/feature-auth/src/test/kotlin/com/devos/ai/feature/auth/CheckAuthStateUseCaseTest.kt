package com.devos.ai.feature.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.feature.auth.model.OAuthProvider
import com.devos.ai.feature.auth.usecase.AuthCheckResult
import com.devos.ai.feature.auth.usecase.CheckAuthStateUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckAuthStateUseCaseTest {

    private val mockDataStore: DataStore<Preferences> = mockk()
    private val mockPreferences: Preferences = mockk()
    private val mockTokenRepository: SecureTokenRepository = mockk()

    private fun buildUseCase() = CheckAuthStateUseCase(
        dataStore = mockDataStore,
        tokenRepository = mockTokenRepository,
    )

    @Test
    fun `returns isAuthenticated true when GitHub token exists`() = runTest {
        every { mockDataStore.data } returns flowOf(mockPreferences)
        every { mockPreferences[CheckAuthStateUseCase.KEY_ONBOARDING_COMPLETE] } returns true
        coEvery { mockTokenRepository.hasToken(OAuthProvider.GITHUB) } returns true
        coEvery { mockTokenRepository.hasToken(OAuthProvider.GITLAB) } returns false

        val result = buildUseCase()()

        assertThat(result.isAuthenticated).isTrue()
    }

    @Test
    fun `returns isAuthenticated true when GitLab token exists`() = runTest {
        every { mockDataStore.data } returns flowOf(mockPreferences)
        every { mockPreferences[CheckAuthStateUseCase.KEY_ONBOARDING_COMPLETE] } returns true
        coEvery { mockTokenRepository.hasToken(OAuthProvider.GITHUB) } returns false
        coEvery { mockTokenRepository.hasToken(OAuthProvider.GITLAB) } returns true

        val result = buildUseCase()()

        assertThat(result.isAuthenticated).isTrue()
    }

    @Test
    fun `returns isAuthenticated false when no tokens`() = runTest {
        every { mockDataStore.data } returns flowOf(mockPreferences)
        every { mockPreferences[CheckAuthStateUseCase.KEY_ONBOARDING_COMPLETE] } returns true
        coEvery { mockTokenRepository.hasToken(OAuthProvider.GITHUB) } returns false
        coEvery { mockTokenRepository.hasToken(OAuthProvider.GITLAB) } returns false

        val result = buildUseCase()()

        assertThat(result.isAuthenticated).isFalse()
    }

    @Test
    fun `returns onboardingComplete true when pref is set`() = runTest {
        every { mockDataStore.data } returns flowOf(mockPreferences)
        every { mockPreferences[CheckAuthStateUseCase.KEY_ONBOARDING_COMPLETE] } returns true
        coEvery { mockTokenRepository.hasToken(any()) } returns false

        val result = buildUseCase()()

        assertThat(result.onboardingComplete).isTrue()
    }

    @Test
    fun `returns safe default on DataStore exception`() = runTest {
        every { mockDataStore.data } returns flow { throw RuntimeException("DataStore error") }

        val result = buildUseCase()()

        assertThat(result).isEqualTo(AuthCheckResult(onboardingComplete = false, isAuthenticated = false))
    }
}
