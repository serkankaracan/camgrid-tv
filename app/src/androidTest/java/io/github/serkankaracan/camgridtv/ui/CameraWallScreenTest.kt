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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.espresso.Espresso
import io.github.serkankaracan.camgridtv.playback.PlaybackState
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenCameraScreen
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenUiAction
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenUiState
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenViewMode
import io.github.serkankaracan.camgridtv.ui.theme.CamGridDimens
import io.github.serkankaracan.camgridtv.ui.theme.CamGridTheme
import io.github.serkankaracan.camgridtv.ui.wall.CameraWallScreen
import io.github.serkankaracan.camgridtv.ui.wall.CameraWallUiAction
import io.github.serkankaracan.camgridtv.ui.wall.CameraWallUiState
import io.github.serkankaracan.camgridtv.ui.wall.WallCameraUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun liveWallUsesHeaderBadgeWithoutDuplicateBottomStatus() {
        val liveCamera =
            WallCameraUiModel(
                id = "live-camera",
                displayName = "Live Camera",
                playbackState = PlaybackState.Live,
            )
        val connectingCamera =
            WallCameraUiModel(
                id = "connecting-camera",
                displayName = "Connecting Camera",
                playbackState = PlaybackState.Connecting,
            )
        val secondaryPlaybackState = mutableStateOf<PlaybackState>(PlaybackState.Connecting)
        composeRule.setContent {
            CamGridTheme {
                CameraWallScreen(
                    state =
                        CameraWallUiState(
                            listOf(
                                liveCamera,
                                connectingCamera.copy(playbackState = secondaryPlaybackState.value),
                            )
                        ),
                    onAction = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(UiTestTags.wallLiveBadge(liveCamera.id), useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(UiTestTags.playbackStatus(liveCamera.id), useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule
            .onNodeWithTag(UiTestTags.wallLiveBadge(connectingCamera.id), useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule
            .onNodeWithTag(UiTestTags.playbackStatus(connectingCamera.id), useUnmergedTree = true)
            .assertIsDisplayed()

        listOf(
                PlaybackState.Retrying(attempt = 2, nextDelayMillis = 4_000),
                PlaybackState.AuthenticationFailed,
            )
            .forEach { nonLiveState ->
                composeRule.runOnIdle { secondaryPlaybackState.value = nonLiveState }

                composeRule
                    .onNodeWithTag(
                        UiTestTags.wallLiveBadge(connectingCamera.id),
                        useUnmergedTree = true,
                    )
                    .assertDoesNotExist()
                composeRule
                    .onNodeWithTag(
                        UiTestTags.playbackStatus(connectingCamera.id),
                        useUnmergedTree = true,
                    )
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
        composeRule
            .onNodeWithTag(UiTestTags.wallFocusIndicator("camera-1"), useUnmergedTree = true)
            .assertExists()
        first.performKeyInput { pressKey(Key.DirectionRight) }
        val second = composeRule.onNodeWithTag(UiTestTags.wallCamera("camera-2"))
        second.assertIsFocused()
        composeRule
            .onNodeWithTag(UiTestTags.wallFocusIndicator("camera-1"), useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule
            .onNodeWithTag(UiTestTags.wallFocusIndicator("camera-2"), useUnmergedTree = true)
            .assertExists()
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
    fun fullscreenUsesSafeViewportAndDpadCyclesViewModes() {
        val viewMode = mutableStateOf(FullscreenViewMode.SAFE)
        composeRule.setContent {
            CamGridTheme {
                FullscreenCameraScreen(
                    state =
                        FullscreenUiState(
                            cameraId = "camera-1",
                            displayName = "Camera 1",
                            playbackState = PlaybackState.Live,
                            viewMode = viewMode.value,
                        ),
                    onAction = { action ->
                        when (action) {
                            FullscreenUiAction.BackToWall -> Unit
                            FullscreenUiAction.PreviousViewMode ->
                                viewMode.value = viewMode.value.previous()
                            FullscreenUiAction.NextViewMode ->
                                viewMode.value = viewMode.value.next()
                        }
                    },
                    videoSurface = { _, modifier ->
                        Box(modifier = modifier.testTag("fake-fullscreen-video"))
                    },
                )
            }
        }

        val screenBounds =
            composeRule.onNodeWithTag(UiTestTags.FullscreenScreen).getUnclippedBoundsInRoot()
        val safeVideoBounds =
            composeRule.onNodeWithTag("fake-fullscreen-video").getUnclippedBoundsInRoot()
        val cameraName = composeRule.onNodeWithTag(UiTestTags.FullscreenCameraName)
        val playbackStatus = composeRule.onNodeWithTag(UiTestTags.playbackStatus("camera-1"))
        val topControls = composeRule.onNodeWithTag(UiTestTags.FullscreenTopControls)
        val viewModeAction = composeRule.onNodeWithTag(UiTestTags.FullscreenViewModeAction)
        cameraName.assertIsDisplayed().assertTextEquals("Camera 1")
        playbackStatus.assertIsDisplayed()
        topControls.assertIsDisplayed()
        viewModeAction.assertIsDisplayed()
        val cameraNameBounds = cameraName.getUnclippedBoundsInRoot()
        val statusBounds = playbackStatus.getUnclippedBoundsInRoot()
        val topControlsBounds = topControls.getUnclippedBoundsInRoot()
        val viewModeBounds = viewModeAction.getUnclippedBoundsInRoot()
        val screenWidth = (screenBounds.right - screenBounds.left).value
        val screenHeight = (screenBounds.bottom - screenBounds.top).value
        val safeVideoWidth = (safeVideoBounds.right - safeVideoBounds.left).value
        val safeVideoHeight = (safeVideoBounds.bottom - safeVideoBounds.top).value
        val safeHorizontalMargin = screenWidth * 0.05f
        val safeVerticalMargin = screenHeight * 0.05f

        assertEquals(screenWidth * 0.90f, safeVideoWidth, 1.5f)
        assertEquals(screenHeight * 0.90f, safeVideoHeight, 1.5f)
        assertEquals(
            screenBounds.left.value + safeHorizontalMargin,
            safeVideoBounds.left.value,
            1.5f,
        )
        assertEquals(screenBounds.top.value + safeVerticalMargin, safeVideoBounds.top.value, 1.5f)
        assertEquals(
            screenBounds.right.value - safeHorizontalMargin,
            safeVideoBounds.right.value,
            1.5f,
        )
        assertEquals(
            screenBounds.bottom.value - safeVerticalMargin,
            safeVideoBounds.bottom.value,
            1.5f,
        )
        val screenHorizontalCenter = screenBounds.left.value + screenWidth / 2f
        val screenVerticalCenter = screenBounds.top.value + screenHeight / 2f
        val safeHorizontal = CamGridDimens.SafeHorizontal.value
        val safeVertical = CamGridDimens.SafeVertical.value
        assertTrue(cameraNameBounds.left.value >= screenHorizontalCenter)
        assertTrue(cameraNameBounds.top.value >= screenVerticalCenter)
        assertTrue(cameraNameBounds.right.value <= screenBounds.right.value - safeHorizontal + 1f)
        assertTrue(cameraNameBounds.bottom.value <= screenBounds.bottom.value - safeVertical + 1f)
        assertTrue(statusBounds.left.value >= screenHorizontalCenter)
        assertTrue(statusBounds.bottom.value <= screenVerticalCenter)
        assertTrue(statusBounds.top.value >= screenBounds.top.value + safeVertical - 1f)
        assertTrue(statusBounds.right.value <= screenBounds.right.value - safeHorizontal + 1f)
        assertTrue(topControlsBounds.left.value >= screenHorizontalCenter)
        assertTrue(topControlsBounds.bottom.value <= screenVerticalCenter)
        assertTrue(statusBounds.bottom.value <= viewModeBounds.top.value)
        viewModeAction.assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.runOnIdle { assertEquals(FullscreenViewMode.FIT, viewMode.value) }
        val fitVideoBounds =
            composeRule.onNodeWithTag("fake-fullscreen-video").getUnclippedBoundsInRoot()
        assertEquals(screenBounds.left.value, fitVideoBounds.left.value, 1.5f)
        assertEquals(screenBounds.top.value, fitVideoBounds.top.value, 1.5f)
        assertEquals(screenBounds.right.value, fitVideoBounds.right.value, 1.5f)
        assertEquals(screenBounds.bottom.value, fitVideoBounds.bottom.value, 1.5f)

        viewModeAction.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(FullscreenViewMode.FILL, viewMode.value) }

        viewModeAction.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.runOnIdle { assertEquals(FullscreenViewMode.SAFE, viewMode.value) }
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

        val retryingTile = composeRule.onNodeWithTag(UiTestTags.wallCamera("camera-1"))
        val rescanAction = composeRule.onNodeWithTag(UiTestTags.WallRescanAction)
        retryingTile.assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        rescanAction.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
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
