package com.saikalyan.jobmate.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.saikalyan.jobmate.data.local.JobMateDatabase
import com.saikalyan.jobmate.data.local.entity.ContactEntity
import com.saikalyan.jobmate.data.local.entity.EmailAttemptEntity
import com.saikalyan.jobmate.data.local.entity.EmailTemplateEntity
import com.saikalyan.jobmate.data.local.entity.JobContactEntity
import com.saikalyan.jobmate.data.local.entity.JobEntity
import com.saikalyan.jobmate.data.local.entity.OutreachEntity
import com.saikalyan.jobmate.data.local.entity.ResumeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseRelationsTest {
    private lateinit var database: JobMateDatabase
    private val now = Instant.parse("2026-01-01T00:00:00Z")

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
    fun initialDatabaseCreatesAllTablesAtCurrentVersion() {
        val sql = database.openHelper.writableDatabase
        val cursor = sql.query("SELECT name FROM sqlite_master WHERE type = 'table'")
        val tables = buildSet {
            cursor.use { while (it.moveToNext()) add(it.getString(0)) }
        }

        assertEquals(2, sql.version)
        assertTrue(
            tables.containsAll(
                setOf(
                    "jobs", "contacts", "job_contacts", "resumes", "email_templates",
                    "outreach", "email_attempts", "sync_queue", "sync_conflicts",
                ),
            ),
        )
    }

    @Test
    fun deletingReferencedRecordsKeepsOutreachSnapshotsAndAttempts() = runBlocking {
        val job = job()
        val contact = contact()
        val template = EmailTemplateEntity("template-1", "Intro", "Subject", "Body", "GENERAL", now)
        val resume = ResumeEntity("resume-1", "Resume", "resume.pdf", "resumes/resume.pdf", "application/pdf", 1024, "sha256", now)
        database.jobDao().upsert(job)
        database.contactDao().upsert(contact)
        database.emailTemplateDao().upsert(template)
        database.resumeDao().upsert(resume)
        database.jobContactDao().upsert(JobContactEntity(job.id, contact.id, "EMAIL_DOMAIN", 0.9, true, now))
        database.outreachDao().upsert(
            OutreachEntity(
                id = "outreach-1",
                jobId = job.id,
                contactId = contact.id,
                templateId = template.id,
                resumeId = resume.id,
                recipientNameSnapshot = contact.fullName,
                recipientEmailSnapshot = contact.normalizedEmail,
                jobTitleSnapshot = job.title,
                companyNameSnapshot = job.companyName,
                templateNameSnapshot = template.name,
                resumeDisplayNameSnapshot = resume.displayName,
                renderedSubject = "Hello",
                renderedBody = "Message body",
                status = "SENT",
                sentAt = now,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.emailAttemptDao().upsert(
            EmailAttemptEntity("attempt-1", "outreach-1", 1, now, now, "SENT", null, "message-1", false),
        )

        database.jobDao().deleteById(job.id)
        database.contactDao().deleteById(contact.id)
        database.emailTemplateDao().deleteById(template.id)
        database.resumeDao().deleteById(resume.id)

        val outreach = database.outreachDao().findById("outreach-1")
        assertNotNull(outreach)
        assertNull(outreach?.jobId)
        assertNull(outreach?.contactId)
        assertNull(outreach?.templateId)
        assertNull(outreach?.resumeId)
        assertEquals("Asha Rao", outreach?.recipientNameSnapshot)
        assertEquals("asha@example.com", outreach?.recipientEmailSnapshot)
        assertEquals("Android Engineer", outreach?.jobTitleSnapshot)
        assertEquals("Northwind", outreach?.companyNameSnapshot)
        assertEquals("Intro", outreach?.templateNameSnapshot)
        assertEquals("Resume", outreach?.resumeDisplayNameSnapshot)
        assertTrue(database.jobContactDao().observeForJob(job.id).first().isEmpty())
        assertEquals(1, database.emailAttemptDao().observeForOutreach("outreach-1").first().size)

        database.outreachDao().deleteById("outreach-1")
        assertTrue(database.emailAttemptDao().observeForOutreach("outreach-1").first().isEmpty())
    }

    private fun job() = JobEntity(
        "job-1", "Android Engineer", "Northwind", null, null, emptyList(), null, null,
        null, null, null, "manual", null, "SAVED", null, null, null, null, "fp-1", now, now,
    )

    private fun contact() = ContactEntity(
        "contact-1", "Asha Rao", "asha@example.com", "Northwind", "example.com", "Recruiter", null, now, now,
    )
}
