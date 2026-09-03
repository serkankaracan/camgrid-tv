package io.github.serkankaracan.camgridtv.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSetupPrimaryActionPolicyTest {
    @Test
    fun `verify requires a selected target and usable credentials`() {
        val noSelection =
            decision(
                cameras = listOf(camera("camera", selected = false)),
                username = COMPLETE_USERNAME,
                password = completePassword(),
            )
        assertEquals(CameraSetupPrimaryActionKind.VerifyConnection, noSelection.kind)
        assertNull(noSelection.verificationTargetCameraId)
        assertFalse(noSelection.enabled)

        val emptyDraft = decision(cameras = listOf(camera("first"), camera("second")))
        assertEquals(CredentialDraftState.Empty, emptyDraft.credentialDraftState)
        assertEquals("first", emptyDraft.verificationTargetCameraId)
        assertFalse(emptyDraft.enabled)

        val incompleteDraft =
            decision(
                cameras = listOf(camera("first"), camera("second")),
                username = COMPLETE_USERNAME,
            )
        assertEquals(CredentialDraftState.Incomplete, incompleteDraft.credentialDraftState)
        assertFalse(incompleteDraft.enabled)

        val completeDraft =
            decision(
                cameras = listOf(camera("first"), camera("second")),
                username = COMPLETE_USERNAME,
                password = completePassword(),
            )
        assertEquals(CredentialDraftState.Complete, completeDraft.credentialDraftState)
        assertTrue(completeDraft.enabled)
    }

    @Test
    fun `stored profile is used only when the draft is empty`() {
        val storedCamera = camera("stored", hasCredentialProfile = true)

        assertTrue(decision(cameras = listOf(storedCamera)).enabled)
        assertFalse(decision(cameras = listOf(storedCamera), username = COMPLETE_USERNAME).enabled)
    }

    @Test
    fun `preview failure and other actionable failures precede untested cameras`() {
        val cameras =
            listOf(
                camera("untested"),
                camera("first-failure", ConnectionTestUiState.AuthenticationFailed),
                camera("preview-failure", ConnectionTestUiState.Offline),
            )

        assertEquals(
            "preview-failure",
            decision(
                    cameras = cameras,
                    username = COMPLETE_USERNAME,
                    password = completePassword(),
                    connectionPreviewCameraId = "preview-failure",
                )
                .verificationTargetCameraId,
        )
        assertEquals(
            "first-failure",
            decision(
                    cameras = cameras,
                    username = COMPLETE_USERNAME,
                    password = completePassword(),
                )
                .verificationTargetCameraId,
        )
    }

    @Test
    fun `next camera without a profile becomes the verification target`() {
        val action =
            decision(
                cameras =
                    listOf(
                        camera(
                            id = "connected",
                            state = ConnectionTestUiState.Connected,
                            hasCredentialProfile = true,
                        ),
                        camera("next"),
                    )
            )

        assertEquals("next", action.verificationTargetCameraId)
        assertFalse(action.enabled)
    }

    @Test
    fun `readiness changes the action to an enabled watch action`() {
        val action =
            decision(
                cameras =
                    listOf(
                        camera(
                            id = "connected",
                            state = ConnectionTestUiState.Connected,
                            hasCredentialProfile = true,
                        ),
                        camera("untested", hasCredentialProfile = true),
                    ),
                canStartWatching = true,
            )

        assertEquals(CameraSetupPrimaryActionKind.StartWatching, action.kind)
        assertNull(action.verificationTargetCameraId)
        assertTrue(action.enabled)
    }

    @Test
    fun `all asynchronous setup locks disable the watch action`() {
        val readyCamera =
            camera(
                id = "connected",
                state = ConnectionTestUiState.Connected,
                hasCredentialProfile = true,
            )
        val blockedStates =
            listOf(
                CameraSetupUiState(
                    cameras = listOf(readyCamera),
                    canStartWatching = true,
                    submitting = true,
                ),
                CameraSetupUiState(
                    cameras = listOf(readyCamera),
                    canStartWatching = true,
                    selectionUpdateCameraId = readyCamera.id,
                ),
                CameraSetupUiState(
                    cameras = listOf(readyCamera),
                    canStartWatching = true,
                    sharedProfileUpdateInProgress = true,
                ),
                CameraSetupUiState(
                    cameras =
                        listOf(readyCamera.copy(connectionState = ConnectionTestUiState.Testing)),
                    canStartWatching = true,
                ),
                CameraSetupUiState(
                    cameras = listOf(readyCamera),
                    canStartWatching = true,
                    credentialRecovery = CredentialRecoveryUiState.Required,
                ),
            )

        blockedStates.forEachIndexed { index, state ->
            val action = CameraSetupPrimaryActionPolicy.resolve(state)
            assertEquals("case $index", CameraSetupPrimaryActionKind.StartWatching, action.kind)
            assertFalse("case $index", action.enabled)
        }
    }

    private fun decision(
        cameras: List<SetupCameraUiModel>,
        username: String = "",
        password: String = "",
        canStartWatching: Boolean = false,
        connectionPreviewCameraId: String? = null,
    ): CameraSetupPrimaryActionDecision =
        CameraSetupPrimaryActionPolicy.resolve(
            CameraSetupUiState(
                cameras = cameras,
                username = username,
                password = password,
                canStartWatching = canStartWatching,
                connectionPreviewCameraId = connectionPreviewCameraId,
            )
        )

    private fun camera(
        id: String,
        state: ConnectionTestUiState = ConnectionTestUiState.NotTested,
        selected: Boolean = true,
        hasCredentialProfile: Boolean = false,
    ): SetupCameraUiModel =
        SetupCameraUiModel(
            id = id,
            displayName = id,
            selected = selected,
            hasCredentialProfile = hasCredentialProfile,
            connectionState = state,
        )

    private fun completePassword(): String = listOf("unit", "fixture").joinToString("-")

    private companion object {
        const val COMPLETE_USERNAME = "fixture-user"
    }
}
