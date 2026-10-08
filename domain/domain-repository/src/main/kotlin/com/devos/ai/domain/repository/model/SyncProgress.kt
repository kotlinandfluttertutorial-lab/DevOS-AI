package com.devos.ai.domain.repository.model

/**
 * Snapshot of an in-flight repository sync operation.
 *
 * Emitted by [com.devos.ai.domain.repository.RepositoryRepository.observeSyncProgress]
 * as a Flow so the UI can update a progress indicator in real time.
 *
 * Pure Kotlin — zero Android imports.
 */
data class SyncProgress(
    val repoId: String,
    val currentStep: SyncStep,
    val completedSteps: List<SyncStep> = emptyList(),
    val errorMessage: String? = null,
)

/**
 * Ordered steps that make up a full repository sync.
 *
 * [label] is the human-readable string shown in the progress UI.
 */
enum class SyncStep(val label: String) {
    CLONE("Clone repository"),
    PARSE("Parse files"),
    INDEX_SYMBOLS("Index symbols"),
    BUILD_VECTORS("Build vector store"),
    DONE("Complete"),
}
