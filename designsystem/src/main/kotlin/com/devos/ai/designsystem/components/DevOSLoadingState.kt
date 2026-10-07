package com.devos.ai.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Centered loading indicator for full-screen loading states.
 *
 * Uses primary color CircularProgressIndicator.
 *
 * @param modifier Optional modifier
 */
@Composable
fun DevOSLoadingState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Loading" },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Animated shimmer placeholder for list items during loading.
 *
 * @param modifier Optional modifier — caller sets width/height
 */
@Composable
fun DevOSShimmerItem(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - DevOSSpacing.huge.value, 0f),
        end   = Offset(translateAnim, 0f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DevOSSpacing.xl)
            .background(brush = brush, shape = MaterialTheme.shapes.small),
    )
}
