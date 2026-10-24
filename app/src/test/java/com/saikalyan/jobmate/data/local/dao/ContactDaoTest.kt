package com.saikalyan.jobmate.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.saikalyan.jobmate.data.local.JobMateDatabase
import com.saikalyan.jobmate.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContactDaoTest {
    private lateinit var database: JobMateDatabase
    private val dao get() = database.contactDao()
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
    fun transactionalImportIgnoresDuplicateNormalizedEmails() = runBlocking {
        val results = dao.importAtomically(
            listOf(
                contact("contact-1", "Asha Rao", "asha@example.com"),
                contact("contact-2", "Duplicate", "asha@example.com"),
                contact("contact-3", "Ben Lee", "ben@example.com"),
            ),
        )

        assertEquals(2, results.count { it != -1L })
        assertEquals(2, dao.observeAll().first().size)
        assertEquals("contact-1", dao.findByNormalizedEmail("asha@example.com")?.id)
        assertTrue(dao.hasEmail("ben@example.com"))
    }

    @Test
    fun searchAndDomainObservationReturnMatchingContacts() = runBlocking {
        dao.upsert(contact("contact-1", "Asha Rao", "asha@example.com", domain = "example.com"))
        dao.upsert(contact("contact-2", "Ben Lee", "ben@other.com", domain = "other.com"))

        assertEquals(listOf("contact-1"), dao.search("asha").first().map(ContactEntity::id))
        assertEquals(listOf("contact-1"), dao.observeByDomain("example.com").first().map(ContactEntity::id))
    }

    private fun contact(
        id: String,
        name: String,
        email: String,
        domain: String = email.substringAfter('@'),
    ) = ContactEntity(id, name, email, null, domain, null, null, now, now)
}
