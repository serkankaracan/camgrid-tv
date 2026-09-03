package io.github.serkankaracan.camgridtv.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialRecoveryTest {
    @Test
    fun clearsKeyBeforeBlobsAndProfileLinks() = runTest {
        val steps = mutableListOf<String>()
        val recovery =
            SafeCredentialRecovery(
                keyInvalidator = RecordingKeyInvalidator { steps += "key" },
                secretStore = RecordingSecretStore { steps += "blobs" },
                resetCredentialProfileLinks = { steps += "profiles" },
            )

        recovery.clearStoredCredentials()

        assertEquals(listOf("key", "blobs", "profiles"), steps)
    }

    @Test
    fun attemptsEveryCleanupWhenIndividualOperationsFail() = runTest {
        val steps = mutableListOf<String>()
        val recovery =
            SafeCredentialRecovery(
                keyInvalidator =
                    RecordingKeyInvalidator {
                        steps += "key"
                        error("fixture key failure")
                    },
                secretStore =
                    RecordingSecretStore {
                        steps += "blobs"
                        error("fixture blob failure")
                    },
                resetCredentialProfileLinks = {
                    steps += "profiles"
                    error("fixture profile failure")
                },
            )

        val failure =
            try {
                recovery.clearStoredCredentials()
                null
            } catch (caught: Exception) {
                caught
            }

        assertTrue(failure is CredentialRecoveryException)
        assertEquals(listOf("key", "blobs", "profiles"), steps)
    }

    private class RecordingKeyInvalidator(private val delete: suspend () -> Unit) :
        CredentialKeyInvalidator {
        override suspend fun deleteCredentialKey() = delete()
    }

    private class RecordingSecretStore(private val clearStore: suspend () -> Unit) :
        CredentialSecretStore {
        override suspend fun put(secretId: String, secret: CredentialSecret) =
            error("Not used by this fixture")

        override suspend fun get(secretId: String): CredentialSecret? =
            error("Not used by this fixture")

        override suspend fun remove(secretId: String) = error("Not used by this fixture")

        override suspend fun clear() = clearStore()
    }
}
