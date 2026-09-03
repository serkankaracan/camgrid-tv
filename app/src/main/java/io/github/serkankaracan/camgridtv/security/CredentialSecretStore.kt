package io.github.serkankaracan.camgridtv.security

interface CredentialSecretStore {
    suspend fun put(secretId: String, secret: CredentialSecret)

    /** The caller owns the returned value and must close it after use. */
    suspend fun get(secretId: String): CredentialSecret?

    suspend fun remove(secretId: String)

    suspend fun clear()
}

class EncryptedCredentialSecretStore(
    private val cipher: CredentialCipher,
    private val repository: EncryptedCredentialRepository,
) : CredentialSecretStore {
    override suspend fun put(secretId: String, secret: CredentialSecret) {
        validateSecretId(secretId)
        repository.put(secretId, cipher.encrypt(secret))
    }

    override suspend fun get(secretId: String): CredentialSecret? {
        validateSecretId(secretId)
        return repository.get(secretId)?.let { cipher.decrypt(it) }
    }

    override suspend fun remove(secretId: String) {
        validateSecretId(secretId)
        repository.remove(secretId)
    }

    override suspend fun clear() = repository.clear()
}
