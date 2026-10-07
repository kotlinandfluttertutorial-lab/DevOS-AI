package com.devos.ai.designsystem.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Button style variants for DevOS AI.
 *
 * - Primary: filled, prominent actions
 * - Secondary: outlined, secondary actions
 * - Tertiary: filled tonal, low-emphasis actions
 * - Destructive: filled with error color, irreversible actions
 * - Ghost: text-only, minimal chrome
 */
enum class DevOSButtonStyle { Primary, Secondary, Tertiary, Destructive, Ghost }

/**
 * Standard button component for all DevOS AI interactions.
 *
 * Enforces 48dp minimum touch target and proper accessibility labels.
 *
 * @param text Button label
 * @param onClick Click callback
 * @param modifier Optional modifier
 * @param style Visual variant — defaults to [DevOSButtonStyle.Primary]
 * @param enabled Whether the button is interactive
 * @param leadingIcon Optional composable shown before the text
 * @param trailingIcon Optional composable shown after the text
 */
@Composable
fun DevOSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DevOSButtonStyle = DevOSButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val minSizeModifier = modifier
        .defaultMinSize(minHeight = DevOSSpacing.touchTarget)
        .semantics {
            contentDescription = text
            role = Role.Button
        }

    when (style) {
        DevOSButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = minSizeModifier,
            enabled = enabled,
        ) {
            ButtonContent(text = text, leadingIcon = leadingIcon, trailingIcon = trailingIcon)
        }

        DevOSButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = minSizeModifier,
            enabled = enabled,
        ) {
            ButtonContent(text = text, leadingIcon = leadingIcon, trailingIcon = trailingIcon)
        }

        DevOSButtonStyle.Tertiary -> Button(
            onClick = onClick,
            modifier = minSizeModifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        ) {
            ButtonContent(text = text, leadingIcon = leadingIcon, trailingIcon = trailingIcon)
        }

        DevOSButtonStyle.Destructive -> Button(
            onClick = onClick,
            modifier = minSizeModifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            ButtonContent(text = text, leadingIcon = leadingIcon, trailingIcon = trailingIcon)
        }

        DevOSButtonStyle.Ghost -> TextButton(
            onClick = onClick,
            modifier = minSizeModifier,
            enabled = enabled,
        ) {
            ButtonContent(text = text, leadingIcon = leadingIcon, trailingIcon = trailingIcon)
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
) {
    if (leadingIcon != null) {
        leadingIcon()
        Spacer(modifier = Modifier.width(DevOSSpacing.xs))
    }
    Text(text = text)
    if (trailingIcon != null) {
        Spacer(modifier = Modifier.width(DevOSSpacing.xs))
        trailingIcon()
    }
}
