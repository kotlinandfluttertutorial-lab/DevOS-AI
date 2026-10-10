package com.devos.ai.feature.git

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
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
import com.devos.ai.designsystem.components.DevOSChip
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Git History screen — DEVOS-040 / DA-53.
 *
 * Implements the #s-git-history mockup:
 * - Scrollable branch chips
 * - AI Summary card (surface bg, 1dp primary border)
 * - Commit timeline: date group headers, commit rows with primary dot + connector line
 *
 * Stateless composable — all state comes from [GitHistoryUiState].
 */
@Composable
fun GitHistoryScreen(
    uiState: GitHistoryUiState,
    onNavigateBack: () -> Unit,
    onBranchSelected: (String) -> Unit,
    onCommitClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Git History",
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
                        onClick = { /* AI summary action */ },
                        modifier = Modifier.semantics { contentDescription = "AI summary" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is GitHistoryUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is GitHistoryUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is GitHistoryUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.History,
                title = "No Commits Found",
                description = "No commits found for this branch",
                modifier = Modifier.padding(innerPadding),
            )

            is GitHistoryUiState.Success -> GitHistoryContent(
                state = state,
                onBranchSelected = onBranchSelected,
                onCommitClick = onCommitClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun GitHistoryContent(
    state: GitHistoryUiState.Success,
    onBranchSelected: (String) -> Unit,
    onCommitClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = DevOSSpacing.xl),
    ) {
        // ── Branch chips ──────────────────────────────────────────────────────
        item(key = "branch_chips") {
            BranchChipRow(
                branches = state.branches,
                activeBranch = state.activeBranch,
                onBranchSelected = onBranchSelected,
            )
        }

        // ── AI Summary card ───────────────────────────────────────────────────
        if (state.aiSummary != null) {
            item(key = "ai_summary") {
                AISummaryCard(
                    summary = state.aiSummary,
                    modifier = Modifier.padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
                )
            }
        }

        // ── Commit groups ─────────────────────────────────────────────────────
        state.groups.forEach { group ->
            item(key = "header_${group.dateLabel}") {
                DateGroupHeader(label = group.dateLabel)
            }
            items(
                items = group.commits,
                key = { commit -> commit.sha },
            ) { commit ->
                CommitTimelineRow(
                    commit = commit,
                    isLast = commit == group.commits.last(),
                    onClick = { onCommitClick(commit.sha) },
                )
            }
        }
    }
}

/** Horizontally scrollable branch selector chips. */
@Composable
private fun BranchChipRow(
    branches: List<String>,
    activeBranch: String,
    onBranchSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        items(items = branches, key = { it }) { branch ->
            DevOSChip(
                label = branch,
                selected = branch == activeBranch,
                onClick = { onBranchSelected(branch) },
            )
        }
    }
}

/**
 * AI Summary card — surface background, 1dp primary border.
 * Matches the #s-git-history mockup card.
 */
@Composable
private fun AISummaryCard(
    summary: String,
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
                    text = "AI Summary",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
        }
    }
}

/** Date group header — 11sp, uppercase, muted color. */
@Composable
private fun DateGroupHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            horizontal = DevOSSpacing.base,
            vertical = DevOSSpacing.xs,
        ),
    )
}

/**
 * Single commit row in the timeline.
 *
 * Left column: 10dp primary filled circle + 2dp vertical connector line (outlineVariant).
 * Right: commit message, sha·author·time, +additions -deletions.
 */
@Composable
private fun CommitTimelineRow(
    commit: GitCommit,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Commit: ${commit.message}" },
        verticalAlignment = Alignment.Top,
    ) {
        // Timeline column: dot + connector
        Column(
            modifier = Modifier
                .padding(start = DevOSSpacing.base, top = DevOSSpacing.sm)
                .width(DevOSSpacing.base),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 10dp primary circle dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            )
            // Vertical connector line (only if not last in group)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(DevOSSpacing.xxs)
                        .height(DevOSSpacing.xxl + DevOSSpacing.xl)
                        .background(
                            color = MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.width(DevOSSpacing.sm))

        // Commit info column — clickable
        Surface(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .padding(end = DevOSSpacing.base, bottom = if (isLast) DevOSSpacing.sm else DevOSSpacing.base),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.padding(vertical = DevOSSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs),
            ) {
                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${commit.shortSha} · ${commit.author} · ${commit.relativeTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
                ) {
                    Text(
                        text = "+${commit.additions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = "-${commit.deletions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
