package com.devos.ai.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DevOS AI shape tokens.
 *
 * Mapped to Material 3 shape scale.
 * Custom shapes for specialized surfaces below.
 */
val DevOSShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // chips, inline badges
    small      = RoundedCornerShape(8.dp),   // buttons, input fields
    medium     = RoundedCornerShape(12.dp),  // cards, dialogs
    large      = RoundedCornerShape(16.dp),  // bottom sheets, panels
    extraLarge = RoundedCornerShape(24.dp),  // bottom nav, modal containers
)

// Custom shapes for specific use cases
val PillShape         = RoundedCornerShape(50)
val CodeBlockShape    = RoundedCornerShape(8.dp)
val SearchBarShape    = RoundedCornerShape(28.dp)
val BottomSheetShape  = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val AvatarShape       = RoundedCornerShape(50)
val BadgeShape        = RoundedCornerShape(4.dp)
