package com.saikalyan.jobmate.domain.model

/** Job lifecycle values persisted as stable strings in the local database. */
enum class JobStatus(
    val storageValue: String,
    val displayLabel: String,
) {
    SAVED("SAVED", "Saved"),
    APPLIED("APPLIED", "Applied"),
    INTERVIEW("INTERVIEW", "Interview"),
    OFFER("OFFER", "Offer"),
    REJECTED("REJECTED", "Rejected"),
    WITHDRAWN("WITHDRAWN", "Withdrawn"),
    CLOSED("CLOSED", "Closed");

    companion object {
        /** Returns null for an unknown persisted value so callers can handle it without crashing. */
        fun fromStorage(value: String?): JobStatus? =
            entries.firstOrNull { it.storageValue == value }
    }
}