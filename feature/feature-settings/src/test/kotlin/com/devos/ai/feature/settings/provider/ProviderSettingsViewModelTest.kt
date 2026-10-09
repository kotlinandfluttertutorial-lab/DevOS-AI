package com.devos.ai.feature.settings.provider

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.devos.ai.domain.ai.model.AIProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ProviderSettingsViewModel].
 *
 * DEVOS-034 / DA-45
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProviderSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProviderSettingsViewModel

    @BeforeEach
    fun setUp() {
        viewModel = ProviderSettingsViewModel()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state transitions to Success with stub providers`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(ProviderSettingsUiState.Success::class)
    }

    @Test
    fun `stub data loads 4 providers`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        assertThat(state.providers.size).isEqualTo(4)
    }

    @Test
    fun `Anthropic card is configured in stub data`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        val card  = state.providers.first { it.provider == AIProvider.ANTHROPIC }
        assertThat(card.isConfigured).isTrue()
        assertThat(card.maskedKey).isNotNull()
    }

    @Test
    fun `Ollama card is not configured in stub data`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        val card  = state.providers.first { it.provider == AIProvider.OLLAMA }
        assertThat(card.isConfigured).isFalse()
        assertThat(card.maskedKey).isNull()
    }

    // ── onEditKey ─────────────────────────────────────────────────────────────

    @Test
    fun `onEditKey sets isEditingKey true for that provider`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onEditKey(AIProvider.ANTHROPIC)
        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        val card  = state.providers.first { it.provider == AIProvider.ANTHROPIC }
        assertThat(card.isEditingKey).isTrue()
    }

    @Test
    fun `onEditKey does not affect other providers`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onEditKey(AIProvider.ANTHROPIC)
        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        val openai = state.providers.first { it.provider == AIProvider.OPENAI }
        assertThat(openai.isEditingKey).isFalse()
    }

    // ── onConfigureProvider ───────────────────────────────────────────────────

    @Test
    fun `onConfigureProvider sets isEditingKey true for unconfigured provider`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onConfigureProvider(AIProvider.OLLAMA)
        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        val card  = state.providers.first { it.provider == AIProvider.OLLAMA }
        assertThat(card.isEditingKey).isTrue()
    }

    // ── onCancelEdit ──────────────────────────────────────────────────────────

    @Test
    fun `onCancelEdit clears isEditingKey`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onEditKey(AIProvider.OPENAI)
        viewModel.onCancelEdit(AIProvider.OPENAI)
        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        val card  = state.providers.first { it.provider == AIProvider.OPENAI }
        assertThat(card.isEditingKey).isFalse()
    }

    // ── onSaveKey ─────────────────────────────────────────────────────────────

    @Test
    fun `onSaveKey with blank key does nothing`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onEditKey(AIProvider.OLLAMA)
        viewModel.onSaveKey(AIProvider.OLLAMA, "   ")
        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        val card  = state.providers.first { it.provider == AIProvider.OLLAMA }
        // Still in editing state (save was ignored)
        assertThat(card.isEditingKey).isTrue()
    }

    @Test
    fun `onSaveKey with valid key starts testing connection`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onEditKey(AIProvider.OLLAMA)
        viewModel.onSaveKey(AIProvider.OLLAMA, "http://localhost:11434")

        // Immediately after call: should be testing
        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        val card  = state.providers.first { it.provider == AIProvider.OLLAMA }
        assertThat(card.isTestingConnection).isTrue()
        assertThat(card.isEditingKey).isFalse()
    }

    @Test
    fun `onSaveKey completes and marks provider as configured`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onEditKey(AIProvider.OLLAMA)
        viewModel.onSaveKey(AIProvider.OLLAMA, "http://localhost:11434")
        // Advance past the delay
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as ProviderSettingsUiState.Success
        val card  = state.providers.first { it.provider == AIProvider.OLLAMA }
        assertThat(card.isConfigured).isTrue()
        assertThat(card.isTestingConnection).isFalse()
    }

    // ── onNavigateBack ────────────────────────────────────────────────────────

    @Test
    fun `onNavigateBack emits NavigateBack event`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.navEvent.test {
            viewModel.onNavigateBack()
            dispatcher.scheduler.advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ProviderSettingsNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
