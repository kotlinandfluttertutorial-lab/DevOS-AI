package com.devos.ai.feature.learning.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Quiz
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Quiz screen (DEVOS-053, FIGMA-31).
 *
 * Stateless — all state and events are supplied by [LearningNavigation].
 * Active → Reviewing → Complete transitions are managed entirely by [QuizViewModel].
 */
@Composable
fun QuizScreen(
    uiState: QuizUiState,
    onSelectOption: (Int) -> Unit,
    onSubmit: () -> Unit,
    onNextQuestion: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topBarTitle = when (uiState) {
        is QuizUiState.Success.Active -> uiState.quizTitle
        is QuizUiState.Success.Reviewing -> uiState.quizTitle
        is QuizUiState.Success.Complete -> uiState.quizTitle
        else -> "Quiz"
    }
    val topBarSubtitle = when (uiState) {
        is QuizUiState.Success.Active ->
            "Question ${uiState.questionIndex + 1} of ${uiState.totalQuestions}"
        is QuizUiState.Success.Reviewing ->
            "Question ${uiState.questionIndex + 1} of ${uiState.totalQuestions}"
        else -> null
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = topBarTitle,
                subtitle = topBarSubtitle,
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
            is QuizUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is QuizUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is QuizUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.Quiz,
                title = "Quiz not found",
                description = "This quiz is no longer available",
                modifier = Modifier.padding(innerPadding),
            )

            is QuizUiState.Success.Active -> ActiveQuizContent(
                state = state,
                onSelectOption = onSelectOption,
                onSubmit = onSubmit,
                modifier = Modifier.padding(innerPadding),
            )

            is QuizUiState.Success.Reviewing -> ReviewingQuizContent(
                state = state,
                onNext = onNextQuestion,
                modifier = Modifier.padding(innerPadding),
            )

            is QuizUiState.Success.Complete -> QuizCompleteContent(
                state = state,
                onFinish = onFinish,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

// ── Active State ─────────────────────────────────────────────────────────────

@Composable
private fun ActiveQuizContent(
    state: QuizUiState.Success.Active,
    onSelectOption: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DevOSSpacing.base),
    ) {
        QuizProgressBar(
            current = state.questionIndex + 1,
            total = state.totalQuestions,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.base))
        Text(
            text = state.question.text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.xl))
        state.question.options.forEachIndexed { index, option ->
            QuizOptionItem(
                text = option,
                state = when {
                    index == state.selectedOption -> QuizOptionState.Selected
                    else -> QuizOptionState.Default
                },
                onClick = { onSelectOption(index) },
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.sm))
        }
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(DevOSSpacing.base))
        DevOSButton(
            text = "Submit",
            onClick = onSubmit,
            enabled = state.selectedOption != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Reviewing State ───────────────────────────────────────────────────────────

@Composable
private fun ReviewingQuizContent(
    state: QuizUiState.Success.Reviewing,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DevOSSpacing.base),
    ) {
        QuizProgressBar(
            current = state.questionIndex + 1,
            total = state.totalQuestions,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.base))
        Text(
            text = state.question.text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.xl))
        state.question.options.forEachIndexed { index, option ->
            val optionState = when {
                index == state.question.correctIndex -> QuizOptionState.Correct
                index == state.selectedOption && index != state.question.correctIndex -> QuizOptionState.Incorrect
                else -> QuizOptionState.Default
            }
            QuizOptionItem(
                text = option,
                state = optionState,
                onClick = {},
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.sm))
        }
        Spacer(modifier = Modifier.height(DevOSSpacing.base))
        ExplanationCard(explanation = state.question.explanation)
        Spacer(modifier = Modifier.height(DevOSSpacing.base))
        val isLastQuestion = state.questionIndex + 1 >= state.totalQuestions
        DevOSButton(
            text = if (isLastQuestion) "See Results" else "Next Question →",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Complete State ────────────────────────────────────────────────────────────

@Composable
private fun QuizCompleteContent(
    state: QuizUiState.Success.Complete,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(DevOSSpacing.base),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.EmojiEvents,
            contentDescription = "Quiz complete",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(DevOSSpacing.iconSizeXLarge),
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.base))
        Text(
            text = "Quiz Complete!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.sm))
        Text(
            text = "Your score",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.sm))
        // Score display
        val isPassing = state.score.toFloat() / state.totalQuestions >= 0.7f
        Text(
            text = "${state.score}/${state.totalQuestions}",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight(800)),
            color = if (isPassing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics {
                contentDescription = "${state.score} out of ${state.totalQuestions} correct"
            },
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.xs))
        val percentage = ((state.score.toFloat() / state.totalQuestions) * 100).toInt()
        Text(
            text = "$percentage% correct",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.xxl))
        DevOSButton(
            text = "Back to Course",
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.sm))
        DevOSButton(
            text = "Retry Quiz",
            onClick = { /* TODO */ },
            style = DevOSButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Quiz Progress Bar ─────────────────────────────────────────────────────────

@Composable
private fun QuizProgressBar(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { current.toFloat() / total.coerceAtLeast(1) },
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

// ── Quiz Option Item ──────────────────────────────────────────────────────────

@Composable
private fun QuizOptionItem(
    text: String,
    state: QuizOptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (state) {
        QuizOptionState.Default -> MaterialTheme.colorScheme.surface
        QuizOptionState.Selected -> MaterialTheme.colorScheme.primaryContainer
        QuizOptionState.Correct -> MaterialTheme.colorScheme.tertiaryContainer
        QuizOptionState.Incorrect -> MaterialTheme.colorScheme.errorContainer
    }
    val borderColor = when (state) {
        QuizOptionState.Default -> MaterialTheme.colorScheme.outline
        QuizOptionState.Selected -> MaterialTheme.colorScheme.primary
        QuizOptionState.Correct -> MaterialTheme.colorScheme.tertiary
        QuizOptionState.Incorrect -> MaterialTheme.colorScheme.error
    }
    val textColor = when (state) {
        QuizOptionState.Default -> MaterialTheme.colorScheme.onSurface
        QuizOptionState.Selected -> MaterialTheme.colorScheme.onPrimaryContainer
        QuizOptionState.Correct -> MaterialTheme.colorScheme.onTertiaryContainer
        QuizOptionState.Incorrect -> MaterialTheme.colorScheme.onErrorContainer
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .border(
                width = DevOSSpacing.dividerThickness,
                color = borderColor,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(
                enabled = state == QuizOptionState.Default || state == QuizOptionState.Selected,
                onClick = onClick,
            )
            .semantics {
                contentDescription = "Option: $text, ${state.name.lowercase()}"
                role = Role.RadioButton
            }
            .padding(DevOSSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
        )
    }
}

// ── Explanation Card ──────────────────────────────────────────────────────────

@Composable
private fun ExplanationCard(
    explanation: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(DevOSSpacing.base),
    ) {
        Text(
            text = "Explanation",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight(600)),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.xs))
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
