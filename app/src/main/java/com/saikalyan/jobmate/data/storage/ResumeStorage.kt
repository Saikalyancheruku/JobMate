package com.saikalyan.jobmate.data.storage

import android.content.Context
import android.net.Uri
import com.saikalyan.jobmate.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

private const val MAX_RESUME_BYTES = 10L * 1024L * 1024L

data class StoredResume(
    val relativePath: String,
    val sizeBytes: Long,
    val sha256: String,
    val originalFileName: String,
)

class ResumeStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Owns PDF bytes inside filesDir/resumes; Room stores metadata only. */
class ResumeStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun import(uri: Uri, originalFileName: String): StoredResume = withContext(ioDispatcher) {
        val directory = resumeDirectory().apply { if (!exists() && !mkdirs()) throw ResumeStorageException("Could not create private resume storage.") }
        val temporary = File.createTempFile("resume-", ".part", directory)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw ResumeStorageException("The selected PDF could not be opened.")
            val digest = MessageDigest.getInstance("SHA-256")
            val size = input.use { source -> copyAndValidate(source, temporary, digest) }
            val id = UUID.randomUUID().toString()
            val destination = File(directory, "$id.pdf")
            if (!temporary.renameTo(destination)) throw ResumeStorageException("Could not store the selected PDF privately.")
            StoredResume("resumes/$id.pdf", size, digest.digest().toHex(), originalFileName)
        } catch (failure: ResumeStorageException) {
            throw failure
        } catch (failure: Exception) {
            throw ResumeStorageException("Could not import the selected PDF.", failure)
        } finally {
            temporary.delete()
        }
    }

    suspend fun file(relativePath: String): File = withContext(ioDispatcher) {
        val name = relativePath.removePrefix("resumes/")
        if (!relativePath.startsWith("resumes/") || name.contains('/') || !name.endsWith(".pdf")) {
            throw ResumeStorageException("Invalid private resume reference.")
        }
        val directory = resumeDirectory().canonicalFile
        val result = File(directory, name).canonicalFile
        if (result.parentFile != directory) throw ResumeStorageException("Invalid private resume reference.")
        if (!result.isFile) throw ResumeStorageException("The resume PDF is missing from private storage.")
        result
    }

    suspend fun delete(relativePath: String): Boolean = withContext(ioDispatcher) {
        val name = relativePath.removePrefix("resumes/")
        if (!relativePath.startsWith("resumes/") || name.contains('/') || !name.endsWith(".pdf")) return@withContext false
        val file = File(resumeDirectory(), name)
        !file.exists() || file.delete()
    }

    private fun copyAndValidate(input: InputStream, destination: File, digest: MessageDigest): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        var header = ByteArray(5)
        var headerSize = 0
        destination.outputStream().buffered().use { output ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (headerSize < header.size) {
                    val copy = minOf(header.size - headerSize, count)
                    buffer.copyInto(header, headerSize, 0, copy)
                    headerSize += copy
                }
                total += count
                if (total > MAX_RESUME_BYTES) throw ResumeStorageException("PDF files are limited to 10 MB.")
                digest.update(buffer, 0, count)
                output.write(buffer, 0, count)
            }
        }
        if (total == 0L || headerSize < 5 || !header.toString(Charsets.US_ASCII).startsWith("%PDF-")) {
            throw ResumeStorageException("The selected file is not a valid PDF.")
        }
        return total
    }

    private fun resumeDirectory() = File(context.filesDir, "resumes")

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
