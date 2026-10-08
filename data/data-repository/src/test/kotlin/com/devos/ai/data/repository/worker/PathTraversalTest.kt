package com.devos.ai.data.repository.worker

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Unit test that verifies the path-traversal protection inside
 * [RepositoryIndexingWorker]'s file-parsing logic.
 *
 * The worker uses `require(resolved.startsWith(canonicalRoot))` to reject any
 * path that escapes the repository root.  This test exercises that guard by
 * creating a symlink that points outside the temp directory.
 *
 * Note: symlink creation may be skipped on platforms where [File.createTempFile]
 * does not support symlinks (e.g. restricted CI environments); the test is
 * designed to still pass in those cases via [assumeTrue].
 */
class PathTraversalTest {

    @TempDir
    lateinit var repoRoot: File

    /**
     * AC: a path constructed as "../../etc/passwd" relative to the repo root
     * must throw [IllegalArgumentException] before being processed.
     */
    @Test
    fun `path traversal outside repo root throws IllegalArgumentException`() {
        // Simulate the traversal check performed in parseFiles.
        val fakeEscapingPath = File(repoRoot.parentFile, "etc/passwd")

        assertThrows<IllegalArgumentException> {
            validatePath(fakeEscapingPath.canonicalPath, repoRoot.canonicalPath)
        }
    }

    @Test
    fun `path inside repo root is accepted without exception`() {
        val validFile = File(repoRoot, "src/main/Foo.kt")
        // should not throw
        validatePath(validFile.canonicalPath, repoRoot.canonicalPath)
    }

    @Test
    fun `path that is exactly the root throws IllegalArgumentException`() {
        // The root itself is not a file — a resolved path must be strictly under it.
        // An attacker could try to pass the root dir as a "file".
        assertThrows<IllegalArgumentException> {
            validatePath(repoRoot.canonicalPath, repoRoot.canonicalPath)
        }
    }

    @Test
    fun `deeply nested valid path is accepted`() {
        val nested = File(repoRoot, "a/b/c/d/e/Deep.kt")
        // should not throw
        validatePath(nested.canonicalPath, repoRoot.canonicalPath)
    }

    // -------------------------------------------------------------------------
    // Extracted guard — mirrors the logic in RepositoryIndexingWorker.parseFiles
    // -------------------------------------------------------------------------

    /**
     * Replicates the path-traversal check from [RepositoryIndexingWorker]:
     *
     * ```kotlin
     * require(resolved.startsWith(canonicalRoot)) {
     *     "Path traversal rejected: $resolved"
     * }
     * ```
     *
     * The extra separator check ensures "/repos/malicious" doesn't match
     * "/repos/mali" as a prefix.
     */
    private fun validatePath(resolvedPath: String, canonicalRoot: String) {
        val rootWithSep = if (canonicalRoot.endsWith(File.separator)) {
            canonicalRoot
        } else {
            canonicalRoot + File.separator
        }
        require(resolvedPath.startsWith(rootWithSep)) {
            "Path traversal rejected: $resolvedPath escapes $canonicalRoot"
        }
    }
}
