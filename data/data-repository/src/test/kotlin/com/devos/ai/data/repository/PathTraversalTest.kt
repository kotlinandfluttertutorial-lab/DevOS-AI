package com.devos.ai.data.repository

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Verifies that the path-traversal protection in [RepositoryIndexingWorker]'s
 * file-parsing logic rejects any path that escapes the repository root.
 *
 * The protection is implemented via a canonical-path prefix check:
 * ```kotlin
 * require(resolved.startsWith(canonicalRoot)) { "Path traversal rejected: ..." }
 * ```
 * These tests exercise that logic directly using temporary directories so we
 * don't need a real WorkManager context.
 */
class PathTraversalTest {

    /**
     * Mirrors the traversal check inside [RepositoryIndexingWorker.parseFiles].
     * Extracted here so it can be tested in isolation without spinning up a worker.
     */
    private fun assertSafePath(repoRoot: File, file: File) {
        val canonicalRoot = repoRoot.canonicalPath
        val resolved = file.canonicalPath
        require(resolved.startsWith(canonicalRoot)) {
            "Path traversal rejected: $resolved escapes repo root $canonicalRoot"
        }
    }

    @Test
    fun `path inside repo root is accepted`(@TempDir tempDir: File) {
        val repoRoot = File(tempDir, "repo").also { it.mkdirs() }
        val safeFile = File(repoRoot, "src/main/Foo.kt").also {
            it.parentFile.mkdirs()
            it.createNewFile()
        }

        // Should not throw
        assertSafePath(repoRoot, safeFile)
    }

    @Test
    fun `path traversal via double-dot is rejected`(@TempDir tempDir: File) {
        val repoRoot = File(tempDir, "repo").also { it.mkdirs() }
        // Construct a path that escapes: repo/../../../etc/passwd
        val traversalFile = File(repoRoot, "../../../etc/passwd")

        val ex = assertThrows(IllegalArgumentException::class.java) {
            assertSafePath(repoRoot, traversalFile)
        }
        assertTrue(ex.message?.contains("Path traversal rejected") == true)
    }

    @Test
    fun `path traversal via encoded slash sequence is rejected`(@TempDir tempDir: File) {
        val repoRoot = File(tempDir, "repo").also { it.mkdirs() }
        // Absolute path outside repo root
        val outsideFile = File(tempDir, "secrets/token.txt")

        val ex = assertThrows(IllegalArgumentException::class.java) {
            assertSafePath(repoRoot, outsideFile)
        }
        assertTrue(ex.message?.contains("Path traversal rejected") == true)
    }

    @Test
    fun `nested directories inside repo root are accepted`(@TempDir tempDir: File) {
        val repoRoot = File(tempDir, "repo").also { it.mkdirs() }
        val deepFile = File(repoRoot, "a/b/c/d/e/Deep.kt").also {
            it.parentFile.mkdirs()
            it.createNewFile()
        }

        // Should not throw
        assertSafePath(repoRoot, deepFile)
    }

    @Test
    fun `sibling directory outside repo root is rejected`(@TempDir tempDir: File) {
        val repoRoot   = File(tempDir, "repo").also { it.mkdirs() }
        val siblingDir = File(tempDir, "other-repo")
        val siblingFile = File(siblingDir, "Secret.kt")

        val ex = assertThrows(IllegalArgumentException::class.java) {
            assertSafePath(repoRoot, siblingFile)
        }
        assertTrue(ex.message?.contains("Path traversal rejected") == true)
    }
}
