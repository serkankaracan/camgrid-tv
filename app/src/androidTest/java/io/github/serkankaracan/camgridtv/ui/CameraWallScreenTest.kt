package io.github.serkankaracan.camgridtv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.espresso.Espresso
import io.github.serkankaracan.camgridtv.playback.PlaybackState
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenCameraScreen
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenUiAction
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenUiState
import io.github.serkankaracan.camgridtv.ui.theme.CamGridTheme
import io.github.serkankaracan.camgridtv.ui.wall.CameraWallScreen
import io.github.serkankaracan.camgridtv.ui.wall.CameraWallUiAction
import io.github.serkankaracan.camgridtv.ui.wall.CameraWallUiState
import io.github.serkankaracan.camgridtv.ui.wall.WallCameraUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CameraWallScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val cameras =
        (1..3).map { index ->
            WallCameraUiModel(
                id = "camera-$index",
                displayName = "Camera $index",
                playbackState = PlaybackState.Live,
            )
        }

    @Test
    fun threeCameraWallRendersThreeTiles() {
        composeRule.setContent {
            CamGridTheme {
                CameraWallScreen(state = CameraWallUiState(cameras), onAction = {})
            }
        }

        cameras.forEach { camera ->
            composeRule.onNodeWithTag(UiTestTags.wallCamera(camera.id)).assertIsDisplayed()
        }
    }

    @Test
    fun fakeVideoSurfaceIsHostedForEveryActiveTile() {
        composeRule.setContent {
            CamGridTheme {
                CameraWallScreen(
                    state = CameraWallUiState(cameras),
                    onAction = {},
                    videoSurface = { cameraId, modifier ->
                        Box(modifier = modifier.testTag("fake-video-$cameraId"))
                    },
                )
            }
        }

        cameras.forEach { camera ->
            composeRule
                .onNodeWithTag("fake-video-${camera.id}", useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun dpadMovesAcrossWallRowsAndColumns() {
        val fourCameras =
            (1..4).map { index ->
                WallCameraUiModel(
                    id = "camera-$index",
                    displayName = "Camera $index",
                    playbackState = PlaybackState.Live,
                )
            }
        composeRule.setContent {
            CamGridTheme {
                CameraWallScreen(state = CameraWallUiState(fourCameras), onAction = {})
            }
        }

        val first = composeRule.onNodeWithTag(UiTestTags.wallCamera("camera-1"))
        first.assertIsFocused()
        first.performKeyInput { pressKey(Key.DirectionRight) }
        val second = composeRule.onNodeWithTag(UiTestTags.wallCamera("camera-2"))
        second.assertIsFocused()
        second.performKeyInput { pressKey(Key.DirectionDown) }
        val fourth = composeRule.onNodeWithTag(UiTestTags.wallCamera("camera-4"))
        fourth.assertIsFocused()
        fourth.performKeyInput { pressKey(Key.DirectionLeft) }
        val third = composeRule.onNodeWithTag(UiTestTags.wallCamera("camera-3"))
        third.assertIsFocused()
        third.performKeyInput { pressKey(Key.DirectionUp) }
        first.assertIsFocused()
    }

    @Test
    fun unknownRestoreTargetFallsBackToFirstCamera() {
        composeRule.setContent {
            CamGridTheme {
                CameraWallScreen(
                    state =
                        CameraWallUiState(
                            cameras = cameras,
                            restoreFocusCameraId = "camera-no-longer-selected",
                        ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.wallCamera("camera-1")).assertIsFocused()
    }

    @Test
    fun retryingCameraOffersAnExplicitRescanAction() {
        var receivedAction: CameraWallUiAction? = null
        composeRule.setContent {
            CamGridTheme {
                CameraWallScreen(
                    state =
                        CameraWallUiState(
                            cameras =
                                listOf(
                                    WallCameraUiModel(
                                        id = "camera-1",
                                        displayName = "Camera 1",
                                        playbackState = PlaybackState.Retrying(2, 4_000),
                                    )
                                )
                        ),
                    onAction = { receivedAction = it },
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.WallRescanAction).performClick()
        composeRule.runOnIdle {
            assertEquals(CameraWallUiAction.RescanCameras, receivedAction)
        }
    }

    @Test
    fun okOpensFullscreenAndBackRestoresCameraFocus() {
        composeRule.setContent {
            var fullscreenCameraId by remember { mutableStateOf<String?>(null) }
            var restoreFocusCameraId by remember { mutableStateOf("camera-2") }
            CamGridTheme {
                val fullscreenCamera = cameras.firstOrNull { it.id == fullscreenCameraId }
                if (fullscreenCamera == null) {
                    CameraWallScreen(
                        state =
                            CameraWallUiState(
                                cameras = cameras,
                                restoreFocusCameraId = restoreFocusCameraId,
                            ),
                        onAction = { action ->
                            if (action is CameraWallUiAction.OpenFullscreen) {
                                restoreFocusCameraId = action.cameraId
                                fullscreenCameraId = action.cameraId
                            }
                        },
                    )
                } else {
                    FullscreenCameraScreen(
                        state =
                            FullscreenUiState(
                                cameraId = fullscreenCamera.id,
                                displayName = fullscreenCamera.displayName,
                                playbackState = fullscreenCamera.playbackState,
                            ),
                        onAction = { action ->
                            if (action == FullscreenUiAction.BackToWall) {
                                fullscreenCameraId = null
                            }
                        },
                    )
                }
            }
        }

        val focusedTile = composeRule.onNodeWithTag(UiTestTags.wallCamera("camera-2"))
        focusedTile.assertIsFocused()
        focusedTile.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag(UiTestTags.FullscreenScreen).assertIsDisplayed()

        Espresso.pressBack()
        composeRule.onNodeWithTag(UiTestTags.WallScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.wallCamera("camera-2")).assertIsFocused()
    }
}
