package com.devos.ai.data.ai.agent.tools

import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.data.ai.agent.AgentToolExecutor
import com.devos.ai.domain.ai.model.AgentTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Agent tool: read a file from the cloned repository.
 *
 * ## Security — path traversal protection
 * The resolved canonical path is checked against the repository root canonical path
 * before reading. Any attempt to escape the root is rejected with an error observation.
 *
 * Argument schema: `{ "path": "<repo-relative path>", "start_line": <int>, "end_line": <int> }`
 * `start_line` and `end_line` are optional (defaults: 1 and EOF).
 */
class ReadFileTool @Inject constructor(
    private val fileDao: FileDao,
) : AgentToolExecutor {

    override val tool = AgentTool(
        name             = "read_file",
        description      = "Read the content of a file in the repository. " +
            "Use to examine source code, configs, or docs. " +
            "Provide a repo-relative path and optional line range.",
        parametersSchema = """{"type":"object","properties":{"path":{"type":"string"},"start_line":{"type":"integer"},"end_line":{"type":"integer"}},"required":["path"]}""",
    )

    override suspend fun execute(argsJson: String, repoId: String?): String {
        if (repoId == null) return "Error: no repository context provided."

        return try {
            val args      = JSONObject(argsJson)
            val relPath   = args.getString("path")
            val startLine = args.optInt("start_line", 1).coerceAtLeast(1)
            val endLine   = args.optInt("end_line", Int.MAX_VALUE)

            // Locate repo local path via a file row
            val localRoot = withContext(Dispatchers.IO) {
                fileDao.getByRepo(repoId).firstOrNull()?.path
                    ?.let { File(it).parentFile?.parentFile?.canonicalPath }
            } ?: return "Error: repository not indexed yet."

            val root     = File(localRoot)
            val target   = File(root, relPath)
            val canonical = target.canonicalPath

            // PATH TRAVERSAL PROTECTION
            if (!canonical.startsWith(root.canonicalPath)) {
                return "Error: path traversal rejected."
            }

            if (!target.exists()) return "Error: file not found: $relPath"

            val lines = withContext(Dispatchers.IO) { target.readLines() }
            val slice = lines
                .drop(startLine - 1)
                .take((endLine - startLine + 1).coerceAtLeast(1))

            buildString {
                appendLine("File: $relPath (lines $startLine–${minOf(endLine, lines.size)} of ${lines.size})")
                appendLine("```")
                slice.forEachIndexed { i, line ->
                    appendLine("${startLine + i}: $line")
                }
                append("```")
            }
        } catch (e: Exception) {
            Timber.w(e, "read_file failed")
            "Error reading file: ${e.message}"
        }
    }
}
