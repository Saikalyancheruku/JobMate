package com.saikalyan.jobmate.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JobStatusTest {
    @Test
    fun storageValuesAreStableAndUnknownValuesAreSafe() {
        JobStatus.entries.forEach { status ->
            assertEquals(status, JobStatus.fromStorage(status.storageValue))
        }
        assertNull(JobStatus.fromStorage("FUTURE_STATUS"))
        assertNull(JobStatus.fromStorage(null))
    }
}