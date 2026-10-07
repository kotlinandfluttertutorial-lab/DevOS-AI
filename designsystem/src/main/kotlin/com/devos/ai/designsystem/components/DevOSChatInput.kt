package com.devos.ai.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.SearchBarShape

/**
 * Multi-line chat input bar with send and optional attach actions.
 *
 * The send button is enabled only when [value] is non-blank.
 * Attach button is shown only when [onAttach] is provided.
 *
 * @param value Current input text
 * @param onValueChange Callback for text changes
 * @param onSend Called when the Send button is tapped
 * @param modifier Optional modifier
 * @param onAttach Optional callback for the attach action — hides the attach icon if null
 * @param enabled Whether the input and buttons are interactive
 */
@Composable
fun DevOSChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    onAttach: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = DevOSSpacing.fabSize)
            .border(
                width = DevOSSpacing.xxs,
                color = MaterialTheme.colorScheme.outline,
                shape = SearchBarShape,
            )
            .semantics { contentDescription = "Chat input" },
        shape = SearchBarShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = DevOSSpacing.sm,
                vertical = DevOSSpacing.xs,
            ),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (onAttach != null) {
                IconButton(
                    onClick = onAttach,
                    enabled = enabled,
                    modifier = Modifier.size(DevOSSpacing.touchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Attach file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = DevOSSpacing.sm,
                        vertical = DevOSSpacing.sm,
                    ),
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Ask AI about your code…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.size(DevOSSpacing.touchTarget),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (enabled && value.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
