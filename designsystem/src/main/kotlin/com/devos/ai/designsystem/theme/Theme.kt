package com.devos.ai.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// Provide spacing via CompositionLocal so it's accessible as MaterialTheme.spacing
val LocalDevOSSpacing = staticCompositionLocalOf { DevOSSpacing }

/**
 * DevOS AI app theme.
 *
 * Wraps [MaterialTheme] with DevOS-specific tokens.
 * Dark mode is the primary experience — always test in dark first.
 *
 * Usage:
 * ```kotlin
 * DevOSTheme {
 *     MyScreen()
 * }
 * ```
 */
@Composable
fun DevOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DevOSDarkColorScheme else DevOSLightColorScheme

    CompositionLocalProvider(
        LocalDevOSSpacing provides DevOSSpacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = DevOSTypography,
            shapes      = DevOSShapes,
            content     = content,
        )
    }
}

/** Convenience accessor: `MaterialTheme.spacing.base` */
val MaterialTheme.spacing: DevOSSpacing
    @Composable @ReadOnlyComposable
    get() = LocalDevOSSpacing.current
