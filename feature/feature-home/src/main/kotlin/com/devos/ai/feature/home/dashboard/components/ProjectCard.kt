package com.devos.ai.feature.home.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.home.model.HealthStatus
import com.devos.ai.feature.home.model.ProjectSummary

/**
 * Horizontal-scroll card for a project shown in the "Recent Projects" section.
 *
 * Matches the 160dp-wide project card in the #s-home mockup.
 * Health status is shown as a color-coded dot: green = healthy, amber = warning, red = critical.
 */
@Composable
internal fun ProjectCard(
    project: ProjectSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DevOSCard(
        onClick = onClick,
        modifier = modifier
            .width(DevOSSpacing.projectCardWidth)
            .semantics {
                contentDescription = "${project.name} project, ${project.healthStatus.label()}"
            },
    ) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.base),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            // Project initial avatar
            Box(
                modifier = Modifier
                    .size(DevOSSpacing.iconSizeLarge)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = project.name.first().uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(DevOSSpacing.xs))

            Text(
                text = project.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

            Text(
                text = project.language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            // Health status dot + label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(DevOSSpacing.statusDotSize)
                        .background(
                            color = project.healthStatus.dotColor(),
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = project.healthStatus.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = project.healthStatus.dotColor(),
                )
            }
        }
    }
}

@Composable
private fun HealthStatus.dotColor(): Color = when (this) {
    HealthStatus.HEALTHY -> MaterialTheme.colorScheme.tertiary
    HealthStatus.WARNING -> MaterialTheme.colorScheme.secondary
    HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error
}

private fun HealthStatus.label(): String = when (this) {
    HealthStatus.HEALTHY -> "Healthy"
    HealthStatus.WARNING -> "Warning"
    HealthStatus.CRITICAL -> "Critical"
}
