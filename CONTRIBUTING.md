# Contributing to DevOS AI

## Prerequisites

- Android Studio Hedgehog or later
- JDK 17 (Temurin recommended)
- Git ≥ 2.40
- [GitHub CLI](https://cli.github.com/) (`gh`) — optional but recommended

## Branch Strategy

We use a lightweight GitFlow: feature/bugfix branches merge into `develop`
(the integration branch), and `develop` merges into `main` at release time.

| Branch | Role |
|--------|------|
| `main` | Release branch — always shippable; only receives merges from `develop` |
| `develop` | Integration branch — default PR target for all feature/bugfix work |
| `feat/DEVOS-XXX-short-desc` | New feature |
| `fix/DEVOS-XXX-short-desc` | Bug fix |
| `refactor/DEVOS-XXX-short-desc` | Refactor |
| `docs/short-desc` | Docs / config only |

> Do not merge feature work directly into `main`. Branch off `develop`, open a PR
> against `develop`, and let `develop` flow to `main` at release time.

## How to Raise a Pull Request

### 1. Create a feature branch from `develop`

```bash
git checkout develop
git pull origin develop
git checkout -b feat/DEVOS-XXX-your-feature
```

### 2. Implement your changes

Follow the [Definition of Done](docs/testing/definition-of-done.md) — every item
in that checklist must be satisfied before marking a PR ready for review.

Key rules (from project steering):
- Never store API keys or tokens in source code — use Android Keystore / EncryptedSharedPreferences.
- Every screen must handle all 4 UI states: Loading | Success | Empty | Error.
- No hardcoded colors, sizes, or spacing — always use design system tokens.
- Feature modules depend on domain interfaces only, never on data implementations.
- Dark mode is first-class — test every screen in dark mode before marking done.

### 3. Run the local checks

```bash
./gradlew testDebugUnitTest   # unit tests must pass
./gradlew assembleDebug       # debug build must succeed
```

### 4. Commit your work

Use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages:

```bash
git add <specific files>
git commit -m "feat(DEVOS-XXX): add repository list screen"
```

### 5. Push and open a PR

```bash
# First push — set up remote tracking
git push -u origin feat/DEVOS-XXX-your-feature
```

Then open the PR via the GitHub CLI:

```bash
gh pr create --base develop --title "feat(DEVOS-XXX): short description" --body-file .github/PULL_REQUEST_TEMPLATE.md
```

Or open it in the GitHub web UI at:
`https://github.com/kotlinandfluttertutorial-lab/DevOS-AI/compare/develop...feat/DEVOS-XXX-your-feature`

GitHub will pre-populate the PR description from `.github/PULL_REQUEST_TEMPLATE.md` automatically.

### 6. CI must pass before merge

Every PR triggers the `CI` workflow (`.github/workflows/ci.yml`) which runs:
- `./gradlew testDebugUnitTest` — unit tests
- `./gradlew assembleDebug` — debug build

The PR cannot be merged until the CI check is green. If the check fails, inspect
the uploaded artifacts (`test-results`) linked in the Actions run for details.

### 7. Get a code review

At least one approval is required before merging. Address all review comments or
document why a comment is not actioned (link a Jira ticket if deferring).

---

## Branch Protection (Repo Admin)

Branch protection rules are a **repository-admin setting**, configured in the
GitHub UI (not from a local clone):

> GitHub → Settings → Branches → Branch protection rules → Require status checks
> to pass before merging → add `build` (the job name from `ci.yml`).

Apply protection to **both** branches:

- **`develop`** — require CI green + at least one review before merge; block
  direct pushes (PRs only). This is the default integration branch and the most
  important one to protect.
- **`main`** — same protection; only receives release merges from `develop`.

Also set the **default branch to `develop`** under
GitHub → Settings → Branches → Default branch, so PRs open against it by default.

A repo admin must set this up once after the first CI run succeeds.

---

## Jira Traceability

Every PR must reference a Jira ticket. See:
- `docs/jira/DEVOS-JIRA.csv` — full ticket list
- `docs/jira/DEVOS-JIRA-TRACEABILITY.md` — Jira ↔ code traceability map

Tickets without a Jira reference will be sent back for triage.

---

## Further Reading

| Document | Path |
|----------|------|
| Definition of Done | `docs/testing/definition-of-done.md` |
| Architecture | `docs/architecture/android-architecture.md` |
| Design System | `docs/architecture/design-system-spec.md` |
| Screen Inventory | `docs/figma/screen-inventory.md` |
| Test Strategy | `docs/testing/test-strategy.md` |
