package com.devos.ai.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.R

/**
 * JetBrains Mono — used for all code rendering surfaces.
 *
 * Font files must be placed in designsystem/src/main/res/font/:
 *   jetbrains_mono_regular.ttf
 *   jetbrains_mono_medium.ttf
 *   jetbrains_mono_bold.ttf
 */
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/**
 * Standard text style for all code-related content.
 * Use everywhere code, file paths, or symbols are displayed.
 */
val DevOSCodeTextStyle = TextStyle(
    fontFamily    = JetBrainsMonoFamily,
    fontSize      = 13.sp,
    lineHeight    = 20.sp,
    letterSpacing = 0.sp,
)

/** DevOS AI type scale — Material 3 scale with developer-focused sizing. */
val DevOSTypography = Typography(
    displayLarge   = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Normal,   lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium  = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Normal,   lineHeight = 52.sp),
    displaySmall   = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Normal,   lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium,   lineHeight = 32.sp),
    titleLarge     = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium,   lineHeight = 28.sp),
    titleMedium    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium,   lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium,   lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal,   lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal,   lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall      = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal,   lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium,   lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium,   lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,   lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
