package com.saikalyan.jobmate.di

import android.content.Context
import androidx.room.Room
import com.saikalyan.jobmate.data.local.JobMateDatabase
import com.saikalyan.jobmate.data.local.dao.ContactDao
import com.saikalyan.jobmate.data.local.dao.EmailAttemptDao
import com.saikalyan.jobmate.data.local.dao.EmailTemplateDao
import com.saikalyan.jobmate.data.local.dao.JobContactDao
import com.saikalyan.jobmate.data.local.dao.JobDao
import com.saikalyan.jobmate.data.local.dao.OutreachDao
import com.saikalyan.jobmate.data.local.dao.ResumeDao
import com.saikalyan.jobmate.data.local.dao.SyncConflictDao
import com.saikalyan.jobmate.data.local.dao.SyncQueueDao
import com.saikalyan.jobmate.data.repository.ContactRepositoryImpl
import com.saikalyan.jobmate.data.repository.JobRepositoryImpl
import com.saikalyan.jobmate.data.repository.JobContactLinkRepositoryImpl
import com.saikalyan.jobmate.data.repository.ResumeRepositoryImpl
import com.saikalyan.jobmate.data.repository.EmailTemplateRepositoryImpl
import com.saikalyan.jobmate.data.repository.OutreachDraftRepositoryImpl
import com.saikalyan.jobmate.data.repository.OutreachSendRepositoryImpl
import com.saikalyan.jobmate.data.secure.EncryptedAppPasswordStore
import com.saikalyan.jobmate.data.secure.SecureCredentialStore
import com.saikalyan.jobmate.domain.repository.ContactRepository
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.JobContactLinkRepository
import com.saikalyan.jobmate.domain.repository.ResumeRepository
import com.saikalyan.jobmate.domain.repository.EmailTemplateRepository
import com.saikalyan.jobmate.domain.repository.OutreachDraftRepository
import com.saikalyan.jobmate.domain.repository.OutreachSendRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JobMateDatabase =
        // No destructive fallback: future schema changes must register explicit migrations.
        Room.databaseBuilder(context, JobMateDatabase::class.java, "jobmate.db")
            .addMigrations(JobMateDatabase.MIGRATION_1_2, JobMateDatabase.MIGRATION_2_3)
            .build()

    @Provides
    @Singleton
    fun provideJobDao(database: JobMateDatabase): JobDao = database.jobDao()

    @Provides
    @Singleton
    fun provideContactDao(database: JobMateDatabase): ContactDao = database.contactDao()

    @Provides
    @Singleton
    fun provideJobContactDao(database: JobMateDatabase): JobContactDao = database.jobContactDao()

    @Provides
    @Singleton
    fun provideResumeDao(database: JobMateDatabase): ResumeDao = database.resumeDao()

    @Provides
    @Singleton
    fun provideEmailTemplateDao(database: JobMateDatabase): EmailTemplateDao = database.emailTemplateDao()

    @Provides
    @Singleton
    fun provideOutreachDao(database: JobMateDatabase): OutreachDao = database.outreachDao()

    @Provides
    @Singleton
    fun provideEmailAttemptDao(database: JobMateDatabase): EmailAttemptDao = database.emailAttemptDao()

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: JobMateDatabase): SyncQueueDao = database.syncQueueDao()

    @Provides
    @Singleton
    fun provideSyncConflictDao(database: JobMateDatabase): SyncConflictDao = database.syncConflictDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds
    @Singleton
    abstract fun bindSecureCredentialStore(implementation: EncryptedAppPasswordStore): SecureCredentialStore
    @Binds
    @Singleton
    abstract fun bindJobRepository(implementation: JobRepositoryImpl): JobRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(implementation: ContactRepositoryImpl): ContactRepository

    @Binds
    @Singleton
    abstract fun bindJobContactLinkRepository(implementation: JobContactLinkRepositoryImpl): JobContactLinkRepository

    @Binds
    @Singleton
    abstract fun bindResumeRepository(implementation: ResumeRepositoryImpl): ResumeRepository

    @Binds
    @Singleton
    abstract fun bindEmailTemplateRepository(implementation: EmailTemplateRepositoryImpl): EmailTemplateRepository

    @Binds
    @Singleton
    abstract fun bindOutreachDraftRepository(implementation: OutreachDraftRepositoryImpl): OutreachDraftRepository

    @Binds
    @Singleton
    abstract fun bindOutreachSendRepository(implementation: OutreachSendRepositoryImpl): OutreachSendRepository
}





