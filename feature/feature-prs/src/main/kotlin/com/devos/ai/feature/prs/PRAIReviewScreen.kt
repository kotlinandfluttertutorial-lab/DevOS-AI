package com.devos.ai.feature.prs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * PR AI Review screen — DEVOS-045 / DA-57.
 *
 * Implements the #s-pr-review mockup:
 * - TopBar: back + "AI Review" + copy icon
 * - PR header card: title, branch info, AI score (28sp 800w), verdict, recommendation
 * - "AI Review Summary" card with 3dp left border (tertiary=good, error=changes)
 * - "File Review (N)" flat rows with dividers
 * - "Copy Review" (outlined) + "Post to GitHub" (primary) action buttons
 * - AlertDialog confirmation before posting to GitHub (non-negotiable)
 *
 * Stateless composable — all state comes from [PRReviewUiState].
 */
@Composable
fun PRAIReviewScreen(
    uiState: PRReviewUiState,
    onNavigateBack: () -> Unit,
    onCopyReview: () -> Unit,
    onPostReview: () -> Unit,
    onConfirmPost: () -> Unit,
    onDismissPost: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "AI Review",
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
                        onClick = onCopyReview,
                        modifier = Modifier.semantics { contentDescription = "Copy review" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is PRReviewUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is PRReviewUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is PRReviewUiState.Success -> {
                // Post to GitHub confirmation dialog — non-negotiable
                if (state.showPostConfirmation) {
                    PostToGitHubDialog(
                        prNumber = state.review.pr.number,
                        onConfirm = onConfirmPost,
                        onDismiss = onDismissPost,
                    )
                }

                PRReviewContent(
                    review = state.review,
                    onCopyReview = onCopyReview,
                    onPostReview = onPostReview,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun PRReviewContent(
    review: PRReview,
    onCopyReview: () -> Unit,
    onPostReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = DevOSSpacing.base,
            vertical = DevOSSpacing.base,
        ),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.md),
    ) {
        // ── PR Header Card ────────────────────────────────────────────────────
        item(key = "pr_header") {
            PRHeaderCard(review = review)
        }

        // ── AI Review Summary section ─────────────────────────────────────────
        item(key = "summary_header") {
            DevOSSectionHeader(
                title = "AI Review Summary",
                modifier = Modifier.padding(vertical = DevOSSpacing.xs),
            )
        }

        item(key = "summary_card") {
            AISummaryCard(review = review)
        }

        // ── File Review section ───────────────────────────────────────────────
        item(key = "file_review_header") {
            DevOSSectionHeader(
                title = "File Review (${review.fileReviews.size})",
                modifier = Modifier.padding(vertical = DevOSSpacing.xs),
            )
        }

        // Flat file rows with dividers
        itemsIndexed(
            items = review.fileReviews,
            key = { _, file -> file.fileName },
        ) { index, file ->
            FileReviewRow(file = file)
            if (index < review.fileReviews.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    thickness = DevOSSpacing.dividerThickness,
                )
            }
        }

        // ── Action buttons ────────────────────────────────────────────────────
        item(key = "action_buttons") {
            Spacer(modifier = Modifier.padding(top = DevOSSpacing.xs))
            ReviewActionButtons(
                onCopyReview = onCopyReview,
                onPostReview = onPostReview,
            )
        }
    }
}

/**
 * PR header card showing title, branch info, AI score and verdict recommendation.
 *
 * Matches mockup: #47 — feat: add AI chat context selector
 * +247 -89 · 8 files
 * [94] AI Score: Excellent / Recommend: Approve ✓
 */
