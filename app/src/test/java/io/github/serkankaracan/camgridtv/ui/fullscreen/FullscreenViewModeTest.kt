package io.github.serkankaracan.camgridtv.ui.fullscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenViewModeTest {
    @Test
    fun `safe mode is the default aspect preserving overscan viewport`() {
        val state =
            FullscreenUiState(
                cameraId = "camera",
                displayName = "Camera",
                playbackState = io.github.serkankaracan.camgridtv.playback.PlaybackState.Live,
            )

        assertEquals(FullscreenViewMode.SAFE, state.viewMode)
        assertEquals(0.90f, state.viewMode.viewportFraction)
        assertFalse(state.viewMode.cropsToFill)
    }

    @Test
    fun `view modes cycle in both directions`() {
        assertEquals(FullscreenViewMode.FIT, FullscreenViewMode.SAFE.next())
        assertEquals(FullscreenViewMode.FILL, FullscreenViewMode.FIT.next())
        assertEquals(FullscreenViewMode.SAFE, FullscreenViewMode.FILL.next())

        assertEquals(FullscreenViewMode.FILL, FullscreenViewMode.SAFE.previous())
        assertEquals(FullscreenViewMode.FIT, FullscreenViewMode.FILL.previous())
        assertEquals(FullscreenViewMode.SAFE, FullscreenViewMode.FIT.previous())
    }

    @Test
    fun `fill is the only cropping mode and viewport fractions are valid`() {
        assertFalse(FullscreenViewMode.SAFE.cropsToFill)
        assertFalse(FullscreenViewMode.FIT.cropsToFill)
        assertTrue(FullscreenViewMode.FILL.cropsToFill)
        FullscreenViewMode.entries.forEach { mode ->
            assertTrue(mode.viewportFraction in 0f..1f)
        }
    }
}
