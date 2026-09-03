package io.github.serkankaracan.camgridtv.security

interface CredentialCipher {
    suspend fun encrypt(secret: CredentialSecret): EncryptedCredential

    /** The caller owns the returned secret and must close it after use. */
    suspend fun decrypt(encryptedCredential: EncryptedCredential): CredentialSecret
}

class CredentialEncryptionException(message: String) : IllegalStateException(message)

class SecretRecoveryRequiredException(message: String) : IllegalStateException(message)
