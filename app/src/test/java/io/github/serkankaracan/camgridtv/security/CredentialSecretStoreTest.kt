package io.github.serkankaracan.camgridtv.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialSecretStoreTest {
    @Test
    fun propagatesRecoveryFailureWithoutPlaintextFallback() = runTest {
        val store =
            EncryptedCredentialSecretStore(
                cipher =
                    object : CredentialCipher {
                        override suspend fun encrypt(
                            secret: CredentialSecret
                        ): EncryptedCredential = error("Not used by this fixture")

                        override suspend fun decrypt(
                            encryptedCredential: EncryptedCredential
                        ): CredentialSecret =
                            throw SecretRecoveryRequiredException("fixture recovery required")
                    },
                repository =
                    object : EncryptedCredentialRepository {
                        override suspend fun get(secretId: String) =
                            EncryptedCredential(
                                schemaVersion = 1,
                                initializationVector = ByteArray(12) { 1 },
                                ciphertext = byteArrayOf(2),
                            )

                        override suspend fun put(
                            secretId: String,
                            encryptedCredential: EncryptedCredential,
                        ) = error("Not used by this fixture")

                        override suspend fun remove(secretId: String) =
                            error("Not used by this fixture")

                        override suspend fun clear() = error("Not used by this fixture")
                    },
            )

        val failure =
            try {
                store.get("fixture-secret")
                null
            } catch (caught: Exception) {
                caught
            }

        assertTrue(failure is SecretRecoveryRequiredException)
    }
}
