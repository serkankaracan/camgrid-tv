package io.github.serkankaracan.camgridtv.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.playback.PlaybackState

@Composable
fun PlaybackStatusOverlay(
    cameraId: String,
    state: PlaybackState,
    modifier: Modifier = Modifier,
) {
    val presentation = playbackStatusPresentation(state) ?: return
    Text(
        text = stringResource(presentation.labelRes),
        modifier =
            modifier
                .background(presentation.background.copy(alpha = 0.88f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag(UiTestTags.playbackStatus(cameraId)),
        color = Color.White,
        fontSize = 16.sp,
    )
}

@Composable
fun ConnectionStatusLabel(
    cameraId: String,
    labelRes: Int,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(labelRes),
        modifier = modifier.testTag(UiTestTags.connectionStatus(cameraId)),
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        fontSize = 16.sp,
    )
}

private data class PlaybackStatusPresentation(
    @param:StringRes val labelRes: Int,
    val background: Color,
)

private fun playbackStatusPresentation(state: PlaybackState): PlaybackStatusPresentation? =
    when (state) {
        PlaybackState.Idle -> null
        PlaybackState.Connecting ->
            PlaybackStatusPresentation(R.string.connecting, Color(0xFF23415D))
        PlaybackState.Live -> PlaybackStatusPresentation(R.string.live, Color(0xFF12634F))
        is PlaybackState.Retrying ->
            PlaybackStatusPresentation(R.string.retrying, Color(0xFF6B4C12))
        PlaybackState.AuthenticationFailed ->
            PlaybackStatusPresentation(R.string.auth_failed, Color(0xFF852F36))
        PlaybackState.Offline -> PlaybackStatusPresentation(R.string.offline, Color(0xFF5A6470))
        PlaybackState.UnsupportedStream ->
            PlaybackStatusPresentation(R.string.unsupported_stream, Color(0xFF852F36))
        PlaybackState.DecoderResourceExhausted ->
            PlaybackStatusPresentation(R.string.decoder_limit, Color(0xFF852F36))
        is PlaybackState.PlaybackFailed ->
            PlaybackStatusPresentation(R.string.playback_failed, Color(0xFF852F36))
    }
