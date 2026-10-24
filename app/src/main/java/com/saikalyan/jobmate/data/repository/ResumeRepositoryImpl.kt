package com.saikalyan.jobmate.data.repository

import android.net.Uri
import com.saikalyan.jobmate.data.local.dao.ResumeDao
import com.saikalyan.jobmate.data.local.entity.ResumeEntity
import com.saikalyan.jobmate.data.storage.ResumeStorage
import com.saikalyan.jobmate.data.storage.ResumeStorageException
import com.saikalyan.jobmate.di.IoDispatcher
import com.saikalyan.jobmate.domain.model.Resume
import com.saikalyan.jobmate.domain.repository.RepositoryError
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import com.saikalyan.jobmate.domain.repository.ResumeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class ResumeRepositoryImpl @Inject constructor(
    private val dao: ResumeDao,
    private val storage: ResumeStorage,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ResumeRepository {
    override fun observeResumes(): Flow<RepositoryResult<List<Resume>>> = dao.observeAll()
        .map { rows ->
            val result: RepositoryResult<List<Resume>> =
                RepositoryResult.Success(rows.map { it.toDomain() })
            result
        }
        .catch { cause ->
            if (cause is CancellationException) throw cause
            emit(RepositoryResult.Failure(RepositoryError.Storage))
        }.flowOn(ioDispatcher)

    override suspend fun importFromUri(uri: String, displayName: String): RepositoryResult<Resume> = try {
        val stored = storage.import(Uri.parse(uri), displayName)
        val entity = ResumeEntity(
            id = stored.relativePath.substringAfterLast('/').removeSuffix(".pdf"),
            displayName = displayName.trim().ifBlank { stored.originalFileName },
            originalFileName = stored.originalFileName,
            relativePath = stored.relativePath,
            mimeType = "application/pdf",
            sizeBytes = stored.sizeBytes,
            sha256 = stored.sha256,
            importedAt = clock.instant(),
        )
        try { dao.upsert(entity) } catch (failure: Exception) {
            storage.delete(stored.relativePath)
            throw failure
        }
        RepositoryResult.Success(entity.toDomain())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: ResumeStorageException) {
        RepositoryResult.Failure(RepositoryError.InvalidInput("resume", "invalid_pdf"))
    } catch (_: Exception) {
        RepositoryResult.Failure(RepositoryError.Storage)
    }

    override suspend fun get(id: String): RepositoryResult<Resume?> = try {
        RepositoryResult.Success(dao.findById(id)?.toDomain())
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { RepositoryResult.Failure(RepositoryError.Storage) }

    override suspend fun rename(id: String, displayName: String): RepositoryResult<Unit> = try {
        val existing = dao.findById(id) ?: return RepositoryResult.Failure(RepositoryError.NotFound)
        val name = displayName.trim()
        if (name.isBlank()) return RepositoryResult.Failure(RepositoryError.InvalidInput("displayName", "blank"))
        dao.upsert(existing.copy(displayName = name))
        RepositoryResult.Success(Unit)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { RepositoryResult.Failure(RepositoryError.Storage) }

    override suspend fun delete(id: String): RepositoryResult<Unit> = try {
        val existing = dao.findById(id) ?: return RepositoryResult.Failure(RepositoryError.NotFound)
        if (!storage.delete(existing.relativePath)) return RepositoryResult.Failure(RepositoryError.Storage)
        dao.deleteById(id)
        RepositoryResult.Success(Unit)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { RepositoryResult.Failure(RepositoryError.Storage) }

    private fun ResumeEntity.toDomain() = Resume(id, displayName, originalFileName, relativePath, mimeType, sizeBytes, sha256, importedAt)
}


