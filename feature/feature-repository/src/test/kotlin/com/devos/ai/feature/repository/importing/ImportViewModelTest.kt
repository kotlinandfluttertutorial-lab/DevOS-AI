package com.devos.ai.feature.repository.importing

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.devos.ai.feature.repository.model.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ImportViewModel].
 *
 * Uses [UnconfinedTestDispatcher] so coroutines run eagerly.
 * Turbine is used for Flow / SharedFlow assertions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ImportViewModel(ioDispatcher = testDispatcher)

    // ── URL change ────────────────────────────────────────────────────────────

    @Test
    fun `onUrlChange updates url state`() = runTest {
        val viewModel = createViewModel()

        viewModel.onUrlChange("https://github.com/owner/repo")

        assertThat(viewModel.url.value).isEqualTo("https://github.com/owner/repo")
    }

    @Test
    fun `onUrlChange resets Preview state to Idle`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUrlChange("https://github.com/owner/repo")
        viewModel.validate()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(ImportUiState.Preview::class)

        viewModel.onUrlChange("https://github.com/new/url")

        assertThat(viewModel.uiState.value).isInstanceOf(ImportUiState.Idle::class)
    }

    @Test
    fun `onUrlChange resets Error state to Idle`() = runTest {
        val viewModel = createViewModel()
        // Trigger an error by validating a blank URL
        viewModel.validate()
        assertThat(viewModel.uiState.value).isInstanceOf(ImportUiState.Error::class)

        viewModel.onUrlChange("https://github.com/owner/repo")

        assertThat(viewModel.uiState.value).isInstanceOf(ImportUiState.Idle::class)
    }

    // ── Validate ──────────────────────────────────────────────────────────────

    @Test
    fun `validate with valid url transitions through Validating to Preview`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUrlChange("https://github.com/owner/repo")

        viewModel.uiState.test {
            skipItems(1) // consume current Idle state
            viewModel.validate()
            val validatingState = awaitItem()
            assertThat(validatingState).isInstanceOf(ImportUiState.Validating::class)
            val previewState = awaitItem()
            assertThat(previewState).isInstanceOf(ImportUiState.Preview::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validate with blank url emits Error state`() = runTest {
        val viewModel = createViewModel()
        // URL is blank by default

        viewModel.validate()

        assertThat(viewModel.uiState.value).isInstanceOf(ImportUiState.Error::class)
    }

    @Test
    fun `validate Preview state contains correct repo name`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUrlChange("https://github.com/myorg/my-repo")

        viewModel.validate()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ImportUiState.Preview
        assertThat(state.preview.name).isEqualTo("my-repo")
    }

    @Test
    fun `validate Preview state contains correct owner`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUrlChange("https://github.com/myorg/my-repo")

        viewModel.validate()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ImportUiState.Preview
        assertThat(state.preview.owner).isEqualTo("myorg")
    }

    // ── Import ────────────────────────────────────────────────────────────────

    @Test
    fun `import emits NavigateToSync nav event`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUrlChange("https://github.com/owner/repo")
        viewModel.validate()
        advanceUntilIdle()

        viewModel.navEvent.test {
            viewModel.import()
            val event = awaitItem()
            assertThat(event).isInstanceOf(ImportNavEvent.NavigateToSync::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `import NavigateToSync event carries a repoId`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUrlChange("https://github.com/owner/repo")
        viewModel.validate()
        advanceUntilIdle()

        viewModel.navEvent.test {
            viewModel.import()
            val event = awaitItem() as ImportNavEvent.NavigateToSync
            assertThat(event.repoId.isNotBlank()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Provider selection ────────────────────────────────────────────────────

    @Test
    fun `onProviderSelect updates selectedProvider to GITLAB`() = runTest {
        val viewModel = createViewModel()

        viewModel.onProviderSelect(RepositoryProvider.GITLAB)

        assertThat(viewModel.selectedProvider.value).isEqualTo(RepositoryProvider.GITLAB)
    }

    @Test
    fun `onProviderSelect updates selectedProvider to LOCAL`() = runTest {
        val viewModel = createViewModel()

        viewModel.onProviderSelect(RepositoryProvider.LOCAL)

        assertThat(viewModel.selectedProvider.value).isEqualTo(RepositoryProvider.LOCAL)
    }

    @Test
    fun `initial selectedProvider is GITHUB`() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.selectedProvider.value).isEqualTo(RepositoryProvider.GITHUB)
    }

    // ── Navigate back ─────────────────────────────────────────────────────────

    @Test
    fun `navigateBack emits NavigateBack nav event`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.navigateBack()
            assertThat(awaitItem()).isEqualTo(ImportNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Branch / AI index ─────────────────────────────────────────────────────

    @Test
    fun `initial branch is main`() = runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.branch.value).isEqualTo("main")
    }

    @Test
    fun `onBranchChange updates branch`() = runTest {
        val viewModel = createViewModel()
        viewModel.onBranchChange("develop")
        assertThat(viewModel.branch.value).isEqualTo("develop")
    }

    @Test
    fun `initial buildAiIndex is true`() = runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.buildAiIndex.value).isEqualTo(true)
    }

    @Test
    fun `onBuildAiIndexToggle sets value to false`() = runTest {
        val viewModel = createViewModel()
        viewModel.onBuildAiIndexToggle(false)
        assertThat(viewModel.buildAiIndex.value).isEqualTo(false)
    }
}
