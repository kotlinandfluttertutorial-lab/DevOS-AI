# DevOS AI — Design System Specification

**Version:** 1.0  
**Date:** 2026-10-07  
**Package:** `com.devos.ai.designsystem`

---

## Overview

The DevOS AI design system is a Material 3 extension with developer-focused token additions. It lives in the `:designsystem` Gradle module and is the single source of truth for all visual definitions.

```
designsystem/
├── theme/
│   ├── Color.kt           ← All color tokens (dark + light)
│   ├── Typography.kt      ← Type scale + code font
│   ├── Shape.kt           ← Corner radius tokens
│   ├── Spacing.kt         ← Spacing constants
│   └── Theme.kt           ← MaterialTheme wrapper
└── components/
    ├── DevOSButton.kt
    ├── DevOSCard.kt
    ├── DevOSTopBar.kt
    ├── DevOSBottomBar.kt
    ├── DevOSSearchBar.kt
    ├── DevOSCodeBlock.kt
    ├── DevOSMarkdownText.kt
    ├── DevOSAIMessage.kt
    ├── DevOSUserMessage.kt
    ├── DevOSAgentStep.kt
    ├── DevOSToolExecution.kt
    ├── DevOSSourceReference.kt
    ├── DevOSStatusBadge.kt
    ├── DevOSHealthIndicator.kt
    ├── DevOSProjectCard.kt
    ├── DevOSRepositoryCard.kt
    ├── DevOSFileItem.kt
    ├── DevOSChip.kt
    ├── DevOSTabRow.kt
    ├── DevOSSectionHeader.kt
    ├── DevOSLanguageBar.kt
    ├── DevOSCommitItem.kt
    ├── DevOSIssueItem.kt
    ├── DevOSPRItem.kt
    ├── DevOSSecurityFinding.kt
    ├── DevOSProgressRing.kt
    ├── DevOSChatInput.kt
    ├── DevOSContextSelector.kt
    ├── DevOSLoadingState.kt
    ├── DevOSEmptyState.kt
    ├── DevOSErrorState.kt
    ├── DevOSBranchChip.kt
    ├── DevOSLessonCard.kt
    ├── DevOSQuizOption.kt
    ├── DevOSMemoryItem.kt
    ├── DevOSMCPTool.kt
    ├── DevOSNotificationItem.kt
    ├── DevOSActionChip.kt
    └── DevOSSyntaxHighlight.kt
```

---

## Color.kt

```kotlin
package com.devos.ai.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Brand Palette ───────────────────────────────────────────────────────────
val DevOSBlue100    = Color(0xFFD6E2FF)
val DevOSBlue300    = Color(0xFF82AAFF)
val DevOSBlue500    = Color(0xFF004DC4)
val DevOSBlue800    = Color(0xFF001E6E)

val DevOSCyan100    = Color(0xFFC4E7FF)
val DevOSCyan300    = Color(0xFF89DDFF)
val DevOSCyan500    = Color(0xFF006688)
val DevOSCyan800    = Color(0xFF001E2A)

val DevOSGreen100   = Color(0xFFDFF5A7)
val DevOSGreen300   = Color(0xFFC3E88D)
val DevOSGreen500   = Color(0xFF3D6900)
val DevOSGreen800   = Color(0xFF1E3700)

val DevOSAmber100   = Color(0xFFFFEFBF)
val DevOSAmber300   = Color(0xFFFFCB6B)
val DevOSAmber500   = Color(0xFF7B5800)

val DevOSRed100     = Color(0xFFFFDAD6)
val DevOSRed300     = Color(0xFFFF5370)
val DevOSRed500     = Color(0xFFBA1A1A)
val DevOSRed800     = Color(0xFF690000)

// ─── Neutral Palette ─────────────────────────────────────────────────────────
val DevOSNavy950    = Color(0xFF0F111A)   // darkest background
val DevOSNavy900    = Color(0xFF1A1C2E)   // surface
val DevOSNavy800    = Color(0xFF252840)   // surfaceVariant
val DevOSNavy700    = Color(0xFF2A2E42)   // outlineVariant
val DevOSNavy600    = Color(0xFF3A3F58)   // outline
val DevOSNavy400    = Color(0xFFA8B3CF)   // onSurfaceVariant
val DevOSNavy100    = Color(0xFFE2E8F0)   // onBackground

// ─── Dark Color Scheme ───────────────────────────────────────────────────────
val DevOSDarkColorScheme = darkColorScheme(
    primary                = DevOSBlue300,
    onPrimary              = DevOSBlue800,
    primaryContainer       = Color(0xFF003298),
    onPrimaryContainer     = DevOSBlue100,
    secondary              = DevOSCyan300,
    onSecondary            = DevOSCyan800,
    secondaryContainer     = Color(0xFF004D64),
    onSecondaryContainer   = DevOSCyan100,
    tertiary               = DevOSGreen300,
    onTertiary             = DevOSGreen800,
    tertiaryContainer      = Color(0xFF2D5000),
    onTertiaryContainer    = DevOSGreen100,
    error                  = DevOSRed300,
    onError                = DevOSRed800,
    errorContainer         = Color(0xFF930000),
    onErrorContainer       = DevOSRed100,
    background             = DevOSNavy950,
    onBackground           = DevOSNavy100,
    surface                = DevOSNavy900,
    onSurface              = DevOSNavy100,
    surfaceVariant         = DevOSNavy800,
    onSurfaceVariant       = DevOSNavy400,
    outline                = DevOSNavy600,
    outlineVariant         = DevOSNavy700,
    inverseSurface         = DevOSNavy100,
    inverseOnSurface       = DevOSNavy900,
    inversePrimary         = DevOSBlue500,
)

// ─── Light Color Scheme ──────────────────────────────────────────────────────
val DevOSLightColorScheme = lightColorScheme(
    primary                = DevOSBlue500,
    onPrimary              = Color.White,
    primaryContainer       = DevOSBlue100,
    onPrimaryContainer     = DevOSBlue800,
    secondary              = DevOSCyan500,
    onSecondary            = Color.White,
    secondaryContainer     = DevOSCyan100,
    onSecondaryContainer   = DevOSCyan800,
    tertiary               = DevOSGreen500,
    onTertiary             = Color.White,
    tertiaryContainer      = DevOSGreen100,
    onTertiaryContainer    = DevOSGreen800,
    error                  = DevOSRed500,
    onError                = Color.White,
    errorContainer         = DevOSRed100,
    onErrorContainer       = DevOSRed800,
    background             = Color(0xFFF5F7FF),
    onBackground           = Color(0xFF191C2A),
    surface                = Color.White,
    onSurface              = Color(0xFF191C2A),
    surfaceVariant         = Color(0xFFE8EEFF),
    onSurfaceVariant       = Color(0xFF44476A),
    outline                = Color(0xFFBBC4DD),
    outlineVariant         = Color(0xFFD4DBEF),
)

// ─── Syntax Highlighting Colors ──────────────────────────────────────────────
object SyntaxColors {
    val keyword    = Color(0xFFC792EA)
    val string     = Color(0xFFC3E88D)
    val number     = Color(0xFFF78C6C)
    val comment    = Color(0xFF546E7A)
    val type       = Color(0xFFFFCB6B)
    val function   = Color(0xFF82AAFF)
    val variable   = Color(0xFFEEFFFF)
    val operator   = Color(0xFF89DDFF)
    val background = Color(0xFF1E1E2E)
    val lineNumber = Color(0xFF4A5568)
}
```

