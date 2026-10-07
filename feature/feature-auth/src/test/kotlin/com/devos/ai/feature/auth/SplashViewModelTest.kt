package com.devos.ai.feature.auth

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.devos.ai.feature.auth.splash.SplashNavEvent
import com.devos.ai.feature.auth.splash.SplashViewModel
import com.devos.ai.feature.auth.usecase.AuthCheckResult
import com.devos.ai.feature.auth.usecase.CheckAuthStateUseCase
import io.mockk.coEvery
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

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var checkAuthState: CheckAuthStateUseCase

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        checkAuthState = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): SplashViewModel = SplashViewModel(checkAuthState)

    @Test
    fun `authenticated user receives ToHome nav event`() = runTest {
        coEvery { checkAuthState() } returns AuthCheckResult(
            onboardingComplete = true,
            isAuthenticated = true,
        )

        val vm = buildViewModel()

        vm.navEvent.test {
            assertThat(awaitItem()).isEqualTo(SplashNavEvent.ToHome)
        }
    }

    @Test
    fun `first launch unauthenticated receives ToOnboarding nav event`() = runTest {
        coEvery { checkAuthState() } returns AuthCheckResult(
            onboardingComplete = false,
            isAuthenticated = false,
        )

        val vm = buildViewModel()

        vm.navEvent.test {
            assertThat(awaitItem()).isEqualTo(SplashNavEvent.ToOnboarding)
        }
    }

    @Test
    fun `returning unauthenticated user receives ToLogin nav event`() = runTest {
        coEvery { checkAuthState() } returns AuthCheckResult(
            onboardingComplete = true,
            isAuthenticated = false,
        )

        val vm = buildViewModel()

        vm.navEvent.test {
            assertThat(awaitItem()).isEqualTo(SplashNavEvent.ToLogin)
        }
    }
}
