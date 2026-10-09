package com.devos.ai.feature.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Security
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
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSChip
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Security Findings screen — DEVOS-046 / DA-59.
 *
 * Implements the #s-security mockup:
 * - Summary severity count cards
 * - Filter chips (All / Critical / OWASP / Open)
 * - Finding list items with left severity strip, badges, file info, AI suggestion, and action buttons
 *
 * Stateless composable — all state comes from [SecurityFindingsUiState].
 */
@Composable
fun SecurityFindingsScreen(
    uiState: SecurityFindingsUiState,
    onNavigateBack: () -> Unit,
    onFilterChange: (Severity?) -> Unit,
    onMarkFixed: (String) -> Unit,
    onAskAI: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Security Findings",
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
            is SecurityFindingsUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is SecurityFindingsUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is SecurityFindingsUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.Security,
                title = "No Security Findings",
                description = "No security issues detected in this repository",
                modifier = Modifier.padding(innerPadding),
            )

            is SecurityFindingsUiState.Success -> SecurityFindingsContent(
                state = state,
                onFilterChange = onFilterChange,
                onMarkFixed = onMarkFixed,
                onAskAI = onAskAI,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SecurityFindingsContent(
    state: SecurityFindingsUiState.Success,
    onFilterChange: (Severity?) -> Unit,
    onMarkFixed: (String) -> Unit,
    onAskAI: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = DevOSSpacing.xl),
    ) {
        // ── Summary severity count cards ──────────────────────────────────────
        item(key = "summary_row") {
            SeveritySummaryRow(
                counts = state.counts,
                activeFilter = state.activeFilter,
                onFilterChange = onFilterChange,
            )
        }

        // ── Filter chips ──────────────────────────────────────────────────────
        item(key = "filter_chips") {
            FilterChipRow(
                activeFilter = state.activeFilter,
                totalCount = state.counts.values.sum(),
                criticalCount = state.counts[Severity.CRITICAL] ?: 0,
                onFilterChange = onFilterChange,
            )
        }

        // ── Finding list ──────────────────────────────────────────────────────
        itemsIndexed(
            items = state.findings,
            key = { _, f -> f.id },
        ) { index, finding ->
            FindingRow(
                finding = finding,
                onMarkFixed = { onMarkFixed(finding.id) },
                onAskAI = { onAskAI(finding.id) },
            )
            if (index < state.findings.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    thickness = DevOSSpacing.dividerThickness,
                )
            }
        }
    }
}

/**
 * 4-card row showing counts per severity level.
 * Each card has a severity-specific background color matching the #s-security mockup.
 */
@Composable
private fun SeveritySummaryRow(
    counts: Map<Severity, Int>,
    activeFilter: Severity?,
    onFilterChange: (Severity?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        Severity.entries.forEach { severity ->
            SeverityCountCard(
                severity = severity,
                count = counts[severity] ?: 0,
                isActive = activeFilter == severity,
                onClick = {
                    onFilterChange(if (activeFilter == severity) null else severity)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Individual severity count card. Background and text colors match the mockup. */
@Composable
private fun SeverityCountCard(
    severity: Severity,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = when (severity) {
        Severity.CRITICAL -> Color(0xFF930000)  // dark red — #s-security mockup
        Severity.HIGH     -> Color(0xFF3D2C00)  // dark amber
        Severity.MEDIUM   -> Color(0xFF003547)  // dark cyan
        Severity.LOW      -> Color(0xFF2D5000)  // dark green
    }
    val valueColor = when (severity) {
        Severity.CRITICAL -> MaterialTheme.colorScheme.error
        Severity.HIGH     -> MaterialTheme.colorScheme.tertiary  // warning uses tertiary in M3
        Severity.MEDIUM   -> MaterialTheme.colorScheme.secondary
        Severity.LOW      -> MaterialTheme.colorScheme.tertiary
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .then(
                Modifier.semantics { contentDescription = "${severity.name} severity: $count findings" },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.xxs))
            Text(
                text = severity.name,
                style = MaterialTheme.typography.labelSmall,
                color = valueColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Horizontally scrollable filter chip row.
 * Chips: All(N) / Critical(N) / OWASP A03 / Open
 */
@Composable
private fun FilterChipRow(
    activeFilter: Severity?,
    totalCount: Int,
    criticalCount: Int,
    onFilterChange: (Severity?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        item(key = "chip_all") {
            DevOSChip(
                label = "All ($totalCount)",
                selected = activeFilter == null,
                onClick = { onFilterChange(null) },
            )
        }
        item(key = "chip_critical") {
            DevOSChip(
                label = "Critical ($criticalCount)",
                selected = activeFilter == Severity.CRITICAL,
                onClick = { onFilterChange(Severity.CRITICAL) },
            )
        }
        item(key = "chip_owasp") {
            DevOSChip(
                label = "OWASP A03",
                selected = false,
                onClick = {},
            )
        }
        item(key = "chip_open") {
            DevOSChip(
                label = "Open",
                selected = false,
                onClick = {},
            )
        }
    }
}

/**
 * Single finding row with:
 * - 4dp colored left severity strip
 * - Title + severity badge (right-aligned)
 * - File:line and OWASP category labels
 * - AI suggestion text
 * - "Mark Fixed" (outlined) + "Ask AI" (primary) action buttons
 */
@Composable
private fun FindingRow(
    finding: SecurityFinding,
    onMarkFixed: () -> Unit,
    onAskAI: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stripColor = when (finding.severity) {
        Severity.CRITICAL -> MaterialTheme.colorScheme.error
        Severity.HIGH     -> MaterialTheme.colorScheme.tertiary
        Severity.MEDIUM   -> MaterialTheme.colorScheme.secondary
        Severity.LOW      -> MaterialTheme.colorScheme.tertiary
    }
    val badgeStatus = when (finding.severity) {
        Severity.CRITICAL -> DevOSBadgeStatus.ERROR
        Severity.HIGH     -> DevOSBadgeStatus.WARNING
        Severity.MEDIUM   -> DevOSBadgeStatus.INFO
        Severity.LOW      -> DevOSBadgeStatus.SUCCESS
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${finding.severity.name}: ${finding.title}" },
    ) {
        // Left severity strip
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(DevOSSpacing.huge)  // stretches via parent height
                .background(stripColor),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            // Title row with severity badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = finding.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(DevOSSpacing.sm))
                DevOSStatusBadge(
                    status = badgeStatus,
                    label = finding.severity.name,
                )
            }

            // File path and line number
            Text(
                text = "${finding.filePath} · line ${finding.lineNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // OWASP category
            Text(
                text = finding.owaspCategory,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // AI suggestion
            if (finding.aiSuggestion != null) {
                Text(
                    text = "AI: ${finding.aiSuggestion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                DevOSButton(
                    text = "Mark Fixed",
                    onClick = onMarkFixed,
                    style = DevOSButtonStyle.Secondary,
                )
                DevOSButton(
                    text = "Ask AI",
                    onClick = onAskAI,
                    style = DevOSButtonStyle.Primary,
                )
            }
        }
    }
}
