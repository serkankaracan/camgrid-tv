package io.github.serkankaracan.camgridtv.discovery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Reads local-network permission state but deliberately does not launch a permission request or UI.
 * The UI layer owns the activity result launcher and can use [shouldShowRationale] before
 * requesting.
 */
@SuppressLint(
    "InlinedApi"
) // The permission string is inlined safely; every runtime use is API-gated.
class LocalNetworkPermissionCoordinator(context: Context) {
    private val applicationContext = context.applicationContext

    val permissionName: String = Manifest.permission.ACCESS_LOCAL_NETWORK

    fun state(shouldShowRationale: Boolean = false): LocalNetworkPermissionState {
        val targetSdk = applicationContext.applicationInfo.targetSdkVersion
        val required =
            LocalNetworkPermissionPolicy.requiresRuntimePermission(
                deviceSdk = Build.VERSION.SDK_INT,
                targetSdk = targetSdk,
            )
        if (!required) return LocalNetworkPermissionState.NotRequired

        val granted =
            ContextCompat.checkSelfPermission(applicationContext, permissionName) ==
                PackageManager.PERMISSION_GRANTED
        return LocalNetworkPermissionPolicy.evaluate(
            deviceSdk = Build.VERSION.SDK_INT,
            targetSdk = targetSdk,
            granted = granted,
            shouldShowRationale = shouldShowRationale,
        )
    }

    /**
     * Returns the permission to pass to an Activity Result launcher, or an empty array if unneeded.
     */
    fun permissionsToRequest(): Array<String> =
        if (state() is LocalNetworkPermissionState.Denied) arrayOf(permissionName) else emptyArray()

    /** This only queries Android's rationale signal; it never displays or launches anything. */
    fun shouldShowRationale(activity: Activity): Boolean =
        LocalNetworkPermissionPolicy.requiresRuntimePermission(
            deviceSdk = Build.VERSION.SDK_INT,
            targetSdk = applicationContext.applicationInfo.targetSdkVersion,
        ) &&
            ContextCompat.checkSelfPermission(applicationContext, permissionName) !=
                PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionName)
}

object LocalNetworkPermissionPolicy {
    const val FIRST_RUNTIME_PERMISSION_API = 37

    fun requiresRuntimePermission(deviceSdk: Int, targetSdk: Int): Boolean =
        deviceSdk >= FIRST_RUNTIME_PERMISSION_API && targetSdk >= FIRST_RUNTIME_PERMISSION_API

    fun evaluate(
        deviceSdk: Int,
        targetSdk: Int,
        granted: Boolean,
        shouldShowRationale: Boolean,
    ): LocalNetworkPermissionState {
        if (!requiresRuntimePermission(deviceSdk, targetSdk)) {
            return LocalNetworkPermissionState.NotRequired
        }
        return if (granted) {
            LocalNetworkPermissionState.Granted
        } else {
            LocalNetworkPermissionState.Denied(shouldShowRationale)
        }
    }
}

sealed interface LocalNetworkPermissionState {
    data object NotRequired : LocalNetworkPermissionState

    data object Granted : LocalNetworkPermissionState

    data class Denied(val shouldShowRationale: Boolean) : LocalNetworkPermissionState
}
