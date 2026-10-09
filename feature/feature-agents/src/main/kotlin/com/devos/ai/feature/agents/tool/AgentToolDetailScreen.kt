package com.devos.ai.feature.agents.tool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSCodeBlock
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Agent Tool Execution Detail screen — DEVOS-036 / FIGMA-20.
 *
 * Matches the `#s-agent-tool` mockup exactly:
 * - Tool card: 40dp wrench icon box (amber bg #3d2c00), tool name (15sp amber #FFCB6B), description, Done badge
 * - Stats row: DURATION / TOKENS / RESULTS
 * - "Input Parameters" section header + DevOSCodeBlock (JSON)
 * - "Output" section header + DevOSCodeBlock (JSON)
 * - "Show Raw JSON" toggle button
 */
@Composable
fun AgentToolDetailScreen(
    uiState: AgentToolDetailUiState,
    onNavigateBack: () -> Unit,
    onToggleRawJson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier       = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar         = {
            DevOSTopBar(
                title          = "Tool Execution",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        when (val s = uiState) {
            is AgentToolDetailUiState.Loading ->
                DevOSLoadingState(modifier = Modifier.padding(innerPadding))

            is AgentToolDetailUiState.Error ->
                DevOSErrorState(
                    description = s.message,
                    onRetry     = null,
                    modifier    = Modifier.padding(innerPadding),
                )

            is AgentToolDetailUiState.Success ->
                ToolDetailContent(
                    state           = s,
                    onToggleRawJson = onToggleRawJson,
                    modifier        = Modifier.padding(innerPadding),
                )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Success content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ToolDetailContent(
    state: AgentToolDetailUiState.Success,
    onToggleRawJson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amberColor = Color(0xFFFFCB6B)

    LazyColumn(
        modifier       = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DevOSSpacing.base),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.base),
    ) {
        // ── Tool card ─────────────────────────────────────────────────────────
        item(key = "tool_card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(DevOSSpacing.base),
            ) {
                Column {
                    // Header row: icon + name/desc + badge
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
                        modifier              = Modifier.padding(bottom = DevOSSpacing.sm),
                    ) {
                        // Wrench icon box — 40×40dp, #3d2c00 bg, 10dp radius
                        Box(
                            modifier         = Modifier
                                .size(40.dp)
                                .background(Color(0xFF3D2C00), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🔧", fontSize = 20.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = state.toolName,
                                color      = amberColor,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text  = state.toolDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        DevOSStatusBadge(
                            status = if (state.isSuccess) DevOSBadgeStatus.SUCCESS else DevOSBadgeStatus.ERROR,
                            label  = if (state.isSuccess) "✓ ${state.statusLabel}" else state.statusLabel,
                        )
                    }

                    // Stats row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(0.dp),
                            )
                            .padding(vertical = DevOSSpacing.sm),
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xl),
                        ) {
                            StatItem(
                                label = "DURATION",
                                value = formatDuration(state.durationMs),
                            )
                            StatItem(label = "TOKENS",  value = "—")
                            StatItem(label = "RESULTS", value = "—")
                        }
                    }
                }
            }
        }

        // ── Input Parameters ──────────────────────────────────────────────────
        item(key = "input_header") {
            DevOSSectionHeader(title = "Input Parameters")
        }
        item(key = "input_code") {
            DevOSCodeBlock(
                code     = state.inputJson,
                language = "json",
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Tool input parameters" },
            )
        }

        // ── Output ────────────────────────────────────────────────────────────
        item(key = "output_header") {
            DevOSSectionHeader(title = "Output")
        }
        item(key = "output_code") {
            DevOSCodeBlock(
                code     = state.outputJson,
                language = "json",
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Tool output" },
            )
        }

        // ── Raw JSON toggle ───────────────────────────────────────────────────
        item(key = "raw_toggle") {
            DevOSButton(
                text     = if (state.showRawJson) "Hide Raw JSON" else "Show Raw JSON",
                onClick  = onToggleRawJson,
                style    = DevOSButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text       = label,
            fontSize   = 10.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatDuration(ms: Long): String = when {
    ms <= 0   -> "—"
    ms < 1000 -> "${ms}ms"
    else      -> "${"%.2f".format(ms / 1000.0)}s"
}
