package com.saikalyan.jobmate.domain.model

/** Room-derived dashboard values; missing status keys represent zero jobs. */
data class JobDashboardSummary(
    val statusCounts: Map<JobStatus, Int> = emptyMap(),
    val recentJobs: List<Job> = emptyList(),
) {
    val totalJobs: Int
        get() = statusCounts.values.sum()

    fun count(status: JobStatus): Int = statusCounts[status] ?: 0
}