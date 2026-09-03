package io.github.serkankaracan.camgridtv.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.serkankaracan.camgridtv.security.EncryptedCredential
import io.github.serkankaracan.camgridtv.security.EncryptedCredentialRepository
import io.github.serkankaracan.camgridtv.security.SecretRecoveryRequiredException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DataStoreEncryptedCredentialRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EncryptedCredentialRepository {
    override suspend fun get(secretId: String): EncryptedCredential? =
        withContext(ioDispatcher) {
            validateIdentifier(secretId)
            dataStore.data.first()[keyFor(secretId)]?.let(::decode)
        }

    override suspend fun put(secretId: String, encryptedCredential: EncryptedCredential) {
        withContext(ioDispatcher) {
            validateIdentifier(secretId)
            dataStore.edit { preferences ->
                preferences[keyFor(secretId)] = encode(encryptedCredential)
            }
        }
    }

    override suspend fun remove(secretId: String) {
        withContext(ioDispatcher) {
            validateIdentifier(secretId)
            dataStore.edit { preferences -> preferences.remove(keyFor(secretId)) }
        }
    }

    override suspend fun clear() {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences
                    .asMap()
                    .keys
                    .filter { it.name.startsWith(KEY_PREFIX) }
                    .forEach { key -> preferences.remove(key) }
            }
        }
    }

    private fun keyFor(secretId: String): Preferences.Key<String> {
        val digest =
            MessageDigest.getInstance("SHA-256")
                .digest(secretId.toByteArray(StandardCharsets.UTF_8))
                .joinToString(separator = "") { "%02x".format(it.toInt() and 0xFF) }
        return stringPreferencesKey(KEY_PREFIX + digest)
    }

    private fun encode(value: EncryptedCredential): String =
        listOf(
                value.schemaVersion.toString(),
                ENCODER.encodeToString(value.initializationVector),
                ENCODER.encodeToString(value.ciphertext),
            )
            .joinToString(":")

    private fun decode(value: String): EncryptedCredential {
        val parts = value.split(':', limit = 3)
        if (parts.size != 3) throw recoveryError()
        return try {
            EncryptedCredential(
                schemaVersion = parts[0].toInt(),
                initializationVector = DECODER.decode(parts[1]),
                ciphertext = DECODER.decode(parts[2]),
            )
        } catch (_: Exception) {
            throw recoveryError()
        }
    }

    private fun validateIdentifier(secretId: String) {
        require(secretId.isNotBlank() && secretId.length <= 256) {
            "Credential secret id is invalid"
        }
    }

    private fun recoveryError() =
        SecretRecoveryRequiredException("Stored camera credentials require recovery")

    private companion object {
        const val KEY_PREFIX = "encrypted_camera_credential_v1_"
        val ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        val DECODER: Base64.Decoder = Base64.getUrlDecoder()
    }
}
