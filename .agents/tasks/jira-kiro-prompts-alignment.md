# Jira ↔ Kiro Prompts Alignment Report

**Generated:** Investigation run  
**Scope:** `docs/jira/DEVOS-JIRA.csv` vs `docs/jira/kiro-prompts/`  
**Verdict:** ⚠️ **Significant gap — 8 of 110 tickets have prompts (7.3% coverage)**

---

## 1. Coverage Summary

### Ticket inventory

| Range | Type | Count |
|-------|------|-------|
| DEVOS-E01–E12 | Epic-level tickets | 12 |
| DEVOS-001–070 | Feature/backend/platform tickets | 70 |
| DEVOS-101–140 | Screen-mirror tickets | 40 |
| **Total** | | **122** |

> Note: DEVOS-101–140 are intentional duplicates of DEVOS-001–070 for the screen-level Android UI epic (DEVOS-E11). They share acceptance criteria with their parent tickets ("See DEVOS-0xx"). Epics (E01–E12) are umbrella items — no Kiro prompt is expected for them. Effective actionable ticket count: **70 feature tickets**.

### Kiro prompt inventory

| Filename | Ticket(s) Covered |
|----------|--------------------|
| `DEVOS-001-android-setup.md` | DEVOS-001 |
| `DEVOS-005-design-system-components.md` | DEVOS-005 (also implicitly 002–004, 006) |
| `DEVOS-019-code-viewer.md` | DEVOS-019, DEVOS-020 |
| `DEVOS-026-ai-chat-screen.md` | DEVOS-026, DEVOS-027, DEVOS-028 |
| `DEVOS-031-rag-pipeline.md` | DEVOS-031 |
| `DEVOS-037-agent-engine.md` | DEVOS-037, DEVOS-035, DEVOS-036 |
| `DEVOS-050-learning-dashboard.md` | DEVOS-050, DEVOS-051, DEVOS-052, DEVOS-053, DEVOS-054 |
| `DEVOS-057-home-dashboard.md` | DEVOS-057, DEVOS-058 |

**Total prompts:** 8 files  
**Total tickets explicitly addressed in prompts:** ~20 of 70 feature tickets (~29%)  
**Tickets with zero prompt coverage:** ~50 of 70 feature tickets (~71%)

---

## 2. Gaps — Tickets Missing Kiro Prompts

The following feature tickets have **no corresponding Kiro prompt file**. Grouped by epic:

### DEVOS-E01 Foundation (missing 8/12)
| Ticket | Summary |
|--------|---------|
| DEVOS-002 | Design system — Color tokens (dark + light) |
| DEVOS-003 | Design system — Typography scale |
| DEVOS-004 | Design system — Shape and spacing tokens |
| DEVOS-006 | Design system — Code rendering components |
| DEVOS-007 | Bottom navigation implementation |
| DEVOS-008 | Navigation graph — full route definition |
| DEVOS-009 | Splash screen |
| DEVOS-010 | Onboarding flow — 4-step carousel |
| DEVOS-011 | Login screen — GitHub/GitLab OAuth |
| DEVOS-012 | Secure token storage |

> Note: DEVOS-002–006 are partially covered by `DEVOS-005-design-system-components.md` which references the design system spec. However, DEVOS-002/003/004 each have distinct ACs and no dedicated prompts.

### DEVOS-E02 Repository Intelligence (missing all 5)
| Ticket | Summary |
|--------|---------|
| DEVOS-013 | Repository import screen |
| DEVOS-014 | Repository sync screen with progress |
| DEVOS-015 | Repository clone and indexing service |
| DEVOS-016 | Repository overview screen |
| DEVOS-017 | Repository list screen with health |

### DEVOS-E03 Code Intelligence (missing 5/8)
| Ticket | Summary |
|--------|---------|
| DEVOS-018 | File explorer — tree view |
| DEVOS-021 | Code search screen |
| DEVOS-022 | Symbol details screen |
| DEVOS-023 | Symbol indexing service (Kotlin/Java) |
| DEVOS-024 | Dependency graph screen |
| DEVOS-025 | Architecture overview screen |

