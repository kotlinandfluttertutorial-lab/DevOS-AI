package com.devos.ai.domain.ai.model

/**
 * The context scope for an AI query.
 *
 * Controls what gets injected into the AI prompt via RAG retrieval.
 * More specific contexts produce more targeted answers.
 */
sealed class AIContext {
    abstract val label: String

    /** Query across the entire indexed workspace. */
    data object Global : AIContext() {
        override val label = "Global workspace"
    }

    /** Query scoped to a specific project's repositories. */
    data class Project(
        val projectId: String,
        val name: String,
    ) : AIContext() {
        override val label = name
    }

    /** Query scoped to a specific repository. */
    data class Repository(
        val repoId: String,
        val name: String,
    ) : AIContext() {
        override val label = name
    }

    /** Query scoped to a single file. */
    data class File(
        val repoId: String,
        val path: String,
    ) : AIContext() {
        override val label = path.substringAfterLast('/')
    }

    /** Query scoped to a single symbol (class, function, etc). */
    data class Symbol(
        val repoId: String,
        val symbolId: String,
        val name: String,
    ) : AIContext() {
        override val label = name
    }
}
