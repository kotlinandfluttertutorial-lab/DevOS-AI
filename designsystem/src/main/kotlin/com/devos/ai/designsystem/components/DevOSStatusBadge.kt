package com.devos.ai.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.PillShape

/** Status variants for the DevOS badge component. */
enum class DevOSBadgeStatus {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    RUNNING,
    PENDING,
}

/**
 * Colored pill badge indicating a status.
 *
 * Maps to M3 color role pairs:
 * - SUCCESS → tertiaryContainer / onTertiaryContainer
 * - ERROR → errorContainer / onErrorContainer
 * - WARNING → secondaryContainer / onSecondaryContainer
 * - INFO, RUNNING → primaryContainer / onPrimaryContainer
 * - PENDING → surfaceVariant / onSurfaceVariant
 *
 * @param status The status variant
 * @param label Text shown inside the badge
 * @param modifier Optional modifier
 */
@Composable
fun DevOSStatusBadge(
    status: DevOSBadgeStatus,
    label: String,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor) = badgeColors(status)

    Box(
        modifier = modifier
            .background(color = containerColor, shape = PillShape)
            .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.xxs)
            .semantics { contentDescription = "$label status badge" },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

@Composable
private fun badgeColors(status: DevOSBadgeStatus): Pair<Color, Color> = when (status) {
    DevOSBadgeStatus.SUCCESS -> Pair(
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
    )
    DevOSBadgeStatus.ERROR -> Pair(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
    )
    DevOSBadgeStatus.WARNING -> Pair(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
    )
    DevOSBadgeStatus.INFO, DevOSBadgeStatus.RUNNING -> Pair(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
    )
    DevOSBadgeStatus.PENDING -> Pair(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
