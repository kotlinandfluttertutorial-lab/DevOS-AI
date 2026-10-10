package com.devos.ai.feature.issues

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSMarkdownText
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Issue Detail screen — DEVOS-043 / DA-56.
 *
 * Implements the #s-issue-detail mockup:
 * - Issue title (18sp 700w)
 * - Labels row + metadata (date, author, comments)
 * - DevOSMarkdownText body
 * - AI Summary card (surface bg, 1dp primary border)
 * - Related code refs
 * - Action buttons (Assign / Close Issue with confirmation dialog)
 *
 * Stateless composable — all state comes from [IssueDetailUiState].
 */
@Composable
fun IssueDetailScreen(
    uiState: IssueDetailUiState,
    onNavigateBack: () -> Unit,
    onRequestClose: () -> Unit,
    onConfirmClose: () -> Unit,
    onDismissClose: () -> Unit,
    onCodeRefClick: (CodeRef) -> Unit,
    onAIFixSuggestion: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topBarTitle = when (val s = uiState) {
        is IssueDetailUiState.Success -> "#${s.issue.number} ${s.issue.labels.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: ""}"
        else -> "Issue"
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = topBarTitle,
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* overflow menu */ },
                        modifier = Modifier.semantics { contentDescription = "More options" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is IssueDetailUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is IssueDetailUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is IssueDetailUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.BugReport,
                title = "Issue Not Found",
                description = "This issue could not be found",
                modifier = Modifier.padding(innerPadding),
            )

            is IssueDetailUiState.Success -> {
                if (state.showCloseConfirmation) {
                    CloseIssueConfirmDialog(
                        onConfirm = onConfirmClose,
                        onDismiss = onDismissClose,
                    )
                }

                IssueDetailContent(
                    state = state,
                    onCodeRefClick = onCodeRefClick,
                    onAIFixSuggestion = onAIFixSuggestion,
                    onRequestClose = onRequestClose,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun IssueDetailContent(
    state: IssueDetailUiState.Success,
    onCodeRefClick: (CodeRef) -> Unit,
    onAIFixSuggestion: () -> Unit,
    onRequestClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val issue = state.issue

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = DevOSSpacing.xl),
    ) {
        // ── Issue title ───────────────────────────────────────────────────────
        item(key = "title") {
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.base,
                ),
            )
        }

        // ── Labels ────────────────────────────────────────────────────────────
        item(key = "labels") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
            ) {
                issue.labels.forEach { label ->
                    IssueDetailLabelBadge(label = label)
                }
            }
            Spacer(modifier = Modifier.height(DevOSSpacing.sm))
        }

        // ── Metadata ──────────────────────────────────────────────────────────
        item(key = "metadata") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.base),
            ) {
                Text(
                    text = "Opened ${issue.openedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "by ${issue.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "💬 ${issue.commentCount} comments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(DevOSSpacing.base))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                thickness = DevOSSpacing.dividerThickness,
            )
        }

        // ── Issue body (markdown) ─────────────────────────────────────────────
        item(key = "body") {
            DevOSMarkdownText(
                markdown = issue.body,
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.base,
                ),
            )
        }

        // ── AI Summary card ───────────────────────────────────────────────────
        if (state.aiSummary != null) {
            item(key = "ai_summary") {
                AIAnalysisCard(
                    summary = state.aiSummary,
                    onAIFix = onAIFixSuggestion,
                    modifier = Modifier.padding(
                        horizontal = DevOSSpacing.base,
                        vertical = DevOSSpacing.xs,
                    ),
                )
            }
        }

        // ── Related code refs ─────────────────────────────────────────────────
        if (state.relatedCodeRefs.isNotEmpty()) {
            item(key = "code_refs_header") {
                DevOSSectionHeader(
                    title = "Related Code (${state.relatedCodeRefs.size})",
                    modifier = Modifier.padding(
                        horizontal = DevOSSpacing.base,
                        vertical = DevOSSpacing.sm,
                    ),
                )
            }
            items(
                items = state.relatedCodeRefs,
                key = { ref -> ref.filePath + ref.lineNumber },
            ) { ref ->
                CodeRefRow(
                    ref = ref,
                    onClick = { onCodeRefClick(ref) },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = DevOSSpacing.dividerThickness,
                    modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                )
            }
        }

        // ── Action buttons ────────────────────────────────────────────────────
        item(key = "actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.base),
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                DevOSButton(
                    text = "Assign",
                    onClick = { /* assign action */ },
                    style = DevOSButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                DevOSButton(
                    text = if (issue.state == IssueState.OPEN) "Close Issue" else "Reopen",
                    onClick = onRequestClose,
                    style = DevOSButtonStyle.Primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Colored label badge for the detail view. */
@Composable
private fun IssueDetailLabelBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor) = when (label.lowercase()) {
        "bug" -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        "enhancement", "feature", "ai" -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        "p1", "p2" -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(DevOSSpacing.xs))
            .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

/**
 * AI Analysis card with primary left border — matches #s-issue-detail mockup.
 */
@Composable
private fun AIAnalysisCard(
    summary: String,
    onAIFix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(DevOSSpacing.sm),
            ),
        shape = RoundedCornerShape(DevOSSpacing.sm),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.md),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                )
                Text(
                    text = "AI Analysis",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            DevOSMarkdownText(
                markdown = summary,
                style = MaterialTheme.typography.bodySmall,
            )
            DevOSButton(
                text = "Get AI fix suggestion →",
                onClick = onAIFix,
                style = DevOSButtonStyle.Ghost,
            )
        }
    }
}

/** Clickable code reference row. */
@Composable
private fun CodeRefRow(
    ref: CodeRef,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${ref.filePath} line ${ref.lineNumber}" },
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        ) {
            // "kt" icon
            Box(
                modifier = Modifier
                    .size(DevOSSpacing.iconSizeLarge)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(DevOSSpacing.xs),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs)) {
                Text(
                    text = ref.filePath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "line ${ref.lineNumber} · ${ref.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** AlertDialog confirmation for closing an issue. */
@Composable
private fun CloseIssueConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Close Issue?",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = "Are you sure you want to close this issue? This action can be reversed later.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            DevOSButton(
                text = "Close Issue",
                onClick = onConfirm,
                style = DevOSButtonStyle.Destructive,
            )
        },
        dismissButton = {
            DevOSButton(
                text = "Cancel",
                onClick = onDismiss,
                style = DevOSButtonStyle.Ghost,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
