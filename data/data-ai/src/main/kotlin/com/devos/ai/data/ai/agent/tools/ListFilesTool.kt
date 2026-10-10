package com.devos.ai.data.ai.agent.tools

import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.data.ai.agent.AgentToolExecutor
import com.devos.ai.domain.ai.model.AgentTool
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

/**
 * Agent tool: list files in the repository, optionally filtered by extension or directory.
 *
 * Argument schema: `{ "directory": "<repo-relative dir>?", "extension": "<ext>?" }`
 */
class ListFilesTool @Inject constructor(
    private val fileDao: FileDao,
) : AgentToolExecutor {

    override val tool = AgentTool(
        name             = "list_files",
        description      = "List files in the repository. Optionally filter by directory prefix " +
            "or file extension (e.g. 'kt', 'java', 'xml'). " +
            "Use to explore the project structure before reading specific files.",
        parametersSchema = """{"type":"object","properties":{"directory":{"type":"string"},"extension":{"type":"string"}}}""",
    )

    override suspend fun execute(argsJson: String, repoId: String?): String {
        if (repoId == null) return "Error: no repository context."

        return try {
            val args      = JSONObject(argsJson)
            val directory = args.optString("directory", "").trimStart('/')
            val extension = args.optString("extension", "").trimStart('.')

            val allFiles = fileDao.getByRepo(repoId)

            val filtered = allFiles.filter { file ->
                (directory.isEmpty() || file.path.startsWith(directory)) &&
                    (extension.isEmpty() || file.extension.equals(extension, ignoreCase = true))
            }

            if (filtered.isEmpty()) return "No files found matching the criteria."

            buildString {
                appendLine("${filtered.size} file(s)${if (directory.isNotEmpty()) " in $directory" else ""}${if (extension.isNotEmpty()) " (.$extension)" else ""}:")
                // Group by top-level directory for readability
                val byDir = filtered.groupBy { it.path.substringBefore('/') }
                byDir.entries.sortedBy { it.key }.forEach { (dir, files) ->
                    appendLine("📁 $dir/ (${files.size} files)")
                    files.take(10).forEach { f -> appendLine("  ${f.path}") }
                    if (files.size > 10) appendLine("  … and ${files.size - 10} more")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "list_files failed")
            "Error listing files: ${e.message}"
        }
    }
}
