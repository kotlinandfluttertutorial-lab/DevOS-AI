# DevOS AI

> **Understand your code. Learn faster. Build smarter.**

DevOS AI is an Android-native **AI Developer Command Center** that brings repository intelligence, code understanding, AI-powered chat, agent execution, and connected learning directly to your mobile device.

---

## What It Does

| Capability | Description |
|-----------|-------------|
| **Repository Intelligence** | Import and index GitHub/GitLab repositories on-device |
| **Code Intelligence** | Browse files, view syntax-highlighted code, explore symbols and dependency graphs |
| **AI Chat** | Ask questions about your code — answers grounded in actual source files |
| **AI Agents** | Multi-step tool-calling agents with transparent execution |
| **RAG** | Code-aware retrieval-augmented generation for accurate answers |
| **MCP** | Connect external tools via Model Context Protocol |
| **Git Intelligence** | Browse commit history, get AI summaries of recent changes |
| **Developer Intelligence** | GitHub/GitLab issues and PRs with AI triage and review |
| **Security Intelligence** | SAST scanning with AI-powered explanations |
| **Testing Intelligence** | Coverage analysis with AI gap suggestions |
| **Learning System** | AI-connected courses, lessons, and quizzes tied to your repositories |
| **Developer Memory** | Persistent AI context that learns your preferences |
| **Developer Command Center** | Home dashboard unifying all insights |

---

## Architecture

```
app/
├── core/               # Network, Database, Security, UI utilities
├── designsystem/       # Material 3 tokens + 40 DevOS components
├── feature/            # 15 feature modules (auth, home, code, AI chat, ...)
├── domain/             # Pure Kotlin domain models + repository interfaces
└── data/               # Repository implementations, AI providers, RAG
```

**Stack:** Kotlin · Jetpack Compose · Material 3 · MVVM · Clean Architecture · Hilt · Room · Retrofit · Coroutines/Flow

**AI:** OpenAI · Anthropic · Gemini · Ollama · Custom endpoints

---

## Project Documentation

### Design & UI
| Document | Path |
|----------|------|
| Figma Design System Analysis | `docs/figma/design-system.md` |
| Navigation Specification | `docs/figma/navigation.md` |
| Screen Inventory (40 screens) | `docs/figma/screen-inventory.md` |
| Component Inventory (40 components) | `docs/figma/component-inventory.md` |
| Figma → Jira Mapping | `docs/figma/figma-jira-mapping.md` |

### Architecture
| Document | Path |
|----------|------|
| Android Architecture | `docs/architecture/android-architecture.md` |
| Design System Specification | `docs/architecture/design-system-spec.md` |

### Planning & Tracking
| Document | Path |
|----------|------|
| Jira Tickets (CSV) | `docs/jira/DEVOS-JIRA.csv` |
| Jira Traceability Matrix | `docs/jira/DEVOS-JIRA-TRACEABILITY.md` |
| Kiro Implementation Prompts | `docs/jira/kiro-prompts/` |

### Quality
| Document | Path |
|----------|------|
| Test Strategy | `docs/testing/test-strategy.md` |
| AI Evaluation Strategy | `docs/testing/ai-evaluation-strategy.md` |
| Definition of Done | `docs/testing/definition-of-done.md` |

### Kiro AI-SDLC
| Document | Path |
|----------|------|
| Steering: Project | `.kiro/steering/project.md` |
| Steering: Android Standards | `.kiro/steering/android.md` |
| Steering: AI-SDLC Process | `.kiro/steering/ai-sdlc.md` |
| Steering: Design System | `.kiro/steering/design-system.md` |
| Skills | `.kiro/skills/` |
| Module Specs | `.kiro/specs/` |

---

## Figma Source

https://www.figma.com/design/FEocNGzA4RbwhYuKSNHYsp/AndroidUi--Community-?node-id=1007-5

---

## Jira Structure

| Epic | ID | Description |
|------|----|-------------|
| Foundation | DEVOS-E01 | Project setup, design system, navigation, auth |
| Repository Intelligence | DEVOS-E02 | Import, sync, indexing, overview |
| Code Intelligence | DEVOS-E03 | File explorer, code viewer, search, symbols, graph |
| AI Platform | DEVOS-E04 | Chat, RAG, providers, settings |
| Agents & MCP | DEVOS-E05 | Agent engine, tool execution, MCP integration |
| Developer Intelligence | DEVOS-E06 | Git, issues, pull requests |
| Quality Intelligence | DEVOS-E07 | Security scanning, test coverage |
| Learning | DEVOS-E08 | Dashboard, courses, lessons, quizzes |
| Developer Memory | DEVOS-E09 | Memory persistence and AI integration |
| Developer Command Center | DEVOS-E10 | Home, search, notifications |
| Android UI/UX | DEVOS-E11 | All 40 screens, dark mode, accessibility, tablet |
| AI-SDLC & Platform | DEVOS-E12 | CI/CD, observability, evaluation |

**Total tickets:** 140+ (70 implementation + 40 screen + 12 epics + supporting)

---

## Navigation

Primary bottom navigation: **Home · Projects · AI · Learn · More**

Secondary: Code · Architecture · Graph · Git · Issues · PRs · Security · Tests · Agents · MCP · Memory · Settings

Full navigation map: `docs/figma/navigation.md`

---

## AI Providers

Configure in **Settings → AI → Providers**:

- OpenAI (GPT-4o, GPT-4)
- Anthropic (Claude 3.5 Sonnet)
- Google (Gemini 1.5 Pro)
- Ollama (local models)
- Custom OpenAI-compatible endpoint

API keys stored in Android Keystore — never in source code.

---

## Security

- API keys: Android Keystore + EncryptedSharedPreferences
- No tokens in logs or BuildConfig
- Destructive operations require explicit confirmation
- Repository content sanitized before AI injection
- File path traversal protection

---

## Getting Started

> Full setup instructions will be added as implementation progresses.

1. Clone this repository
2. Open in Android Studio Hedgehog or later
3. Run `./gradlew assembleDebug`
4. Connect a device or start an emulator (API 26+)
5. Configure an AI provider in Settings

---

## AI-SDLC Lifecycle

```
DISCOVER → SPECIFY → DESIGN → PLAN → IMPLEMENT → VERIFY → REVIEW → DEPLOY → OBSERVE → LEARN → IMPROVE
```

Every feature is traceable from Jira ticket → Figma screen → Kiro spec → implementation → tests → acceptance criteria.

---

## License

Copyright © 2026 DevOS AI. All rights reserved.
