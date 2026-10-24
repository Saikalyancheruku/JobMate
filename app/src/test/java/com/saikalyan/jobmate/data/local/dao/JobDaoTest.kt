package com.saikalyan.jobmate.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.saikalyan.jobmate.data.local.JobMateDatabase
import com.saikalyan.jobmate.data.local.entity.JobEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JobDaoTest {
    private lateinit var database: JobMateDatabase
    private val dao get() = database.jobDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JobMateDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observesEmptyAndThenSavedJobs() = runBlocking {
        assertTrue(dao.observeJobs(null).first().isEmpty())
        dao.upsert(job("job-1", "Android Engineer", "Northwind"))
        assertEquals(listOf("job-1"), dao.observeJobs(null).first().map(JobEntity::id))
    }

    @Test
    fun filtersStatusSearchesFieldsAndFindsFingerprint() = runBlocking {
        dao.saveAllAtomically(
            listOf(
                job("job-1", "Android Engineer", "Northwind", status = "SAVED", fingerprint = "fp-1"),
                job("job-2", "Kotlin Developer", "Contoso", status = "APPLIED", location = "Remote", fingerprint = "fp-2"),
            ),
        )

        assertEquals(listOf("job-2"), dao.observeJobs("APPLIED").first().map(JobEntity::id))
        assertEquals(listOf("job-2"), dao.search("Remote").map(JobEntity::id))
        assertEquals(listOf("job-1"), dao.search("northwind").map(JobEntity::id))
        assertEquals("job-2", dao.findByFingerprint("fp-2")?.id)
        assertEquals(mapOf("SAVED" to 1, "APPLIED" to 1), dao.observeStatusCounts().first().associate { it.status to it.count })
    }

    @Test
    fun statusUpdateAndDeleteReportAffectedRows() = runBlocking {
        dao.upsert(job("job-1", "Android Engineer", "Northwind"))
        val changed = dao.updateStatus("job-1", "CLOSED", Instant.parse("2026-01-02T00:00:00Z"))
        val missing = dao.updateStatus("missing", "CLOSED", Instant.parse("2026-01-02T00:00:00Z"))

        assertEquals(1, changed)
        assertEquals(0, missing)
        assertEquals("CLOSED", dao.findById("job-1")?.status)
        assertEquals(1, dao.deleteById("job-1"))
        assertEquals(0, dao.deleteById("job-1"))
        assertFalse(dao.observeJobs(null).first().any())
    }

    private fun job(
        id: String,
        title: String,
        companyName: String,
        status: String = "SAVED",
        location: String? = null,
        fingerprint: String = id,
    ) = JobEntity(
        id = id,
        title = title,
        companyName = companyName,
        location = location,
        description = null,
        skills = listOf("Kotlin", "Android"),
        experienceMin = null,
        experienceMax = null,
        salaryText = null,
        employmentType = null,
        postedAt = null,
        source = "manual",
        sourceUrl = null,
        status = status,
        appliedAt = null,
        followUpAt = null,
        notes = null,
        captureMethod = null,
        fingerprint = fingerprint,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
