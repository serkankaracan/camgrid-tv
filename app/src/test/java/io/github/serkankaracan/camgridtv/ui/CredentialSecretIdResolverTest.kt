package io.github.serkankaracan.camgridtv.ui

import io.github.serkankaracan.camgridtv.model.CredentialProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialSecretIdResolverTest {
    private val profile =
        CredentialProfile(
            id = "living-room-profile",
            displayName = "Living room",
            secretId = "encrypted-secret-slot",
            createdAtEpochMillis = 1L,
        )

    @Test
    fun `uses the profile secret id instead of its metadata id`() {
        assertEquals(
            "encrypted-secret-slot",
            CredentialSecretIdResolver.resolve(profile.id, listOf(profile)),
        )
    }

    @Test
    fun `returns null for a missing profile reference`() {
        assertNull(CredentialSecretIdResolver.resolve("missing-profile", listOf(profile)))
        assertNull(CredentialSecretIdResolver.resolve(null, listOf(profile)))
    }
}
