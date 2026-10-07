---
inclusion: auto
name: design-fidelity
description: Activated whenever implementing a UI screen or Compose component. Provides design tokens, per-screen visual specs extracted from the HTML mockup, and rules for icon/color/typography fidelity.
---

# DevOS AI — Design Fidelity Standards

## Source of truth hierarchy

1. **`docs/mockups/devos-ai-mockups.html`** — rendered pixel-accurate mockups for all 40 screens. Read the relevant screen's HTML section before implementing any composable. The inline CSS contains the exact colors, spacing, font sizes, and layout decisions.
2. **`docs/figma/design-system.md`** — full token definitions (colors, typography, spacing, shapes).
3. **`docs/figma/screen-inventory.md`** — per-screen descriptions and component lists.
4. **`docs/figma/component-inventory.md`** — component specs and API signatures.

When implementing any screen, **open `docs/mockups/devos-ai-mockups.html` and read the corresponding section** (search for the screen name or its HTML id: `s-login`, `s-home`, `s-onboarding`, etc.) before writing a single composable.

---

## Color tokens (always use these — never hardcode hex in Kotlin)

| Token | Dark value | Light value | Usage |
|-------|-----------|-------------|-------|
| `background` | `#0F111A` | `#F5F7FF` | Screen backgrounds |
| `surface` | `#1A1C2E` | `#FFFFFF` | Cards, inputs |
| `surfaceVariant` | `#252840` | `#E8EEFF` | Elevated cards, search bars |
| `outline` | `#3A3F58` | `#BBC4DD` | Borders, dividers |
| `onSurfaceVariant` | `#A8B3CF` | `#44476A` | Secondary text, labels |
| `primary` | `#82AAFF` | `#004DC4` | AI/interactive, active states |
| `secondary` | `#89DDFF` | `#006688` | Code/data elements |
| `tertiary` | `#C3E88D` | `#3D6900` | Success, tests passing |
| `error` | `#FF5370` | `#BA1A1A` | Errors, security alerts |
| `warning` | `#FFCB6B` | `#7B5800` | Warnings, stale data |
| `onBackground` | `#E2E8F0` | `#191C2A` | Primary text |
| `onSurface` | `#E2E8F0` | `#191C2A` | Card content |

**Syntax colors (always fixed, theme-independent):**

| Token | Value | Usage |
|-------|-------|-------|
| `SyntaxColors.keyword` | `#C792EA` | fun, val, class, etc. |
| `SyntaxColors.string` | `#C3E88D` | String literals |
| `SyntaxColors.number` | `#F78C6C` | Numeric literals |
| `SyntaxColors.comment` | `#546E7A` | // comments |
| `SyntaxColors.type` | `#FFCB6B` | Types, generics |
| `SyntaxColors.function` | `#82AAFF` | Function names |
| `SyntaxColors.background` | `#1E1E2E` | Code block background — ALWAYS this, both themes |

Access in Kotlin: `MaterialTheme.colorScheme.*` for theme tokens, `SyntaxColors.*` for code colors.

---

## Typography rules

- Screen titles: `titleLarge` (22sp Medium)
- Section headers: `titleSmall` (14sp Medium)
- Card primary text: `bodyLarge` (16sp) or `titleMedium` (16sp Medium)
- Card secondary / subtitles: `bodyMedium` (14sp) or `bodySmall` (12sp)
- Labels, badges, chips: `labelSmall` (11sp Medium)
- Code, file paths, symbols: `DevOSCodeTextStyle` (JetBrains Mono 13sp) — NEVER a system font

---

## Spacing rules (use `MaterialTheme.spacing.*`)

| Token | Value | Usage |
|-------|-------|-------|
| `xs` | 4dp | Icon gaps, tight padding |
| `sm` | 8dp | Item spacing, small gaps |
| `base` | 16dp | Screen horizontal padding, card padding |
| `xl` | 24dp | Section spacing |
| `xxl` | 32dp | Large section gaps |
| `touchTarget` | 48dp | Minimum touch target for all interactive elements |

---

## Icon rules

- **Icon set:** Material Symbols Rounded (via `Icons.Outlined.*` for inactive, `Icons.Filled.*` for active)
- **Standard size:** 24dp (`MaterialTheme.spacing.iconSize`)
- **Compact size:** 20dp (`MaterialTheme.spacing.iconSizeSm`)
- **Every icon-only interactive element MUST have a non-null `contentDescription`**
- **GitHub icon:** use a vector drawable `ic_github.xml` — a simplified octocat outline path, 24dp viewBox
- **GitLab icon:** use a vector drawable `ic_gitlab.xml` — simplified fox-head outline, 24dp viewBox

