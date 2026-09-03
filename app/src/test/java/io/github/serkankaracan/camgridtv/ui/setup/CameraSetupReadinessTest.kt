package io.github.serkankaracan.camgridtv.ui.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSetupReadinessTest {
    @Test
    fun `active test blocks wall even when another camera already connected`() {
        val ready =
            CameraSetupReadiness.canStartWatching(
                selectedCameraIds = setOf("one", "two"),
                camerasWithCredentialProfiles = setOf("one", "two"),
                connectionStates =
                    mapOf(
                        "one" to ConnectionTestUiState.Connected,
                        "two" to ConnectionTestUiState.Testing,
                    ),
                submitting = false,
                credentialRecovery = CredentialRecoveryUiState.NotRequired,
            )

        assertFalse(ready)
    }

    @Test
    fun `settled successful test permits wall and saved bootstrap can skip retest`() {
        val common =
            mapOf(
                "one" to ConnectionTestUiState.Connected,
                "two" to ConnectionTestUiState.Offline,
            )
        assertTrue(
            CameraSetupReadiness.canStartWatching(
                selectedCameraIds = setOf("one", "two"),
                camerasWithCredentialProfiles = setOf("one", "two"),
                connectionStates = common,
                submitting = false,
                credentialRecovery = CredentialRecoveryUiState.NotRequired,
            )
        )
        assertTrue(
            CameraSetupReadiness.canStartWatching(
                selectedCameraIds = setOf("one"),
                camerasWithCredentialProfiles = setOf("one"),
                connectionStates = emptyMap(),
                submitting = false,
                credentialRecovery = CredentialRecoveryUiState.NotRequired,
                requireSuccessfulTest = false,
            )
        )
    }
}
