package io.github.serkankaracan.camgridtv.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialValidatorTest {
    @Test
    fun `accepts Unicode and URI reserved characters`() {
        val result =
            CredentialValidator.validate("kamera kullanıcı", "p@ss:/%#Türkçe".toCharArray())

        assertEquals(CredentialValidationResult.Valid, result)
    }

    @Test
    fun `requires both fields without echoing their values`() {
        val result =
            CredentialValidator.validate(" ", charArrayOf()) as CredentialValidationResult.Invalid

        assertTrue(CredentialValidationIssue.USERNAME_REQUIRED in result.issues)
        assertTrue(CredentialValidationIssue.PASSWORD_REQUIRED in result.issues)
    }

    @Test
    fun `rejects control characters that could forge logs or protocol fields`() {
        val result =
            CredentialValidator.validate("operator\nforged", "safe".toCharArray())
                as CredentialValidationResult.Invalid

        assertTrue(CredentialValidationIssue.USERNAME_CONTAINS_CONTROL_CHARACTER in result.issues)
    }
}
