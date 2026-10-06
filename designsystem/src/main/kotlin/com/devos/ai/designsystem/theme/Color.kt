package com.devos.ai.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Brand Palette ────────────────────────────────────────────────────────────

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

val DevOSAmber300   = Color(0xFFFFCB6B)
val DevOSAmber500   = Color(0xFF7B5800)

val DevOSRed100     = Color(0xFFFFDAD6)
val DevOSRed300     = Color(0xFFFF5370)
val DevOSRed500     = Color(0xFFBA1A1A)
val DevOSRed800     = Color(0xFF690000)

// ─── Neutral Palette ──────────────────────────────────────────────────────────

val DevOSNavy950    = Color(0xFF0F111A)   // darkest background
val DevOSNavy900    = Color(0xFF1A1C2E)   // card surface
val DevOSNavy800    = Color(0xFF252840)   // surfaceVariant
val DevOSNavy700    = Color(0xFF2A2E42)   // outlineVariant
val DevOSNavy600    = Color(0xFF3A3F58)   // outline / dividers
val DevOSNavy400    = Color(0xFFA8B3CF)   // secondary text
val DevOSNavy100    = Color(0xFFE2E8F0)   // primary text on dark

// ─── Dark Color Scheme ────────────────────────────────────────────────────────

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

// ─── Light Color Scheme ───────────────────────────────────────────────────────

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

// ─── Syntax Highlighting Colors ───────────────────────────────────────────────
// These are always used at the same values regardless of light/dark theme.

object SyntaxColors {
    val keyword    = Color(0xFFC792EA)   // purple
    val string     = Color(0xFFC3E88D)   // green
    val number     = Color(0xFFF78C6C)   // orange
    val comment    = Color(0xFF546E7A)   // slate
    val type       = Color(0xFFFFCB6B)   // amber
    val function   = Color(0xFF82AAFF)   // blue
    val variable   = Color(0xFFEEFFFF)   // almost white
    val operator   = Color(0xFF89DDFF)   // cyan
    val annotation = Color(0xFFFFCB6B)   // amber
    val background = Color(0xFF1E1E2E)   // deep dark — ALWAYS this, both themes
    val lineNumber = Color(0xFF4A5568)   // muted
    val selection  = Color(0xFF003298)   // primary container
}