---

## Typography.kt

```kotlin
package com.devos.ai.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.R

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

val DevOSCodeTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontSize   = 13.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp,
)

val DevOSTypography = Typography(
    displayLarge   = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Normal, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium  = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Normal, lineHeight = 52.sp),
    displaySmall   = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Normal, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall      = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
```

---

## Shape.kt

```kotlin
package com.devos.ai.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val DevOSShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// Custom shapes
val PillShape        = RoundedCornerShape(50)
val CodeBlockShape   = RoundedCornerShape(8.dp)
val SearchBarShape   = RoundedCornerShape(28.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
```

---

## Spacing.kt

```kotlin
package com.devos.ai.designsystem.theme

import androidx.compose.ui.unit.dp

object DevOSSpacing {
    val xxs  = 2.dp
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val base = 16.dp
    val lg   = 20.dp
    val xl   = 24.dp
    val xxl  = 32.dp
    val xxxl = 48.dp
    val huge = 64.dp

    // Layout constants
    val screenHorizontalPadding = 16.dp
    val screenVerticalPadding   = 16.dp
    val cardPadding             = 16.dp
    val sectionSpacing          = 24.dp
    val listItemVerticalPadding = 12.dp
    val listItemHorizontalPadding = 16.dp
    val iconSize                = 24.dp
    val iconSizeSmall           = 20.dp
    val iconSizeLarge           = 32.dp
    val iconSizeXLarge          = 48.dp
    val touchTarget             = 48.dp
    val bottomNavHeight         = 80.dp
    val topBarHeight            = 64.dp
    val fabSize                 = 56.dp
    val chipHeight              = 32.dp
    val badgeHeight             = 24.dp
    val dividerThickness        = 1.dp
    val codeLineHeight          = 20.dp
}
```

---

## Theme.kt

```kotlin
package com.devos.ai.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDevOSSpacing = staticCompositionLocalOf { DevOSSpacing }

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

// Convenience accessor
val MaterialTheme.spacing get() = LocalDevOSSpacing.current
```

---

## Usage Guidelines

### DO
- Always reference tokens via `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, `MaterialTheme.shapes.*`
- Use `MaterialTheme.spacing.*` for all padding and margin
- Use `DevOSCodeTextStyle` for all code-related text
- Use `SyntaxColors.*` for syntax highlighting

### DON'T
- Hardcode colors, sizes, or spacing values
- Create one-off `TextStyle` overrides in feature modules
- Duplicate component logic from `designsystem` in feature modules
- Skip dark-mode testing for any component

---

## Component Accessibility Standards

Every component must:
1. Provide `contentDescription` on all icon-only elements
2. Meet 48dp minimum touch target
3. Use semantic roles (`Role.Button`, `Role.Tab`, etc.)
4. Support `LocalContentColor` inheritance
5. Test with font scale 1.0 and 2.0