---

## Per-screen design specs

### Login Screen (FIGMA-03, route: `login`)

From `docs/mockups/devos-ai-mockups.html` → `#s-login`:

```
LAYOUT (top to bottom):
  Status bar (9:41, signal, battery)
  ↓ 48dp top padding
  Logo: 64×64dp rounded-rect (16dp corners), gradient background
        linear-gradient(135°, #82AAFF → #89DDFF)
        🤖 robot icon centered (28sp, or use a custom ic_devos_logo vector)
  ↓ 16dp
  Title: "Sign in to DevOS AI"  — titleLarge (24sp 700w), onBackground
  ↓ 6dp
  Subtitle: "Connect your developer workspace" — bodySmall (13sp), onSurfaceVariant
  ↓ 32dp
  GitHub button: outlined/secondary style, left-aligned content,
                 leading ic_github icon (18dp), "Continue with GitHub" (14sp)
  ↓ 12dp
  GitLab button: outlined/secondary style, left-aligned content,
                 leading ic_gitlab icon (18dp), "Continue with GitLab" (14sp)
  ↓ 8dp
  OR divider: Row [ 1dp line (outline color) — "or" (12sp, #546E7A) — 1dp line ]
  ↓ 8dp
  Email input: surface bg, 1dp outline border, 8dp radius, 14px 16px padding
               "EMAIL" label (labelSmall, onSurfaceVariant), email value text (bodyMedium)
  ↓ 12dp (gap between inputs)
  Password input: same style, "PASSWORD" label, masked "••••••••"
  ↓ 4dp
  "Sign In" primary button: filled, full-width
  ↓ 16dp
  Footer: "By continuing you agree to our Terms & Privacy Policy"
          Terms and Privacy Policy in primary (#82AAFF)

BACKGROUND: MaterialTheme.colorScheme.background (#0F111A in dark)
NO top app bar — full-screen no-chrome layout
```

**Key rules for Login:**
- Buttons left-align their content (icon + text), not centered — use `Arrangement.Start` inside button
- The email/password fields use a custom-styled `OutlinedTextField` matching the mockup style — `background=surface`, border=`outline`, radius=8dp
- Error state: red banner below the buttons (NOT a full-screen error), `error` color text
- Loading state: GitHub/GitLab buttons disable and show `CircularProgressIndicator(16dp)` inside the active button, replacing its text

### Splash Screen (FIGMA-01)

From mockup `#s-splash`:
- Full-screen `background` color
- Centered: logo (64dp), app name "DevOS AI" (displaySmall), tagline
- Tagline: "Understand your code. Learn faster. Build smarter." — bodyMedium, onSurfaceVariant
- Linear progress bar at the very bottom
- NO status bar chrome (edge-to-edge)

### Onboarding Screen (FIGMA-02)

From mockup `#s-onboarding`:
- Background: `background`
- Step illustrations use emoji + large icon approach (not custom images)
- Dot indicators: active = primary, inactive = outline color (6dp diameter)
- "Skip" button: Ghost style, top-right
- "Next" / "Get Started" button: Primary style, full-width, bottom of screen

---

## What NOT to do

- ❌ Never use hardcoded hex colors — always `MaterialTheme.colorScheme.*` or `SyntaxColors.*`
- ❌ Never hardcode `dp` values — always `MaterialTheme.spacing.*`
- ❌ Never use system default fonts for code — always `DevOSCodeTextStyle`
- ❌ Never make buttons center their icon+text content — use `Arrangement.Start` when icons precede text
- ❌ Never use emoji in production UI — use proper vector drawables or Material icons
- ❌ Never skip the `contentDescription` on icon-only elements
- ❌ Never use `DevOSErrorState` for inline errors — use a `Text` with `error` color inside the layout

---

## Checking your work

Before marking any screen ticket done, open `docs/mockups/devos-ai-mockups.html` in a browser and compare your implementation visually against the mockup. Key checks:

1. Colors match the tokens above
2. Typography hierarchy matches (screen title vs section header vs body vs label)
3. Spacing feels comfortable — 16dp screen padding, 8dp item gaps
4. Buttons, inputs, and cards use the correct corner radius (8dp buttons, 12dp cards)
5. Dark mode: code blocks always use `#1E1E2E` background
6. All interactive elements have 48dp minimum touch target
