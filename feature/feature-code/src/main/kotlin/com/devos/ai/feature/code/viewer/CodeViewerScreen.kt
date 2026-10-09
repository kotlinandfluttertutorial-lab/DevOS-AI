package com.devos.ai.feature.code.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSCodeBlock
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.theme.DevOSCodeTextStyle
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.SyntaxColors
import com.devos.ai.feature.code.model.CodeLine

/**
 * Code Viewer screen — DEVOS-019/020 / FIGMA-11.
 *
 * Renders syntax-highlighted code with line numbers, highlight strip,
 * symbol tooltip overlay, and bottom AI action bar.
 */
@Composable
fun CodeViewerScreen(
    uiState: CodeViewerUiState,
    activeAiChip: AiActionChip,
    onLineTap: (Int) -> Unit,
    onDismissTooltip: () -> Unit,
    onAiActionTap: (AiActionChip) -> Unit,
    onSearchTap: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = SyntaxColors.background,
        topBar = {
            CodeViewerTopBar(
                uiState = uiState,
                onSearchTap = onSearchTap,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val s = uiState) {
                is CodeViewerUiState.Loading -> DevOSLoadingState()
                is CodeViewerUiState.Empty -> DevOSEmptyState(
                    icon = Icons.Outlined.Search,
                    title = "No file loaded",
                    description = "Select a file to view its contents",
                )
                is CodeViewerUiState.Error -> DevOSErrorState(
                    description = s.message,
                    onRetry = if (s.retryable) ({}) else null,
                )
                is CodeViewerUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Code lines list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(SyntaxColors.background),
                        ) {
                            items(s.lines, key = { it.lineNumber }) { line ->
                                CodeLineRow(
                                    line = line,
                                    isHighlighted = line.lineNumber == s.selectedLine,
                                    onTap = { onLineTap(line.lineNumber) },
                                )
                            }
                        }

                        // AI action bar
                        AiActionBar(
                            activeChip = activeAiChip,
                            onChipTap = onAiActionTap,
                        )
                    }

                    // Symbol tooltip overlay
                    if (s.showSymbolTooltip) {
                        SymbolTooltip(
                            symbolName = s.tooltipSymbolName,
                            onDismiss = onDismissTooltip,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = DevOSSpacing.base),
                        )
                    }
                }
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun CodeViewerTopBar(
    uiState: CodeViewerUiState,
    onSearchTap: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SyntaxColors.background)
            .height(DevOSSpacing.topBarHeight),
    ) {
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomStart),
            color = MaterialTheme.colorScheme.outline,
            thickness = DevOSSpacing.dividerThickness,
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DevOSSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(DevOSSpacing.touchTarget)
                    .semantics { contentDescription = "Navigate back" },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // File path + name
            Column(modifier = Modifier.weight(1f)) {
                if (uiState is CodeViewerUiState.Success) {
                    Text(
                        text = uiState.filePath.substringBeforeLast("/"),
                        style = DevOSCodeTextStyle.copy(fontSize = DevOSCodeTextStyle.fontSize),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = uiState.fileName,
                        style = DevOSCodeTextStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }

            IconButton(
                onClick = onSearchTap,
                modifier = Modifier
                    .size(DevOSSpacing.touchTarget)
                    .semantics { contentDescription = "Search in file" },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(DevOSSpacing.touchTarget)
                    .semantics { contentDescription = "More options" },
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ── Code line row ─────────────────────────────────────────────────────────────

@Composable
private fun CodeLineRow(
    line: CodeLine,
    isHighlighted: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isHighlighted) Color(0xFF2D3748) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onTap)
            .padding(vertical = DevOSSpacing.xxs)
            .semantics { contentDescription = "Line ${line.lineNumber}: ${line.content}" },
        verticalAlignment = Alignment.Top,
    ) {
        // Line number
        Text(
            text = "${line.lineNumber}",
            style = DevOSCodeTextStyle,
            color = SyntaxColors.lineNumber,
            modifier = Modifier
                .width(DevOSSpacing.xxl)
                .padding(end = DevOSSpacing.xs),
        )

        // Code content — using DevOSCodeBlock's highlight logic inline
        // We render the line content as syntax highlighted text
        Text(
            text = line.content,
            style = DevOSCodeTextStyle,
            color = SyntaxColors.variable,
            modifier = Modifier
                .weight(1f)
                .padding(end = DevOSSpacing.base)
                .horizontalScroll(rememberScrollState()),
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ── Symbol tooltip ────────────────────────────────────────────────────────────

@Composable
private fun SymbolTooltip(
    symbolName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DevOSCard(
        onClick = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.base),
        ) {
            Text(
                text = symbolName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            Text(
                text = "Tap to see all usages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── AI action bar ─────────────────────────────────────────────────────────────

@Composable
private fun AiActionBar(
    activeChip: AiActionChip,
    onChipTap: (AiActionChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = DevOSSpacing.dividerThickness,
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            AiActionChip.entries.forEach { chip ->
                FilterChip(
                    selected = chip == activeChip,
                    onClick = { onChipTap(chip) },
                    label = {
                        Text(
                            text = chip.label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
        // Safe area padding
        Spacer(modifier = Modifier.height(DevOSSpacing.xl))
    }
}
