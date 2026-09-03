package io.github.serkankaracan.camgridtv.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretRedactorTest {
    @Test
    fun `redacts complete RTSP user info`() {
        val unsafe = "Failed " + credentialBearingUri("operator", "p%40ss%3Aword")
        val redacted = SecretRedactor.redact(unsafe)

        assertTrue(redactedUri() in redacted)
        assertFalse("operator" in redacted)
        assertFalse("p%40ss" in redacted)
    }

    @Test
    fun `redacts malformed raw at signs conservatively`() {
        val redacted = SecretRedactor.redact(credentialBearingUri("name", "p@ss"))

        assertFalse("name" in redacted)
        assertFalse("p@ss" in redacted)
    }

    @Test
    fun `redacts named values authorization headers and caller supplied secrets`() {
        val redacted =
            SecretRedactor.redact(
                "username=operator password='camera-pass' Authorization: Basic encoded-value token-123",
                knownSecrets = listOf("token-123"),
            )

        assertFalse("operator" in redacted)
        assertFalse("camera-pass" in redacted)
        assertFalse("encoded-value" in redacted)
        assertFalse("token-123" in redacted)
    }

    private fun credentialBearingUri(username: String, password: String): String =
        "rtsp://" + username + ":" + password + "@192.168.50.100:554/stream2"

    private fun redactedUri(): String =
        "rtsp://" + "***" + ":" + "***" + "@192.168.50.100:554/stream2"
}
