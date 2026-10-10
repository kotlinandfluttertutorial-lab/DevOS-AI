package com.devos.ai.feature.agents.mcp

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.domain.ai.model.MCPServer
import com.devos.ai.domain.ai.model.MCPServerStatus
import com.devos.ai.domain.ai.model.MCPTool

/**
 * MCP Tools screen — DEVOS-039 / DA-51.
 *
 * Matches the `#s-mcp-tools` mockup:
 * - DevOSTopBar: back + "MCP Tools"
 * - Connected Servers section: server cards with colored status dot + badge + tool count
 * - GitHub MCP Tools section: tool rows (icon + name + desc + Run button)
 * - Destructive tools: amber ⚠ icon, error-container Run button
 * - AlertDialog confirmation for destructive tool execution
 */
@Composable
fun MCPToolsScreen(
    uiState: MCPUiState,
    onNavigateBack: () -> Unit,
    onSelectServer: (String) -> Unit,
    onRunTool: (MCPTool) -> Unit,
    onConfirmDestructive: () -> Unit,
    onCancelDestructive: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier       = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar         = {
            DevOSTopBar(
                title          = "MCP Tools",
                navigationIcon = {
                    IconButton(
                        onClick  = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val s = uiState) {
            is MCPUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is MCPUiState.Empty -> DevOSEmptyState(
                icon        = Icons.Outlined.Extension,
                title       = "No MCP Servers",
                description = "Connect an MCP server to use external tools",
                modifier    = Modifier.padding(innerPadding),
            )

            is MCPUiState.Error -> DevOSErrorState(
                description = s.message,
                onRetry     = if (s.retryable) onRetry else null,
                modifier    = Modifier.padding(innerPadding),
            )

            is MCPUiState.Success -> {
                MCPSuccessContent(
                    state                = s,
                    onSelectServer       = onSelectServer,
                    onRunTool            = onRunTool,
                    modifier             = Modifier.padding(innerPadding),
                )

                // Destructive confirmation dialog
                if (s.pendingDestructiveTool != null) {
                    DestructiveConfirmationDialog(
                        tool      = s.pendingDestructiveTool,
                        onConfirm = onConfirmDestructive,
                        onCancel  = onCancelDestructive,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Success content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MCPSuccessContent(
    state: MCPUiState.Success,
    onSelectServer: (String) -> Unit,
    onRunTool: (MCPTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier       = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = DevOSSpacing.xxl,
        ),
    ) {
        // ── Connected Servers section header ──────────────────────────────────
        item(key = "servers_header") {
            SectionHeader(title = "Connected Servers")
        }

        // ── Server cards ──────────────────────────────────────────────────────
        items(
            items = state.servers,
            key   = { it.id },
        ) { server ->
            ServerCard(
                server     = server,
                isSelected = server.id == state.selectedServer?.id,
                onClick    = { onSelectServer(server.id) },
            )
        }

        // ── Tools section header ──────────────────────────────────────────────
        val selectedServerName = state.selectedServer?.name ?: "Server"
        item(key = "tools_header") {
            Spacer(Modifier.height(DevOSSpacing.xs))
            SectionHeader(title = "$selectedServerName — Tools")
        }

        // ── Tool rows ─────────────────────────────────────────────────────────
        items(
            items = state.tools,
            key   = { it.name },
        ) { tool ->
            ToolRow(
                tool     = tool,
                onRunTool = { onRunTool(tool) },
            )
            if (tool != state.tools.lastOrNull()) {
                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = DevOSSpacing.base),
                    thickness = DevOSSpacing.dividerThickness,
                    color     = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            horizontal = DevOSSpacing.base,
            vertical   = DevOSSpacing.sm,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Server card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ServerCard(
    server: MCPServer,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline

    DevOSCard(
        onClick  = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.xs)
            .semantics { contentDescription = "${server.name} server, ${server.status.name}" },
    ) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.cardPadding),
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                // Status dot — 8dp, color matches server status
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = serverStatusDotColor(server.status),
                            shape = CircleShape,
                        ),
                )

                // Server name
                Text(
                    text       = server.name,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.weight(1f),
                )

                // Status badge
                DevOSStatusBadge(
                    status = serverStatusBadge(server.status),
                    label  = serverStatusLabel(server.status),
                )
            }

            Spacer(Modifier.height(DevOSSpacing.xs))

            // Tool count subtitle
            Text(
                text  = "${server.toolCount} tools available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun serverStatusDotColor(status: MCPServerStatus): Color = when (status) {
    MCPServerStatus.CONNECTED    -> MaterialTheme.colorScheme.tertiary
    MCPServerStatus.IDLE         -> MaterialTheme.colorScheme.secondary
    MCPServerStatus.DISCONNECTED -> MaterialTheme.colorScheme.error
}

private fun serverStatusBadge(status: MCPServerStatus): DevOSBadgeStatus = when (status) {
    MCPServerStatus.CONNECTED    -> DevOSBadgeStatus.SUCCESS
    MCPServerStatus.IDLE         -> DevOSBadgeStatus.WARNING
    MCPServerStatus.DISCONNECTED -> DevOSBadgeStatus.ERROR
}

private fun serverStatusLabel(status: MCPServerStatus): String = when (status) {
    MCPServerStatus.CONNECTED    -> "Connected"
    MCPServerStatus.IDLE         -> "Idle"
    MCPServerStatus.DISCONNECTED -> "Disconnected"
}

// ─────────────────────────────────────────────────────────────────────────────
// Tool row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ToolRow(
    tool: MCPTool,
    onRunTool: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Icon background: amber for both normal and destructive (matches mockup #3d2c00)
    val iconBg    = Color(0xFF3D2C00)
    val iconEmoji = if (tool.isDestructive) "⚠" else "🔧"

    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm)
            .semantics { contentDescription = "${tool.name}: ${tool.description}" },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        // Icon box — 40dp with amber background
        Box(
            modifier         = Modifier
                .size(DevOSSpacing.stepIconSize)
                .background(iconBg, RoundedCornerShape(DevOSSpacing.sm)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = iconEmoji,
                fontSize = 16.sp,
            )
        }

        // Tool name + description
        Column(
            modifier              = Modifier.weight(1f),
            verticalArrangement   = Arrangement.spacedBy(DevOSSpacing.xxs),
        ) {
            Text(
                text  = tool.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(DevOSSpacing.xs))

        // Run button — destructive uses error container colors; normal uses outlined
        if (tool.isDestructive) {
            Button(
                onClick  = onRunTool,
                modifier = Modifier
                    .height(32.dp)
                    .semantics {
                        contentDescription = "Run ${tool.name} — destructive action"
                        role               = Role.Button
                    },
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = DevOSSpacing.sm,
                    vertical   = DevOSSpacing.xxs,
                ),
                shape = RoundedCornerShape(DevOSSpacing.xs),
            ) {
                Text(
                    text     = "Run",
                    fontSize = 11.sp,
                )
            }
        } else {
            OutlinedButton(
                onClick  = onRunTool,
                modifier = Modifier
                    .height(32.dp)
                    .semantics {
                        contentDescription = "Run ${tool.name}"
                        role               = Role.Button
                    },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = DevOSSpacing.sm,
                    vertical   = DevOSSpacing.xxs,
                ),
                shape = RoundedCornerShape(DevOSSpacing.xs),
            ) {
                Text(
                    text     = "Run",
                    fontSize = 11.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Destructive confirmation dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DestructiveConfirmationDialog(
    tool: MCPTool,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest  = onCancel,
        title             = {
            Text(
                text  = "⚠ Confirm Destructive Action",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        },
        text              = {
            Text(
                text  = "Are you sure you want to run \"${tool.name}\"?\n\nThis action is destructive and cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton     = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Run Anyway")
            }
        },
        dismissButton     = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
    )
}
