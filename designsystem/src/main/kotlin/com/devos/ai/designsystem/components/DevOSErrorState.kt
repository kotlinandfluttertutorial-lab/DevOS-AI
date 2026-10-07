package com.devos.ai.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Full-screen error state with optional retry action.
 *
 * @param description Error message — bodyMedium typography
 * @param modifier Optional modifier
 * @param onRetry Called when user taps Retry. Pass null to hide the Retry button.
 */
@Composable
fun DevOSErrorState(
    description: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Error: $description" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(DevOSSpacing.iconSizeXLarge),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(DevOSSpacing.base))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(DevOSSpacing.xl))
            DevOSButton(
                text = "Retry",
                onClick = onRetry,
            )
        }
    }
}
