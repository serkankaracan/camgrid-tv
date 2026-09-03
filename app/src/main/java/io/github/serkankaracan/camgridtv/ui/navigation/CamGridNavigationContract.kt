package io.github.serkankaracan.camgridtv.ui.navigation

sealed interface CamGridRoute {
    data object Discovery : CamGridRoute

    data object CameraSetup : CamGridRoute

    data class Wall(val restoreFocusCameraId: String? = null) : CamGridRoute

    data class Fullscreen(val cameraId: String) : CamGridRoute {
        init {
            require(cameraId.isNotBlank()) { "Fullscreen camera id is required" }
        }
    }
}

sealed interface CamGridNavigationAction {
    data object OpenCameraSetup : CamGridNavigationAction

    data object OpenWall : CamGridNavigationAction

    data class OpenFullscreen(val cameraId: String) : CamGridNavigationAction

    data object Back : CamGridNavigationAction
}
