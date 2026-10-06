# Kiro Prompt — DEVOS-005: Design System Core Components

**Jira:** DEVOS-005  
**Epic:** DEVOS-E01  
**Figma:** N/A (system-wide)  
**Kiro Spec:** `.kiro/specs/foundation/design-system.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the DevOS AI design system component library in the `:designsystem` Gradle module.

**Architecture context:**
- Package: `com.devos.ai.designsystem`
- Module: `designsystem/`
- Full token specs: `docs/architecture/design-system-spec.md`
- Full component specs: `docs/figma/component-inventory.md`
- Design language reference: `docs/figma/design-system.md`

**Technology:** Kotlin + Jetpack Compose + Material 3

**What to implement:**

1. **Token files** in `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/`:
   - `Color.kt` — `DevOSDarkColorScheme`, `DevOSLightColorScheme`, `SyntaxColors`
   - `Typography.kt` — `DevOSTypography`, `JetBrainsMonoFamily`, `DevOSCodeTextStyle`
   - `Shape.kt` — `DevOSShapes`, `PillShape`, `CodeBlockShape`, `SearchBarShape`
   - `Spacing.kt` — `DevOSSpacing` object with all constants
   - `Theme.kt` — `DevOSTheme()` composable, `LocalDevOSSpacing`, `MaterialTheme.spacing`

2. **P0 components** (required before any screen work):
   - `DevOSLoadingState` — shimmer skeleton + spinner variants
   - `DevOSEmptyState` — icon + title + description + optional action button
   - `DevOSErrorState` — error icon + message + optional retry button
   - `DevOSTopBar` — wraps `TopAppBar` with DevOS title/subtitle/actions
   - `DevOSBottomBar` — 5-tab navigation: Home, Projects, AI, Learn, More
   - `DevOSButton` — Primary, Secondary, Tertiary, Destructive, Ghost variants
   - `DevOSCard` — clickable/static card container with DevOS border style

3. **P1 components:**
   - `DevOSSearchBar` — pill-shaped global search input
   - `DevOSCodeBlock` — syntax-highlighted code with JetBrains Mono, line numbers, copy
   - `DevOSMarkdownText` — markdown renderer (headings, bold, italic, code, lists, links)
   - `DevOSStatusBadge` — colored pill for Success/Warning/Error/Info/Running/Pending
   - `DevOSChip` — filter chip with optional leading icon
   - `DevOSTabRow` — M3 TabRow styled for DevOS
   - `DevOSSectionHeader` — section title with optional action

**Rules:**
- No hardcoded colors, dp, or sp values — always use tokens
- Every public composable takes `modifier: Modifier = Modifier`
- Every icon-only element has `contentDescription`
- All touch targets ≥ 48dp
- Test in both dark and light theme
- Code blocks: background always `SyntaxColors.background` regardless of theme

**Acceptance Criteria:**
- [ ] `./gradlew :designsystem:assembleDebug` passes
- [ ] DevOSTheme wraps MaterialTheme correctly
- [ ] All P0 components render without crash
- [ ] DevOSCodeBlock shows correct syntax colors for Kotlin
- [ ] DevOSBottomBar highlights active tab correctly
- [ ] Dark theme: background = `#0F111A`, surface = `#1A1C2E`
- [ ] Light theme: background = `#F5F7FF`, surface = `#FFFFFF`
