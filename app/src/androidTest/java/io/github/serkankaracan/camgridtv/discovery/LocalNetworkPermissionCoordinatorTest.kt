package io.github.serkankaracan.camgridtv.discovery

import android.Manifest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkPermissionCoordinatorTest {
    @Test
    fun reportsPlatformAppropriatePermissionStateWithoutLaunchingUi() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val coordinator = LocalNetworkPermissionCoordinator(context)

        assertEquals(Manifest.permission.ACCESS_LOCAL_NETWORK, coordinator.permissionName)
        val state = coordinator.state()
        if (Build.VERSION.SDK_INT < LocalNetworkPermissionPolicy.FIRST_RUNTIME_PERMISSION_API) {
            assertEquals(LocalNetworkPermissionState.NotRequired, state)
            assertTrue(coordinator.permissionsToRequest().isEmpty())
        } else {
            assertTrue(
                state is LocalNetworkPermissionState.Granted ||
                    state is LocalNetworkPermissionState.Denied
            )
        }
    }
}
