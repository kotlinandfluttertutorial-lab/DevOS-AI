# DevOS AI — Figma Design System Analysis

**Source:** https://www.figma.com/design/FEocNGzA4RbwhYuKSNHYsp/AndroidUi--Community-?node-id=1007-5  
**Adapted for:** DevOS AI — Android Developer Command Center  
**Last analyzed:** 2026-10-07

---

## 1. Design Language Foundation

The Figma source is an Android UI Community kit built around Material 3 principles for a 402×874 iPhone/Android form factor. DevOS AI adapts its structural language while establishing a developer-focused identity layer on top.

### 1.1 Core Design Principles

| Principle | Description |
|-----------|-------------|
| **Developer-first** | Information density prioritized over decorative elements |
| **AI-native** | Every surface exposes AI actions contextually |
| **Dark-first** | Dark mode is the primary experience; light supported |
| **Mobile-first** | 360–412dp phone target; tablet as responsive enhancement |
| **Material 3** | Dynamic color, expressive typography, M3 component tokens |
| **Code readability** | Monospace rendering, syntax highlighting, line density |

---

## 2. Color System

### 2.1 Dark Theme (Primary)

```kotlin
// DevOS Dark Color Palette
val DevOSDark = darkColorScheme(
    primary          = Color(0xFF82AAFF),   // Soft Blue — AI/interactive
    onPrimary        = Color(0xFF001E6E),
    primaryContainer = Color(0xFF003298),
    onPrimaryContainer = Color(0xFFD6E2FF),

    secondary        = Color(0xFF89DDFF),   // Cyan — Code/data
    onSecondary      = Color(0xFF003547),
    secondaryContainer = Color(0xFF004D64),
    onSecondaryContainer = Color(0xFFC4E7FF),

    tertiary         = Color(0xFFC3E88D),   // Green — Success/tests
    onTertiary       = Color(0xFF1E3700),
    tertiaryContainer = Color(0xFF2D5000),
    onTertiaryContainer = Color(0xFFDFF5A7),

    error            = Color(0xFFFF5370),   // Red — Errors/security
    onError          = Color(0xFF690000),
    errorContainer   = Color(0xFF930000),
    onErrorContainer = Color(0xFFFFDAD6),

    background       = Color(0xFF0F111A),   // Deep navy-black
    onBackground     = Color(0xFFE2E8F0),

    surface          = Color(0xFF1A1C2E),   // Card surface
    onSurface        = Color(0xFFE2E8F0),
    surfaceVariant   = Color(0xFF252840),   // Elevated surface
    onSurfaceVariant = Color(0xFFA8B3CF),

    outline          = Color(0xFF3A3F58),   // Borders/dividers
    outlineVariant   = Color(0xFF2A2E42),

    inverseSurface   = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF1A1C2E),
    inversePrimary   = Color(0xFF004DC4),
)
```

### 2.2 Light Theme

```kotlin
val DevOSLight = lightColorScheme(
    primary          = Color(0xFF004DC4),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E2FF),
    onPrimaryContainer = Color(0xFF001E6E),

    secondary        = Color(0xFF006688),
    onSecondary      = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC4E7FF),
    onSecondaryContainer = Color(0xFF001E2A),

    tertiary         = Color(0xFF3D6900),
    onTertiary       = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDFF5A7),
    onTertiaryContainer = Color(0xFF1E3700),

    error            = Color(0xFFBA1A1A),
    background       = Color(0xFFF5F7FF),
    onBackground     = Color(0xFF191C2A),
    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF191C2A),
    surfaceVariant   = Color(0xFFE8EEFF),
    outline          = Color(0xFFBBC4DD),
)
```

### 2.3 Semantic Color Tokens

| Token | Dark | Light | Usage |
|-------|------|-------|-------|
| `colorAI` | `#82AAFF` | `#004DC4` | AI features, chat |
| `colorCode` | `#89DDFF` | `#006688` | Code blocks, symbols |
| `colorSuccess` | `#C3E88D` | `#3D6900` | Tests passing, health |
| `colorWarning` | `#FFCB6B` | `#7B5800` | Warnings, stale data |
| `colorError` | `#FF5370` | `#BA1A1A` | Errors, security issues |
| `colorInfo` | `#B2CCD6` | `#3B5268` | Info badges |
| `colorCode.keyword` | `#C792EA` | `#7C00BF` | Syntax: keywords |
| `colorCode.string` | `#C3E88D` | `#2E6200` | Syntax: strings |
| `colorCode.number` | `#F78C6C` | `#B44800` | Syntax: numbers |
| `colorCode.comment` | `#546E7A` | `#546E7A` | Syntax: comments |
| `colorCode.type` | `#FFCB6B` | `#7B5800` | Syntax: types |
| `colorCode.function` | `#82AAFF` | `#004DC4` | Syntax: functions |

---

## 3. Typography

Based on Material 3 type scale, adapted for developer readability.

