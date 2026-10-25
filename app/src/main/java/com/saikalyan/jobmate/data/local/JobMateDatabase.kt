package com.saikalyan.jobmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.saikalyan.jobmate.data.local.converters.AppTypeConverters
import com.saikalyan.jobmate.data.local.dao.ContactDao
import com.saikalyan.jobmate.data.local.dao.EmailAttemptDao
import com.saikalyan.jobmate.data.local.dao.EmailTemplateDao
import com.saikalyan.jobmate.data.local.dao.JobContactDao
import com.saikalyan.jobmate.data.local.dao.JobDao
import com.saikalyan.jobmate.data.local.dao.OutreachDao
import com.saikalyan.jobmate.data.local.dao.ResumeDao
import com.saikalyan.jobmate.data.local.dao.SyncConflictDao
import com.saikalyan.jobmate.data.local.dao.SyncQueueDao
import com.saikalyan.jobmate.data.local.entity.ContactEntity
import com.saikalyan.jobmate.data.local.entity.EmailAttemptEntity
import com.saikalyan.jobmate.data.local.entity.EmailTemplateEntity
import com.saikalyan.jobmate.data.local.entity.JobContactEntity
import com.saikalyan.jobmate.data.local.entity.JobEntity
import com.saikalyan.jobmate.data.local.entity.OutreachEntity
import com.saikalyan.jobmate.data.local.entity.ResumeEntity
import com.saikalyan.jobmate.data.local.entity.SyncConflictEntity
import com.saikalyan.jobmate.data.local.entity.SyncQueueEntity

/** Initial schema version; register an explicit Migration before any future version increment. */
@Database(
    entities = [
        JobEntity::class,
        ContactEntity::class,
        JobContactEntity::class,
        ResumeEntity::class,
        EmailTemplateEntity::class,
        OutreachEntity::class,
        EmailAttemptEntity::class,
        SyncQueueEntity::class,
        SyncConflictEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(AppTypeConverters::class)
abstract class JobMateDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun contactDao(): ContactDao
    abstract fun jobContactDao(): JobContactDao
    abstract fun resumeDao(): ResumeDao
    abstract fun emailTemplateDao(): EmailTemplateDao
    abstract fun outreachDao(): OutreachDao
    abstract fun emailAttemptDao(): EmailAttemptDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncConflictDao(): SyncConflictDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE jobs ADD COLUMN workArrangement TEXT")
                database.execSQL("ALTER TABLE jobs ADD COLUMN mandatorySkills TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE jobs ADD COLUMN preferredSkills TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE jobs ADD COLUMN companyRating TEXT")
                database.execSQL("ALTER TABLE jobs ADD COLUMN companyReviewCount TEXT")
                database.execSQL("ALTER TABLE jobs ADD COLUMN applicantCount INTEGER")
                database.execSQL("ALTER TABLE jobs ADD COLUMN postedText TEXT")
                database.execSQL("ALTER TABLE jobs ADD COLUMN vacancyCount INTEGER")
                database.execSQL("ALTER TABLE jobs ADD COLUMN experienceLevel TEXT")
                database.execSQL("ALTER TABLE jobs ADD COLUMN education TEXT")
                database.execSQL("ALTER TABLE jobs ADD COLUMN rawCapturedText TEXT")
                database.execSQL("ALTER TABLE jobs ADD COLUMN capturedAt INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE jobs ADD COLUMN aiCorrected INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE outreach ADD COLUMN messageKind TEXT NOT NULL DEFAULT 'INITIAL'")
                database.execSQL("ALTER TABLE outreach ADD COLUMN parentOutreachId TEXT REFERENCES outreach(id) ON DELETE SET NULL")
                database.execSQL("ALTER TABLE outreach ADD COLUMN nextFollowUpAt INTEGER")
                database.execSQL("ALTER TABLE outreach ADD COLUMN followUpCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_outreach_nextFollowUpAt ON outreach(nextFollowUpAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_outreach_parentOutreachId ON outreach(parentOutreachId)")
            }
        }
    }
}
