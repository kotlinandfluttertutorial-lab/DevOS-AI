package com.devos.ai.feature.testing

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Science
import androidx.compose.foundation.Canvas
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Test Intelligence screen — DEVOS-048 / DA-61.
 *
 * Implements the #s-test-intel mockup:
 * - Coverage gauge (circular arc, 67%, warning color)
 * - Stats row (Passing / Failing / Flaky)
 * - AI Suggestions card with primary left border
 * - Uncovered Files list with kt badge and coverage % in error color
 *
 * Stateless composable — all state comes from [TestIntelligenceUiState].
 */
@Composable
fun TestIntelligenceScreen(
    uiState: TestIntelligenceUiState,
    onNavigateBack: () -> Unit,
    onGenerateTests: (String) -> Unit,
    onNavigateToFile: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Test Intelligence",
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
            is TestIntelligenceUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is TestIntelligenceUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is TestIntelligenceUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.Science,
                title = "No Coverage Data",
                description = "Run your test suite to see coverage analysis",
                modifier = Modifier.padding(innerPadding),
            )

            is TestIntelligenceUiState.Success -> TestIntelligenceContent(
                coverage = state.coverage,
                onGenerateTests = onGenerateTests,
                onNavigateToFile = onNavigateToFile,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun TestIntelligenceContent(
    coverage: TestCoverage,
    onGenerateTests: (String) -> Unit,
    onNavigateToFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = DevOSSpacing.base,
            vertical = DevOSSpacing.sm,
        ),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        // ── Coverage gauge card ───────────────────────────────────────────────
        item(key = "coverage_gauge") {
            CoverageGaugeCard(coverage = coverage)
        }

        // ── Stats row ─────────────────────────────────────────────────────────
        item(key = "stats_row") {
            TestStatsRow(
                passing = coverage.passing,
                failing = coverage.failing,
                flaky = coverage.flaky,
            )
        }

        // ── AI Suggestions ────────────────────────────────────────────────────
        item(key = "ai_suggestions_header") {
            DevOSSectionHeader(
                title = "AI Suggestions",
                modifier = Modifier.padding(top = DevOSSpacing.xs),
            )
        }

        item(key = "ai_suggestions_card") {
            AISuggestionsCard(
                suggestion = coverage.aiSuggestion,
                onGenerateTests = { onGenerateTests(coverage.uncoveredFiles.firstOrNull()?.name ?: "") },
            )
        }

        // ── Uncovered Files ───────────────────────────────────────────────────
        item(key = "uncovered_header") {
            DevOSSectionHeader(
                title = "Uncovered Files (${coverage.uncoveredFiles.size})",
                modifier = Modifier.padding(top = DevOSSpacing.xs),
            )
        }

        items(
            items = coverage.uncoveredFiles,
            key = { it.name },
        ) { file ->
            UncoveredFileItem(
                file = file,
                onClick = { onNavigateToFile(file.name) },
            )
            if (file != coverage.uncoveredFiles.last()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    thickness = DevOSSpacing.dividerThickness,
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(DevOSSpacing.base))
        }
    }
}

/**
 * Circular arc coverage gauge card.
 * Uses a Canvas arc to draw the track and fill — matching the SVG in the mockup.
 * Warning color (#FFCB6B = MaterialTheme.colorScheme.tertiary in DevOS dark theme).
 */
@Composable
private fun CoverageGaugeCard(
    coverage: TestCoverage,
    modifier: Modifier = Modifier,
) {
    val warningColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val percent = coverage.overallPercent

    DevOSCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DevOSSpacing.base),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Arc gauge
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .size(100.dp)
                        .rotate(-90f)
                        .semantics { contentDescription = "Coverage gauge: $percent%" },
                ) {
                    val strokeWidth = 10.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    val arcSize = Size(radius * 2f, radius * 2f)

                    // Track
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth),
                    )

                    // Fill arc — percent of 360 degrees
                    val sweepAngle = (percent / 100f) * 360f
                    drawArc(
                        color = warningColor,
                        startAngle = 0f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }

                // Centered percentage text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = warningColor,
                    )
                    Text(
                        text = "Coverage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(DevOSSpacing.sm))

            // Target and gap info
            Text(
                text = "Target: ${coverage.targetPercent}% · Gap: ${coverage.targetPercent - percent}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 3-card stats row: Passing / Failing / Flaky.
 * Background colors match the #s-test-intel mockup.
 */
@Composable
private fun TestStatsRow(
    passing: Int,
    failing: Int,
    flaky: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        TestStatCard(
            count = passing,
            label = "Passing",
            bgColor = Color(0xFF2D5000),
            valueColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        TestStatCard(
            count = failing,
            label = "Failing",
            bgColor = Color(0xFF930000),
            valueColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TestStatCard(
            count = flaky,
            label = "Flaky",
            bgColor = Color(0xFF3D2C00),
            valueColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TestStatCard(
    count: Int,
    label: String,
    bgColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(10.dp))
            .padding(DevOSSpacing.sm)
            .semantics { contentDescription = "$label: $count" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.xxs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = valueColor,
            )
        }
    }
}

/**
 * AI suggestions card with 3dp primary left border.
 * Suggestion text with inline code highlighted in secondary color.
 * "Generate Tests with AI" primary button.
 */
@Composable
private fun AISuggestionsCard(
    suggestion: String,
    onGenerateTests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        // 3dp primary left border
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(DevOSSpacing.huge)
                .background(
                    color = primaryColor,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        bottomStart = 12.dp,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = DevOSSpacing.sm,
                    vertical = DevOSSpacing.sm,
                ),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        ) {
            Text(
                text = suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DevOSButton(
                text = "Generate Tests with AI",
                onClick = onGenerateTests,
                style = DevOSButtonStyle.Primary,
            )
        }
    }
}

/**
 * Single uncovered file list item.
 * Shows: kt badge (primaryContainer bg), filename, subtitle, coverage % badge in error color.
 */
@Composable
private fun UncoveredFileItem(
    file: UncoveredFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = DevOSSpacing.sm)
            .semantics { contentDescription = "${file.name}: ${file.coveragePercent}% coverage, ${file.untestedFunctions} functions untested" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        // kt badge
        Box(
            modifier = Modifier
                .size(DevOSSpacing.xl)
                .background(
                    color = Color(0xFF003298),
                    shape = RoundedCornerShape(DevOSSpacing.xs),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "kt",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // File name and subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${file.coveragePercent}% · ${file.untestedFunctions} functions untested",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Coverage % badge in error color for low coverage
        DevOSStatusBadge(
            status = if (file.coveragePercent < 20) DevOSBadgeStatus.ERROR else DevOSBadgeStatus.WARNING,
            label = "${file.coveragePercent}%",
        )
    }
}
