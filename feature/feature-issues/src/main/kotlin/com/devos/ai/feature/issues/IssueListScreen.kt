package com.devos.ai.feature.issues

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Add as AddIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSChip
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Issue List screen — DEVOS-042 / DA-54.
 *
 * Implements the #s-issues mockup:
 * - Filter chips: Open / Closed / My Issues / Bug / Feature
 * - Flat issue rows with status dot, title, labels, AI priority, comment count
 * - FAB (+) to create new issue
 * - AlertDialog confirmation for close issue action
 *
 * Stateless composable — all state comes from [IssueListUiState].
 */
@Composable
fun IssueListScreen(
    uiState: IssueListUiState,
    onNavigateBack: () -> Unit,
    onFilterChange: (IssueFilter) -> Unit,
    onIssueClick: (String) -> Unit,
    onRequestCloseIssue: (String) -> Unit,
    onConfirmClose: () -> Unit,
    onDismissClose: () -> Unit,
    onCreateIssue: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Issues",
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
                        onClick = onCreateIssue,
                        modifier = Modifier.semantics { contentDescription = "Create new issue" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateIssue,
                modifier = Modifier
                    .size(DevOSSpacing.fabSize)
                    .semantics { contentDescription = "Create issue" },
                shape = RoundedCornerShape(DevOSSpacing.base),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                )
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is IssueListUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is IssueListUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is IssueListUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.BugReport,
                title = "No Issues",
                description = "No issues match the current filter",
                modifier = Modifier.padding(innerPadding),
            )

            is IssueListUiState.Success -> {
                // Close issue confirmation dialog
                if (state.showCloseConfirmation) {
                    CloseIssueDialog(
                        onConfirm = onConfirmClose,
                        onDismiss = onDismissClose,
                    )
                }

                IssueListContent(
                    state = state,
                    onFilterChange = onFilterChange,
                    onIssueClick = onIssueClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun IssueListContent(
    state: IssueListUiState.Success,
    onFilterChange: (IssueFilter) -> Unit,
    onIssueClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = DevOSSpacing.xxxl + DevOSSpacing.xl),
    ) {
        // ── Filter chips ──────────────────────────────────────────────────────
        item(key = "filter_chips") {
            IssueFilterChipRow(
                filter = state.filter,
                onFilterChange = onFilterChange,
            )
        }

        // ── Issue rows with dividers ──────────────────────────────────────────
        itemsIndexed(
            items = state.issues,
            key = { _, issue -> issue.id },
        ) { index, issue ->
            IssueRow(
                issue = issue,
                onClick = { onIssueClick(issue.id) },
            )
            if (index < state.issues.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    thickness = DevOSSpacing.dividerThickness,
                    modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                )
            }
        }
    }
}

/** Horizontally scrollable filter chip row: Open / Closed / My Issues / Bug / Feature. */
@Composable
private fun IssueFilterChipRow(
    filter: IssueFilter,
    onFilterChange: (IssueFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val openCount = 12 // stub count matching mockup
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        item(key = "chip_open") {
            DevOSChip(
                label = "Open ($openCount)",
                selected = filter.showOpen && !filter.myIssues && filter.labelFilter == null,
                onClick = { onFilterChange(IssueFilter(showOpen = true)) },
            )
        }
        item(key = "chip_closed") {
            DevOSChip(
                label = "Closed",
                selected = !filter.showOpen,
                onClick = { onFilterChange(IssueFilter(showOpen = false)) },
            )
        }
        item(key = "chip_my_issues") {
            DevOSChip(
                label = "My Issues",
                selected = filter.myIssues,
                onClick = { onFilterChange(filter.copy(myIssues = !filter.myIssues)) },
            )
        }
        item(key = "chip_bug") {
            DevOSChip(
                label = "Bug",
                selected = filter.labelFilter == "bug",
                onClick = {
                    onFilterChange(
                        filter.copy(labelFilter = if (filter.labelFilter == "bug") null else "bug"),
                    )
                },
            )
        }
        item(key = "chip_feature") {
            DevOSChip(
                label = "Feature",
                selected = filter.labelFilter == "enhancement",
                onClick = {
                    onFilterChange(
                        filter.copy(
                            labelFilter = if (filter.labelFilter == "enhancement") null else "enhancement",
                        ),
                    )
                },
            )
        }
    }
}

/**
 * Flat issue row — no card wrapping, matches #s-issues mockup.
 *
 * Layout: status dot | content | comment count
 */
@Composable
private fun IssueRow(
    issue: Issue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotColor = when {
        issue.state == IssueState.CLOSED -> MaterialTheme.colorScheme.error
        issue.labels.contains("bug") -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Issue #${issue.number}: ${issue.title}" },
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm + DevOSSpacing.xs,
                ),
            verticalAlignment = Alignment.Top,
        ) {
            // Status dot — 8dp
            Box(
                modifier = Modifier
                    .padding(top = DevOSSpacing.xs)
                    .size(8.dp)
                    .background(color = dotColor, shape = CircleShape),
            )

            Spacer(modifier = Modifier.width(DevOSSpacing.sm))

            // Issue content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs),
            ) {
                Text(
                    text = "#${issue.number} · ${issue.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "opened ${issue.openedAt} by ${issue.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (issue.labels.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
                        modifier = Modifier.padding(top = DevOSSpacing.xxs),
                    ) {
                        issue.labels.forEach { label ->
                            IssueLabelBadge(label = label)
                        }
                    }
                }
                if (issue.aiPriority != null || issue.aiFixHint != null) {
                    val aiText = buildString {
                        if (issue.aiPriority != null) append("AI priority: ${issue.aiPriority.name}")
                        if (issue.aiFixHint != null) {
                            if (issue.aiPriority != null) append(" · ")
                            append(issue.aiFixHint)
                        }
                    }
                    Text(
                        text = aiText,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (issue.aiPriority) {
                            AiPriority.HIGH -> MaterialTheme.colorScheme.error
                            AiPriority.MEDIUM -> MaterialTheme.colorScheme.primary
                            AiPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                            null -> MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.width(DevOSSpacing.sm))

            // Comment count
            if (issue.commentCount > 0) {
                Text(
                    text = "💬 ${issue.commentCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Colored label chip badge. */
@Composable
private fun IssueLabelBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor) = when (label.lowercase()) {
        "bug" -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        "enhancement", "feature" -> Pair(
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
 * AlertDialog shown before closing an issue.
 * Closing an issue is a destructive action — always confirm first.
 */
@Composable
private fun CloseIssueDialog(
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
