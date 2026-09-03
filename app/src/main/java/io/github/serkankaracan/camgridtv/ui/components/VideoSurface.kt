package io.github.serkankaracan.camgridtv.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW

typealias CameraVideoSurface = @Composable (cameraId: String, modifier: Modifier) -> Unit

@Composable
fun EmptyCameraVideoSurface(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color.Black))
}

/** Hosts a Media3 player without accepting, displaying, or logging its source URI. */
@OptIn(markerClass = [UnstableApi::class])
@Composable
fun Media3VideoSurface(
    player: Player?,
    modifier: Modifier = Modifier,
    keepScreenOn: Boolean = true,
    surfaceKey: Long? = null,
    contentScale: ContentScale = ContentScale.Fit,
    useTextureView: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        key(surfaceKey) {
            ContentFrame(
                player = player,
                modifier =
                    Modifier.fillMaxSize()
                        .then(if (keepScreenOn) Modifier.keepScreenOn() else Modifier),
                surfaceType =
                    if (useTextureView) {
                        SURFACE_TYPE_TEXTURE_VIEW
                    } else {
                        SURFACE_TYPE_SURFACE_VIEW
                    },
                contentScale = contentScale,
                keepContentOnReset = false,
            )
        }
    }
}
