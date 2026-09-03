package io.github.serkankaracan.camgridtv.ui

import io.github.serkankaracan.camgridtv.security.CredentialEncryptionException
import io.github.serkankaracan.camgridtv.security.SecretRecoveryRequiredException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialRecoveryClassificationTest {
    @Test
    fun `only verified stored secret failures require destructive recovery`() {
        assertTrue(SecretRecoveryRequiredException("fixture recovery").requiresCredentialRecovery())
        assertFalse(
            CredentialEncryptionException("fixture transient encryption failure")
                .requiresCredentialRecovery()
        )
        assertFalse(IllegalStateException("fixture generic failure").requiresCredentialRecovery())
    }
}
