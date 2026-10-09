package com.devos.ai.designsystem.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSCodeBlock
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSStatusBadge

/**
 * Dark mode preview helper that wraps any composable in [DevOSTheme] with dark mode forced on.
 *
 * Usage:
 * ```
 * @Preview
 * @Composable
 * fun MyScreenDarkPreview() {
 *     DarkModePreview { MyScreen() }
 * }
 * ```
 *
 * Critical rule: code blocks MUST always use [SyntaxColors.background] (#1E1E2E)
 * regardless of theme — verified in [DevOSCodeBlock].
 */
@Composable
fun DarkModePreview(content: @Composable () -> Unit) {
    DevOSTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

/**
 * Light mode preview helper — mirrors [DarkModePreview] for side-by-side comparison.
 */
@Composable
fun LightModePreview(content: @Composable () -> Unit) {
    DevOSTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

// ─── Component previews: Dark ──────────────────────────────────────────────────

@Preview(name = "Design system — Dark", showBackground = true, backgroundColor = 0xFF0F111A)
@Composable
private fun DesignSystemDarkPreview() {
    DarkModePreview {
        DesignSystemSampler()
    }
}

@Preview(name = "Design system — Light", showBackground = true, backgroundColor = 0xFFF5F7FF)
@Composable
private fun DesignSystemLightPreview() {
    LightModePreview {
        DesignSystemSampler()
    }
}

// ─── Sampler ───────────────────────────────────────────────────────────────────

@Composable
private fun DesignSystemSampler() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.base)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        // Typography
        Text("Screen Title", style = MaterialTheme.typography.titleLarge)
        Text("Section Header", style = MaterialTheme.typography.titleSmall)
        Text("Body Large — primary content", style = MaterialTheme.typography.bodyLarge)
        Text("Body Medium — secondary text", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Caption / meta", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs))

        // Buttons
        DevOSButton(text = "Primary Button", onClick = {})
        DevOSButton(text = "Secondary Button", onClick = {}, style = DevOSButtonStyle.Secondary)
        DevOSButton(text = "Ghost Button", onClick = {}, style = DevOSButtonStyle.Ghost)

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs))

        // Status badges
        DevOSStatusBadge(label = "Critical", status = DevOSBadgeStatus.ERROR)
        DevOSStatusBadge(label = "High", status = DevOSBadgeStatus.WARNING)
        DevOSStatusBadge(label = "Medium", status = DevOSBadgeStatus.INFO)
        DevOSStatusBadge(label = "Low", status = DevOSBadgeStatus.SUCCESS)

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs))

        // Card
        DevOSCard {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.cardPadding)) {
                Text("Card Title", style = MaterialTheme.typography.titleSmall)
                Text("Card subtitle text", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs))

        // Code block — ALWAYS uses SyntaxColors.background (#1E1E2E), never surface color
        DevOSCodeBlock(
            code = "fun hello(): String {\n    return \"DevOS AI\"\n}",
            language = "kotlin",
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs))

        // Loading / Empty / Error states
        DevOSLoadingState()
        DevOSEmptyState(
            icon = Icons.Outlined.FolderOff,
            title = "No items",
            description = "Nothing to show here yet",
        )
        DevOSErrorState(description = "Something went wrong", onRetry = {})
    }
}
