package io.github.serkankaracan.camgridtv.ui.setup

internal enum class CredentialDraftState {
    Empty,
    Incomplete,
    Complete,
}

internal enum class CameraSetupPrimaryActionKind {
    VerifyConnection,
    StartWatching,
}

internal data class CameraSetupPrimaryActionDecision(
    val kind: CameraSetupPrimaryActionKind,
    val verificationTargetCameraId: String?,
    val credentialDraftState: CredentialDraftState,
    val verificationCredentialsAvailable: Boolean,
    val enabled: Boolean,
)

/** Keeps the adaptive setup call-to-action deterministic and independently testable. */
internal object CameraSetupPrimaryActionPolicy {
    fun resolve(state: CameraSetupUiState): CameraSetupPrimaryActionDecision {
        val selectedCameras = state.cameras.filter(SetupCameraUiModel::selected)
        val draftState = credentialDraftState(state.username, state.password)
        val kind =
            if (state.canStartWatching) {
                CameraSetupPrimaryActionKind.StartWatching
            } else {
                CameraSetupPrimaryActionKind.VerifyConnection
            }
        val verificationTarget =
            if (kind == CameraSetupPrimaryActionKind.VerifyConnection) {
                verificationTarget(selectedCameras, state.connectionPreviewCameraId)
            } else {
                null
            }
        val blocked =
            state.submitting ||
                state.selectionUpdateInProgress ||
                state.sharedProfileUpdateInProgress ||
                state.connectionTestInProgress ||
                state.credentialRecovery != CredentialRecoveryUiState.NotRequired
        val verificationCredentialsAvailable =
            verificationTarget != null &&
                when (draftState) {
                    CredentialDraftState.Empty -> verificationTarget.hasCredentialProfile
                    CredentialDraftState.Incomplete -> false
                    CredentialDraftState.Complete -> true
                }
        val enabled =
            !blocked &&
                when (kind) {
                    CameraSetupPrimaryActionKind.StartWatching -> true
                    CameraSetupPrimaryActionKind.VerifyConnection ->
                        verificationCredentialsAvailable
                }

        return CameraSetupPrimaryActionDecision(
            kind = kind,
            verificationTargetCameraId = verificationTarget?.id,
            credentialDraftState = draftState,
            verificationCredentialsAvailable = verificationCredentialsAvailable,
            enabled = enabled,
        )
    }

    private fun credentialDraftState(username: String, password: String): CredentialDraftState =
        when {
            username.isEmpty() && password.isEmpty() -> CredentialDraftState.Empty
            username.isNotBlank() && password.isNotEmpty() -> CredentialDraftState.Complete
            else -> CredentialDraftState.Incomplete
        }

    private fun verificationTarget(
        selectedCameras: List<SetupCameraUiModel>,
        connectionPreviewCameraId: String?,
    ): SetupCameraUiModel? {
        selectedCameras
            .firstOrNull { camera ->
                camera.id == connectionPreviewCameraId && camera.connectionState.isActionableFailure
            }
            ?.let {
                return it
            }
        return selectedCameras.firstOrNull { it.connectionState.isActionableFailure }
            ?: selectedCameras.firstOrNull {
                it.connectionState == ConnectionTestUiState.NotTested
            }
            ?: selectedCameras.firstOrNull {
                it.connectionState != ConnectionTestUiState.Connected
            }
            ?: selectedCameras.firstOrNull()
    }

    private val ConnectionTestUiState.isActionableFailure: Boolean
        get() =
            this == ConnectionTestUiState.CredentialsRequired ||
                this == ConnectionTestUiState.AuthenticationFailed ||
                this == ConnectionTestUiState.Offline ||
                this == ConnectionTestUiState.Failed
}
