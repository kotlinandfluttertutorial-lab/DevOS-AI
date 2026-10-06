package com.devos.ai.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DevOS AI spacing system.
 *
 * All padding, margin, gap, and size values must come from here.
 * Never hardcode dp values in feature or component code.
 *
 * Access via: `MaterialTheme.spacing.base`
 */
object DevOSSpacing {
    val xxs:  Dp = 2.dp
    val xs:   Dp = 4.dp
    val sm:   Dp = 8.dp
    val md:   Dp = 12.dp
    val base: Dp = 16.dp
    val lg:   Dp = 20.dp
    val xl:   Dp = 24.dp
    val xxl:  Dp = 32.dp
    val xxxl: Dp = 48.dp
    val huge: Dp = 64.dp

    // Layout
    val screenHorizontalPadding:  Dp = 16.dp
    val screenVerticalPadding:    Dp = 16.dp
    val cardPadding:              Dp = 16.dp
    val sectionSpacing:           Dp = 24.dp
    val listItemVerticalPadding:  Dp = 12.dp
    val listItemHorizontalPadding:Dp = 16.dp

    // Components
    val iconSize:       Dp = 24.dp
    val iconSizeSmall:  Dp = 20.dp
    val iconSizeLarge:  Dp = 32.dp
    val iconSizeXLarge: Dp = 48.dp
    val touchTarget:    Dp = 48.dp
    val bottomNavHeight:Dp = 80.dp
    val topBarHeight:   Dp = 64.dp
    val fabSize:        Dp = 56.dp
    val chipHeight:     Dp = 32.dp
    val badgeHeight:    Dp = 24.dp
    val dividerThickness: Dp = 1.dp
    val codeLineHeight: Dp = 20.dp
}
