package com.devos.ai.domain.repository.model

/** Lifecycle state of a repository's synchronisation job. */
enum class SyncStatus {
    /** No active or pending sync. */
    IDLE,

    /** A WorkManager clone/index job is currently running. */
    SYNCING,

    /** Last sync completed successfully. */
    SYNCED,

    /** Last sync failed — see the repository's error details. */
    ERROR,
}
