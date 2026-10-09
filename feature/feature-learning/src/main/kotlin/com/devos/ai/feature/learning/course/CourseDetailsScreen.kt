package com.devos.ai.feature.learning.course

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
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
import com.devos.ai.feature.learning.model.Course
import com.devos.ai.feature.learning.model.LessonStatus
import com.devos.ai.feature.learning.model.LessonSummary

/**
 * Course Details screen (DEVOS-051, FIGMA-29).
 *
 * Stateless — all state and events are supplied by [LearningNavigation].
 */
@Composable
fun CourseDetailsScreen(
    uiState: CourseDetailsUiState,
    onContinueCourse: () -> Unit,
    onLessonTap: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Course",
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(DevOSSpacing.touchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is CourseDetailsUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is CourseDetailsUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is CourseDetailsUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.RadioButtonUnchecked,
                title = "Course not found",
                description = "This course is no longer available",
                modifier = Modifier.padding(innerPadding),
            )

            is CourseDetailsUiState.Success -> CourseDetailsContent(
                course = state.course,
                onContinue = onContinueCourse,
                onLessonTap = onLessonTap,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun CourseDetailsContent(
    course: Course,
    onContinue: () -> Unit,
    onLessonTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = DevOSSpacing.xl),
    ) {
        // ── Course header ───────────────────────────────────────────────────
        item(key = "header") {
            CourseHeaderPanel(course = course, onContinue = onContinue)
        }

        // ── Lessons section ─────────────────────────────────────────────────
        item(key = "lessons_header") {
            DevOSSectionHeader(
                title = "Lessons",
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
            )
        }

        itemsIndexed(
            items = course.lessons,
            key = { _, lesson -> lesson.id },
        ) { index, lesson ->
            LessonListItem(
                lesson = lesson,
                onClick = { if (lesson.status != LessonStatus.LOCKED) onLessonTap(lesson.id) },
            )
            if (index < course.lessons.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = DevOSSpacing.dividerThickness,
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(DevOSSpacing.base))
        }
    }
}

// ── Course Header Panel ──────────────────────────────────────────────────────

@Composable
private fun CourseHeaderPanel(
    course: Course,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(DevOSSpacing.base),
    ) {
        // "KOTLIN · 8 LESSONS · 45 MIN"
        Text(
            text = "${course.language} · ${course.totalLessons} LESSONS · ${course.durationMinutes} MIN",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight(600)),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.xs))
        Text(
            text = course.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight(700), fontSize = 22.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.xs))
        Text(
            text = course.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.sm))
        // Progress bar
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
                text = "${course.completedLessons} of ${course.totalLessons} lessons done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${(course.fraction * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(DevOSSpacing.sm))
        // Tag chips
        if (course.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs)) {
                course.tags.take(3).forEach { tag ->
                    TagChip(label = tag)
                }
            }
            Spacer(modifier = Modifier.height(DevOSSpacing.sm))
        }
        // Continue button — shows the next lesson number
        val nextLessonNum = course.completedLessons + 1
        DevOSButton(
            text = "Continue Lesson $nextLessonNum →",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = DevOSSpacing.dividerThickness,
    )
}

@Composable
private fun TagChip(label: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

// ── Lesson List Item ─────────────────────────────────────────────────────────

@Composable
private fun LessonListItem(
    lesson: LessonSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isClickable = lesson.status != LessonStatus.LOCKED

    val rowBackground = when (lesson.status) {
        LessonStatus.CURRENT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.background
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .then(
                if (isClickable) Modifier.semantics {
                    contentDescription = "${lesson.title}, ${lesson.status.name.lowercase()}"
                    role = Role.Button
                } else Modifier.semantics {
                    contentDescription = "${lesson.title}, locked"
                },
            )
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(DevOSSpacing.iconSizeLarge)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(lesson.status.circleBackground()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (lesson.status) {
                    LessonStatus.COMPLETED -> Icons.Outlined.CheckCircle
                    LessonStatus.CURRENT -> Icons.Outlined.PlayCircle
                    LessonStatus.LOCKED -> Icons.Outlined.Lock
                },
                contentDescription = null,
                tint = lesson.status.iconTint(),
                modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
            )
        }
        Spacer(modifier = Modifier.width(DevOSSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (lesson.status == LessonStatus.LOCKED) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            val subLabel = when (lesson.status) {
                LessonStatus.COMPLETED -> "Completed · ${lesson.durationMinutes} min"
                LessonStatus.CURRENT -> "Up next · ${lesson.durationMinutes} min"
                LessonStatus.LOCKED -> "Locked · ${lesson.durationMinutes} min"
            }
            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LessonStatus.circleBackground() = when (this) {
    LessonStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
    LessonStatus.CURRENT -> MaterialTheme.colorScheme.primaryContainer
    LessonStatus.LOCKED -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun LessonStatus.iconTint() = when (this) {
    LessonStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
    LessonStatus.CURRENT -> MaterialTheme.colorScheme.onPrimaryContainer
    LessonStatus.LOCKED -> MaterialTheme.colorScheme.onSurfaceVariant
}
