package io.github.serkankaracan.camgridtv.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

typealias CameraVideoSurface = @Composable (cameraId: String, modifier: Modifier) -> Unit

@Composable
fun EmptyCameraVideoSurface(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color.Black))
}

/** Hosts a Media3 player without accepting, displaying, or logging its source URI. */
@OptIn(markerClass = [UnstableApi::class])
@Composable
fun Media3PlayerViewHost(
    player: Player?,
    modifier: Modifier = Modifier,
    keepScreenOn: Boolean = true,
) {
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    DisposableEffect(Unit) { onDispose { playerView?.player = null } }

    AndroidView(
        factory = { context ->
            PlayerView(context).also { view ->
                view.useController = false
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                view.keepScreenOn = keepScreenOn
                view.player = player
                playerView = view
            }
        },
        update = { view ->
            view.keepScreenOn = keepScreenOn
            if (view.player !== player) view.player = player
        },
        modifier = modifier.fillMaxSize(),
    )
}
