## Summary

<!-- Describe what this PR does and why. -->

## Related Jira Ticket

<!-- Link the ticket from docs/jira/DEVOS-JIRA.csv, e.g. DEVOS-001 -->
<!-- Traceability: docs/jira/DEVOS-JIRA-TRACEABILITY.md -->

Ticket: <!-- DEVOS-XXX -->

## Type of Change

- [ ] ✨ Feature
- [ ] 🐛 Bug fix
- [ ] ♻️ Refactor
- [ ] 📝 Docs / config only

## Testing Performed

<!-- Describe what you tested, how, and on which device/emulator. -->

- [ ] Unit tests written and passing (`./gradlew testDebugUnitTest`)
- [ ] UI (Compose) tests written and passing (if UI changed)
- [ ] Tested on emulator / device (API level: ___)

## Definition of Done Checklist

### Requirements & Traceability
- [ ] All acceptance criteria in the Jira ticket are checked off
- [ ] Kiro spec (`.kiro/specs/<module>/<spec>.md`) updated to reflect implementation
- [ ] Traceability chain intact: Jira → Figma → Spec → Code → Tests

### Implementation
- [ ] Clean Architecture layers respected (feature depends on domain only, not data)
- [ ] No hardcoded colors, sizes, or spacing (design system tokens used throughout)
- [ ] Only `DevOS*` components used — no ad-hoc duplicates of design system components
- [ ] No secrets, API keys, or tokens in source code or logs

### UI States
- [ ] **Loading** state implemented (`DevOSLoadingState`)
- [ ] **Success** state implemented
- [ ] **Empty** state implemented (`DevOSEmptyState`)
- [ ] **Error** state implemented (`DevOSErrorState` with retry where applicable)

### Design & Accessibility
- [ ] Dark mode verified — screenshots reviewed
- [ ] Light mode verified — no invisible text or broken contrast
- [ ] Icon-only elements have `contentDescription`
- [ ] Clickable non-Button elements use `Modifier.semantics { role = Role.Button }`
- [ ] Minimum 48dp touch target on all interactive elements

### Code Quality
- [ ] No `println` or leftover debug logging
- [ ] No unresolved `TODO` comments (link to Jira if keeping)
- [ ] Self-review completed

### CI
- [ ] CI workflow passes (unit tests + debug build green)
- [ ] Jira ticket updated to **Done** (after merge)
