package com.saikalyan.jobmate.data.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiApiKeyStore @Inject constructor(@ApplicationContext context: Context) {
    private val store = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile("ai_provider_credentials") },
    )

    val provider: Flow<String?> = store.data.map { it[PROVIDER] }

    suspend fun saveProfile(profile: String) = withContext(Dispatchers.IO) {
        require(profile.length <= 8_000) { "Profile must be 8,000 characters or fewer." }
        if (profile.isBlank()) {
            store.edit { it.remove(ENCRYPTED_PROFILE) }
            return@withContext
        }
        val clear = profile.trim().toByteArray(Charsets.UTF_8)
        val payload = try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
            cipher.iv + cipher.doFinal(clear)
        } finally { clear.fill(0) }
        try {
            store.edit { it[ENCRYPTED_PROFILE] = Base64.encodeToString(payload, Base64.NO_WRAP) }
        } finally { payload.fill(0) }
    }

    suspend fun readProfile(): String? = withContext(Dispatchers.IO) {
        val encoded = store.data.first()[ENCRYPTED_PROFILE] ?: return@withContext null
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > IV_SIZE) { "Saved profile data is incomplete." }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload, 0, IV_SIZE))
            val clear = cipher.doFinal(payload, IV_SIZE, payload.size - IV_SIZE)
            try { clear.toString(Charsets.UTF_8) } finally { clear.fill(0) }
        } finally { payload.fill(0) }
    }

    suspend fun save(provider: String, apiKey: String) = withContext(Dispatchers.IO) {
        require(provider in SUPPORTED_PROVIDERS) { "Choose a supported AI provider." }
        require(apiKey.isNotBlank()) { "API key is required." }
        val clear = apiKey.trim().toByteArray(Charsets.UTF_8)
        val payload = try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
            cipher.iv + cipher.doFinal(clear)
        } finally { clear.fill(0) }
        try {
            store.edit { prefs ->
                prefs[PROVIDER] = provider
                prefs[ENCRYPTED_KEY] = Base64.encodeToString(payload, Base64.NO_WRAP)
            }
        } finally { payload.fill(0) }
    }

    suspend fun readKey(): String? = withContext(Dispatchers.IO) {
        val encoded = store.data.first()[ENCRYPTED_KEY] ?: return@withContext null
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > IV_SIZE) { "Saved API key data is incomplete." }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload, 0, IV_SIZE))
            val clear = cipher.doFinal(payload, IV_SIZE, payload.size - IV_SIZE)
            try { clear.toString(Charsets.UTF_8) } finally { clear.fill(0) }
        } finally { payload.fill(0) }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        store.edit { it.remove(PROVIDER); it.remove(ENCRYPTED_KEY) }
        // Keep the AES key because the separately stored profile uses the same key alias.
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ALIAS = "jobmate.ai.api-key.aes-gcm.v1"
        const val IV_SIZE = 12
        val PROVIDER = stringPreferencesKey("ai_provider")
        val ENCRYPTED_KEY = stringPreferencesKey("encrypted_ai_api_key")
        val ENCRYPTED_PROFILE = stringPreferencesKey("encrypted_ai_profile")
        val SUPPORTED_PROVIDERS = setOf("Gemini")
    }
}
