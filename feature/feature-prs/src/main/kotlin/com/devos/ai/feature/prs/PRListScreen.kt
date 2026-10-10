package com.devos.ai.feature.prs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CallMerge
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
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSChip
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Pull Request List screen — DEVOS-044 / DA-55.
 *
 * Implements the #s-pull-requests mockup:
 * - Filter chips: Open(5) / Draft(2) / My PRs / Needs Review
 * - Flat PR rows with dividers (no card wrapping)
 * - PR title + AI score badge, branch/author/time, CI status, AI comment
 *
 * Stateless composable — all state comes from [PRListUiState].
 */
@Composable
fun PRListScreen(
    uiState: PRListUiState,
    onNavigateBack: () -> Unit,
    onFilterChange: (PRFilter) -> Unit,
    onPRClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Pull Requests",
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
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is PRListUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is PRListUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is PRListUiState.Empty -> DevOSEmptyState(
                icon = Icons.AutoMirrored.Outlined.CallMerge,
                title = "No Pull Requests",
                description = "No PRs match the current filter",
                modifier = Modifier.padding(innerPadding),
            )

            is PRListUiState.Success -> PRListContent(
                state = state,
                onFilterChange = onFilterChange,
                onPRClick = onPRClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun PRListContent(
    state: PRListUiState.Success,
    onFilterChange: (PRFilter) -> Unit,
    onPRClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = DevOSSpacing.xl),
    ) {
        // ── Filter chips ──────────────────────────────────────────────────────
        item(key = "filter_chips") {
            PRFilterChipRow(
                filter = state.filter,
                onFilterChange = onFilterChange,
            )
        }

        // ── PR rows with dividers ─────────────────────────────────────────────
        itemsIndexed(
            items = state.prs,
            key = { _, pr -> pr.id },
        ) { index, pr ->
            PRRow(
                pr = pr,
                onClick = { onPRClick(pr.id) },
            )
            if (index < state.prs.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    thickness = DevOSSpacing.dividerThickness,
                )
            }
        }
    }
}

/** Horizontally scrollable filter chip row: Open / Draft / My PRs / Needs Review. */
@Composable
private fun PRFilterChipRow(
    filter: PRFilter,
    onFilterChange: (PRFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        item(key = "chip_open") {
            DevOSChip(
                label = "Open (5)",
                selected = filter == PRFilter.OPEN,
                onClick = { onFilterChange(PRFilter.OPEN) },
            )
        }
        item(key = "chip_draft") {
            DevOSChip(
                label = "Draft (2)",
                selected = filter == PRFilter.DRAFT,
                onClick = { onFilterChange(PRFilter.DRAFT) },
            )
        }
        item(key = "chip_my_prs") {
            DevOSChip(
                label = "My PRs",
                selected = filter == PRFilter.MY_PRS,
                onClick = { onFilterChange(PRFilter.MY_PRS) },
            )
        }
        item(key = "chip_needs_review") {
            DevOSChip(
                label = "Needs Review",
                selected = filter == PRFilter.NEEDS_REVIEW,
                onClick = { onFilterChange(PRFilter.NEEDS_REVIEW) },
            )
        }
    }
}

/**
 * Flat PR row — no card wrapping, matches #s-pull-requests mockup.
 *
 * Layout:
 * - Row: PR title (14sp 500w onBackground) + AI score badge (green/error bg)
 * - Branch info row: "branch → main · author · time" (12sp onSurfaceVariant)
 * - Status badges: CI status + additional chips
 * - AI comment: 12sp primary (LGTM) or error (changes requested)
 */
@Composable
private fun PRRow(
    pr: PullRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "PR #${pr.number}: ${pr.title}" },
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            // ── Title row + AI score badge ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "#${pr.number} · ${pr.title}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = DevOSSpacing.sm),
                )
                AIScoreBadge(score = pr.aiScore)
            }

            // ── Branch info row ───────────────────────────────────────────────
            Text(
                text = "${pr.fromBranch} → ${pr.toBranch} · ${pr.author} · ${pr.relativeTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // ── Status badges row ─────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // CI status badge
                when (pr.ciStatus) {
                    CIStatus.PASSING -> DevOSStatusBadge(
                        status = DevOSBadgeStatus.SUCCESS,
                        label = "✓ CI Passed",
                    )
                    CIStatus.FAILING -> DevOSStatusBadge(
                        status = DevOSBadgeStatus.ERROR,
                        label = "✗ CI Failed",
                    )
                    CIStatus.RUNNING -> DevOSStatusBadge(
                        status = DevOSBadgeStatus.RUNNING,
                        label = "⟳ CI Running",
                    )
                }
                // Draft badge
                if (pr.isDraft) {
                    DevOSStatusBadge(
                        status = DevOSBadgeStatus.PENDING,
                        label = "Draft",
                    )
                }
            }

            // ── AI comment ────────────────────────────────────────────────────
            if (pr.aiComment.isNotEmpty()) {
                val isChangesRequested = pr.aiScore < 80
                Text(
                    text = pr.aiComment,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isChangesRequested) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}

/**
 * AI score badge — green background for high scores, error background for low.
 *
 * Score >= 80 → tertiary (green) color scheme.
 * Score < 80 → error color scheme.
 */
@Composable
private fun AIScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val isGoodScore = score >= 80
    val bgColor = if (isGoodScore) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val textColor = if (isGoodScore) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .background(
                color = bgColor,
                shape = RoundedCornerShape(DevOSSpacing.xs),
            )
            .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.xxs)
            .semantics { contentDescription = "AI score: $score" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "AI: $score",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
        )
    }
}
