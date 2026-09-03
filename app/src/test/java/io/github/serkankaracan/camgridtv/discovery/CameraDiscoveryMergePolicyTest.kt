package io.github.serkankaracan.camgridtv.discovery

import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import io.github.serkankaracan.camgridtv.model.CameraDevice
import io.github.serkankaracan.camgridtv.model.CredentialProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CameraDiscoveryMergePolicyTest {
    private val policy = CameraDiscoveryMergePolicy()

    @Test
    fun `uuid transition preserves persisted identity name credential and selection`() {
        val profile =
            CredentialProfile(
                id = "profile",
                displayName = "Camera account",
                secretId = "encrypted-secret",
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1,
            )
        val existing =
            camera(
                id = "xaddr-derived-id",
                endpointUuid = null,
                host = "192.168.50.20",
                displayName = "Front door",
                credentialProfileId = profile.id,
                selected = true,
                selectionOrder = 0,
            )
        val discovered =
            discovery(
                id = "uuid-derived-id",
                endpointUuid = "11111111-1111-1111-1111-111111111111",
                host = "192.168.50.20",
            )

        val merged =
            policy.merge(
                current =
                    CameraConfiguration(
                        cameras = listOf(existing),
                        credentialProfiles = listOf(profile),
                    ),
                discoveredDevices = listOf(discovered),
                selectedByDiscoveryId = mapOf(discovered.id to true),
            )

        assertEquals(1, merged.cameras.size)
        with(merged.cameras.single()) {
            assertEquals(existing.id, id)
            assertEquals("Front door", displayName)
            assertEquals(profile.id, credentialProfileId)
            assertEquals(0, selectionOrder)
            assertEquals(discovered.endpointUuid, endpointUuid)
        }
    }

    @Test
    fun `endpoint uuid match updates changed address without duplicating camera`() {
        val endpointUuid = "22222222-2222-2222-2222-222222222222"
        val existing =
            camera(
                id = "persisted-id",
                endpointUuid = endpointUuid,
                host = "192.168.50.30",
            )
        val discovered =
            discovery(
                id = "new-ephemeral-id",
                endpointUuid = endpointUuid,
                host = "192.168.50.31",
            )

        val merged =
            policy.merge(
                current = CameraConfiguration(cameras = listOf(existing)),
                discoveredDevices = listOf(discovered),
                selectedByDiscoveryId = emptyMap(),
            )

        assertEquals(1, merged.cameras.size)
        assertEquals(existing.id, merged.cameras.single().id)
        assertEquals("192.168.50.31", merged.cameras.single().host)
    }

    @Test
    fun `unmatched camera is appended and selected in deterministic order`() {
        val existing =
            camera(id = "existing", host = "192.168.50.40", selected = true, selectionOrder = 2)
        val discovered = discovery(id = "new", host = "192.168.50.41")

        val merged =
            policy.merge(
                current = CameraConfiguration(cameras = listOf(existing)),
                discoveredDevices = listOf(discovered),
                selectedByDiscoveryId = mapOf(discovered.id to true),
            )

        assertSame(existing, merged.cameras.first())
        assertEquals(3, merged.cameras.last().selectionOrder)
    }

    @Test
    fun `saved selected camera keeps its last known address when absent from discovery`() {
        val existing =
            camera(
                id = "temporarily-offline",
                host = "192.168.50.42",
                selected = true,
                selectionOrder = 0,
            )
        val unrelated = discovery(id = "new-camera", host = "192.168.50.43")

        val merged =
            policy.merge(
                current = CameraConfiguration(cameras = listOf(existing)),
                discoveredDevices = listOf(unrelated),
                selectedByDiscoveryId = emptyMap(),
            )

        assertSame(existing, merged.cameras.first())
        assertEquals("192.168.50.42", merged.selectedCameras().single().host)
    }

    @Test
    fun `conflicting endpoint uuid cannot overwrite an exact id credential binding`() {
        val profile =
            CredentialProfile(
                id = "saved-profile",
                displayName = "Saved camera account",
                secretId = "saved-secret",
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1,
            )
        val existing =
            camera(
                id = "colliding-id",
                endpointUuid = "11111111-1111-4111-8111-111111111111",
                host = "192.168.50.44",
                credentialProfileId = profile.id,
            )
        val conflicting =
            discovery(
                id = existing.id,
                endpointUuid = "22222222-2222-4222-8222-222222222222",
                host = "192.168.50.45",
            )

        val merged =
            policy.merge(
                current =
                    CameraConfiguration(
                        cameras = listOf(existing),
                        credentialProfiles = listOf(profile),
                    ),
                discoveredDevices = listOf(conflicting),
                selectedByDiscoveryId = emptyMap(),
            )

        assertSame(existing, merged.cameras.single())
    }

    private fun camera(
        id: String,
        host: String,
        endpointUuid: String? = null,
        displayName: String = "Saved camera",
        credentialProfileId: String? = null,
        selected: Boolean = false,
        selectionOrder: Int? = null,
    ) =
        CameraDevice(
            id = id,
            endpointUuid = endpointUuid,
            onvifXAddr = "http://$host:2020/onvif/device_service",
            displayName = displayName,
            host = host,
            credentialProfileId = credentialProfileId,
            selected = selected,
            selectionOrder = selectionOrder,
            lastSeenEpochMillis = 1,
        )

    private fun discovery(
        id: String,
        host: String,
        endpointUuid: String? = null,
    ) =
        DiscoveredOnvifDevice(
            id = id,
            endpointUuid = endpointUuid,
            xAddrs = listOf("http://$host:2020/onvif/device_service"),
            scopes = emptyList(),
            types = emptyList(),
            host = host,
            onvifPort = 2020,
            discoveredName = "Discovered camera",
            manufacturer = null,
            model = null,
            lastSeenEpochMillis = 2,
        )
}
