package com.devos.ai.feature.home.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.theme.DevOSAmber300
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.home.model.HealthStatus

/**
 * Single cell in the 2×2 project health grid on the Home Dashboard.
 *
 * Matches the `.health-cell` in `#s-home` mockup:
 * - Label: 11sp uppercase, onSurfaceVariant
 * - Value: 24sp bold, color driven by [HealthStatus]
 *   • HEALTHY  → tertiary  (#C3E88D)
 *   • WARNING  → #FFCB6B  (DevOSAmber300 — NOT secondary/cyan)
 *   • CRITICAL → error     (#FF5370)
 * - Sub-label: 11sp, onSurfaceVariant
 */
@Composable
internal fun HealthCell(
    label: String,
    value: String,
    subLabel: String,
    status: HealthStatus,
    modifier: Modifier = Modifier,
) {
    DevOSCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label: $value. $subLabel" },
    ) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.base),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            Text(
                text  = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text       = value,
                style      = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color      = status.valueColor(),
            )
            Text(
                text  = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Maps [HealthStatus] to the exact color used in the mockup health grid.
 *
 * WARNING uses [DevOSAmber300] (#FFCB6B) directly — this is the `--warning` CSS variable
 * in the mockup. It is NOT the M3 `secondary` role (which is cyan #89DDFF in dark theme).
 */
@Composable
private fun HealthStatus.valueColor(): Color = when (this) {
    HealthStatus.HEALTHY  -> MaterialTheme.colorScheme.tertiary   // #C3E88D
    HealthStatus.WARNING  -> DevOSAmber300                         // #FFCB6B
    HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error       // #FF5370
}
