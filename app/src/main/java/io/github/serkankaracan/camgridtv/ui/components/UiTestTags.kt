package io.github.serkankaracan.camgridtv.ui.components

object UiTestTags {
    const val AppRoot = "app_root"
    const val PermissionScreen = "permission_screen"
    const val PermissionPrimaryAction = "permission_primary_action"
    const val DiscoveryScreen = "discovery_screen"
    const val DiscoveryScanAction = "discovery_scan_action"
    const val DiscoverySelectedCount = "discovery_selected_count"
    const val DiscoveryContinueAction = "discovery_continue_action"
    const val SetupScreen = "setup_screen"
    const val UsernameField = "username_field"
    const val PasswordField = "password_field"
    const val CredentialRecoveryPanel = "credential_recovery_panel"
    const val ClearStoredCredentialsAction = "clear_stored_credentials_action"
    const val SharedProfileToggle = "shared_profile_toggle"
    const val StartWatchingAction = "start_watching_action"
    const val SetupReadiness = "setup_readiness"
    const val SetupCameraList = "setup_camera_list"
    const val SetupConnectionPreview = "setup_connection_preview"
    const val SetupConnectionPreviewStatus = "setup_connection_preview_status"
    const val WallScreen = "wall_screen"
    const val WallRescanAction = "wall_rescan_action"
    const val FullscreenScreen = "fullscreen_screen"
    const val FullscreenCameraName = "fullscreen_camera_name"
    const val FullscreenTopControls = "fullscreen_top_controls"
    const val FullscreenViewModeAction = "fullscreen_view_mode_action"

    fun discoveryCamera(cameraId: String): String = "discovery_camera_${safeId(cameraId)}"

    fun setupCamera(cameraId: String): String = "setup_camera_${safeId(cameraId)}"

    fun editCamera(cameraId: String): String = "edit_camera_${safeId(cameraId)}"

    fun testConnection(cameraId: String): String = "test_connection_${safeId(cameraId)}"

    fun connectionStatus(cameraId: String): String = "connection_status_${safeId(cameraId)}"

    fun connectionPreview(cameraId: String): String = "connection_preview_${safeId(cameraId)}"

    fun wallCamera(cameraId: String): String = "wall_camera_${safeId(cameraId)}"

    fun wallLiveBadge(cameraId: String): String = "wall_live_badge_${safeId(cameraId)}"

    fun wallFocusIndicator(cameraId: String): String = "wall_focus_indicator_${safeId(cameraId)}"

    fun playbackStatus(cameraId: String): String = "playback_status_${safeId(cameraId)}"

    private fun safeId(value: String): String =
        value
            .map { character -> if (character.isLetterOrDigit()) character else '_' }
            .joinToString("")
}
