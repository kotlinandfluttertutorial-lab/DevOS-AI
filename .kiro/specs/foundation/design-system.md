# Spec: Foundation — Design System

**Jira:** DEVOS-002 / DEVOS-003 / DEVOS-004 / DEVOS-005 / DEVOS-006  
**Epic:** DEVOS-E01  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement the complete DevOS AI design system — all tokens and all 40 components — in the `:designsystem` module.

## Reference Documents
- `docs/figma/design-system.md` — color, typography, shape, spacing analysis
- `docs/architecture/design-system-spec.md` — full Kotlin specifications
- `docs/figma/component-inventory.md` — all 40 components with signatures

## Token Files

### `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/`

| File | Content |
|------|---------|
| `Color.kt` | `DevOSDarkColorScheme`, `DevOSLightColorScheme`, `SyntaxColors` |
| `Typography.kt` | `DevOSTypography`, `JetBrainsMonoFamily`, `DevOSCodeTextStyle` |
| `Shape.kt` | `DevOSShapes`, `PillShape`, `CodeBlockShape`, `SearchBarShape` |
| `Spacing.kt` | `DevOSSpacing` object with all spacing constants |
| `Theme.kt` | `DevOSTheme()` composable, `LocalDevOSSpacing`, `MaterialTheme.spacing` extension |

## Component Priority Order

### P0 — Must exist before any screen work
1. `DevOSLoadingState` — shimmer skeleton
2. `DevOSEmptyState` — icon + title + description + optional action
3. `DevOSErrorState` — error icon + message + optional retry
4. `DevOSTopBar` — wraps `TopAppBar` with DevOS styling
5. `DevOSBottomBar` — 5-tab navigation bar
6. `DevOSButton` — Primary, Secondary, Tertiary, Destructive variants
7. `DevOSCard` — clickable/non-clickable card container

### P1 — Required for core feature screens
8. `DevOSSearchBar` — global search pill
9. `DevOSCodeBlock` — syntax-highlighted code
10. `DevOSMarkdownText` — AI response renderer
11. `DevOSStatusBadge` — colored status pill
12. `DevOSChip` — filter chip
13. `DevOSTabRow` — screen-level tab row
14. `DevOSSectionHeader` — section divider with title

### P2 — Feature-specific components
15–40: All remaining components per `docs/figma/component-inventory.md`

## Font Resources
Place in `designsystem/src/main/res/font/`:
- `jetbrains_mono_regular.ttf`
- `jetbrains_mono_medium.ttf`
- `jetbrains_mono_bold.ttf`

## Test Requirements
- `ColorContrastTest` — verify WCAG AA for all text/background pairs
- `ComponentScreenshotTest` — screenshot tests for each component in dark + light
- Each component renders without crash at font scale 1.0 and 2.0

## Acceptance Criteria
- [ ] All token files compile and are used in `DevOSTheme()`
- [ ] All P0 components implemented with dark + light rendering
- [ ] All P1 components implemented
- [ ] `DevOSCodeBlock` renders 5 languages with correct syntax colors
- [ ] WCAG AA contrast verified for primary text combinations
- [ ] All components accessible (content descriptions, touch targets)
- [ ] No hardcoded dp/sp/color values in any component
