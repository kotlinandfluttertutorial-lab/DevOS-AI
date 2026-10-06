# Spec: Foundation — Android Project Setup

**Jira:** DEVOS-001  
**Epic:** DEVOS-E01  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Create the multi-module Android Gradle project structure for DevOS AI.

## Modules to Create

| Module | Purpose |
|--------|---------|
| `app` | Entry point, MainActivity, NavGraph, Hilt wiring |
| `core:core-common` | Extensions, utilities, Result type, base interfaces |
| `core:core-network` | Retrofit, OkHttp, interceptors |
| `core:core-database` | Room DB, all DAOs and entities |
| `core:core-security` | Keystore, EncryptedSharedPreferences |
| `core:core-ui` | Compose base utilities, Modifier extensions |
| `core:core-testing` | Shared test utilities, fakes, TestDispatcher |
| `designsystem` | All design tokens + components |
| `feature:feature-auth` | Login, splash, onboarding |
| `feature:feature-home` | Home dashboard, notifications, search, profile |
| `feature:feature-project` | Project list, overview, settings |
| `feature:feature-repository` | Repo import, sync, overview, file explorer |
| `feature:feature-code` | Code viewer, search, symbols, graph, architecture |
| `feature:feature-ai-chat` | AI chat, answers, evidence |
| `feature:feature-agents` | Agent run, tool exec, MCP |
| `feature:feature-git` | Git history |
| `feature:feature-issues` | Issues list + detail |
| `feature:feature-prs` | PR list + AI review |
| `feature:feature-security` | Security findings |
| `feature:feature-testing` | Test intelligence |
| `feature:feature-learning` | Dashboard, course, lesson, quiz |
| `feature:feature-memory` | Developer memory |
| `feature:feature-settings` | AI, provider, project settings |
| `domain:domain-repository` | Repository intelligence domain models + interfaces |
| `domain:domain-code` | Code intelligence domain |
| `domain:domain-ai` | AI platform domain |
| `domain:domain-git` | Git intelligence domain |
| `domain:domain-learning` | Learning domain |
| `data:data-repository` | Repository data implementations |
| `data:data-code` | Code indexer, symbol extractor |
| `data:data-ai` | AI provider impls, RAG |
| `data:data-git` | Git + GitHub/GitLab API |
| `data:data-learning` | Learning content data |

## Key Configuration

### `app/build.gradle.kts`
- `applicationId = "com.devos.ai"`
- `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`
- Hilt plugin applied
- Navigation Compose dependency
- All feature modules included

### Version Catalog (`libs.versions.toml`)
All dependency versions centralized. No hardcoded versions in module build files.

### `gradle.properties`
- `android.useAndroidX=true`
- `kotlin.code.style=official`
- `android.nonTransitiveRClass=true`

## Acceptance Criteria
- [ ] `./gradlew assembleDebug` succeeds with zero errors
- [ ] All modules listed above exist and compile
- [ ] Hilt DI wired in app module
- [ ] Room DB initializes without error
- [ ] Navigation graph launches to Splash screen
- [ ] `./gradlew test` runs and passes in all modules

## Files to Create
```
settings.gradle.kts         ← include all modules
gradle/libs.versions.toml   ← version catalog
app/build.gradle.kts
core/core-common/build.gradle.kts
core/core-database/build.gradle.kts
core/core-network/build.gradle.kts
core/core-security/build.gradle.kts
core/core-ui/build.gradle.kts
core/core-testing/build.gradle.kts
designsystem/build.gradle.kts
[all feature|domain|data modules]/build.gradle.kts
```
