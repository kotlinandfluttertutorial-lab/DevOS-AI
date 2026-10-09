package com.devos.ai.feature.code.symbol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSCodeBlock
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSCodeTextStyle
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.code.model.CodeSymbolUi
import com.devos.ai.feature.code.model.SymbolMethod
import com.devos.ai.feature.code.model.SymbolRef

/**
 * Symbol Details screen — DEVOS-022 / FIGMA-13.
 *
 * Shows symbol kind badge, name, package, file:line link, signature code block,
 * AI explanation card, references list, and methods list.
 */
@Composable
fun SymbolDetailsScreen(
    uiState: SymbolDetailsUiState,
    onFileLinkTap: () -> Unit,
    onReferenceTap: (SymbolRef) -> Unit,
    onAskAiTap: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Symbol Details",
                navigationIcon = {
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
                },
            )
        },
    ) { innerPadding ->
        when (val s = uiState) {
            is SymbolDetailsUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )
            is SymbolDetailsUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.FolderOff,
                title = "Symbol not found",
                description = "The requested symbol could not be located in the index",
                modifier = Modifier.padding(innerPadding),
            )
            is SymbolDetailsUiState.Error -> DevOSErrorState(
                description = s.message,
                onRetry = if (s.retryable) ({}) else null,
                modifier = Modifier.padding(innerPadding),
            )
            is SymbolDetailsUiState.Success -> {
                SymbolDetailsContent(
                    symbol = s.symbol,
                    onFileLinkTap = onFileLinkTap,
                    onReferenceTap = onReferenceTap,
                    onAskAiTap = onAskAiTap,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun SymbolDetailsContent(
    symbol: CodeSymbolUi,
    onFileLinkTap: () -> Unit,
    onReferenceTap: (SymbolRef) -> Unit,
    onAskAiTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.base),
    ) {
        // ── Symbol header card ─────────────────────────────────────────────
        item {
            SymbolHeaderCard(
                symbol = symbol,
                onFileLinkTap = onFileLinkTap,
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
            )
        }

        // ── Signature ──────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                DevOSSectionHeader(title = "Signature")
                DevOSCodeBlock(
                    code = symbol.signature,
                    language = "kotlin",
                    showLineNumbers = false,
                )
            }
        }

        // ── AI Explanation ─────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                DevOSSectionHeader(title = "AI Explanation")
                AiExplanationCard(explanation = symbol.aiExplanation)
            }
        }

        // ── References ─────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DevOSSectionHeader(
                    title = "References (${symbol.references.size})",
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {},
                    modifier = Modifier.semantics { contentDescription = "View all references" },
                ) {
                    Text(
                        text = "All →",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        items(symbol.references, key = { "${it.fileName}:${it.lineNumber}" }) { ref ->
            ReferenceRow(
                ref = ref,
                onTap = { onReferenceTap(ref) },
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                thickness = DevOSSpacing.dividerThickness,
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
            )
        }

        // ── Methods ────────────────────────────────────────────────────────
        item {
            DevOSSectionHeader(
                title = "Methods (${symbol.methods.size})",
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
            )
        }

        items(symbol.methods, key = { it.name }) { method ->
            MethodRow(method = method)
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                thickness = DevOSSpacing.dividerThickness,
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
            )
        }

        // ── Ask AI button ──────────────────────────────────────────────────
        item {
            DevOSButton(
                text = "Explain with AI",
                onClick = onAskAiTap,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.xl))
        }
    }
}

// ── Symbol header card ────────────────────────────────────────────────────────

@Composable
private fun SymbolHeaderCard(
    symbol: CodeSymbolUi,
    onFileLinkTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DevOSCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.base),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        ) {
            // Kind badge
            KindBadge(kind = symbol.kind)

            // Symbol name
            Text(
                text = symbol.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = DevOSCodeTextStyle.fontSize * 18 / 13,
                ),
                color = MaterialTheme.colorScheme.primary,
                fontFamily = DevOSCodeTextStyle.fontFamily,
            )

            // Package name
            Text(
                text = symbol.packageName,
                style = DevOSCodeTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // File:line link
            Row(
                modifier = Modifier
                    .clickable(onClick = onFileLinkTap)
                    .semantics {
                        contentDescription = "Open ${symbol.filePath} at line ${symbol.lineNumber}"
                        role = Role.Button
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                )
                Text(
                    text = "${symbol.filePath.substringAfterLast("/")}:${symbol.lineNumber}",
                    style = DevOSCodeTextStyle,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ── Kind badge ────────────────────────────────────────────────────────────────

@Composable
private fun KindBadge(
    kind: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.xxs),
    ) {
        Text(
            text = kind,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

// ── AI explanation card ───────────────────────────────────────────────────────

@Composable
private fun AiExplanationCard(
    explanation: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(DevOSSpacing.base),
    ) {
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Reference row ─────────────────────────────────────────────────────────────

@Composable
private fun ReferenceRow(
    ref: SymbolRef,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(
                horizontal = DevOSSpacing.base,
                vertical = DevOSSpacing.sm,
            )
            .semantics {
                contentDescription = "${ref.fileName} line ${ref.lineNumber}"
                role = Role.Button
            },
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        // File badge
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = DevOSSpacing.xs, vertical = DevOSSpacing.xxs),
        ) {
            Text(
                text = "file",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ref.fileName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "line ${ref.lineNumber} · ${ref.context}",
                style = DevOSCodeTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

// ── Method row ────────────────────────────────────────────────────────────────

@Composable
private fun MethodRow(
    method: SymbolMethod,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = DevOSSpacing.base,
                vertical = DevOSSpacing.sm,
            ),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        // fn badge
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = DevOSSpacing.xs, vertical = DevOSSpacing.xxs),
        ) {
            Text(
                text = "fn",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = method.name,
                style = DevOSCodeTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = method.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
