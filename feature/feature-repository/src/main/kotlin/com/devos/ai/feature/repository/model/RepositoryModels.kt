package com.devos.ai.feature.repository.model

/**
 * Stub preview model shown after URL validation.
 * Will be replaced by domain model once DEVOS-015 lands.
 */
data class RepositoryPreview(
    val name: String,
    val owner: String,
    val stars: Int,
    val fileCount: Int,
    val size: String,
    val branch: String,
    val isValid: Boolean,
)

/** Source provider for repository import. */
enum class RepositoryProvider {
    GITHUB,
    GITLAB,
    LOCAL,
}

/** State of a single sync step. */
enum class StepState {
    PENDING,
    IN_PROGRESS,
    COMPLETE,
    FAILED,
}

/**
 * A single step in the repository sync pipeline.
 *
 * @param name Human-readable step title
 * @param subtitle Status subtitle (e.g. "247 / 847 files indexed")
 * @param state Current state of this step
 */
data class SyncStep(
    val name: String,
    val subtitle: String,
    val state: StepState,
)
