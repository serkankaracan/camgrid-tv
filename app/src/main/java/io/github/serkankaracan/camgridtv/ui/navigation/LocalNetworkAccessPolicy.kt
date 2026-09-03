package io.github.serkankaracan.camgridtv.ui.navigation

import io.github.serkankaracan.camgridtv.discovery.LocalNetworkPermissionState
import io.github.serkankaracan.camgridtv.ui.discovery.LocalNetworkPermissionUiState

internal data class LocalNetworkAccessDecision(
    val permissionUiState: LocalNetworkPermissionUiState,
    val route: CamGridRoute,
    val allowsLocalWork: Boolean,
    val stopLocalWork: Boolean,
)

/**
 * Keeps Android's API-gated permission result separate from the UI representation.
 *
 * [LocalNetworkPermissionState.NotRequired] must remain an allowed state on pre-37 devices, while
 * an actual denial always owns navigation and stops every local-network operation.
 */
internal object LocalNetworkAccessPolicy {
    fun decide(
        permissionState: LocalNetworkPermissionState,
        requestCompleted: Boolean,
        currentRoute: CamGridRoute,
    ): LocalNetworkAccessDecision =
        when (permissionState) {
            LocalNetworkPermissionState.Granted,
            LocalNetworkPermissionState.NotRequired ->
                LocalNetworkAccessDecision(
                    permissionUiState = LocalNetworkPermissionUiState.Granted,
                    route = currentRoute,
                    allowsLocalWork = true,
                    stopLocalWork = false,
                )
            is LocalNetworkPermissionState.Denied ->
                LocalNetworkAccessDecision(
                    permissionUiState =
                        if (permissionState.shouldShowRationale || !requestCompleted) {
                            LocalNetworkPermissionUiState.RationaleRequired
                        } else {
                            LocalNetworkPermissionUiState.Denied
                        },
                    route = CamGridRoute.Discovery,
                    allowsLocalWork = false,
                    stopLocalWork = true,
                )
        }
}

internal enum class LocalRouteSurface {
    Discovery,
    CameraSetup,
    Wall,
    Fullscreen,
}

/** Rejects delayed UI events after a lifecycle, route, or permission transition. */
internal object LocalRouteActionPolicy {
    fun allows(
        foreground: Boolean,
        permissionUiState: LocalNetworkPermissionUiState,
        currentRoute: CamGridRoute,
        expectedSurface: LocalRouteSurface,
    ): Boolean =
        foreground &&
            permissionUiState == LocalNetworkPermissionUiState.Granted &&
            when (expectedSurface) {
                LocalRouteSurface.Discovery -> currentRoute == CamGridRoute.Discovery
                LocalRouteSurface.CameraSetup -> currentRoute == CamGridRoute.CameraSetup
                LocalRouteSurface.Wall -> currentRoute is CamGridRoute.Wall
                LocalRouteSurface.Fullscreen -> currentRoute is CamGridRoute.Fullscreen
            }
}
