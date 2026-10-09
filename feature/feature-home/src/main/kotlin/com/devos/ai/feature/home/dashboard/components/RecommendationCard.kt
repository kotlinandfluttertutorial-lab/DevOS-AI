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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.theme.DevOSAmber300
import com.devos.ai.designsystem.theme.DevOSBlue300
import com.devos.ai.designsystem.theme.DevOSRed300
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.home.model.AIRecommendation
import com.devos.ai.feature.home.model.RecommendationType

/**
 * Card for an AI recommendation in the "AI Recommendations" section.
 *
 * Matches the recommendation cards in `#s-home` mockup:
 * - 3dp left border in severity color (security=error, tests=warning, learning/arch=primary)
 * - Icon + title (13sp 600w) + description (12sp, onSurfaceVariant)
 * - Dismiss ✕ button (top-right)
 *
 * The left border is painted with [drawBehind] so it sits on top of the card's
 * background without requiring an extra container composable.
 */
@Composable
internal fun RecommendationCard(
    recommendation: AIRecommendation,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = recommendation.type.borderColor()

    DevOSCard(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // 3dp start (left) border — mirrors `border-left: 3px solid` in the mockup
                val borderWidthPx = 3.dp.toPx()
                drawLine(
                    color       = borderColor,
                    start       = Offset(borderWidthPx / 2f, 0f),
                    end         = Offset(borderWidthPx / 2f, size.height),
                    strokeWidth = borderWidthPx,
                )
            }
            .semantics { contentDescription = recommendation.title },
    ) {
        Row(
            modifier          = Modifier.padding(DevOSSpacing.base),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector        = recommendation.type.icon(),
                contentDescription = null,
                tint               = borderColor,
                modifier           = Modifier.size(DevOSSpacing.iconSize),
            )

            Spacer(modifier = Modifier.width(DevOSSpacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                Text(
                    text  = recommendation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick  = { onDismiss(recommendation.id) },
                modifier = Modifier.size(DevOSSpacing.touchTarget),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Close,
                    contentDescription = "Dismiss ${recommendation.title}",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Maps [RecommendationType] to the exact left-border color from the mockup:
 * - SECURITY   → `#FF5370` error red
 * - TESTS      → `#FFCB6B` amber warning
 * - LEARNING   → `#82AAFF` primary blue
 * - ARCHITECTURE → `#82AAFF` primary blue
 */
@Composable
private fun RecommendationType.borderColor(): Color = when (this) {
    RecommendationType.SECURITY     -> DevOSRed300    // #FF5370
    RecommendationType.TESTS        -> DevOSAmber300   // #FFCB6B
    RecommendationType.LEARNING     -> DevOSBlue300    // #82AAFF
    RecommendationType.ARCHITECTURE -> DevOSBlue300    // #82AAFF
}

private fun RecommendationType.icon(): ImageVector = when (this) {
    RecommendationType.SECURITY     -> Icons.Outlined.Security
    RecommendationType.TESTS        -> Icons.Outlined.Science
    RecommendationType.LEARNING     -> Icons.Outlined.MenuBook
    RecommendationType.ARCHITECTURE -> Icons.Outlined.Architecture
}
