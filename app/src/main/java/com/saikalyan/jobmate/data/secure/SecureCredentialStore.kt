package com.saikalyan.jobmate.data.secure

import kotlinx.coroutines.flow.Flow

data class SmtpCredential(val email: String, val appPassword: String)

interface SecureCredentialStore {
    val configuredEmail: Flow<String?>
    suspend fun save(email: String, appPassword: String)
    suspend fun read(): SmtpCredential?
    suspend fun clear()
}

class CredentialStoreException(message: String, cause: Throwable? = null) : Exception(message, cause)
