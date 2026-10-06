# Spec: Security Intelligence — Core

**Jira:** DEVOS-046 / DEVOS-047  
**Epic:** DEVOS-E07  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement security vulnerability scanning and the security findings UI.

## Domain Models

```kotlin
data class SecurityFinding(
    val id: String,
    val repoId: String,
    val severity: Severity,       // CRITICAL, HIGH, MEDIUM, LOW, INFO
    val category: SecurityCategory,
    val title: String,
    val description: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val codeSnippet: String,
    val cweId: String?,           // CWE-xxx
    val cvssScore: Float?,
    val owaspCategory: String?,   // e.g., "A03:2021 – Injection"
    val recommendation: String,
    val aiExplanation: String?,
    val status: FindingStatus,    // OPEN, FIXED, DISMISSED
    val detectedAt: Long,
)

enum class SecurityCategory {
    INJECTION, BROKEN_AUTH, SENSITIVE_DATA, XXE, BROKEN_ACCESS,
    SECURITY_MISCONFIGURATION, XSS, INSECURE_DESERIALIZATION,
    VULNERABLE_COMPONENTS, LOGGING_MONITORING, HARDCODED_SECRET,
    PATH_TRAVERSAL, CRYPTO_WEAKNESS, OTHER
}

data class SecuritySummary(
    val critical: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
    val info: Int,
    val lastScanAt: Long?,
    val scanStatus: ScanStatus,
)
```

## Scanner Architecture

```kotlin
interface SecurityScanner {
    fun scan(repoId: String): Flow<ScanProgress>
}

class CompositeSecurityScanner @Inject constructor(
    private val staticAnalysisScanner: StaticAnalysisScanner,   // SAST rules
    private val secretScanner: SecretScanner,                    // Hardcoded secrets
    private val dependencyScanner: DependencyScanner,            // CVE check
) : SecurityScanner {
    override fun scan(repoId: String): Flow<ScanProgress> = merge(
        staticAnalysisScanner.scan(repoId),
        secretScanner.scan(repoId),
        dependencyScanner.scan(repoId),
    )
}
```

## SAST Rules (initial set)
- Hardcoded API keys / passwords (regex patterns)
- SQL injection (string concatenation in queries)
- Path traversal (unvalidated file paths)
- Insecure random (Math.random() for security context)
- Weak cryptography (MD5, SHA1 for password hashing)
- Exported components without permission (Android)
- World-readable files (Android)
- Cleartext network traffic (Android)

## Screen: SecurityFindingsScreen (FIGMA-26)

**States:** Loading | Scanning | Success | Empty (no findings) | Error

**Layout:**
```
Summary bar: CRITICAL(n) | HIGH(n) | MEDIUM(n) | LOW(n)
Rescan button (if stale)

FindingCard:
  [SEVERITY BADGE] Title
  category • file.kt:42
  AI explanation (collapsed, tap to expand)
  Actions: Mark Fixed | Dismiss | Ask AI
```

**Filters:** Severity | Category | Status | File

## AI Explanation
For each finding, generate an AI explanation:
```
"This hardcoded API key on line 42 of ApiClient.kt will be visible to anyone 
who decompiles the APK. Use Android Keystore or a secrets management solution instead."
```

## Acceptance Criteria
- [ ] Scanner produces findings for a repository with known vulnerabilities
- [ ] Summary counts correct by severity
- [ ] AI explanation present on each finding
- [ ] Mark Fixed persists status change
- [ ] Dismiss hides finding with undo
- [ ] Ask AI navigates to chat with finding context
- [ ] Scan progress visible during scanning
- [ ] No false positives for standard Android patterns
- [ ] Empty state when no findings