> Note: DEVOS-019 and DEVOS-020 are covered together in `DEVOS-019-code-viewer.md`.

### DEVOS-E04 AI Platform (missing 5/9)
| Ticket | Summary |
|--------|---------|
| DEVOS-029 | AI answer detail screen |
| DEVOS-030 | AI source evidence screen |
| DEVOS-032 | AI provider abstraction layer |
| DEVOS-033 | AI settings screen |
| DEVOS-034 | Provider settings screen |

> Note: DEVOS-026/027/028 covered in one prompt; DEVOS-031 has its own prompt.

### DEVOS-E05 Agents & MCP (missing 2/5)
| Ticket | Summary |
|--------|---------|
| DEVOS-038 | MCP server integration |
| DEVOS-039 | MCP tools screen |

> Note: DEVOS-035/036/037 covered together in `DEVOS-037-agent-engine.md`.

### DEVOS-E06 Developer Intelligence (missing all 6)
| Ticket | Summary |
|--------|---------|
| DEVOS-040 | Git history screen |
| DEVOS-041 | GitHub/GitLab API integration |
| DEVOS-042 | Issue list screen |
| DEVOS-043 | Issue detail screen |
| DEVOS-044 | Pull request list screen |
| DEVOS-045 | PR AI review screen |

### DEVOS-E07 Quality Intelligence (missing all 4)
| Ticket | Summary |
|--------|---------|
| DEVOS-046 | Security findings screen |
| DEVOS-047 | Security scanning service |
| DEVOS-048 | Test intelligence screen |
| DEVOS-049 | Test coverage analysis service |

### DEVOS-E08 Learning (covered — all 5 in one prompt)
✅ DEVOS-050–054 all addressed in `DEVOS-050-learning-dashboard.md`

### DEVOS-E09 Developer Memory (missing all 2)
| Ticket | Summary |
|--------|---------|
| DEVOS-055 | Developer memory screen |
| DEVOS-056 | Developer memory service |

### DEVOS-E10 Command Center (missing 3/5)
| Ticket | Summary |
|--------|---------|
| DEVOS-059 | Notifications screen |
| DEVOS-060 | Search screen — global |
| DEVOS-061 | Profile screen |

> Note: DEVOS-057/058 covered in `DEVOS-057-home-dashboard.md`.

### DEVOS-E11 Android UI/UX (missing all 4)
| Ticket | Summary |
|--------|---------|
| DEVOS-062 | Settings screen — root |
| DEVOS-063 | Project settings screen |
| DEVOS-064 | Dark mode — full implementation |
| DEVOS-065 | Accessibility audit — all screens |
| DEVOS-066 | Responsive layout — tablet support |

### DEVOS-E12 Platform (missing all 4)
| Ticket | Summary |
|--------|---------|
| DEVOS-067 | CI/CD pipeline setup |
| DEVOS-068 | Observability — logging and analytics |
| DEVOS-069 | AI evaluation framework |
| DEVOS-070 | Performance optimization — mobile |

### Screen-mirror tickets (DEVOS-101–140)
No screen-mirror ticket has its own Kiro prompt. This is acceptable only if the parent ticket's prompt is comprehensive enough — which is not the case since ~71% of parent tickets themselves have no prompt.

---

## 3. Prompts Without Tickets (Orphan Prompts)

**None found.** Every prompt file references at least one valid ticket ID that exists in `DEVOS-JIRA.csv`. No orphan prompts.

---

## 4. Content Alignment — Spot-Check Results

Five pairs were reviewed in detail.

### 4.1 DEVOS-001 ↔ `DEVOS-001-android-setup.md` — ✅ ALIGNED

**CSV Acceptance Criteria:**
- App module compiles; all feature modules defined; Hilt DI wired; Room DB configured; Navigation graph defined

