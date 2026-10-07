package com.devos.ai.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.PillShape

/** Identifies the sender of an AI chat message. */
enum class DevOSMessageRole { AI, USER }

/**
 * Chat message bubble for AI conversation.
 *
 * - AI messages: rendered as markdown in a surface card
 * - User messages: plain text in a primaryContainer rounded bubble
 *
 * The copy icon becomes visible when the user presses the message.
 *
 * @param content Message text (markdown for AI, plain text for USER)
 * @param role Message sender role
 * @param modifier Optional modifier
 * @param onCopy Called with [content] when copy is tapped. Pass null to hide.
 */
@Composable
fun DevOSAIMessage(
    content: String,
    role: DevOSMessageRole,
    modifier: Modifier = Modifier,
    onCopy: ((String) -> Unit)? = null,
) {
    var showCopy by remember { mutableStateOf(false) }

    when (role) {
        DevOSMessageRole.AI -> AIMessageBubble(
            content = content,
            modifier = modifier,
            showCopy = showCopy,
            onPress = { showCopy = !showCopy },
            onCopy = onCopy,
        )
        DevOSMessageRole.USER -> UserMessageBubble(
            content = content,
            modifier = modifier,
            showCopy = showCopy,
            onPress = { showCopy = !showCopy },
            onCopy = onCopy,
        )
    }
}

@Composable
private fun AIMessageBubble(
    content: String,
    modifier: Modifier,
    showCopy: Boolean,
    onPress: () -> Unit,
    onCopy: ((String) -> Unit)?,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "AI message" }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onPress() },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DevOSSpacing.xxs,
    ) {
        Column(modifier = Modifier.padding(DevOSSpacing.base)) {
            DevOSMarkdownText(markdown = content)
            if (showCopy && onCopy != null) {
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { onCopy(content) },
                        modifier = Modifier.size(DevOSSpacing.touchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy message",
                            modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(
    content: String,
    modifier: Modifier,
    showCopy: Boolean,
    onPress: () -> Unit,
    onCopy: ((String) -> Unit)?,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = PillShape,
                    )
                    .padding(
                        horizontal = DevOSSpacing.base,
                        vertical = DevOSSpacing.sm,
                    )
                    .semantics { contentDescription = "Your message" }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onPress() },
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (showCopy && onCopy != null) {
                IconButton(
                    onClick = { onCopy(content) },
                    modifier = Modifier.size(DevOSSpacing.touchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy message",
                        modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