@Composable
private fun PRHeaderCard(
    review: PRReview,
    modifier: Modifier = Modifier,
) {
    val isGoodScore = review.aiScore >= 80
    val scoreColor = if (isGoodScore) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }
    val verdictLabel = when (review.verdict) {
        PRVerdict.APPROVE -> "AI Score: Excellent"
        PRVerdict.CHANGES_REQUESTED -> "AI Score: Needs Work"
        PRVerdict.NEUTRAL -> "AI Score: Neutral"
    }
    val recommendation = when (review.verdict) {
        PRVerdict.APPROVE -> "Recommend: Approve ✓"
        PRVerdict.CHANGES_REQUESTED -> "Recommend: Changes Requested ✗"
        PRVerdict.NEUTRAL -> "Recommend: Neutral"
    }
    val totalAdditions = review.fileReviews.sumOf { it.additions }
    val totalDeletions = review.fileReviews.sumOf { it.deletions }

    DevOSCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DevOSSpacing.cardPadding),
        ) {
            // PR title
            Text(
                text = "#${review.pr.number} — ${review.pr.title}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Branch + stats info
            Text(
                text = "${review.pr.fromBranch} → ${review.pr.toBranch} · +$totalAdditions -$totalDeletions · ${review.fileReviews.size} files",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DevOSSpacing.xs),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = DevOSSpacing.sm),
            )

            // AI score + verdict row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                // Score — 28sp 800w
                Text(
                    text = "${review.aiScore}",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                    ),
                    color = scoreColor,
                    modifier = Modifier.semantics { contentDescription = "AI score ${review.aiScore} out of 100" },
                )

                Column {
                    Text(
                        text = verdictLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scoreColor,
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * AI Review Summary card with 3dp left border.
 *
 * Border color: tertiary for APPROVE verdict, error for CHANGES_REQUESTED.
 * Bullet list with icon prefix per point.
 */
@Composable
private fun AISummaryCard(
    review: PRReview,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (review.verdict == PRVerdict.APPROVE) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
            )
            .border(
                width = 3.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(
                start = DevOSSpacing.md,
                end = DevOSSpacing.cardPadding,
                top = DevOSSpacing.cardPadding,
                bottom = DevOSSpacing.cardPadding,
            ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm)) {
            review.summaryPoints.forEach { point ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = point.icon,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { contentDescription = "" },
                    )
                    Text(
                        text = point.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

/**
 * Flat file review row — no card wrapping.
 *
 * Layout:
 * - filename (13sp primary monospace) + status badge
 * - stats: "+additions -deletions · description" (12sp onSurfaceVariant)
 */
@Composable
private fun FileReviewRow(
    file: FileReview,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = DevOSSpacing.base,
                vertical = DevOSSpacing.sm,
            ),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = DevOSSpacing.sm),
            )

            DevOSStatusBadge(
                status = when (file.status) {
                    FileReviewStatus.NO_ISSUES -> DevOSBadgeStatus.SUCCESS
                    FileReviewStatus.HAS_SUGGESTION -> DevOSBadgeStatus.WARNING
                    FileReviewStatus.HAS_ISSUE -> DevOSBadgeStatus.ERROR
                },
                label = when (file.status) {
                    FileReviewStatus.NO_ISSUES -> "No issues"
                    FileReviewStatus.HAS_SUGGESTION -> "${file.additions.coerceAtMost(1)} suggestion"
                    FileReviewStatus.HAS_ISSUE -> "Has issue"
                },
            )
        }

        Text(
            text = "+${file.additions} -${file.deletions} · ${file.description}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Action buttons row: "Copy Review" (outlined) + "Post to GitHub" (primary).
 * Each takes equal width (weight = 1f). Height 40dp as per mockup.
 */
@Composable
private fun ReviewActionButtons(
    onCopyReview: () -> Unit,
    onPostReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        DevOSButton(
            text = "Copy Review",
            onClick = onCopyReview,
            style = DevOSButtonStyle.Secondary,
            modifier = Modifier.weight(1f),
        )
        DevOSButton(
            text = "Post to GitHub",
            onClick = onPostReview,
            style = DevOSButtonStyle.Primary,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * AlertDialog shown before posting the review to GitHub.
 *
 * Posting is an external side-effect — always confirm first.
 * This dialog is NON-NEGOTIABLE per spec.
 */
@Composable
private fun PostToGitHubDialog(
    prNumber: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Post to GitHub?",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = "This will post the AI review as a comment on PR #$prNumber on GitHub. This action is visible to all collaborators.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            DevOSButton(
                text = "Post Review",
                onClick = onConfirm,
                style = DevOSButtonStyle.Primary,
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