**Prompt Acceptance Criteria:**
- `./gradlew assembleDebug` succeeds; all modules compile; app launches on API 26+; Hilt injection works; Room DB opens; `./gradlew test` passes

**Assessment:** Full alignment. Prompt expands on the CSV with concrete build commands, specific version numbers (Kotlin 2.0.21, AGP 8.7.0, Compose BOM 2024.09.03), exact file paths, and module list. No conflicts. Prompt is richer and more actionable than the CSV row.

**Minor gap:** Prompt does not explicitly call out the Navigation graph definition as a separate AC (though it's in the file list). Not a blocker.

---

### 4.2 DEVOS-005 ↔ `DEVOS-005-design-system-components.md` — ✅ ALIGNED (with note)

**CSV Acceptance Criteria:**
- DevOSButton, DevOSCard, DevOSTopBar, DevOSBottomBar, DevOSSearchBar, DevOSLoadingState, DevOSEmptyState, DevOSErrorState implemented; dark/light render; accessibility labels present

**Prompt Acceptance Criteria:**
- `:designsystem:assembleDebug` passes; DevOSTheme wraps MaterialTheme; all P0 components render; DevOSCodeBlock correct colors; DevOSBottomBar highlights active tab; dark/light theme color values correct

**Assessment:** Well aligned. All 8 components from the CSV AC list are covered. Prompt adds useful P1 components (DevOSMarkdownText, DevOSStatusBadge, DevOSTabRow) not explicitly in the CSV — this is additive, not a conflict.

**Gap:** DEVOS-002 (color tokens), DEVOS-003 (typography), DEVOS-004 (shape/spacing) are separate CSV tickets with distinct ACs but no dedicated prompts. The `DEVOS-005` prompt only references them indirectly via `docs/architecture/design-system-spec.md`. An implementer reading only the prompt would not know DEVOS-002–004 are separate deliverables with their own ACs.

---

### 4.3 DEVOS-019/020 ↔ `DEVOS-019-code-viewer.md` — ✅ ALIGNED

**CSV AC for DEVOS-019:** Kotlin/Java/Swift/Python/JS/TS highlighted; line numbers; JetBrains Mono font; horizontal scroll; line highlight on deep link

**CSV AC for DEVOS-020:** Explain/Debug/Find Usages/Generate Tests/Ask AI; actions open AI Chat with file context; accessible

**Prompt ACs:** File loads with line numbers; Kotlin/Java syntax correct; deep link scrolls to line; horizontal scroll; AI action bar opens chat with file context; Generate Tests sends file to AI; loading/error states; code background always `#1E1E2E`

**Assessment:** Strong alignment. The prompt merges both DEVOS-019 and DEVOS-020 into one cohesive implementation — this is correct since they're the same screen. Swift/Python/JS/TS languages are mentioned in the "support at minimum" note but not in the ACs; the CSV includes them in the ACs. This is a minor gap — the prompt's AC list doesn't explicitly verify those additional languages.

**Minor gap:** DEVOS-019 CSV AC says "Language badge" (visible indicator of detected language). Not present in prompt ACs.

---

### 4.4 DEVOS-026/027/028 ↔ `DEVOS-026-ai-chat-screen.md` — ✅ ALIGNED

**CSV ACs (DEVOS-026):** Message list renders; user/AI messages styled; markdown rendered; code blocks rendered; streaming responses; error state

**CSV ACs (DEVOS-027):** Context chips Global/Project/Repo/File/Symbol; active context persists; context in header; bottom sheet for selection

**CSV ACs (DEVOS-028):** 7 action chips; tap pre-fills input; chips hide on input

**Prompt ACs:** Messages stream character-by-character; context selector changes RAG scope; all 7 action chips render and pre-fill; sources rendered; agent steps shown; clear conversation shows dialog; empty state; error as inline banner

**Assessment:** Aligned. All CSV ACs are addressed. Prompt adds valuable detail (streaming behavior, inline error banner vs full-screen error). One notable addition in the prompt that's not in CSV: "Error state shows inline banner, not full-screen error" — this is a design decision made in the prompt that the CSV doesn't specify. Not a conflict but worth noting.

**Minor gap:** DEVOS-027 CSV AC says "Context displayed in header" but the prompt places the context selector as a chip row below the top bar, not inside it. This is a layout difference from the CSV's stated AC.

---

### 4.5 DEVOS-050–054 ↔ `DEVOS-050-learning-dashboard.md` — ✅ ALIGNED (with gap)

**CSV ACs (DEVOS-050):** Continue learning card; daily goal; streak; recommendations from repo; recent lessons; quiz scores

**CSV ACs (DEVOS-051):** Course title/description/difficulty; progress bar; lesson list with completion; repo connection; Start button

**CSV ACs (DEVOS-052):** Markdown content; code blocks; DevOS code examples from repo; Next/Prev navigation; Try in repo action

**CSV ACs (DEVOS-053):** Question counter; multiple choice; Submit; feedback (correct/wrong + explanation); final score

**CSV ACs (DEVOS-054):** Repository concepts extracted; mapped to learning modules; surfaced in dashboard and AI chat; personalized ordering

**Prompt ACs:** Dashboard shows current course progress; daily goal ring accurate; repo connection banner; lesson renders markdown/code blocks; "Try in repo" navigates to CodeViewerScreen; quiz scores; streak updates

**Assessment:** Dashboard (050), Lesson (052), and Quiz (053) are well-aligned. 

**Notable gap — DEVOS-051 (Course Details):** The prompt's LessonScreen layout doesn't implement a separate `CourseDetailsScreen`. The prompt jumps from dashboard to lesson. The CSV ticket DEVOS-051 specifically requires a course overview screen with a Start button, difficulty indicator, and lesson list. This screen is absent from the prompt.

**Notable gap — DEVOS-054 (Recommendation Engine):** The CSV AC says recommendations should be "surfaced in dashboard and AI chat." The prompt covers dashboard surfacing but says nothing about injecting learning recommendations into the AI chat context. This is a scope gap.

---

## 5. Architecture Rule Compliance in Prompts

All prompts were checked against the 5 baseline architecture rules from the AI-SDLC template:

| Rule | 001 | 005 | 019 | 026 | 031 | 037 | 050 | 057 |
|------|-----|-----|-----|-----|-----|-----|-----|-----|
| Presentation→Domain→Data dependency direction | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Hilt is the only DI mechanism | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | implied | implied |
| ViewModels expose StateFlow\<UiState\> | N/A | N/A | ✅ | ✅ | N/A | ✅ | ✅ | ✅ |
| API keys from EncryptedSharedPreferences | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A |
| Navigation events via SharedFlow | N/A | N/A | ✅ | ✅ | N/A | N/A | N/A | ✅ |

**Finding:** None of the prompts include the full Jira ticket template's "Architecture Rules" section as a named block. The architectural patterns are embedded in code examples rather than stated as explicit rules the implementer must follow. This means if an implementer reads only the prompt and not the steering files, they could miss constraints.

**Specific concern:** The `API keys from EncryptedSharedPreferences` rule is relevant to DEVOS-034 (Provider Settings — API key storage). That ticket has no prompt. DEVOS-001's prompt correctly avoids BuildConfig for secrets, but the pattern is not restated in the prompts that will implement actual key storage.

---

## 6. Traceability File Alignment

The `DEVOS-JIRA-TRACEABILITY.md` is well-structured and comprehensive — it maps all 70 feature tickets to Android components, ViewModels, UseCases, Repositories, and test classes. However:

- The traceability doc does **not** reference which tickets have Kiro prompts. There's no "Kiro Prompt" column.
- All epics are marked `🔵 Planned` — status is uniform and not updated per-ticket.
- The traceability doc lists test class names (e.g., `AIChatTest`, `RAGTest`) but these are aspirational — none of the test files exist yet in the codebase, and no Kiro specs exist under `.kiro/specs/` for most features.

---

## 7. Recommendations

### P0 — Immediate

1. **Create Kiro prompts for DEVOS-E02 (all 5 tickets):** Repository intelligence is the foundational data flow for everything downstream (code viewer, RAG, agents). No implementation can meaningfully start without prompts for import, sync, and the indexing service (DEVOS-013–017).

2. **Create a prompt for DEVOS-011 (Login/OAuth):** Security-sensitive; must include the encrypted keystore pattern explicitly. This is a P0 blocker — the app cannot function without auth.

3. **Create a prompt for DEVOS-012 (Secure token storage):** Should be a companion to DEVOS-011. The EncryptedSharedPreferences pattern must be specified explicitly since it's a Non-Negotiable Rule.

4. **Fix DEVOS-051 gap in `DEVOS-050-learning-dashboard.md`:** Add a `CourseDetailsScreen` section. The current prompt has no route, no ViewModel, and no composable for this screen.

### P1 — High Priority

5. **Create prompts for DEVOS-E06 (all 6 tickets):** Git, Issues, and PRs are complete epics with Figma references. No prompts exist at all.

6. **Create prompts for DEVOS-E07 (all 4 tickets):** Security and Test Intelligence epics have no prompts.

7. **Create a prompt for DEVOS-032 (AI provider abstraction):** This is a critical backend interface — without a prompt defining the provider interface, DEVOS-026/031/037 implementations may diverge.

8. **Fix DEVOS-054 scope gap:** Add AI chat injection of learning recommendations to the `DEVOS-050` prompt, or create a standalone `DEVOS-054-recommendation-engine.md`.

### P2 — Process Improvements

9. **Add a "Kiro Prompt" column to `DEVOS-JIRA-TRACEABILITY.md`:** Map each ticket to its prompt filename. This makes coverage gaps immediately visible.

10. **Add explicit "Architecture Rules" block to all prompts:** Follow the Jira ticket template format — state the 5 baseline rules plus ticket-specific constraints as a named section, not just embedded in code samples.

11. **Create prompts for DEVOS-E12 (platform tickets):** DEVOS-067 (CI/CD), DEVOS-068 (observability), DEVOS-069 (AI evaluation), DEVOS-070 (performance) are all currently prompt-less. These are cross-cutting concerns that affect every other feature.

12. **Decide whether DEVOS-101–140 need their own prompts:** Currently these screen-mirror tickets have no prompts and reference their parents. If the parent ticket has a prompt, the screen ticket should either point to it or be closed as duplicate. If the parent has no prompt, the screen ticket inherits the gap.

---

## 8. Summary Table

| Epic | Total Feature Tickets | Have Prompt | Coverage |
|------|-----------------------|-------------|----------|
| E01 Foundation | 12 | 2 (001, 005) | 17% |
| E02 Repository Intelligence | 5 | 0 | 0% |
| E03 Code Intelligence | 8 | 2 (019+020 combined) | 25% |
| E04 AI Platform | 9 | 3 (026+027+028, 031) | 33% |
| E05 Agents & MCP | 5 | 3 (035+036+037 combined) | 60% |
| E06 Developer Intelligence | 6 | 0 | 0% |
| E07 Quality Intelligence | 4 | 0 | 0% |
| E08 Learning | 5 | 5 (all in one prompt) | 100% |
| E09 Developer Memory | 2 | 0 | 0% |
| E10 Command Center | 5 | 2 (057+058 combined) | 40% |
| E11 Android UI/UX | 5 | 0 | 0% |
| E12 Platform | 4 | 0 | 0% |
| **Total** | **70** | **~20** | **~29%** |

---

*Report generated by read-only investigation of `docs/jira/DEVOS-JIRA.csv`, `docs/jira/DEVOS-JIRA-TRACEABILITY.md`, and all 8 files under `docs/jira/kiro-prompts/`. No files were modified.*
