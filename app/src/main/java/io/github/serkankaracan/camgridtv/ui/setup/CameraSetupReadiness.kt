package io.github.serkankaracan.camgridtv.ui.setup

/** Pure gate shared by the UI state and the action boundary. */
object CameraSetupReadiness {
    fun canStartWatching(
        selectedCameraIds: Set<String>,
        camerasWithCredentialProfiles: Set<String>,
        connectionStates: Map<String, ConnectionTestUiState>,
        submitting: Boolean,
        credentialRecovery: CredentialRecoveryUiState,
        requireSuccessfulTest: Boolean = true,
    ): Boolean =
        selectedCameraIds.isNotEmpty() &&
            selectedCameraIds.all(camerasWithCredentialProfiles::contains) &&
            connectionStates.values.none { it == ConnectionTestUiState.Testing } &&
            (!requireSuccessfulTest ||
                selectedCameraIds.any {
                    connectionStates[it] == ConnectionTestUiState.Connected
                }) &&
            !submitting &&
            credentialRecovery == CredentialRecoveryUiState.NotRequired
}
