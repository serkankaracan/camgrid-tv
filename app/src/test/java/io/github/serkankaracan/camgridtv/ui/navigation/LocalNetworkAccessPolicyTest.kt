package io.github.serkankaracan.camgridtv.ui.navigation

import io.github.serkankaracan.camgridtv.discovery.LocalNetworkPermissionState
import io.github.serkankaracan.camgridtv.ui.discovery.LocalNetworkPermissionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkAccessPolicyTest {
    @Test
    fun `denial stops local work and returns every active route to permission discovery`() {
        val activeRoutes =
            listOf(
                CamGridRoute.CameraSetup,
                CamGridRoute.Wall(),
                CamGridRoute.Fullscreen("camera"),
            )

        activeRoutes.forEach { route ->
            val decision =
                LocalNetworkAccessPolicy.decide(
                    permissionState =
                        LocalNetworkPermissionState.Denied(shouldShowRationale = true),
                    requestCompleted = true,
                    currentRoute = route,
                )

            assertEquals(CamGridRoute.Discovery, decision.route)
            assertEquals(
                LocalNetworkPermissionUiState.RationaleRequired,
                decision.permissionUiState,
            )
            assertFalse(decision.allowsLocalWork)
            assertTrue(decision.stopLocalWork)
        }
    }

    @Test
    fun `completed denial shows settings state and cannot resume foreground work`() {
        val decision =
            LocalNetworkAccessPolicy.decide(
                permissionState = LocalNetworkPermissionState.Denied(shouldShowRationale = false),
                requestCompleted = true,
                currentRoute = CamGridRoute.Wall(),
            )

        assertEquals(LocalNetworkPermissionUiState.Denied, decision.permissionUiState)
        assertEquals(CamGridRoute.Discovery, decision.route)
        assertFalse(decision.allowsLocalWork)
        assertTrue(decision.stopLocalWork)
    }

    @Test
    fun `first denial remains requestable even without Android rationale`() {
        val decision =
            LocalNetworkAccessPolicy.decide(
                permissionState = LocalNetworkPermissionState.Denied(shouldShowRationale = false),
                requestCompleted = false,
                currentRoute = CamGridRoute.Discovery,
            )

        assertEquals(
            LocalNetworkPermissionUiState.RationaleRequired,
            decision.permissionUiState,
        )
        assertFalse(decision.allowsLocalWork)
    }

    @Test
    fun `pre 37 not required state preserves route and foreground playback behavior`() {
        val route = CamGridRoute.Fullscreen("camera")
        val decision =
            LocalNetworkAccessPolicy.decide(
                permissionState = LocalNetworkPermissionState.NotRequired,
                requestCompleted = false,
                currentRoute = route,
            )

        assertEquals(LocalNetworkPermissionUiState.Granted, decision.permissionUiState)
        assertEquals(route, decision.route)
        assertTrue(decision.allowsLocalWork)
        assertFalse(decision.stopLocalWork)
    }

    @Test
    fun `granted permission preserves setup route and allows local work`() {
        val decision =
            LocalNetworkAccessPolicy.decide(
                permissionState = LocalNetworkPermissionState.Granted,
                requestCompleted = true,
                currentRoute = CamGridRoute.CameraSetup,
            )

        assertEquals(CamGridRoute.CameraSetup, decision.route)
        assertTrue(decision.allowsLocalWork)
        assertFalse(decision.stopLocalWork)
    }

    @Test
    fun `route actions require foreground granted permission and the current surface`() {
        val validRoutes =
            listOf(
                LocalRouteSurface.Discovery to CamGridRoute.Discovery,
                LocalRouteSurface.CameraSetup to CamGridRoute.CameraSetup,
                LocalRouteSurface.Wall to CamGridRoute.Wall(),
                LocalRouteSurface.Fullscreen to CamGridRoute.Fullscreen("camera"),
            )

        validRoutes.forEach { (surface, route) ->
            assertTrue(
                LocalRouteActionPolicy.allows(
                    foreground = true,
                    permissionUiState = LocalNetworkPermissionUiState.Granted,
                    currentRoute = route,
                    expectedSurface = surface,
                )
            )
            assertFalse(
                LocalRouteActionPolicy.allows(
                    foreground = false,
                    permissionUiState = LocalNetworkPermissionUiState.Granted,
                    currentRoute = route,
                    expectedSurface = surface,
                )
            )
            assertFalse(
                LocalRouteActionPolicy.allows(
                    foreground = true,
                    permissionUiState = LocalNetworkPermissionUiState.Denied,
                    currentRoute = route,
                    expectedSurface = surface,
                )
            )
            assertFalse(
                LocalRouteActionPolicy.allows(
                    foreground = true,
                    permissionUiState = LocalNetworkPermissionUiState.Granted,
                    currentRoute = CamGridRoute.Discovery,
                    expectedSurface = LocalRouteSurface.Fullscreen,
                )
            )
        }
    }
}
