package io.github.serkankaracan.camgridtv.ui.navigation

import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import io.github.serkankaracan.camgridtv.model.CameraDevice
import io.github.serkankaracan.camgridtv.model.CredentialProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedCameraBootstrapPolicyTest {
    @Test
    fun `waits until configuration is loaded`() {
        val decision =
            SavedCameraBootstrapPolicy.decide(
                configuration = null,
                permissionGranted = true,
                alreadyHandled = false,
            )

        assertEquals(SavedCameraBootstrapDecision.AwaitConfiguration, decision)
    }

    @Test
    fun `waits for permission after configuration is loaded`() {
        val decision =
            SavedCameraBootstrapPolicy.decide(
                configuration = CameraConfiguration(),
                permissionGranted = false,
                alreadyHandled = false,
            )

        assertEquals(SavedCameraBootstrapDecision.AwaitPermission, decision)
    }

    @Test
    fun `opens saved selected cameras in persisted order without connection test state`() {
        val profile = credentialProfile()
        val second = camera(id = "second", profileId = profile.id, selectionOrder = 1)
        val first = camera(id = "first", profileId = profile.id, selectionOrder = 0)
        val unselected = camera(id = "unselected", profileId = profile.id, selected = false)
        val configuration =
            CameraConfiguration(
                cameras = listOf(second, unselected, first),
                credentialProfiles = listOf(profile),
            )

        val decision =
            SavedCameraBootstrapPolicy.decide(
                configuration = configuration,
                permissionGranted = true,
                alreadyHandled = false,
            )

        assertEquals(
            SavedCameraBootstrapDecision.OpenWall(listOf(first, second)),
            decision,
        )
    }

    @Test
    fun `completes without wall when a selected camera has no credential profile`() {
        val profile = credentialProfile()
        val configuration =
            CameraConfiguration(
                cameras =
                    listOf(
                        camera(id = "ready", profileId = profile.id, selectionOrder = 0),
                        camera(id = "missing-profile", profileId = null, selectionOrder = 1),
                    ),
                credentialProfiles = listOf(profile),
            )

        val decision =
            SavedCameraBootstrapPolicy.decide(
                configuration = configuration,
                permissionGranted = true,
                alreadyHandled = false,
            )

        assertEquals(SavedCameraBootstrapDecision.Skip, decision)
    }

    @Test
    fun `completes without wall when no camera is selected`() {
        val profile = credentialProfile()
        val configuration =
            CameraConfiguration(
                cameras = listOf(camera(id = "camera", profileId = profile.id, selected = false)),
                credentialProfiles = listOf(profile),
            )

        val decision =
            SavedCameraBootstrapPolicy.decide(
                configuration = configuration,
                permissionGranted = true,
                alreadyHandled = false,
            )

        assertEquals(SavedCameraBootstrapDecision.Skip, decision)
    }

    @Test
    fun `already handled bootstrap completes without another wall action`() {
        val decision =
            SavedCameraBootstrapPolicy.decide(
                configuration = null,
                permissionGranted = false,
                alreadyHandled = true,
            )

        assertEquals(SavedCameraBootstrapDecision.Skip, decision)
    }

    @Test
    fun `cancelled bootstrap can retry and stale attempt cannot cancel replacement`() {
        val gate = SavedCameraBootstrapAttemptGate()
        val cancelled = requireNotNull(gate.tryStart())

        gate.cancelActive()
        val replacement = requireNotNull(gate.tryStart())

        assertFalse(gate.cancel(cancelled))
        assertTrue(gate.complete(replacement))
        assertTrue(gate.handled)
        assertNull(gate.tryStart())
    }

    @Test
    fun `only a successful active bootstrap marks startup handled`() {
        val gate = SavedCameraBootstrapAttemptGate()
        val first = requireNotNull(gate.tryStart())

        assertTrue(gate.cancel(first))
        assertFalse(gate.handled)
        val retry = requireNotNull(gate.tryStart())
        assertTrue(gate.complete(retry))
        assertTrue(gate.handled)
    }

    private fun credentialProfile() =
        CredentialProfile(
            id = "profile",
            displayName = "Saved account",
            secretId = "encrypted-secret-reference",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
        )

    private fun camera(
        id: String,
        profileId: String?,
        selected: Boolean = true,
        selectionOrder: Int? = if (selected) 0 else null,
    ) =
        CameraDevice(
            id = id,
            displayName = "Camera $id",
            host = "192.168.50.10",
            credentialProfileId = profileId,
            selected = selected,
            selectionOrder = selectionOrder,
            lastSeenEpochMillis = 1,
        )
}