```kotlin
val DevOSTypography = Typography(
    // Display — App name, hero text
    displayLarge  = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Normal, lineHeight = 64.sp),
    displayMedium = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Normal, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Normal, lineHeight = 44.sp),

    // Headline — Screen titles, section headers
    headlineLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp),

    // Title — Card titles, list item primaries
    titleLarge  = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.1.sp),

    // Body — Primary content, AI responses
    bodyLarge   = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp, letterSpacing = 0.4.sp),

    // Label — Badges, chips, tabs, captions
    labelLarge  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// Code font — for code blocks, file paths, symbols
val DevOSCodeFont = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)
val DevOSCodeStyle = TextStyle(
    fontFamily = DevOSCodeFont,
    fontSize = 13.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp,
)
```

---

## 4. Shape Tokens

```kotlin
val DevOSShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Chips, inline badges
    small      = RoundedCornerShape(8.dp),   // Buttons, input fields
    medium     = RoundedCornerShape(12.dp),  // Cards, dialogs
    large      = RoundedCornerShape(16.dp),  // Bottom sheets, panels
    extraLarge = RoundedCornerShape(24.dp),  // Bottom nav, modal containers
)
```

---

## 5. Spacing System

```kotlin
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

    // Layout
    val screenPadding    = 16.dp
    val cardPadding      = 16.dp
    val sectionSpacing   = 24.dp
    val listItemSpacing  = 8.dp
    val iconSize         = 24.dp
    val iconSizeSm       = 20.dp
    val iconSizeLg       = 32.dp
    val touchTarget      = 48.dp
    val bottomNavHeight  = 80.dp
    val topBarHeight     = 64.dp
    val fabSize          = 56.dp
}
```

---

## 6. Elevation & Surfaces

| Level | dp | Usage |
|-------|----|-------|
| 0 | 0dp | Background |
| 1 | 1dp | Cards, list items |
| 2 | 3dp | Search bars, nav bars |
| 3 | 6dp | FABs, raised cards |
| 4 | 8dp | Dialogs, bottom sheets |
| 5 | 12dp | Modal overlays |

---

## 7. Navigation Patterns (Extracted from Figma)

### Bottom Navigation
- 5 primary tabs: Home, Projects, AI, Learn, More
- Active indicator: pill-shaped background, primary color
- Icon + label for all items
- Height: 80dp with bottom inset
- Background: `surfaceVariant` with slight elevation

### Top App Bar
- Compact: title + action icons (back, search, overflow)
- Medium: larger title, collapses on scroll
- Centered title variant for modal flows
- Background merges with screen on scroll

### Navigation Drawer / Modal
- Secondary destinations accessible via "More"
- Drawer: 360dp wide, rounded right corners
- Scrim: 50% black overlay

---

## 8. Component Patterns

### Cards
- Rounded 12dp corners
- 1dp border: `outline` color in dark, transparent in light
- 16dp internal padding
- Elevation level 1
- Press state: ripple + slight scale (0.97)

### Buttons
- Primary: filled, 8dp radius, 48dp height
- Secondary: outlined, same geometry
- Tertiary: text only
- Destructive: error color variant
- Icon buttons: 40×40dp touch target minimum

### Input Fields
- Outlined style (M3 OutlinedTextField)
- 8dp corner radius
- 48dp minimum height
- AI search bar: rounded pill (24dp), elevated, prominent

### Chips
- Filter chips for tabs / context selectors
- 8dp radius, 32dp height
- Icon optional

### Status Badges
- Pill shape, 4dp radius
- Color-coded: success/warning/error/info
- 12sp label text

---

## 9. Icon Usage

- Material Symbols (Rounded) as primary icon set
- 24dp standard size, 20dp compact
- Outlined style for inactive state
- Filled style for active state
- Custom icons: code-specific glyphs (branches, nodes, graph)

---

## 10. Dark Mode Considerations

| Element | Dark Implementation |
|---------|-------------------|
| Code blocks | `background = #1E1E2E`, monospace, syntax colors |
| AI messages | Distinct bubble: `surfaceVariant` tint |
| User messages | Primary-tinted bubble |
| Cards | Slight border to separate from background |
| Status indicators | Sufficient contrast (WCAG AA minimum) |
| Agent steps | Progress indicators use tertiary color |

---

## 11. Accessibility Targets

- Minimum contrast ratio: 4.5:1 for normal text, 3:1 for large
- Touch targets: minimum 48×48dp
- All interactive elements: `contentDescription` required
- Support `LocalTextStyle` overrides for font scaling
- Focus indicators visible in keyboard/switch access
- Screen reader labels for icons and status indicators

---

## 12. Responsive Breakpoints

| Form Factor | Width | Layout Adaptation |
|-------------|-------|-------------------|
| Small phone | < 360dp | Compact spacing, single column |
| Phone | 360–412dp | Standard layout (primary target) |
| Large phone | 412–480dp | Comfortable spacing |
| Tablet | > 600dp | Navigation rail, split pane |
| Large tablet | > 840dp | Three-pane (nav + list + detail) |
