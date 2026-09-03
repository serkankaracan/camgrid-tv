package io.github.serkankaracan.camgridtv.data

import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import io.github.serkankaracan.camgridtv.model.CameraDevice
import io.github.serkankaracan.camgridtv.model.CredentialProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CameraConfigurationCodecTest {
    @Test
    fun `round trips non-secret camera configuration`() {
        val profile =
            CredentialProfile(
                id = "shared-profile",
                displayName = "Shared account",
                secretId = "encrypted-secret-reference",
                createdAtEpochMillis = 10L,
                updatedAtEpochMillis = 20L,
            )
        val configuration =
            CameraConfiguration(
                cameras =
                    listOf(
                        CameraDevice(
                            id = "camera-id",
                            endpointUuid = "11111111-2222-4333-8444-555555555555",
                            onvifXAddr = "http://192.168.50.100:2020/onvif/device_service",
                            displayName = "Living room",
                            discoveredName = "Example Camera",
                            manufacturer = "Example",
                            model = "Model 1",
                            host = "192.168.50.100",
                            credentialProfileId = profile.id,
                            selected = true,
                            selectionOrder = 0,
                            lastSeenEpochMillis = 30L,
                        )
                    ),
                credentialProfiles = listOf(profile),
            )

        val encoded = CameraConfigurationCodec.encode(configuration)
        val restored = CameraConfigurationCodec.decode(encoded)

        assertEquals(configuration, restored)
        assertFalse(encoded.contains("Living room"))
    }

    @Test(expected = CameraConfigurationStorageException::class)
    fun `rejects corrupt persisted input`() {
        CameraConfigurationCodec.decode("not-valid-base64!!")
    }
}
