package com.devos.ai.feature.git

/**
 * Domain models for the Git History screen.
 *
 * DEVOS-040 / DA-53
 */

/** A single Git commit. */
data class GitCommit(
    val sha: String,
    val shortSha: String,
    val message: String,
    val author: String,
    val relativeTime: String,
    val additions: Int,
    val deletions: Int,
)

/** A date-grouped list of commits for the timeline UI. */
data class CommitGroup(
    val dateLabel: String,
    val commits: List<GitCommit>,
)
