# DevOS AI — Definition of Done

**Version:** 1.0  
**Date:** 2026-10-07

---

## Universal Definition of Done

Every Jira ticket in DevOS AI is **Done** when ALL of the following are checked:

---

### ✅ Requirements & Traceability

- [ ] Jira ticket has all required fields completed (User Story, Acceptance Criteria, Figma ref, Kiro Spec ref)
- [ ] All Acceptance Criteria are checked off in Jira
- [ ] Kiro Spec (`.kiro/specs/<module>/<spec>.md`) updated to reflect implementation
- [ ] Traceability chain intact: Jira → Figma → Spec → Code → Tests

---

### ✅ Implementation

- [ ] Feature implemented following Clean Architecture (UI → ViewModel → UseCase → Repository)
- [ ] All layers separated: feature module depends only on domain interfaces
- [ ] No hardcoded colors, sizes, or spacing (all via design system tokens)
- [ ] Only `DevOS*` components used (no ad-hoc UI elements that duplicate design system)
- [ ] All public composables have `modifier: Modifier = Modifier` parameter
- [ ] Navigation events exposed as callbacks, not direct navController references
- [ ] No secrets, API keys, or tokens in source code

---

### ✅ UI States

Every screen must implement and render:
- [ ] **Loading** state — `DevOSLoadingState` (shimmer or spinner)
- [ ] **Success** state — actual content rendered
- [ ] **Empty** state — `DevOSEmptyState` with contextual message and action
- [ ] **Error** state — `DevOSErrorState` with message and retry when applicable

---

### ✅ Design & Accessibility

- [ ] Dark mode verified — screen screenshots reviewed in dark theme
- [ ] Light mode verified — no invisible text, no broken contrast
- [ ] All icon-only elements have `contentDescription`
- [ ] All clickable non-Button elements have `Modifier.semantics { role = Role.Button }`
- [ ] Minimum touch target 48dp enforced on all interactive elements
- [ ] Text readable at 2× font scale (no clipping, no overflow)
- [ ] Color contrast ≥ 4.5:1 for body text, ≥ 3:1 for large text and UI components
- [ ] TalkBack traversal order is logical

---

### ✅ Tests

- [ ] ViewModel unit tests written and passing (all UiState transitions covered)
- [ ] UseCase unit tests written and passing
- [ ] Repository integration test written (if data layer changed)
- [ ] Compose UI tests written for all 4 states
- [ ] Navigation test written if new routes added
- [ ] No skipped or `@Disabled` tests without documented reason
- [ ] `./gradlew testDebugUnitTest` passes with zero failures
- [ ] `./gradlew connectedDebugAndroidTest` passes with zero failures

---

### ✅ AI Features (additional requirements)

For any ticket involving AI chat, RAG, agents, or AI analysis:

- [ ] AI responses stream incrementally (no blocking wait for full response)
- [ ] Source references shown with every grounded answer
- [ ] Agent steps shown transparently in UI (not hidden)
- [ ] Destructive tool calls require explicit user confirmation
- [ ] Repository content sanitized before AI context injection
- [ ] Retrieval precision test written (if RAG involved)
- [ ] Grounding accuracy test written (if answer generation involved)
- [ ] Agent test written (if agent execution involved)

---

### ✅ Security

- [ ] No API keys or tokens in source, logs, or BuildConfig
- [ ] Secrets stored via EncryptedSharedPreferences or Keystore
- [ ] File paths validated (no path traversal vulnerability)
- [ ] Destructive operations require explicit confirmation dialog
- [ ] Security review completed for auth, token, and data-handling changes

---

### ✅ Performance

- [ ] Lists use `LazyColumn` / `LazyRow` with pagination (no `Column { forEach }`)
- [ ] Heavy work runs on `Dispatchers.IO` (not Main)
- [ ] AI streaming does not block the UI thread
- [ ] No ANR risk (no synchronous main-thread network/disk I/O)
- [ ] No memory leak (ViewModel properly scoped, no context leaks)

---

### ✅ Code Review

- [ ] PR created against `main` (or feature branch as agreed)
- [ ] Self-review checklist completed
- [ ] Peer code review approved
- [ ] All review comments addressed or documented
- [ ] No `TODO` comments left without a linked Jira ticket
- [ ] No `println` or leftover debug logging

---

### ✅ CI/CD

- [ ] PR CI pipeline passes (unit tests, lint, build)
- [ ] No new lint errors introduced
- [ ] Build size regression check passes (APK size delta acceptable)

---

## Screen-Specific Checklist

For any ticket implementing a new screen, additionally verify:

- [ ] Route registered in `DevOSNavGraph`
- [ ] Deep link registered (if applicable per `docs/figma/navigation.md`)
- [ ] Screen listed in `docs/figma/screen-inventory.md`
- [ ] Figma → Jira mapping updated in `docs/figma/figma-jira-mapping.md`
- [ ] Back navigation works correctly
- [ ] Screen handles system back gesture / back button
- [ ] Status bar and navigation bar insets handled (edge-to-edge)
- [ ] Tablet layout reviewed (NavigationRail at ≥600dp)

---

## AI-SDLC Phase Mapping

| Phase | Done Criteria |
|-------|--------------|
| DISCOVER | Problem statement documented |
| SPECIFY | Jira ticket complete with all fields |
| DESIGN | Figma screen ref + UI states defined |
| PLAN | Kiro spec created |
| IMPLEMENT | Code written, above checklist complete |
| VERIFY | All tests pass |
| REVIEW | PR reviewed and merged |
| DEPLOY | CI passes, included in release |
| OBSERVE | Metrics collected, no regressions |
| LEARN | Learnings added to steering/skills |
| IMPROVE | Identified improvements tracked as new Jira tickets |

---

## Quick Reference Card

```
PRE-MERGE CHECKLIST
====================
[ ] All 4 UI states implemented
[ ] No hardcoded values
[ ] Dark mode looks correct  
[ ] Accessibility labels present
[ ] ViewModel tests pass
[ ] UI tests pass
[ ] No secrets in code
[ ] PR reviewed
[ ] CI green
```
