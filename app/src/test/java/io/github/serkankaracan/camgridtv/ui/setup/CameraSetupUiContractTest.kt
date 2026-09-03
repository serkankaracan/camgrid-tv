package io.github.serkankaracan.camgridtv.ui.setup

import org.junit.Assert.assertFalse
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
}
