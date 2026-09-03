package io.github.serkankaracan.camgridtv.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.serkankaracan.camgridtv.app.CamGridApplication
import io.github.serkankaracan.camgridtv.playback.PlaybackState
import io.github.serkankaracan.camgridtv.ui.components.Media3PlayerViewHost
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryScreen
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryUiAction
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenCameraScreen
import io.github.serkankaracan.camgridtv.ui.navigation.CamGridRoute
import io.github.serkankaracan.camgridtv.ui.setup.CameraSetupScreen
import io.github.serkankaracan.camgridtv.ui.wall.CameraWallScreen

@Composable
fun CamGridApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as CamGridApplication
    val factory = remember(application) { CamGridViewModel.Factory(application.container) }
    val appViewModel: CamGridViewModel = viewModel(factory = factory)
    CamGridApp(viewModel = appViewModel, modifier = modifier)
}

@Composable
fun CamGridApp(
    viewModel: CamGridViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            val rationale = activity?.let(viewModel::shouldShowPermissionRationale) ?: false
            viewModel.refreshPermission(
                shouldShowRationale = rationale,
                requestCompleted = true,
            )
        }

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    val rationale = activity?.let(viewModel::shouldShowPermissionRationale) ?: false
                    viewModel.onForeground(shouldShowRationale = rationale)
                }
                Lifecycle.Event.ON_STOP -> viewModel.onBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            val rationale = activity?.let(viewModel::shouldShowPermissionRationale) ?: false
            viewModel.onForeground(shouldShowRationale = rationale)
        } else {
            viewModel.onBackground()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onBackground()
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag(UiTestTags.AppRoot)) {
        when (state.route) {
            CamGridRoute.Discovery ->
                DiscoveryScreen(
                    state = state.discovery,
                    onAction = { action ->
                        when (action) {
                            DiscoveryUiAction.RequestPermission -> {
                                val permission = viewModel.permissionToRequest()
                                if (permission == null) {
                                    viewModel.refreshPermission(shouldShowRationale = false)
                                } else {
                                    permissionLauncher.launch(permission)
                                }
                            }
                            DiscoveryUiAction.OpenAppSettings -> context.openAppSettings()
                            else -> viewModel.onDiscoveryAction(action)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            CamGridRoute.CameraSetup ->
                CameraSetupScreen(
                    state = state.cameraSetup,
                    onAction = viewModel::onCameraSetupAction,
                    modifier = Modifier.fillMaxSize(),
                    videoSurface = { cameraId, surfaceModifier ->
                        val player = viewModel.connectionPreviewPlayerFor(cameraId)
                        Media3PlayerViewHost(
                            player = player,
                            modifier = surfaceModifier,
                            keepScreenOn = false,
                        )
                    },
                )
            is CamGridRoute.Wall ->
                CameraWallScreen(
                    state = state.wall,
                    onAction = viewModel::onCameraWallAction,
                    modifier = Modifier.fillMaxSize(),
                    videoSurface = { cameraId, surfaceModifier ->
                        val player = viewModel.playerFor(cameraId)
                        Media3PlayerViewHost(
                            player = player,
                            modifier = surfaceModifier,
                            keepScreenOn =
                                state.wall.cameras.any { camera ->
                                    camera.id == cameraId &&
                                        camera.playbackState == PlaybackState.Live
                                },
                        )
                    },
                )
            is CamGridRoute.Fullscreen -> {
                val fullscreen = state.fullscreen
                if (fullscreen != null) {
                    FullscreenCameraScreen(
                        state = fullscreen,
                        onAction = viewModel::onFullscreenAction,
                        modifier = Modifier.fillMaxSize(),
                        videoSurface = { cameraId, surfaceModifier ->
                            val player = viewModel.playerFor(cameraId)
                            Media3PlayerViewHost(
                                player = player,
                                modifier = surfaceModifier,
                                keepScreenOn = fullscreen.playbackState == PlaybackState.Live,
                            )
                        },
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.openAppSettings() {
    startActivity(
        Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
