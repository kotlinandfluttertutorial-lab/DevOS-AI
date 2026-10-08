package com.devos.ai.feature.home.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.home.model.AIRecommendation
import com.devos.ai.feature.home.model.RecommendationType

/**
 * Card for an AI recommendation in the "AI Recommendations" section.
 *
 * Shows a type badge, title, description, and a dismiss button.
 * The dismiss button calls [onDismiss] which removes the recommendation from the list
 * and persists the dismissal to DataStore.
 */
@Composable
internal fun RecommendationCard(
    recommendation: AIRecommendation,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DevOSCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = recommendation.title },
    ) {
        Row(
            modifier = Modifier.padding(DevOSSpacing.base),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = recommendation.type.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DevOSSpacing.iconSize),
            )

            Spacer(modifier = Modifier.width(DevOSSpacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                DevOSStatusBadge(
                    status = recommendation.type.badgeStatus,
                    label = recommendation.type.label,
                )
                Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = { onDismiss(recommendation.id) },
                modifier = Modifier.size(DevOSSpacing.touchTarget),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss ${recommendation.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun RecommendationType.icon(): ImageVector = when (this) {
    RecommendationType.SECURITY -> Icons.Outlined.Security
    RecommendationType.TESTS -> Icons.Outlined.Science
    RecommendationType.LEARNING -> Icons.Outlined.MenuBook
    RecommendationType.ARCHITECTURE -> Icons.Outlined.Architecture
}
