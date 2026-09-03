package io.github.serkankaracan.camgridtv.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkPermissionPolicyTest {
    @Test
    fun `permission is runtime gated by both device and target API 37`() {
        assertFalse(LocalNetworkPermissionPolicy.requiresRuntimePermission(36, 37))
        assertFalse(LocalNetworkPermissionPolicy.requiresRuntimePermission(37, 36))
        assertTrue(LocalNetworkPermissionPolicy.requiresRuntimePermission(37, 37))
        assertTrue(LocalNetworkPermissionPolicy.requiresRuntimePermission(38, 38))
    }

    @Test
    fun `older platform is not required regardless of grant signal`() {
        assertEquals(
            LocalNetworkPermissionState.NotRequired,
            LocalNetworkPermissionPolicy.evaluate(
                deviceSdk = 36,
                targetSdk = 37,
                granted = false,
                shouldShowRationale = true,
            ),
        )
    }

    @Test
    fun `required permission distinguishes grant and rationale`() {
        assertEquals(
            LocalNetworkPermissionState.Granted,
            LocalNetworkPermissionPolicy.evaluate(
                37,
                37,
                granted = true,
                shouldShowRationale = false,
            ),
        )
        assertEquals(
            LocalNetworkPermissionState.Denied(shouldShowRationale = true),
            LocalNetworkPermissionPolicy.evaluate(
                37,
                37,
                granted = false,
                shouldShowRationale = true,
            ),
        )
    }
}
