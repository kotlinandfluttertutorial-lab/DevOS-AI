# JetpackCompose — Remaining Ticket Queue

**As of 2026-10-09** | Workspace: `j:\Android\AndroidStudioProjects\Kiro\DevOS\DevOS-AI`

Before starting: `git pull origin main` to get all of Firoj's completed UI screens.

---

## Your remaining tickets (in dependency order)

### Next up — DEVOS-015 (DA-27) ← Already In Progress
See `docs/jira/kiro-prompts/DEVOS-015-repository-indexing-JetpackCompose.md`

---

### Group 1 — RAG + AI Providers (no dependencies beyond DEVOS-015)
| Ticket | DA | Summary | Prompt |
|--------|-----|---------|--------|
| DEVOS-031 | DA-39 | RAG pipeline — embedding and retrieval | `docs/jira/kiro-prompts/DEVOS-031-rag-pipeline.md` |
| DEVOS-032 | DA-41 | AI provider abstraction layer | `docs/jira/kiro-prompts/DEVOS-029-ai-platform-extended.md` (AIProvider section) |
| DEVOS-034 | DA-45 | Provider settings screen (API key storage UI) | `docs/jira/kiro-prompts/DEVOS-029-ai-platform-extended.md` (ProviderSettings section) |

---

### Group 2 — Agents & MCP (needs DEVOS-032)
| Ticket | DA | Summary | Prompt |
|--------|-----|---------|--------|
| DEVOS-037 | DA-47 | Agent orchestration engine (ReAct loop) | `docs/jira/kiro-prompts/DEVOS-037-agent-engine.md` |
| DEVOS-035 | DA-48 | Agent run screen | `docs/jira/kiro-prompts/DEVOS-037-agent-engine.md` (AgentRunScreen section) |
| DEVOS-036 | DA-50 | Agent tool execution detail screen | Same prompt |
| DEVOS-038 | DA-49 | MCP server integration | `docs/jira/kiro-prompts/DEVOS-038-mcp-integration.md` |
| DEVOS-039 | DA-51 | MCP tools screen | Same prompt |

---

### Group 3 — Developer Intelligence (needs DEVOS-041 GitHub/GitLab client)
| Ticket | DA | Summary | Prompt |
|--------|-----|---------|--------|
| DEVOS-041 | DA-52 | GitHub/GitLab API client | `docs/jira/kiro-prompts/DEVOS-040-developer-intelligence.md` |
| DEVOS-040 | DA-53 | Git history screen | Same prompt |
| DEVOS-042 | DA-54 | Issue list screen | Same prompt |
| DEVOS-043 | DA-56 | Issue detail screen | Same prompt |
| DEVOS-044 | DA-55 | Pull request list screen | Same prompt |
| DEVOS-045 | DA-57 | PR AI review screen | Same prompt |

---

### Group 4 — Quality Intelligence backend (no screen dep — you provide the data Firoj's screens call)
| Ticket | DA | Summary | Prompt |
|--------|-----|---------|--------|
| DEVOS-047 | DA-58 | Security scanning service (SAST + CVE) | `docs/jira/kiro-prompts/DEVOS-046-quality-intelligence.md` |
| DEVOS-049 | DA-60 | Test coverage analysis service (JaCoCo/Kover parser) | Same prompt |

---

### Group 5 — AI Engine add-ons (needs RAG done)
| Ticket | DA | Summary | Prompt |
|--------|-----|---------|--------|
| DEVOS-054 | DA-66 | Learning recommendation engine | `docs/jira/kiro-prompts/DEVOS-050-learning-dashboard.md` (LearningContextInjector section) |
| DEVOS-056 | DA-67 | Developer memory service | `docs/jira/kiro-prompts/DEVOS-055-developer-memory.md` |
| DEVOS-058 | DA-70 | Home AI recommendations engine | `docs/jira/kiro-prompts/DEVOS-057-home-dashboard.md` (AI recommendations section) |

---

### Group 6 — Platform (run last, needs everything done)
| Ticket | DA | Summary | Prompt |
|--------|-----|---------|--------|
| DEVOS-067 | DA-79 | CI/CD pipeline (GitHub Actions) | `docs/jira/kiro-prompts/DEVOS-067-platform-ai-sdlc.md` |
| DEVOS-068 | DA-81 | Observability — logging and analytics | Same prompt |
| DEVOS-069 | DA-80 | AI evaluation framework | Same prompt |
| DEVOS-066 | DA-76 | Responsive layout — tablet (WindowSizeClass 3-pane) | `docs/jira/kiro-prompts/DEVOS-062-settings-and-ui-polish.md` (Responsive section) — Note: basic NavigationRail already done by Firoj in DEVOS-066, you need the 3-pane split-view |

---

## Notes
- Firoj has completed all UI screens. Your domain interfaces, Room entities, and WorkManager workers are the data layer those screens call into.
- When your DEVOS-015 domain interfaces are merged, Firoj's stub ViewModels will start wiring to real data.
- Jira project key: `DA`, cloudId: `369f94fe-ea26-41c7-82b3-73ce0c43cce8`
- Status tracker: `.kiro/implementation-status.md`
