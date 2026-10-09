package com.devos.ai.data.ai.agent.tools

import com.devos.ai.core.database.dao.SymbolDao
import com.devos.ai.data.ai.agent.AgentToolExecutor
import com.devos.ai.domain.ai.model.AgentTool
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

/**
 * Agent tool: search the symbol index for classes, functions, or properties.
 *
 * Argument schema: `{ "query": "<search term>", "kind": "<CLASS|FUNCTION|PROPERTY|...>?" }`
 */
class SearchSymbolsTool @Inject constructor(
    private val symbolDao: SymbolDao,
) : AgentToolExecutor {

    override val tool = AgentTool(
        name             = "search_symbols",
        description      = "Search the code symbol index for classes, functions, interfaces, " +
            "properties, or objects by name. Supports substring matching. " +
            "Optionally filter by kind: CLASS, FUNCTION, INTERFACE, PROPERTY, OBJECT.",
        parametersSchema = """{"type":"object","properties":{"query":{"type":"string"},"kind":{"type":"string"}},"required":["query"]}""",
    )

    override suspend fun execute(argsJson: String, repoId: String?): String {
        if (repoId == null) return "Error: no repository context."

        return try {
            val args  = JSONObject(argsJson)
            val query = args.getString("query")
            val kind  = args.optString("kind", "")

            val results = if (kind.isBlank()) {
                symbolDao.searchByName(query, repoId).first()
            } else {
                symbolDao.searchByNameAndKind(query, kind, repoId).first()
            }

            if (results.isEmpty()) return "No symbols found matching '$query'."

            buildString {
                appendLine("Found ${results.size} symbol(s) matching '$query':")
                results.take(20).forEach { sym ->
                    appendLine("  • ${sym.kind} ${sym.name} — ${sym.filePath}:${sym.lineStart}")
                    if (sym.signature != null) appendLine("    ${sym.signature}")
                    if (sym.docComment != null) appendLine("    // ${sym.docComment.take(80)}")
                }
                if (results.size > 20) appendLine("  … and ${results.size - 20} more")
            }
        } catch (e: Exception) {
            Timber.w(e, "search_symbols failed")
            "Error searching symbols: ${e.message}"
        }
    }
}
