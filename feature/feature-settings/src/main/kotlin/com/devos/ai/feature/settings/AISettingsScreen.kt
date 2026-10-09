package com.devos.ai.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing

/**
 * AI Settings screen matching mockup #s-ai-settings.
 *
 * Stateless composable — all state comes from [AISettingsUiState], all events go up
 * via callback parameters. No ViewModel reference flows through the tree.
 *
 * DEVOS-033 / DA-46
 */
@Composable
fun AISettingsScreen(
    uiState: AISettingsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToModelPicker: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onUpdateTopKResults: (Int) -> Unit,
    onUpdateChunkSize: (Int) -> Unit,
    onUpdateAgentMaxSteps: (Int) -> Unit,
    onUpdateAutoApproveSafeTools: (Boolean) -> Unit,
    onUpdateMemoryEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DevOSTopBar(
                title = "AI Settings",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is AISettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    DevOSLoadingState()
                }
            }

            is AISettingsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    DevOSErrorState(
                        description = state.message,
                        onRetry = null,
                    )
                }
            }

            is AISettingsUiState.Success -> {
                AISettingsContent(
                    settings = state.settings,
                    onNavigateToModelPicker = onNavigateToModelPicker,
                    onNavigateToProviders = onNavigateToProviders,
                    onUpdateTopKResults = onUpdateTopKResults,
                    onUpdateChunkSize = onUpdateChunkSize,
                    onUpdateAgentMaxSteps = onUpdateAgentMaxSteps,
                    onUpdateAutoApproveSafeTools = onUpdateAutoApproveSafeTools,
                    onUpdateMemoryEnabled = onUpdateMemoryEnabled,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

// ── Success content ──────────────────────────────────────────────────────────────

@Composable
private fun AISettingsContent(
    settings: AISettings,
    onNavigateToModelPicker: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onUpdateTopKResults: (Int) -> Unit,
    onUpdateChunkSize: (Int) -> Unit,
    onUpdateAgentMaxSteps: (Int) -> Unit,
    onUpdateAutoApproveSafeTools: (Boolean) -> Unit,
    onUpdateMemoryEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {

        // ── Token usage card ─────────────────────────────────────────────────
        item {
            TokenUsageCard(
                usage = settings.tokenUsage,
                limit = settings.tokenLimit,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.base,
                    vertical = MaterialTheme.spacing.md,
                ),
            )
        }

        // ── GROUP: MODEL ─────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(title = "MODEL")
            SettingsNavRow(
                icon = Icons.Outlined.SmartToy,
                iconContentDescription = "Default Model",
                label = "Default Model",
                subtitle = settings.defaultModel,
                onClick = onNavigateToModelPicker,
            )
            SettingsNavRow(
                icon = Icons.Outlined.Key,
                iconContentDescription = "API Providers",
                label = "API Providers",
                subtitle = "OpenAI, Anthropic configured",
                onClick = onNavigateToProviders,
            )
        }

        // ── Divider ──────────────────────────────────────────────────────────
        item {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = DevOSSpacing.dividerThickness,
            )
        }

        // ── GROUP: RAG ───────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(title = "RAG")
            SettingsStepperRow(
                icon = Icons.Outlined.FolderOpen,
                iconContentDescription = "Top-K Results",
                label = "Top-K Results",
                subtitle = "${settings.topKResults} chunks per query",
                value = settings.topKResults,
                onIncrement = { onUpdateTopKResults(settings.topKResults + 1) },
                onDecrement = {
                    if (settings.topKResults > 1) onUpdateTopKResults(settings.topKResults - 1)
                },
            )
            SettingsStepperRow(
                icon = Icons.Outlined.Architecture,
                iconContentDescription = "Chunk Size",
                label = "Chunk Size",
                subtitle = "${settings.chunkSize} tokens",
                value = settings.chunkSize,
                onIncrement = { onUpdateChunkSize(settings.chunkSize + 64) },
                onDecrement = {
                    if (settings.chunkSize > 64) onUpdateChunkSize(settings.chunkSize - 64)
                },
            )
        }

        // ── Divider ──────────────────────────────────────────────────────────
        item {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = DevOSSpacing.dividerThickness,
            )
        }

        // ── GROUP: AGENT ─────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(title = "AGENT")
            SettingsStepperRow(
                icon = Icons.Outlined.Numbers,
                iconContentDescription = "Max Steps",
                label = "Max Steps",
                subtitle = "${settings.agentMaxSteps} steps per run",
                value = settings.agentMaxSteps,
                onIncrement = {
                    if (settings.agentMaxSteps < 50) onUpdateAgentMaxSteps(settings.agentMaxSteps + 1)
                },
                onDecrement = {
                    if (settings.agentMaxSteps > 1) onUpdateAgentMaxSteps(settings.agentMaxSteps - 1)
                },
            )
            SettingsToggleRow(
                icon = Icons.Outlined.CheckCircleOutline,
                iconContentDescription = "Auto-approve safe tools",
                label = "Auto-approve safe tools",
                subtitle = "Read-only tools run without confirmation",
                checked = settings.autoApproveSafeTools,
                onCheckedChange = onUpdateAutoApproveSafeTools,
            )
        }

        // ── Divider ──────────────────────────────────────────────────────────
        item {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = DevOSSpacing.dividerThickness,
            )
        }

        // ── GROUP: MEMORY ────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(title = "MEMORY")
            SettingsToggleRow(
                icon = Icons.Outlined.Memory,
                iconContentDescription = "Enable Developer Memory",
                label = "Enable Developer Memory",
                subtitle = "AI learns your preferences",
                checked = settings.memoryEnabled,
                onCheckedChange = onUpdateMemoryEnabled,
            )
        }

        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
        }
    }
}

// ── Token usage card ─────────────────────────────────────────────────────────────

@Composable
private fun TokenUsageCard(
    usage: Int,
    limit: Int,
    modifier: Modifier = Modifier,
) {
    val percentage = if (limit > 0) (usage.toFloat() / limit.toFloat()) else 0f
    val percentLabel = "${(percentage * 100).toInt()}%"

    DevOSCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.cardPadding)) {
            Text(
                text = "TOKEN USAGE THIS MONTH",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.W600,
                    letterSpacing = 0.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "%,d / %,d".format(usage, limit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = percentLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
