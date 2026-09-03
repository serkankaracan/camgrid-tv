package io.github.serkankaracan.camgridtv.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CredentialPayloadCodecTest {
    @Test
    fun `round trips Unicode without exposing it in object strings`() {
        val secret = CredentialSecret("kamera-kullanıcısı", "güçlü parola".toCharArray())
        val payload = CredentialPayloadCodec.encode(secret)
        val restored = CredentialPayloadCodec.decode(payload)
        val restoredPassword = restored.copyPassword()
        try {
            assertEquals("kamera-kullanıcısı", restored.username)
            assertEquals("güçlü parola", restoredPassword.concatToString())
            assertFalse(secret.toString().contains(secret.username))
            assertFalse(restored.toString().contains(restored.username))
        } finally {
            restoredPassword.fill('\u0000')
            restored.close()
            secret.close()
            payload.fill(0)
        }
    }
}
