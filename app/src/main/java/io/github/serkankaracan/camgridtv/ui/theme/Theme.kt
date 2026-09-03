package io.github.serkankaracan.camgridtv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val CamGridColors =
    darkColorScheme(
        primary = Color(0xFF45D6CF),
        onPrimary = Color(0xFF021313),
        secondary = Color(0xFFFFB52E),
        background = Color(0xFF07111F),
        onBackground = Color(0xFFF1F6FA),
        surface = Color(0xFF101D2D),
        onSurface = Color(0xFFF1F6FA),
        error = Color(0xFFFF6B6B),
    )

@Composable
fun CamGridTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CamGridColors, content = content)
}
