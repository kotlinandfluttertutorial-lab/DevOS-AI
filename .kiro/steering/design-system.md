---
inclusion: always
---

# DevOS AI — Design System Standards

## Single Source of Truth

All visual definitions live in `:designsystem`. Feature modules never define their own colors, typography, spacing, or shapes.

## Token Usage

```kotlin
// Colors — always via MaterialTheme
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.onSurfaceVariant

// Typography — always via MaterialTheme
MaterialTheme.typography.titleMedium
MaterialTheme.typography.bodyLarge
MaterialTheme.typography.labelSmall

// Shapes — always via MaterialTheme
MaterialTheme.shapes.medium        // 12dp cards
MaterialTheme.shapes.small         // 8dp buttons/inputs
MaterialTheme.shapes.extraLarge    // 24dp bottom sheets

// Spacing — always via MaterialTheme.spacing
MaterialTheme.spacing.base         // 16dp screen padding
MaterialTheme.spacing.cardPadding  // 16dp card padding
MaterialTheme.spacing.sectionSpacing // 24dp between sections

// Code font — always DevOSCodeTextStyle
DevOSCodeTextStyle                 // JetBrains Mono 13sp
SyntaxColors.keyword               // #C792EA
SyntaxColors.string                // #C3E88D
```

## Component Usage

Always use DevOS components. Never re-implement:

| Need | Use |
|------|-----|
| Any button | `DevOSButton` |
| Any card container | `DevOSCard` |
| Top app bar | `DevOSTopBar` |
| Bottom navigation | `DevOSBottomBar` |
| Search bar | `DevOSSearchBar` |
| Code display | `DevOSCodeBlock` |
| AI message | `DevOSAIMessage` |
| Loading state | `DevOSLoadingState` |
| Empty state | `DevOSEmptyState` |
| Error state + retry | `DevOSErrorState` |
| Status pill | `DevOSStatusBadge` |
| Health ring | `DevOSHealthIndicator` |
| Markdown | `DevOSMarkdownText` |
| Chat input | `DevOSChatInput` |

## Dark Mode

- All components automatically respect `MaterialTheme.colorScheme`
- Code blocks always use `SyntaxColors.background = #1E1E2E` regardless of theme
- Test every component in both dark and light before PR

## Accessibility Minimum Bar

Every component must:
- Meet WCAG AA contrast (4.5:1 text, 3:1 large/UI)
- Have 48×48dp minimum touch target
- Provide `contentDescription` on icon-only elements
- Use `Modifier.semantics { role = Role.Button }` on clickable non-Button elements

## Typography Hierarchy in Screens

```
Screen title      → titleLarge (22sp Medium)
Section header    → titleSmall (14sp Medium)
Card primary      → bodyLarge (16sp) or titleMedium (16sp Medium)
Card secondary    → bodyMedium (14sp)
Captions/meta     → bodySmall (12sp) or labelSmall (11sp)
Badges/chips      → labelSmall (11sp Medium)
Code              → DevOSCodeTextStyle (JetBrains Mono 13sp)
```

## State Pattern in Screens

```kotlin
when (val s = uiState) {
    is MyUiState.Loading -> DevOSLoadingState()
    is MyUiState.Success -> MySuccessContent(s)
    is MyUiState.Empty   -> DevOSEmptyState(
        icon = Icons.Outlined.FolderOff,
        title = "No items yet",
        description = "Import a repository to get started",
        action = { DevOSButton("Import", onClick = onImport) }
    )
    is MyUiState.Error   -> DevOSErrorState(
        description = s.message,
        onRetry = if (s.retryable) viewModel::retry else null
    )
}
```

## Card Pattern

```kotlin
DevOSCard(onClick = { onNavigate() }) {
    Row(
        modifier = Modifier.padding(MaterialTheme.spacing.cardPadding),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        // icon
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```
