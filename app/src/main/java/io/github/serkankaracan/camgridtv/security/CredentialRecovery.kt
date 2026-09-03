package io.github.serkankaracan.camgridtv.security

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface CredentialRecovery {
    /**
     * Irrecoverably removes locally stored camera credentials and detaches their metadata.
     * Implementations must never attempt to recover or persist plaintext credentials.
     */
    suspend fun clearStoredCredentials()
}

class SafeCredentialRecovery(
    private val keyInvalidator: CredentialKeyInvalidator,
    private val secretStore: CredentialSecretStore,
    private val resetCredentialProfileLinks: suspend () -> Unit,
) : CredentialRecovery {
    private val recoveryMutex = Mutex()

    override suspend fun clearStoredCredentials() {
        recoveryMutex.withLock {
            withContext(NonCancellable) {
                var cleanupFailed = false

                // Delete the key first so any surviving blob is immediately unrecoverable.
                cleanupFailed = attemptCleanup(keyInvalidator::deleteCredentialKey) || cleanupFailed
                cleanupFailed = attemptCleanup(secretStore::clear) || cleanupFailed
                cleanupFailed = attemptCleanup(resetCredentialProfileLinks) || cleanupFailed

                if (cleanupFailed) {
                    throw CredentialRecoveryException(
                        "Stored camera credentials could not be fully cleared"
                    )
                }
            }
        }
    }

    private suspend fun attemptCleanup(cleanup: suspend () -> Unit): Boolean =
        try {
            cleanup()
            false
        } catch (_: Exception) {
            true
        }
}

class CredentialRecoveryException(message: String) : IllegalStateException(message)
