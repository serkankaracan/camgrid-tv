package io.github.serkankaracan.camgridtv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val CamGridColors =
    darkColorScheme(
        primary = CamGridPalette.Primary,
        onPrimary = CamGridPalette.OnPrimary,
        secondary = CamGridPalette.Selection,
        background = CamGridPalette.BackgroundBottom,
        onBackground = CamGridPalette.TextPrimary,
        surface = CamGridPalette.Surface,
        surfaceVariant = CamGridPalette.SurfaceRaised,
        onSurface = CamGridPalette.TextPrimary,
        error = CamGridPalette.Error,
    )

@Composable
fun CamGridTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CamGridColors, content = content)
}
