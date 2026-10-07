package com.devos.ai.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Circular health ring drawn with Canvas.
 *
 * Shows a circular track with a filled arc representing [percentage].
 *
 * @param percentage Value 0..100
 * @param modifier Optional modifier
 * @param color Arc color — defaults to MaterialTheme.colorScheme.primary
 * @param ringThickness Stroke width of the ring — defaults to 4dp
 */
@Composable
fun DevOSHealthIndicator(
    percentage: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    ringThickness: Dp = 4.dp,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val clampedPct = percentage.coerceIn(0, 100)

    Canvas(
        modifier = modifier
            .size(DevOSSpacing.iconSizeXLarge)
            .semantics { contentDescription = "Health: $clampedPct percent" },
    ) {
        val stroke = Stroke(width = ringThickness.toPx(), cap = StrokeCap.Round)
        val inset = ringThickness.toPx() / 2f
        val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
        val topLeft = Offset(inset, inset)

        // Track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )

        // Progress arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * clampedPct / 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}
