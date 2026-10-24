package com.saikalyan.jobmate.data.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.saikalyan.jobmate.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import java.util.Locale
import javax.inject.Singleton

@Singleton
class EncryptedAppPasswordStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SecureCredentialStore {
    private val mutex = Mutex()
    private val dataStore = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + ioDispatcher),
        produceFile = { context.preferencesDataStoreFile(STORE_NAME) },
    )

    override val configuredEmail: Flow<String?> = dataStore.data.map { it[EMAIL_KEY] }

    override suspend fun save(email: String, appPassword: String): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            val normalizedEmail = email.trim().lowercase(Locale.ROOT)
            val normalizedPassword = appPassword.filterNot(Char::isWhitespace)
            if (normalizedEmail.isBlank() || normalizedPassword.isBlank()) {
                throw CredentialStoreException("Gmail address and App Password are required.")
            }
            val clearBytes = normalizedPassword.toByteArray(Charsets.UTF_8)
            val encrypted = try { encrypt(clearBytes) } finally { clearBytes.fill(0) }
            try {
                dataStore.edit { preferences ->
                    preferences[EMAIL_KEY] = normalizedEmail
                    preferences[PASSWORD_KEY] = Base64.encodeToString(encrypted, Base64.NO_WRAP)
                }
            } finally {
                encrypted.fill(0)
            }
        }
    }

    override suspend fun read(): SmtpCredential? = withContext(ioDispatcher) {
        mutex.withLock {
            val preferences = dataStore.data.first()
            val email = preferences[EMAIL_KEY] ?: return@withLock null
            val payload = preferences[PASSWORD_KEY] ?: return@withLock null
            val encrypted = try { Base64.decode(payload, Base64.NO_WRAP) }
            catch (failure: IllegalArgumentException) { throw CredentialStoreException("Stored App Password data is invalid.", failure) }
            val clear = decrypt(encrypted)
            try { SmtpCredential(email, clear.toString(Charsets.UTF_8)) }
            finally { encrypted.fill(0); clear.fill(0) }
        }
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        mutex.withLock {
            dataStore.edit { it.remove(EMAIL_KEY); it.remove(PASSWORD_KEY) }
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    private fun encrypt(clear: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(clear)
        return cipher.iv + encrypted
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        if (payload.size <= IV_LENGTH) throw CredentialStoreException("Stored App Password data is incomplete.")
        try {
            val iv = payload.copyOfRange(0, IV_LENGTH)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            return cipher.doFinal(payload, IV_LENGTH, payload.size - IV_LENGTH)
        } catch (failure: Exception) {
            throw CredentialStoreException("Stored App Password cannot be decrypted. Reconnect Gmail.", failure)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val STORE_NAME = "smtp_credentials"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "jobmate.smtp.app-password.aes-gcm.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
        val EMAIL_KEY = stringPreferencesKey("gmail_address")
        val PASSWORD_KEY = stringPreferencesKey("encrypted_app_password")
    }
}

