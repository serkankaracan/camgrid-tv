package io.github.serkankaracan.camgridtv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared control-room palette used for status and non-Material surfaces. */
object CamGridPalette {
    val BackgroundTop = Color(0xFF08111B)
    val BackgroundBottom = Color(0xFF03070C)
    val Surface = Color(0xFF0D1722)
    val SurfaceRaised = Color(0xFF142231)
    val FocusedSurface = Color(0xFF183141)
    val Primary = Color(0xFF49DED4)
    val OnPrimary = Color(0xFF00201D)
    val Selection = Color(0xFFFFBC57)
    val TextPrimary = Color(0xFFF4F7FA)
    val TextMuted = Color(0xFF9FB0C2)
    val Outline = Color(0xFF304255)
    val Success = Color(0xFF43D79A)
    val Error = Color(0xFFFF6B7D)
    val Warning = Color(0xFFFFBC57)
    val Scrim = Color(0xE60A1018)
}

object CamGridDimens {
    val SafeHorizontal = 48.dp
    val SafeVertical = 27.dp
    val ScreenHorizontal = 56.dp
    val ScreenVertical = 28.dp
    val PanelGap = 16.dp
    val PanelRadius = 14.dp
    val ControlRadius = 10.dp
}
