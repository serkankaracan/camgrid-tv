package io.github.serkankaracan.camgridtv.security

import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidKeystoreCredentialCipherTest {
    @Test
    fun roundTripUsesFreshIvAndDoesNotStorePlaintext() = runBlocking {
        val keyAlias = "camgrid_tv_instrumented_${UUID.randomUUID()}"
        val cipher = AndroidKeystoreCredentialCipher(keyAlias = keyAlias)
        val username = "fixture-camera-user"
        val password = "fixture-only-password-42".toCharArray()
        val secret = CredentialSecret(username, password)
        var restored: CredentialSecret? = null
        try {
            val first = cipher.encrypt(secret)
            val second = cipher.encrypt(secret)

            assertFalse(first.initializationVector.contentEquals(second.initializationVector))
            assertFalse(
                first.ciphertext.containsSubsequence(username.toByteArray(StandardCharsets.UTF_8))
            )
            assertFalse(
                first.ciphertext.containsSubsequence(
                    String(password).toByteArray(StandardCharsets.UTF_8)
                )
            )

            restored = cipher.decrypt(first)
            val restoredPassword = restored.copyPassword()
            try {
                assertEquals(username, restored.username)
                assertArrayEquals(password, restoredPassword)
            } finally {
                restoredPassword.fill('\u0000')
            }
        } finally {
            restored?.close()
            secret.close()
            password.fill('\u0000')
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(keyAlias)
        }
    }

    private fun ByteArray.containsSubsequence(candidate: ByteArray): Boolean {
        if (candidate.isEmpty() || candidate.size > size) return false
        return (0..size - candidate.size).any { offset ->
            candidate.indices.all { index -> this[offset + index] == candidate[index] }
        }
    }
}
