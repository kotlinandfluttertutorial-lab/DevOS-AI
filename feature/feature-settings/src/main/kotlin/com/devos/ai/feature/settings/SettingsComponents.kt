package com.devos.ai.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing

/**
 * Shared settings-row composables for the feature-settings module.
 *
 * These are package-internal so that SettingsRootScreen and AISettingsScreen
 * can both use them without duplication.
 */

// ── Section header ────────────────────────────────────────────────────────────────

@Composable
internal fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.W700,
            letterSpacing = 0.5.sp,
            fontSize = 12.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            horizontal = MaterialTheme.spacing.base,
            vertical = MaterialTheme.spacing.sm,
        ),
    )
}

// ── Nav row (chevron trailing) ────────────────────────────────────────────────────

@Composable
internal fun SettingsNavRow(
    icon: ImageVector,
    iconContentDescription: String,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DevOSSpacing.touchTarget)
            .clickable(
                onClickLabel = label,
                onClick = onClick,
            )
            .semantics { role = Role.Button }
            .padding(horizontal = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconContentDescription,
            modifier = Modifier.size(MaterialTheme.spacing.iconSizeSmall),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.spacing.iconSizeSmall),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Info row (no chevron, for static values like Version) ─────────────────────────

@Composable
internal fun SettingsInfoRow(
    icon: ImageVector,
    iconContentDescription: String,
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DevOSSpacing.touchTarget)
            .padding(horizontal = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconContentDescription,
            modifier = Modifier.size(MaterialTheme.spacing.iconSizeSmall),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Toggle row (Switch trailing) ─────────────────────────────────────────────────

@Composable
internal fun SettingsToggleRow(
    icon: ImageVector,
    iconContentDescription: String,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DevOSSpacing.touchTarget)
            .clickable(
                onClickLabel = if (checked) "Disable $label" else "Enable $label",
                onClick = { onCheckedChange(!checked) },
            )
            .semantics { role = Role.Switch }
            .padding(horizontal = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconContentDescription,
            modifier = Modifier.size(MaterialTheme.spacing.iconSizeSmall),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

// ── Stepper row (value › trailing) ────────────────────────────────────────────────

@Composable
internal fun SettingsStepperRow(
    icon: ImageVector,
    iconContentDescription: String,
    label: String,
    subtitle: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DevOSSpacing.touchTarget)
            .padding(horizontal = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconContentDescription,
            modifier = Modifier.size(MaterialTheme.spacing.iconSizeSmall),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "$value ›",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.W600),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClickLabel = "Increase $label", onClick = onIncrement)
                .semantics {
                    role = Role.Button
                    contentDescription = "Change $label value, currently $value"
                }
                .padding(MaterialTheme.spacing.xs),
        )
    }
}
