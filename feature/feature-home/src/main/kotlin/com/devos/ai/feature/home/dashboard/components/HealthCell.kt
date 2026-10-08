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
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.home.model.HealthStatus

/**
 * Single cell in the 2×2 project health grid on the Home Dashboard.
 *
 * Shows a large colored value (e.g. "2", "34%", "B+") with a label and sub-label.
 * Color is driven by [HealthStatus]: healthy=tertiary, warning=secondary, critical=error.
 *
 * Uses the non-clickable DevOSCard overload — these cells are display-only.
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
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = status.valueColor(),
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Maps HealthStatus to a color from the Material 3 scheme.
 *
 * WARNING maps to secondary (DevOSCyan300 / #89DDFF in dark, DevOSAmber300 is SyntaxColors.type).
 * The amber warning color (#FFCB6B) is not a standard M3 role in this theme; secondary is the
 * closest available role. TODO(DEVOS-060): add custom ExtendedColors.warning if needed.
 */
@Composable
private fun HealthStatus.valueColor(): Color = when (this) {
    HealthStatus.HEALTHY -> MaterialTheme.colorScheme.tertiary
    HealthStatus.WARNING -> MaterialTheme.colorScheme.secondary
    HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error
}
