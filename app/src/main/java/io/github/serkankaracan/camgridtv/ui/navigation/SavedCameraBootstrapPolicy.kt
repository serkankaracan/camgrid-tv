package io.github.serkankaracan.camgridtv.ui.navigation

import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import io.github.serkankaracan.camgridtv.model.CameraDevice

sealed interface SavedCameraBootstrapDecision {
    data object AwaitConfiguration : SavedCameraBootstrapDecision

    data object AwaitPermission : SavedCameraBootstrapDecision

    data object Skip : SavedCameraBootstrapDecision

    data class OpenWall(val selectedCameras: List<CameraDevice>) : SavedCameraBootstrapDecision
}

/**
 * Selects the one-time startup action for persisted cameras.
 *
 * This policy deliberately does not depend on transient in-memory connection tests. A saved wall
 * can open as soon as configuration and local-network permission are ready, provided every selected
 * camera has a credential profile reference.
 */
object SavedCameraBootstrapPolicy {
    fun decide(
        configuration: CameraConfiguration?,
        permissionGranted: Boolean,
        alreadyHandled: Boolean,
    ): SavedCameraBootstrapDecision {
        if (alreadyHandled) return SavedCameraBootstrapDecision.Skip
        if (configuration == null) return SavedCameraBootstrapDecision.AwaitConfiguration
        if (!permissionGranted) return SavedCameraBootstrapDecision.AwaitPermission

        val selectedCameras = configuration.selectedCameras()
        return if (
            selectedCameras.isNotEmpty() && selectedCameras.all { it.credentialProfileId != null }
        ) {
            SavedCameraBootstrapDecision.OpenWall(selectedCameras)
        } else {
            SavedCameraBootstrapDecision.Skip
        }
    }
}

/**
 * Tracks the single startup-wall attempt without letting a cancelled job affect a newer attempt.
 *
 * The token is intentionally owned by the caller. Lifecycle cancellation invalidates the active
 * token, so a later foreground transition may retry immediately; a stale job can then only cancel
 * its own token and cannot clear the replacement attempt.
 */
internal class SavedCameraBootstrapAttemptGate {
    private var nextToken = 0L
    private var activeToken: Long? = null

    var handled: Boolean = false
        private set

    fun tryStart(): Long? {
        if (handled || activeToken != null) return null
        return (++nextToken).also { activeToken = it }
    }

    fun complete(token: Long): Boolean {
        if (activeToken != token) return false
        activeToken = null
        handled = true
        return true
    }

    fun cancel(token: Long): Boolean {
        if (activeToken != token) return false
        activeToken = null
        return true
    }

    fun cancelActive() {
        activeToken = null
    }

    fun skip() {
        activeToken = null
        handled = true
    }
}
