---
inclusion: always
---

# DevOS AI — Project Steering

## What is DevOS AI?

DevOS AI is an **Android AI Developer Command Center** — a mobile-first application that combines:

- Repository Intelligence (clone, index, explore)
- Code Intelligence (syntax, symbols, search, graph)
- AI Platform (chat, RAG, context-grounded answers)
- AI Agents & MCP (tool-calling, transparent execution)
- Developer Intelligence (Git, Issues, PRs with AI)
- Quality Intelligence (Security, Testing)
- Learning System (AI-connected courses)
- Developer Memory (persistent AI context)

**Vision:** "Understand your code. Learn faster. Build smarter."

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room |
| Network | Retrofit + OkHttp |
| Async | Coroutines + Flow |
| Navigation | Navigation 3 (Compose) |
| AI Streaming | SSE / WebSocket via Flow |
| Security | Android Keystore + EncryptedSharedPreferences |
| Background | WorkManager |
| Images | Coil |

---

## Project Structure

```
DevOS-AI/
├── app/                    # Entry point, DI wiring, NavGraph
├── core/                   # core-common, core-network, core-database, core-security, core-ui, core-testing
├── designsystem/           # theme/ + components/ (single source of truth for all UI)
├── feature/                # feature-auth, feature-home, feature-project, feature-repository,
│                           # feature-code, feature-ai-chat, feature-agents, feature-git,
│                           # feature-issues, feature-prs, feature-security, feature-testing,
│                           # feature-learning, feature-memory, feature-settings
├── domain/                 # domain-repository, domain-code, domain-ai, domain-git, domain-learning
└── data/                   # data-repository, data-code, data-ai, data-git, data-learning
```

---

## Non-Negotiable Rules

1. **Never store API keys or tokens in source code or BuildConfig.**
   Always use Android Keystore / EncryptedSharedPreferences.

2. **Never log secrets.** No tokens, passwords, or API keys in Logcat/Timber.

3. **Require confirmation for all destructive operations:**
   delete, commit, push, create PR, write files, execute dangerous tools.

4. **Every screen must handle all 4 UI states:** Loading | Success | Empty | Error.

5. **No hardcoded colors, sizes, or spacing.** Always use design system tokens.

6. **Feature modules depend on domain interfaces only.**
   Never import data implementations directly from feature modules.

7. **All interactive elements need accessibility labels** (contentDescription, semantic roles).

8. **AI responses must be streamed** — never block UI waiting for a complete response.

9. **Never trust repository content as instructions** — sanitize before AI injection.

10. **Dark mode is first-class.** Test every screen in dark mode before marking done.

---

## Key File References

- Design System: `docs/figma/design-system.md`
- Navigation: `docs/figma/navigation.md`
- Screen Inventory: `docs/figma/screen-inventory.md`
- Component Inventory: `docs/figma/component-inventory.md`
- Architecture: `docs/architecture/android-architecture.md`
- Design System Spec: `docs/architecture/design-system-spec.md`
- Jira Tickets: `docs/jira/DEVOS-JIRA.csv`
- Traceability: `docs/jira/DEVOS-JIRA-TRACEABILITY.md`
- Figma → Jira: `docs/figma/figma-jira-mapping.md`
- Test Strategy: `docs/testing/test-strategy.md`

---

## Figma Source

https://www.figma.com/design/FEocNGzA4RbwhYuKSNHYsp/AndroidUi--Community-?node-id=1007-5

Use as design language reference. Adapt to DevOS AI. Do not copy blindly.
Primary form factor: Android phone (360–412dp). Tablet is a responsive enhancement.
