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
        val action = CameraSetupUiAction.PasswordChanged(fixture)

        assertFalse(state.toString().contains(fixture))
        assertFalse(action.toString().contains(fixture))
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
}
