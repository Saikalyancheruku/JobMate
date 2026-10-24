package com.saikalyan.jobmate.data.repository

import com.saikalyan.jobmate.data.local.dao.EmailTemplateDao
import com.saikalyan.jobmate.data.local.entity.EmailTemplateEntity
import com.saikalyan.jobmate.di.IoDispatcher
import com.saikalyan.jobmate.domain.model.EmailTemplate
import com.saikalyan.jobmate.domain.repository.EmailTemplateRepository
import com.saikalyan.jobmate.domain.repository.RepositoryError
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class EmailTemplateRepositoryImpl @Inject constructor(
    private val dao: EmailTemplateDao,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : EmailTemplateRepository {
    override fun observeTemplates(): Flow<RepositoryResult<List<EmailTemplate>>> = dao.observeAll()
        .map { entities ->
            seedBuiltIns()
            RepositoryResult.Success(entities.map(EmailTemplateEntity::toDomain))
        }
        .catch { cause ->
            if (cause is CancellationException) throw cause
            emit(RepositoryResult.Failure(RepositoryError.Storage))
        }.flowOn(ioDispatcher)

    override suspend fun save(template: EmailTemplate): RepositoryResult<EmailTemplate> = try {
        if (template.name.isBlank() || template.subjectTemplate.isBlank() || template.bodyTemplate.isBlank()) {
            return RepositoryResult.Failure(RepositoryError.InvalidInput("template", "required_field_blank"))
        }
        val entity = template.copy(name = template.name.trim(), updatedAt = clock.instant()).toEntity()
        dao.upsert(entity)
        RepositoryResult.Success(entity.toDomain())
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { RepositoryResult.Failure(RepositoryError.Storage) }

    override suspend fun delete(id: String): RepositoryResult<Unit> = try {
        if (id in BUILT_IN_IDS) return RepositoryResult.Failure(RepositoryError.InvalidInput("template", "built_in_protected"))
        if (dao.deleteById(id) == 0) RepositoryResult.Failure(RepositoryError.NotFound) else RepositoryResult.Success(Unit)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { RepositoryResult.Failure(RepositoryError.Storage) }

    private suspend fun seedBuiltIns() {
        val now = clock.instant()
        BUILT_INS.forEach { template ->
            if (dao.findById(template.id) == null) dao.upsert(template.copy(updatedAt = now).toEntity())
        }
    }

    private fun EmailTemplateEntity.toDomain() = EmailTemplate(id, name, subjectTemplate, bodyTemplate, type == "BUILT_IN", updatedAt)
    private fun EmailTemplate.toEntity() = EmailTemplateEntity(id, name, subjectTemplate, bodyTemplate, if (isBuiltIn) "BUILT_IN" else "CUSTOM", updatedAt)

    private companion object {
        val BUILT_IN_IDS = setOf("template-referral-v1", "template-open-v1")
        val BUILT_INS = listOf(
            EmailTemplate("template-referral-v1", "Referral Request", "Referral request: {{jobTitle}}", "Hi {{firstName}},\n\nI noticed the {{jobTitle}} opportunity at {{companyName}}. Would you be open to referring me?\n\nJob: {{jobUrl}}\n\nThank you,\n{{senderName}}", true, java.time.Instant.EPOCH),
            EmailTemplate("template-open-v1", "Open for Opportunities", "Open to opportunities at {{companyName}}", "Hi {{firstName}},\n\nI am exploring opportunities at {{companyName}} and would appreciate any advice about the {{jobTitle}} role.\n\nJob: {{jobUrl}}\n\nThank you,\n{{senderName}}", true, java.time.Instant.EPOCH),
        )
    }
}
