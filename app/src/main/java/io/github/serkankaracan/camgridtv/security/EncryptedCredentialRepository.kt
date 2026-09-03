package io.github.serkankaracan.camgridtv.security

interface EncryptedCredentialRepository {
    suspend fun get(secretId: String): EncryptedCredential?

    suspend fun put(secretId: String, encryptedCredential: EncryptedCredential)

    suspend fun remove(secretId: String)

    suspend fun clear()
}

class InMemoryEncryptedCredentialRepository : EncryptedCredentialRepository {
    private val values = linkedMapOf<String, EncryptedCredential>()
    private val lock = Any()

    override suspend fun get(secretId: String): EncryptedCredential? =
        synchronized(lock) { values[secretId] }

    override suspend fun put(secretId: String, encryptedCredential: EncryptedCredential) {
        validateSecretId(secretId)
        synchronized(lock) { values[secretId] = encryptedCredential }
    }

    override suspend fun remove(secretId: String) {
        validateSecretId(secretId)
        synchronized(lock) { values.remove(secretId) }
    }

    override suspend fun clear() {
        synchronized(lock) { values.clear() }
    }
}

internal fun validateSecretId(secretId: String) {
    require(secretId.isNotBlank() && secretId.length <= 256) { "Credential secret id is invalid" }
}
