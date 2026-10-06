---
inclusion: always
---

# DevOS AI — AI-SDLC Process Standards

## Lifecycle

Every feature follows this lifecycle. No phase may be skipped.

```
DISCOVER → SPECIFY → DESIGN → PLAN → IMPLEMENT → VERIFY → REVIEW → DEPLOY → OBSERVE → LEARN → IMPROVE
```

## Phase Gates

### DISCOVER
- Problem and user need clearly stated
- Existing code/patterns identified
- Constraints documented

### SPECIFY
- Jira ticket created with full template (see below)
- Acceptance criteria written as testable assertions
- Dependencies identified

### DESIGN
- Figma screen referenced or created
- UI states defined (Loading / Success / Empty / Error)
- API contract defined
- Data model defined

### PLAN
- Kiro spec created under `.kiro/specs/<module>/`
- Implementation steps ordered
- Test plan included in spec

### IMPLEMENT
- Feature module created following architecture rules
- ViewModel + UiState defined
- UseCase(s) defined
- Repository interface + implementation
- Compose screen implemented
- Dark mode verified
- Accessibility verified

### VERIFY
- Unit tests pass
- UI tests pass
- All 4 UI states exercised
- Dark mode screenshot reviewed
- Accessibility scan run

### REVIEW
- Code review completed
- Jira acceptance criteria checked off
- Definition of Done verified

### DEPLOY
- CI/CD pipeline passes
- Release notes updated

### OBSERVE
- Metrics collected
- Crash-free rate monitored

### LEARN
- Retrospective notes added
- Learnings fed back into steering/skills

### IMPROVE
- Performance regressions addressed
- AI evaluation regressions addressed

---

## Jira Ticket Template

Every ticket MUST contain ALL of the following sections:

```
**Backend dependency:** <service name> — <endpoint(s)> | None (if no backend call)

---

**Description:**
<Full context: what this ticket delivers, why it matters, key constraints>

**Required Skills:**
- <skill-1>
- <skill-2>
- <skill-3>

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- <Any ticket-specific rules>

**Implementation Tasks:**
1. <Concrete step>
2. <Concrete step>
...

**Acceptance Criteria:**
- AC1: <Testable, verifiable assertion — includes expected value or behavior>
- AC2: <Testable, verifiable assertion>
...

**Workflow:**
1. <Implementation step>
2. <Verification step>
...
N. Run ./gradlew testDebugUnitTest assembleDebug
N+1. Verify every AC individually with evidence.

**Definition of Done:**
Implementation complete; all ACs individually verified with evidence; tests pass; debug build succeeds.

> Never claim an AC is PASS without evidence.
```

### Key Rules for Ticket Quality
- **Backend dependency** must name the exact endpoint(s) or state "None".
- **Architecture Rules** must include at least the 5 baseline rules above + ticket-specific rules.
- **Acceptance Criteria** must be testable assertions with expected values — not vague goals.
- **Workflow** must end with a build + test command.
- **DoD** must include the "Never claim an AC is PASS without evidence" enforcement clause.
- ACs are numbered AC1, AC2... for traceability in test reports.

---

## Traceability Requirements

Every implementation artifact MUST be traceable:

```
Jira Ticket
  → Figma Screen (if UI)
    → Kiro Spec (.kiro/specs/)
      → Kiro Prompt (docs/jira/kiro-prompts/)
        → Compose Screen
          → ViewModel
            → UseCase
              → Repository
                → Unit Tests
                  → Acceptance Criteria checked
```

Any gap in this chain is a process violation and must be resolved before marking Done.

---

## Definition of Done (Summary)

A feature is Done when:
- [ ] All acceptance criteria checked
- [ ] Unit tests written and passing
- [ ] UI tests written and passing
- [ ] Dark mode tested
- [ ] Accessibility labels present
- [ ] No hardcoded values
- [ ] Code reviewed
- [ ] Jira ticket updated to Done
- [ ] Kiro spec marked complete

Full DoD: `docs/testing/definition-of-done.md`

---

## AI Evaluation Requirements

Every AI feature additionally requires:
- [ ] Retrieval precision test (RAG)
- [ ] Grounding accuracy test (does answer cite real code?)
- [ ] Agent success rate test
- [ ] Regression evaluation baseline set

Full AI eval strategy: `docs/testing/ai-evaluation-strategy.md`
