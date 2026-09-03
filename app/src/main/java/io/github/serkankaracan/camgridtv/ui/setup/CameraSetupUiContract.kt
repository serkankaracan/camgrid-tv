package io.github.serkankaracan.camgridtv.ui.setup

enum class ConnectionTestUiState {
    NotTested,
    CredentialsRequired,
    Testing,
    Connected,
    AuthenticationFailed,
    Offline,
    Failed,
}

enum class CredentialRecoveryUiState {
    NotRequired,
    Required,
    Clearing,
    ClearFailed,
}

data class SetupCameraUiModel(
    val id: String,
    val displayName: String,
    val detail: String? = null,
    val selected: Boolean = true,
    val connectionState: ConnectionTestUiState = ConnectionTestUiState.NotTested,
)

class CameraSetupUiState(
    val cameras: List<SetupCameraUiModel>,
    val username: String = "",
    val password: String = "",
    val useSharedProfile: Boolean = true,
    val editingCameraId: String? = null,
    val editedCameraName: String = "",
    val credentialRecovery: CredentialRecoveryUiState = CredentialRecoveryUiState.NotRequired,
    val canStartWatching: Boolean = false,
    val submitting: Boolean = false,
) {
    val connectionTestInProgress: Boolean
        get() = cameras.any { it.connectionState == ConnectionTestUiState.Testing }

    override fun toString(): String =
        "CameraSetupUiState(cameras=$cameras, username=***, password=***, " +
            "useSharedProfile=$useSharedProfile, editingCameraId=$editingCameraId, " +
            "credentialRecovery=$credentialRecovery, canStartWatching=$canStartWatching, " +
            "submitting=$submitting)"
}

sealed interface CameraSetupUiAction {
    data class CameraSelectionChanged(val cameraId: String, val selected: Boolean) :
        CameraSetupUiAction

    data class EditCameraName(val cameraId: String) : CameraSetupUiAction

    data class CameraNameChanged(val value: String) : CameraSetupUiAction

    data object SaveCameraName : CameraSetupUiAction

    data class UsernameChanged(val value: String) : CameraSetupUiAction {
        override fun toString(): String = "UsernameChanged(value=***)"
    }

    class PasswordChanged(val value: String) : CameraSetupUiAction {
        override fun toString(): String = "PasswordChanged(value=***)"
    }

    data class SharedProfileChanged(val enabled: Boolean) : CameraSetupUiAction

    data class TestConnection(val cameraId: String) : CameraSetupUiAction

    data object StartWatching : CameraSetupUiAction

    data object ClearStoredCredentials : CameraSetupUiAction
}

internal data class ConnectionTestOperation(val cameraId: String, val token: Long)

/** Prevents overlapping tests and rejects completions from cancelled operations. */
internal class ConnectionTestOperationGate {
    private var nextToken = 0L

    var active: ConnectionTestOperation? = null
        private set

    fun tryStart(cameraId: String): ConnectionTestOperation? {
        if (active != null) return null
        return ConnectionTestOperation(cameraId, ++nextToken).also { active = it }
    }

    fun isCurrent(operation: ConnectionTestOperation): Boolean = active == operation

    fun finish(operation: ConnectionTestOperation): Boolean {
        if (!isCurrent(operation)) return false
        active = null
        return true
    }

    fun cancelActive(): ConnectionTestOperation? = active.also { active = null }
}
