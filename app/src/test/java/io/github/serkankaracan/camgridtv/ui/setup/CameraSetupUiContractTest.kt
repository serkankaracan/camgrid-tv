package io.github.serkankaracan.camgridtv.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSetupUiContractTest {
    @Test
    fun `state and password action redact credentials from diagnostics`() {
        val fixture = listOf("not", "-", "for", "-", "logs").joinToString("")
        val state =
            CameraSetupUiState(cameras = emptyList(), username = fixture, password = fixture)
        val passwordAction = CameraSetupUiAction.PasswordChanged(fixture)
        val usernameAction = CameraSetupUiAction.UsernameChanged(fixture)

        assertFalse(state.toString().contains(fixture))
        assertFalse(passwordAction.toString().contains(fixture))
        assertFalse(usernameAction.toString().contains(fixture))
    }

    @Test
    fun `credential recovery state remains safe for diagnostics`() {
        val state =
            CameraSetupUiState(
                cameras = emptyList(),
                credentialRecovery = CredentialRecoveryUiState.Required,
                canStartWatching = false,
            )

        assertFalse(state.canStartWatching)
        assertTrue(state.toString().contains("credentialRecovery=Required"))
        assertTrue(CameraSetupUiAction.ClearStoredCredentials.toString().isNotBlank())
    }

    @Test
    fun `testing state locks credential interactions`() {
        val state =
            CameraSetupUiState(
                cameras =
                    listOf(
                        SetupCameraUiModel(
                            id = "camera",
                            displayName = "Camera",
                            connectionState = ConnectionTestUiState.Testing,
                        )
                    )
            )

        assertTrue(state.connectionTestInProgress)
    }

    @Test
    fun `connection operation gate rejects overlap and stale completion`() {
        val gate = ConnectionTestOperationGate()
        val cancelled = requireNotNull(gate.tryStart("camera"))

        assertTrue(gate.tryStart("other") == null)
        assertTrue(gate.cancelActive() == cancelled)
        val replacement = requireNotNull(gate.tryStart("camera"))

        assertFalse(gate.finish(cancelled))
        assertTrue(gate.isCurrent(replacement))
        assertTrue(gate.finish(replacement))
    }

    @Test
    fun `shared profile update gate serializes writes and rejects stale completion`() {
        val gate = SharedProfileUpdateOperationGate()
        val disabled = requireNotNull(gate.tryStart(enabled = false))

        assertNull(gate.tryStart(enabled = true))
        assertTrue(gate.finish(disabled))
        val enabled = requireNotNull(gate.tryStart(enabled = true))

        assertFalse(gate.finish(disabled))
        assertTrue(gate.isCurrent(enabled))
        assertTrue(gate.finish(enabled))
    }

    @Test
    fun `disabled shared mode cannot reuse a stale shared profile`() {
        val sharedProfileId = "shared-profile"

        assertNull(
            ConnectionTestCredentialProfilePolicy.storedProfileForTest(
                assignedProfileId = sharedProfileId,
                useSharedProfile = false,
                sharedProfileId = sharedProfileId,
            )
        )
        assertEquals(
            "camera-profile",
            ConnectionTestCredentialProfilePolicy.storedProfileForTest(
                assignedProfileId = "camera-profile",
                useSharedProfile = false,
                sharedProfileId = sharedProfileId,
            ),
        )
        assertEquals(
            sharedProfileId,
            ConnectionTestCredentialProfilePolicy.storedProfileForTest(
                assignedProfileId = sharedProfileId,
                useSharedProfile = true,
                sharedProfileId = sharedProfileId,
            ),
        )
    }

    @Test
    fun `preview exposes only its matching camera`() {
        val preview =
            SetupCameraUiModel(
                id = "preview",
                displayName = "Preview camera",
                connectionState = ConnectionTestUiState.Connected,
            )
        val state =
            CameraSetupUiState(
                cameras = listOf(preview, SetupCameraUiModel("other", "Other camera")),
                connectionPreviewCameraId = preview.id,
            )

        assertTrue(state.connectionPreviewCamera == preview)
        assertFalse(
            CameraSetupUiState(
                    cameras = state.cameras,
                    connectionPreviewCameraId = "missing",
                )
                .connectionPreviewCamera != null
        )
    }

    @Test
    fun `new preview supersedes old hold and rejects its stale release`() {
        val gate = ConnectionTestPreviewGate()
        val oldSession = gate.replace("old")
        val newSession = gate.replace("new")

        assertFalse(gate.finish(oldSession))
        assertTrue(gate.active == newSession)
        assertTrue(gate.finish(newSession))
        assertTrue(gate.active == null)
    }
}
