package com.devos.ai.feature.learning.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Code
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
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSCodeBlock
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSMarkdownText
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.learning.model.CodeExample
import com.devos.ai.feature.learning.model.LessonContent

/**
 * Lesson screen (DEVOS-052, FIGMA-30).
 *
 * Stateless — all state and events are supplied by [LearningNavigation].
 */
@Composable
fun LessonScreen(
    uiState: LessonUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTryInRepo: (CodeExample) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val subtitle = if (uiState is LessonUiState.Success) {
                "Lesson ${uiState.lesson.order} of ${uiState.lesson.totalInCourse}"
            } else null
            DevOSTopBar(
                title = if (uiState is LessonUiState.Success) uiState.lesson.title else "Lesson",
                subtitle = subtitle,
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
        bottomBar = {
            if (uiState is LessonUiState.Success) {
                LessonNavigationBar(
                    canGoPrevious = uiState.canGoPrevious,
                    canGoNext = uiState.canGoNext,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is LessonUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is LessonUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is LessonUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.AutoStories,
                title = "Lesson not found",
                description = "This lesson is no longer available",
                modifier = Modifier.padding(innerPadding),
            )

            is LessonUiState.Success -> LessonContent(
                lesson = state.lesson,
                onTryInRepo = onTryInRepo,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun LessonContent(
    lesson: LessonContent,
    onTryInRepo: (CodeExample) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = DevOSSpacing.xl),
    ) {
        item(key = "lesson_title") {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
            )
        }

        item(key = "lesson_markdown") {
            DevOSMarkdownText(
                markdown = lesson.markdownContent,
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
            )
        }

        if (lesson.codeExamples.isNotEmpty()) {
            item(key = "examples_spacer") {
                Spacer(modifier = Modifier.height(DevOSSpacing.base))
            }

            items(
                items = lesson.codeExamples,
                key = { it.id },
            ) { example ->
                CodeExampleCard(
                    example = example,
                    onTryInRepo = { onTryInRepo(example) },
                    modifier = Modifier.padding(
                        horizontal = DevOSSpacing.base,
                        vertical = DevOSSpacing.xs,
                    ),
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(DevOSSpacing.base))
        }
    }
}

// ── Code Example Card ────────────────────────────────────────────────────────

@Composable
private fun CodeExampleCard(
    example: CodeExample,
    onTryInRepo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (example.description.isNotEmpty()) {
            Text(
                text = example.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = DevOSSpacing.xs),
            )
        }
        DevOSCodeBlock(
            code = example.code,
            language = example.language,
            showLineNumbers = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.xs))
        DevOSButton(
            text = "Try in Repo",
            onClick = onTryInRepo,
            style = DevOSButtonStyle.Secondary,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Code,
                    contentDescription = null,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                )
            },
            modifier = Modifier.semantics {
                contentDescription = "Try ${example.description} in repository"
            },
        )
    }
}

// ── Lesson Navigation Bar ────────────────────────────────────────────────────

@Composable
private fun LessonNavigationBar(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DevOSSpacing.xs,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DevOSButton(
                text = "← Previous",
                onClick = onPrevious,
                style = DevOSButtonStyle.Secondary,
                enabled = canGoPrevious,
                modifier = Modifier.weight(1f).padding(end = DevOSSpacing.xs),
            )
            DevOSButton(
                text = "Next →",
                onClick = onNext,
                enabled = canGoNext,
                modifier = Modifier.weight(1f).padding(start = DevOSSpacing.xs),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                    )
                },
            )
        }
    }
}
