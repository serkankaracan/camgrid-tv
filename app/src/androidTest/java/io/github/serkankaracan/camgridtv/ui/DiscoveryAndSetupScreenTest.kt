package io.github.serkankaracan.camgridtv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryCameraUiModel
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryContentUiState
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryErrorUiState
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryScreen
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryUiAction
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryUiState
import io.github.serkankaracan.camgridtv.ui.discovery.LocalNetworkPermissionUiState
import io.github.serkankaracan.camgridtv.ui.setup.CameraSetupScreen
import io.github.serkankaracan.camgridtv.ui.setup.CameraSetupUiAction
import io.github.serkankaracan.camgridtv.ui.setup.CameraSetupUiState
import io.github.serkankaracan.camgridtv.ui.setup.ConnectionTestUiState
import io.github.serkankaracan.camgridtv.ui.setup.CredentialRecoveryUiState
import io.github.serkankaracan.camgridtv.ui.setup.SetupCameraUiModel
import io.github.serkankaracan.camgridtv.ui.theme.CamGridTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiscoveryAndSetupScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun permissionExplanationOwnsInitialFocus() {
        composeRule.setContent {
            CamGridTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            permission = LocalNetworkPermissionUiState.RationaleRequired
                        ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.PermissionScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.PermissionPrimaryAction).assertIsFocused()
    }

    @Test
    fun emptyAndNetworkErrorStatesOfferFocusedRetry() {
        val content = mutableStateOf<DiscoveryContentUiState>(DiscoveryContentUiState.Loading)
        composeRule.setContent {
            CamGridTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            permission = LocalNetworkPermissionUiState.Granted,
                            content = content.value,
                        ),
                    onAction = {},
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.scanning)).assertIsDisplayed()
        composeRule.runOnIdle { content.value = DiscoveryContentUiState.Empty }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(UiTestTags.DiscoveryScanAction).assertIsFocused()
        composeRule.runOnIdle {
            content.value =
                DiscoveryContentUiState.Error(DiscoveryErrorUiState.NoActiveLocalNetwork)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(UiTestTags.DiscoveryScanAction).assertIsFocused()
    }

    @Test
    fun oneTwoAndFourCameraResultSetsRenderWithoutTouch() {
        val cameras = mutableStateOf(cameraFixtures(1))
        composeRule.setContent {
            CamGridTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            permission = LocalNetworkPermissionUiState.Granted,
                            content = DiscoveryContentUiState.Results(cameras.value),
                        ),
                    onAction = {},
                )
            }
        }

        listOf(1, 2, 4).forEach { count ->
            composeRule.runOnIdle { cameras.value = cameraFixtures(count) }
            composeRule.waitForIdle()
            (1..count).forEach { index ->
                composeRule
                    .onNodeWithTag(UiTestTags.discoveryCamera("camera-$index"))
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun dpadMovesFocusBetweenDiscoveryResults() {
        val cameras = cameraFixtures(3)
        composeRule.setContent {
            CamGridTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            permission = LocalNetworkPermissionUiState.Granted,
                            content = DiscoveryContentUiState.Results(cameras),
                        ),
                    onAction = {},
                )
            }
        }

        val first = composeRule.onNodeWithTag(UiTestTags.discoveryCamera("camera-1"))
        first.assertIsFocused()
        first.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithTag(UiTestTags.discoveryCamera("camera-2")).assertIsFocused()
    }

    @Test
    fun threeCamerasCanBeSelected() {
        val initialCameras = cameraFixtures(3)
        composeRule.setContent {
            var cameras by remember { mutableStateOf(initialCameras) }
            CamGridTheme {
                DiscoveryScreen(
                    state =
                        DiscoveryUiState(
                            permission = LocalNetworkPermissionUiState.Granted,
                            content = DiscoveryContentUiState.Results(cameras),
                        ),
                    onAction = { action ->
                        if (action is DiscoveryUiAction.CameraSelectionChanged) {
                            cameras = cameras.map { camera ->
                                if (camera.id == action.cameraId) {
                                    camera.copy(selected = action.selected)
                                } else {
                                    camera
                                }
                            }
                        }
                    },
                )
            }
        }

        initialCameras.forEach { camera ->
            composeRule.onNodeWithTag(UiTestTags.discoveryCamera(camera.id)).performClick()
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule
            .onNodeWithTag(UiTestTags.DiscoverySelectedCount)
            .assertTextEquals(
                context.resources.getQuantityString(R.plurals.selected_camera_count, 3, 3)
            )
    }

    @Test
    fun authenticationAndOfflineStatusesUseSafeLabels() {
        composeRule.setContent {
            CamGridTheme {
                CameraSetupScreen(
                    state =
                        CameraSetupUiState(
                            cameras =
                                listOf(
                                    SetupCameraUiModel(
                                        id = "auth",
                                        displayName = "Camera A",
                                        connectionState =
                                            ConnectionTestUiState.AuthenticationFailed,
                                    ),
                                    SetupCameraUiModel(
                                        id = "offline",
                                        displayName = "Camera B",
                                        connectionState = ConnectionTestUiState.Offline,
                                    ),
                                )
                        ),
                    onAction = {},
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule
            .onNodeWithTag(UiTestTags.connectionStatus("auth"))
            .assertTextEquals(context.getString(R.string.auth_failed))
        composeRule
            .onNodeWithTag(UiTestTags.connectionStatus("offline"))
            .assertTextEquals(context.getString(R.string.offline))
    }

    @Test
    fun passwordFieldIsMarkedSensitiveAndVisuallyTransformed() {
        val password = mutableStateOf(listOf("screen", "-", "fixture").joinToString(""))
        composeRule.setContent {
            CamGridTheme {
                CameraSetupScreen(
                    state =
                        CameraSetupUiState(
                            cameras = emptyList(),
                            password = password.value,
                        ),
                    onAction = {},
                )
            }
        }

        val passwordField = composeRule.onNodeWithTag(UiTestTags.PasswordField)
        passwordField.assert(SemanticsMatcher.expectValue(SemanticsProperties.Password, Unit))
        val firstRendering = passwordField.captureToImage().asAndroidBitmap()

        composeRule.runOnIdle {
            password.value = listOf("masked", "-", "content").joinToString("")
        }
        composeRule.waitForIdle()
        val secondRendering = passwordField.captureToImage().asAndroidBitmap()

        assertTrue(
            "Equal-length passwords must render identically when masking is active",
            firstRendering.sameAs(secondRendering),
        )
    }

    @Test
    fun credentialRecoveryRequiresExplicitCleanupBeforeReentry() {
        var clearRequested = false
        composeRule.setContent {
            CamGridTheme {
                CameraSetupScreen(
                    state =
                        CameraSetupUiState(
                            cameras = emptyList(),
                            credentialRecovery = CredentialRecoveryUiState.Required,
                        ),
                    onAction = { action ->
                        if (action == CameraSetupUiAction.ClearStoredCredentials) {
                            clearRequested = true
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.CredentialRecoveryPanel).assertExists()
        composeRule.onNodeWithTag(UiTestTags.UsernameField).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.PasswordField).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.ClearStoredCredentialsAction).performClick()

        composeRule.runOnIdle { assertTrue(clearRequested) }
    }

    @Test
    fun activeConnectionTestLocksCredentialsAndAdditionalTests() {
        composeRule.setContent {
            CamGridTheme {
                CameraSetupScreen(
                    state =
                        CameraSetupUiState(
                            cameras =
                                listOf(
                                    SetupCameraUiModel(
                                        id = "testing",
                                        displayName = "Testing camera",
                                        connectionState = ConnectionTestUiState.Testing,
                                    ),
                                    SetupCameraUiModel(
                                        id = "other",
                                        displayName = "Other camera",
                                    ),
                                )
                        ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.UsernameField).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.PasswordField).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.setupCamera("testing")).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.setupCamera("other")).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.testConnection("testing")).assertIsEnabled()
        composeRule.onNodeWithTag(UiTestTags.testConnection("other")).assertIsNotEnabled()
    }

    @Test
    fun pendingSelectionWriteLocksSetupActionsUntilRepositoryStateCatchesUp() {
        val initialCameras =
            listOf(
                SetupCameraUiModel(id = "pending", displayName = "Pending camera"),
                SetupCameraUiModel(id = "other", displayName = "Other camera"),
            )
        val setupState = mutableStateOf(CameraSetupUiState(cameras = initialCameras))
        composeRule.setContent {
            CamGridTheme {
                CameraSetupScreen(
                    state = setupState.value,
                    onAction = { action ->
                        if (
                            action == CameraSetupUiAction.CameraSelectionChanged("pending", false)
                        ) {
                            setupState.value =
                                CameraSetupUiState(
                                    cameras = initialCameras,
                                    canStartWatching = true,
                                    selectionUpdateCameraId = "pending",
                                )
                        }
                    },
                )
            }
        }

        val pendingCard = composeRule.onNodeWithTag(UiTestTags.setupCamera("pending"))
        pendingCard.performSemanticsAction(SemanticsActions.RequestFocus)
        pendingCard.assertIsFocused().performClick().assertIsFocused().assertIsEnabled()

        composeRule.onNodeWithTag(UiTestTags.setupCamera("other")).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.editCamera("pending")).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.testConnection("pending")).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.UsernameField).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.StartWatchingAction).assertIsNotEnabled()

        composeRule.runOnIdle {
            setupState.value =
                CameraSetupUiState(
                    cameras =
                        initialCameras.map { camera ->
                            if (camera.id == "pending") camera.copy(selected = false) else camera
                        }
                )
        }

        composeRule.onNodeWithTag(UiTestTags.setupCamera("pending")).assertIsFocused()
    }

    @Test
    fun pendingSharedProfileWriteLocksTestAndRetainsToggleFocus() {
        val cameras =
            listOf(
                SetupCameraUiModel(id = "camera", displayName = "Camera"),
                SetupCameraUiModel(id = "other", displayName = "Other camera"),
            )
        val setupState = mutableStateOf(CameraSetupUiState(cameras = cameras))
        composeRule.setContent {
            CamGridTheme {
                CameraSetupScreen(
                    state = setupState.value,
                    onAction = { action ->
                        if (action == CameraSetupUiAction.SharedProfileChanged(false)) {
                            setupState.value =
                                CameraSetupUiState(
                                    cameras = cameras,
                                    useSharedProfile = false,
                                    canStartWatching = true,
                                    sharedProfileUpdateInProgress = true,
                                )
                        }
                    },
                )
            }
        }

        val sharedProfileToggle = composeRule.onNodeWithTag(UiTestTags.SharedProfileToggle)
        sharedProfileToggle.performSemanticsAction(SemanticsActions.RequestFocus)
        sharedProfileToggle.assertIsFocused().performClick().assertIsFocused().assertIsEnabled()

        composeRule.onNodeWithTag(UiTestTags.setupCamera("camera")).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.editCamera("camera")).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.testConnection("camera")).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.UsernameField).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.PasswordField).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.StartWatchingAction).assertIsNotEnabled()

        composeRule.runOnIdle {
            setupState.value = CameraSetupUiState(cameras = cameras, useSharedProfile = false)
        }

        composeRule.onNodeWithTag(UiTestTags.SharedProfileToggle).assertIsFocused()
    }

    @Test
    fun successfulConnectionPreviewShowsOnlyTestedCameraWithoutTakingDpadFocus() {
        val initialState =
            CameraSetupUiState(
                cameras =
                    listOf(
                        SetupCameraUiModel(
                            id = "tested",
                            displayName =
                                "A very long living-room camera name that must remain bounded",
                        ),
                        SetupCameraUiModel(
                            id = "other",
                            displayName = "Other camera",
                        ),
                    )
            )
        val setupState = mutableStateOf(initialState)
        composeRule.setContent {
            CamGridTheme {
                CameraSetupScreen(
                    state = setupState.value,
                    onAction = { action ->
                        if (action == CameraSetupUiAction.TestConnection("tested")) {
                            setupState.value =
                                CameraSetupUiState(
                                    cameras =
                                        initialState.cameras.map { camera ->
                                            if (camera.id == "tested") {
                                                camera.copy(
                                                    connectionState = ConnectionTestUiState.Testing
                                                )
                                            } else {
                                                camera
                                            }
                                        },
                                    connectionPreviewCameraId = "tested",
                                )
                        }
                    },
                    videoSurface = { cameraId, modifier ->
                        Box(modifier = modifier.testTag("fake-preview-$cameraId"))
                    },
                )
            }
        }

        val testedAction = composeRule.onNodeWithTag(UiTestTags.testConnection("tested"))
        testedAction.performSemanticsAction(SemanticsActions.RequestFocus)
        testedAction.assertIsFocused().performClick().assertIsFocused().assertIsEnabled()

        composeRule.onNodeWithTag(UiTestTags.SetupConnectionPreview).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SetupCameraList).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.connectionPreview("tested")).assertIsDisplayed()
        composeRule.onNodeWithTag("fake-preview-tested").assertIsDisplayed()
        composeRule.onNodeWithTag("fake-preview-other").assertDoesNotExist()
        composeRule.onNodeWithTag(UiTestTags.SetupConnectionPreviewStatus).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.editCamera("tested")).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.testConnection("tested")).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.PasswordField).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.StartWatchingAction).assertIsDisplayed()

        composeRule.runOnIdle {
            setupState.value =
                CameraSetupUiState(
                    cameras =
                        initialState.cameras.map { camera ->
                            if (camera.id == "tested") {
                                camera.copy(connectionState = ConnectionTestUiState.Connected)
                            } else {
                                camera
                            }
                        },
                    connectionPreviewCameraId = "tested",
                )
        }

        composeRule.onNodeWithTag(UiTestTags.testConnection("tested")).assertIsFocused()
        composeRule.onNodeWithTag(UiTestTags.UsernameField).assertIsEnabled()
    }

    private fun cameraFixtures(count: Int): List<DiscoveryCameraUiModel> =
        (1..count).map { index ->
            DiscoveryCameraUiModel(
                id = "camera-$index",
                displayName = "Camera $index",
            )
        }
}
