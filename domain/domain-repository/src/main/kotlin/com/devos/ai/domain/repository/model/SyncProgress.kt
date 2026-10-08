package com.devos.ai.domain.repository.model

/**
 * Live progress snapshot for an active or recently-completed repository sync.
 *
 * Emitted by [RepositoryRepository.observeSyncProgress] as WorkManager progress
 * data changes.
 */
data class SyncProgress(
    val repoId: String,
    val currentStep: SyncStep,
    val completedSteps: List<SyncStep> = emptyList(),
    val errorMessage: String? = null,
)

/**
 * Ordered steps of the repository indexing pipeline.
 *
 * [label] is shown in the sync-progress UI.
 */
enum class SyncStep(val label: String) {
    CLONE("Clone repository"),
    PARSE("Parse files"),
    INDEX_SYMBOLS("Index symbols"),
    BUILD_VECTORS("Build vector store"),
    DONE("Complete"),
}
