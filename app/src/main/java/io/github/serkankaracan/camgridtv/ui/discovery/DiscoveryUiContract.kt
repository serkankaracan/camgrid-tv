package io.github.serkankaracan.camgridtv.ui.discovery

sealed interface LocalNetworkPermissionUiState {
    data object Granted : LocalNetworkPermissionUiState

    data object RationaleRequired : LocalNetworkPermissionUiState

    data object Denied : LocalNetworkPermissionUiState
}

sealed interface DiscoveryContentUiState {
    data object Ready : DiscoveryContentUiState

    data object Loading : DiscoveryContentUiState

    data class Scanning(val camerasFound: List<DiscoveryCameraUiModel> = emptyList()) :
        DiscoveryContentUiState

    data object Empty : DiscoveryContentUiState

    data class Error(val reason: DiscoveryErrorUiState) : DiscoveryContentUiState

    data class Results(val cameras: List<DiscoveryCameraUiModel>) : DiscoveryContentUiState
}

enum class DiscoveryErrorUiState {
    NoActiveLocalNetwork,
    NetworkLost,
    TransportUnavailable,
}

data class DiscoveryCameraUiModel(
    val id: String,
    val displayName: String,
    val detail: String? = null,
    val selected: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "Discovery camera id is required" }
        require(displayName.isNotBlank()) { "Discovery camera name is required" }
    }
}

data class DiscoveryUiState(
    val permission: LocalNetworkPermissionUiState,
    val content: DiscoveryContentUiState = DiscoveryContentUiState.Ready,
)

sealed interface DiscoveryUiAction {
    data object RequestPermission : DiscoveryUiAction

    data object OpenAppSettings : DiscoveryUiAction

    data object StartScan : DiscoveryUiAction

    data object CancelScan : DiscoveryUiAction

    data object ContinueToCameraSetup : DiscoveryUiAction

    data class CameraSelectionChanged(
        val cameraId: String,
        val selected: Boolean,
    ) : DiscoveryUiAction
}
