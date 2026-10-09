package com.devos.ai.feature.learning.dashboard

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Schema
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.learning.model.CourseProgress
import com.devos.ai.feature.learning.model.CourseRecommendation
import com.devos.ai.feature.learning.model.IconCategory
import com.devos.ai.feature.learning.model.QuizScore

/**
 * Learning Dashboard screen (DEVOS-050, FIGMA-28).
 *
 * Stateless — all state and events are supplied by [LearningNavigation].
 * Handles all 4 UI states: Loading | Success | Empty | Error.
 */
@Composable
fun LearningDashboardScreen(
    uiState: LearningDashboardUiState,
    onContinueCourse: (String) -> Unit,
    onRecommendationTap: (String) -> Unit,
    onSearchTap: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Learn",
                actions = {
                    IconButton(
                        onClick = onSearchTap,
                        modifier = Modifier.size(DevOSSpacing.touchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search courses",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is LearningDashboardUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is LearningDashboardUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is LearningDashboardUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.AutoStories,
                title = "No courses yet",
                description = "Start learning to improve your development skills",
                modifier = Modifier.padding(innerPadding),
            )

            is LearningDashboardUiState.Success -> LearningDashboardContent(
                state = state,
                onContinueCourse = onContinueCourse,
                onRecommendationTap = onRecommendationTap,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun LearningDashboardContent(
    state: LearningDashboardUiState.Success,
    onContinueCourse: (String) -> Unit,
    onRecommendationTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = DevOSSpacing.xl),
    ) {
        // ── Daily goal + streak row ─────────────────────────────────────────
        item(key = "daily_streak") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                DailyGoalCard(
                    lessonsToday = state.lessonsToday,
                    dailyGoal = state.dailyGoal,
                    fraction = state.dailyFraction,
                    modifier = Modifier.weight(1f),
                )
                StreakCard(
                    streak = state.streak,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Continue Learning ───────────────────────────────────────────────
        if (state.currentCourse != null) {
            item(key = "continue_header") {
                DevOSSectionHeader(
                    title = "Continue Learning",
                    modifier = Modifier.padding(
                        horizontal = DevOSSpacing.base,
                        vertical = DevOSSpacing.sm,
                    ),
                )
            }
            item(key = "continue_card") {
                ContinueLearningCard(
                    course = state.currentCourse,
                    onContinue = { onContinueCourse(state.currentCourse.courseId) },
                    modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                )
            }
        }

        // ── Recommended for You ─────────────────────────────────────────────
        item(key = "recommendations_header") {
            DevOSSectionHeader(
                title = "Recommended for You",
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
            )
        }

        items(
            items = state.recommendations,
            key = { it.id },
        ) { recommendation ->
            RecommendationItem(
                recommendation = recommendation,
                onClick = { onRecommendationTap(recommendation.id) },
            )
            if (recommendation != state.recommendations.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = DevOSSpacing.dividerThickness,
                )
            }
        }

        // ── Recent Scores ───────────────────────────────────────────────────
        if (state.recentScores.isNotEmpty()) {
            item(key = "scores_header") {
                DevOSSectionHeader(
                    title = "Recent Scores",
                    modifier = Modifier.padding(
                        horizontal = DevOSSpacing.base,
                        vertical = DevOSSpacing.sm,
                    ),
                )
            }
            item(key = "scores_card") {
                RecentScoresCard(
                    scores = state.recentScores,
                    modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(DevOSSpacing.base))
        }
    }
}

// ── Daily Goal Card ─────────────────────────────────────────────────────────

@Composable
private fun DailyGoalCard(
    lessonsToday: Int,
    dailyGoal: Int,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    DevOSCard(
        modifier = modifier
            .border(
                width = DevOSSpacing.dividerThickness,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.base),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            Text(
                text = "$lessonsToday/$dailyGoal",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight(800),
                    fontSize = 22.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = "$lessonsToday of $dailyGoal lessons today" },
            )
            Text(
                text = "Lessons today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

// ── Streak Card ─────────────────────────────────────────────────────────────

@Composable
private fun StreakCard(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    DevOSCard(
        modifier = modifier
            .border(
                width = DevOSSpacing.dividerThickness,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.base),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                )
                Spacer(modifier = Modifier.width(DevOSSpacing.xs))
                Text(
                    text = "$streak",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight(800),
                        fontSize = 22.sp,
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.semantics { contentDescription = "$streak day streak" },
                )
            }
            Text(
                text = "Day streak",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Continue Learning Card ───────────────────────────────────────────────────

@Composable
private fun ContinueLearningCard(
    course: CourseProgress,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(gradientBrush)
            .border(
                width = DevOSSpacing.dividerThickness,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        Column(modifier = Modifier.padding(DevOSSpacing.base)) {
            // Label: "IN PROGRESS · Lesson X of Y"
            val progressLabel = "IN PROGRESS · Lesson ${course.completedLessons + 1} of ${course.totalLessons}"
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight(600)),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(700)),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (course.subtitle.isNotEmpty()) {
                Text(
                    text = course.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(DevOSSpacing.sm))
            LinearProgressIndicator(
                progress = { course.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${(course.fraction * 100).toInt()}% complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "~${course.estimatedMinutesLeft} min left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(DevOSSpacing.md))
            DevOSButton(
                text = "Continue →",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Recommendation Item ──────────────────────────────────────────────────────

@Composable
private fun RecommendationItem(
    recommendation: CourseRecommendation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon background circle
        Box(
            modifier = Modifier
                .size(DevOSSpacing.iconSizeXLarge)
                .clip(MaterialTheme.shapes.medium)
                .background(recommendation.iconCategory.containerColor()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = recommendation.iconCategory.icon(),
                contentDescription = null,
                tint = recommendation.iconCategory.contentColor(),
                modifier = Modifier.size(DevOSSpacing.iconSize),
            )
        }
        Spacer(modifier = Modifier.width(DevOSSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (recommendation.isNew) {
                    Spacer(modifier = Modifier.width(DevOSSpacing.xs))
                    NewBadge()
                }
            }
            Text(
                text = recommendation.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(DevOSSpacing.sm))
        DevOSButton(
            text = "Start",
            onClick = onClick,
            modifier = Modifier.size(width = DevOSSpacing.xxxl.times(2), height = DevOSSpacing.touchTarget),
        )
    }
}

@Composable
private fun NewBadge() {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = DevOSSpacing.xs, vertical = DevOSSpacing.xxs),
    ) {
        Text(
            text = "New",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun IconCategory.containerColor() = when (this) {
    IconCategory.ARCHITECTURE -> MaterialTheme.colorScheme.secondaryContainer
    IconCategory.TESTING -> MaterialTheme.colorScheme.tertiaryContainer
    IconCategory.AI -> MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun IconCategory.contentColor() = when (this) {
    IconCategory.ARCHITECTURE -> MaterialTheme.colorScheme.onSecondaryContainer
    IconCategory.TESTING -> MaterialTheme.colorScheme.onTertiaryContainer
    IconCategory.AI -> MaterialTheme.colorScheme.onPrimaryContainer
}

private fun IconCategory.icon(): ImageVector = when (this) {
    IconCategory.ARCHITECTURE -> Icons.Outlined.Schema
    IconCategory.TESTING -> Icons.Outlined.Science
    IconCategory.AI -> Icons.Outlined.Psychology
}

// ── Recent Scores Card ───────────────────────────────────────────────────────

@Composable
private fun RecentScoresCard(
    scores: List<QuizScore>,
    modifier: Modifier = Modifier,
) {
    DevOSCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(DevOSSpacing.cardPadding)) {
            scores.forEachIndexed { index, score ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = score.quizTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    // Score badge: tertiary if perfect or near, warning/secondary if lower
                    val isPassing = score.score.toFloat() / score.total >= 0.8f
                    Text(
                        text = "${score.score}/${score.total}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight(700)),
                        color = if (isPassing) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    )
                }
                if (index < scores.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = DevOSSpacing.xs),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = DevOSSpacing.dividerThickness,
                    )
                }
            }
        }
    }
}
