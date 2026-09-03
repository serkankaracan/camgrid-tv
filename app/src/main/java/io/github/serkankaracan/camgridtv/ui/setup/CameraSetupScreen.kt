package io.github.serkankaracan.camgridtv.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.model.CameraDevice
import io.github.serkankaracan.camgridtv.security.CredentialValidator
import io.github.serkankaracan.camgridtv.ui.components.CamGridBackground
import io.github.serkankaracan.camgridtv.ui.components.CameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.ConnectionStatusLabel
import io.github.serkankaracan.camgridtv.ui.components.ControlPanel
import io.github.serkankaracan.camgridtv.ui.components.EmptyCameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.ScreenHeader
import io.github.serkankaracan.camgridtv.ui.components.StatusPill
import io.github.serkankaracan.camgridtv.ui.components.TvFocusableSurface
import io.github.serkankaracan.camgridtv.ui.components.TvTextField
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
import io.github.serkankaracan.camgridtv.ui.theme.CamGridDimens
import io.github.serkankaracan.camgridtv.ui.theme.CamGridPalette

@Composable
fun CameraSetupScreen(
    state: CameraSetupUiState,
    onAction: (CameraSetupUiAction) -> Unit,
    modifier: Modifier = Modifier,
    videoSurface: CameraVideoSurface = { _, surfaceModifier ->
        EmptyCameraVideoSurface(surfaceModifier)
    },
) {
    val selectedCount = state.cameras.count(SetupCameraUiModel::selected)
    val credentialControlsEnabled =
        !state.submitting &&
            !state.selectionUpdateInProgress &&
            !state.sharedProfileUpdateInProgress &&
            !state.connectionTestInProgress &&
            state.credentialRecovery == CredentialRecoveryUiState.NotRequired
    val sharedProfileToggleEnabled =
        !state.submitting &&
            !state.selectionUpdateInProgress &&
            !state.connectionTestInProgress &&
            state.credentialRecovery == CredentialRecoveryUiState.NotRequired
    val primaryAction = CameraSetupPrimaryActionPolicy.resolve(state)
    val recoveryFocusRequester = remember { FocusRequester() }
    LaunchedEffect(state.credentialRecovery) {
        if (state.credentialRecovery != CredentialRecoveryUiState.NotRequired) {
            recoveryFocusRequester.requestFocus()
        }
    }

    CamGridBackground(modifier = modifier.fillMaxSize().testTag(UiTestTags.SetupScreen)) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        horizontal = CamGridDimens.ScreenHorizontal,
                        vertical = CamGridDimens.ScreenVertical,
                    )
        ) {
            ScreenHeader(
                title = stringResource(R.string.setup_title),
                subtitle = stringResource(R.string.setup_subtitle),
                eyebrow = stringResource(R.string.setup_step_label),
                trailing = {
                    StatusPill(
                        text =
                            pluralStringResource(
                                R.plurals.selected_camera_count,
                                selectedCount,
                                selectedCount,
                            ),
                        color =
                            if (selectedCount > 0) {
                                CamGridPalette.Selection
                            } else {
                                CamGridPalette.TextMuted
                            },
                    )
                },
            )

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(CamGridDimens.PanelGap),
            ) {
                CameraRosterPanel(
                    state = state,
                    onAction = onAction,
                    videoSurface = videoSurface,
                    modifier = Modifier.weight(0.53f).fillMaxHeight(),
                )
                CredentialPanel(
                    state = state,
                    credentialControlsEnabled = credentialControlsEnabled,
                    sharedProfileToggleEnabled = sharedProfileToggleEnabled,
                    primaryAction = primaryAction,
                    recoveryFocusRequester = recoveryFocusRequester,
                    onAction = onAction,
                    modifier = Modifier.weight(0.47f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun CameraRosterPanel(
    state: CameraSetupUiState,
    onAction: (CameraSetupUiAction) -> Unit,
    videoSurface: CameraVideoSurface,
    modifier: Modifier = Modifier,
) {
    val initialCameraId = remember {
        state.cameras.firstOrNull(SetupCameraUiModel::selected)?.id
            ?: state.cameras.firstOrNull()?.id
    }
    ControlPanel(modifier = modifier) {
        Text(
            text = stringResource(R.string.selected_cameras_title),
            color = CamGridPalette.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.selected_cameras_subtitle),
            modifier = Modifier.padding(top = 3.dp),
            color = CamGridPalette.TextMuted,
            fontSize = 14.sp,
        )
        state.connectionPreviewCamera?.let { previewCamera ->
            ConnectionPreview(
                camera = previewCamera,
                videoSurface = videoSurface,
                modifier = Modifier.fillMaxWidth().height(118.dp).padding(top = 10.dp),
            )
        }
        LazyColumn(
            modifier =
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag(UiTestTags.SetupCameraList),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(state.cameras, key = { _, camera -> camera.id }) { _, camera ->
                SetupCameraItem(
                    camera = camera,
                    state = state,
                    requestInitialFocus =
                        camera.id == initialCameraId &&
                            state.credentialRecovery == CredentialRecoveryUiState.NotRequired,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun CredentialPanel(
    state: CameraSetupUiState,
    credentialControlsEnabled: Boolean,
    sharedProfileToggleEnabled: Boolean,
    primaryAction: CameraSetupPrimaryActionDecision,
    recoveryFocusRequester: FocusRequester,
    onAction: (CameraSetupUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = state.cameras.count(SetupCameraUiModel::selected)
    val credentialScrollState = rememberScrollState()
    val primaryActionFocusRequester = remember { FocusRequester() }
    var primaryActionFocused by remember { mutableStateOf(false) }
    var restorePrimaryActionFocus by remember { mutableStateOf(false) }
    LaunchedEffect(state.connectionTestInProgress, primaryAction.kind) {
        if (!state.connectionTestInProgress && restorePrimaryActionFocus) {
            if (primaryAction.enabled) primaryActionFocusRequester.requestFocus()
            restorePrimaryActionFocus = false
        }
    }
    ControlPanel(modifier = modifier) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(credentialScrollState)
        ) {
            Text(
                text = stringResource(R.string.camera_account_panel_title),
                color = CamGridPalette.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.camera_account_explanation),
                modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
                color = CamGridPalette.TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            CredentialRecoveryPanel(
                recovery = state.credentialRecovery,
                clearActionFocusRequester = recoveryFocusRequester,
                onAction = onAction,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TvTextField(
                    label = stringResource(R.string.username),
                    value = state.username,
                    onValueChange = { onAction(CameraSetupUiAction.UsernameChanged(it)) },
                    enabled = credentialControlsEnabled,
                    maxLength = CredentialValidator.MAX_USERNAME_LENGTH,
                    modifier = Modifier.weight(1f).testTag(UiTestTags.UsernameField),
                )
                TvTextField(
                    label = stringResource(R.string.password),
                    value = state.password,
                    onValueChange = { onAction(CameraSetupUiAction.PasswordChanged(it)) },
                    password = true,
                    enabled = credentialControlsEnabled,
                    maxLength = CredentialValidator.MAX_PASSWORD_LENGTH,
                    modifier = Modifier.weight(1f).testTag(UiTestTags.PasswordField),
                )
            }
            SharedProfileToggle(
                checked = state.useSharedProfile,
                enabled = sharedProfileToggleEnabled,
                onCheckedChange = { onAction(CameraSetupUiAction.SharedProfileChanged(it)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            SetupReadinessBanner(
                state = state,
                primaryAction = primaryAction,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )

            Button(
                onClick = {
                    when (primaryAction.kind) {
                        CameraSetupPrimaryActionKind.StartWatching ->
                            onAction(CameraSetupUiAction.StartWatching)
                        CameraSetupPrimaryActionKind.VerifyConnection -> {
                            if (primaryActionFocused) restorePrimaryActionFocus = true
                            primaryAction.verificationTargetCameraId?.let { cameraId ->
                                onAction(CameraSetupUiAction.TestConnection(cameraId))
                            }
                        }
                    }
                },
                enabled = primaryAction.enabled,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(top = 10.dp)
                        .focusRequester(primaryActionFocusRequester)
                        .onFocusChanged { primaryActionFocused = it.isFocused }
                        .testTag(UiTestTags.StartWatchingAction),
            ) {
                Text(
                    when (primaryAction.kind) {
                        CameraSetupPrimaryActionKind.VerifyConnection ->
                            stringResource(R.string.verify_connection)
                        CameraSetupPrimaryActionKind.StartWatching ->
                            if (selectedCount == 0) {
                                stringResource(R.string.start_watching)
                            } else {
                                pluralStringResource(
                                    R.plurals.watch_camera_count,
                                    selectedCount,
                                    selectedCount,
                                )
                            }
                    }
                )
            }
        }
    }
}

@Composable
private fun ConnectionPreview(
    camera: SetupCameraUiModel,
    videoSurface: CameraVideoSurface,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(CamGridPalette.BackgroundBottom, RoundedCornerShape(10.dp))
                .border(1.dp, CamGridPalette.Outline, RoundedCornerShape(10.dp))
                .testTag(UiTestTags.SetupConnectionPreview)
    ) {
        Box(modifier = Modifier.fillMaxSize().testTag(UiTestTags.connectionPreview(camera.id))) {
            videoSurface(camera.id, Modifier.fillMaxSize())
        }
        Text(
            text = camera.displayName,
            modifier =
                Modifier.align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(CamGridPalette.Scrim)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            color = CamGridPalette.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ConnectionStatus(
            camera = camera,
            announceChanges = true,
            modifier =
                Modifier.align(Alignment.BottomStart)
                    .background(CamGridPalette.Scrim)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            testTag = UiTestTags.SetupConnectionPreviewStatus,
        )
    }
}

@Composable
private fun CredentialRecoveryPanel(
    recovery: CredentialRecoveryUiState,
    clearActionFocusRequester: FocusRequester,
    onAction: (CameraSetupUiAction) -> Unit,
) {
    if (recovery == CredentialRecoveryUiState.NotRequired) return

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(bottom = 10.dp)
                .background(CamGridPalette.Error.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                .border(1.dp, CamGridPalette.Error, RoundedCornerShape(10.dp))
                .padding(12.dp)
                .testTag(UiTestTags.CredentialRecoveryPanel),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.credential_recovery_required),
            color = CamGridPalette.Error,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        if (recovery == CredentialRecoveryUiState.ClearFailed) {
            Text(
                text = stringResource(R.string.credential_recovery_clear_failed),
                color = CamGridPalette.Error,
                fontSize = 14.sp,
            )
        }
        Button(
            onClick = { onAction(CameraSetupUiAction.ClearStoredCredentials) },
            enabled = recovery != CredentialRecoveryUiState.Clearing,
            modifier =
                Modifier.focusRequester(clearActionFocusRequester)
                    .testTag(UiTestTags.ClearStoredCredentialsAction),
        ) {
            Text(
                stringResource(
                    if (recovery == CredentialRecoveryUiState.Clearing) {
                        R.string.credential_recovery_clearing
                    } else {
                        R.string.credential_recovery_clear
                    }
                )
            )
        }
    }
}

@Composable
private fun SetupCameraItem(
    camera: SetupCameraUiModel,
    state: CameraSetupUiState,
    requestInitialFocus: Boolean,
    onAction: (CameraSetupUiAction) -> Unit,
) {
    val editorFocusRequester = remember(camera.id) { FocusRequester() }
    LaunchedEffect(state.editingCameraId) {
        if (state.editingCameraId == camera.id) editorFocusRequester.requestFocus()
    }
    val commonInteractionsEnabled =
        !state.connectionTestInProgress &&
            !state.submitting &&
            !state.sharedProfileUpdateInProgress &&
            state.credentialRecovery == CredentialRecoveryUiState.NotRequired
    val selectionToggleEnabled =
        (!state.selectionUpdateInProgress || state.selectionUpdateCameraId == camera.id) &&
            commonInteractionsEnabled
    val cameraActionsEnabled = !state.selectionUpdateInProgress && commonInteractionsEnabled

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    CamGridPalette.BackgroundTop.copy(alpha = 0.54f),
                    RoundedCornerShape(12.dp),
                )
                .padding(6.dp)
    ) {
        TvFocusableSurface(
            onClick = {
                onAction(CameraSetupUiAction.CameraSelectionChanged(camera.id, !camera.selected))
            },
            selected = camera.selected,
            enabled = selectionToggleEnabled,
            requestInitialFocus = requestInitialFocus,
            modifier = Modifier.fillMaxWidth().testTag(UiTestTags.setupCamera(camera.id)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier.size(12.dp)
                            .background(
                                if (camera.selected) {
                                    CamGridPalette.Selection
                                } else {
                                    CamGridPalette.Outline
                                },
                                RoundedCornerShape(50),
                            )
                )
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = camera.displayName,
                        color = CamGridPalette.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = camera.detail ?: stringResource(R.string.generic_onvif_camera),
                        modifier = Modifier.padding(top = 2.dp),
                        color = CamGridPalette.TextMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CameraStatePill(camera.connectionState)
            }
        }

        if (state.editingCameraId == camera.id) {
            TvTextField(
                label = stringResource(R.string.edit_camera_name),
                value = state.editedCameraName,
                onValueChange = { onAction(CameraSetupUiAction.CameraNameChanged(it)) },
                enabled = cameraActionsEnabled,
                maxLength = CameraDevice.MAX_DISPLAY_NAME_LENGTH,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(top = 6.dp)
                        .focusRequester(editorFocusRequester)
                        .testTag(UiTestTags.editCamera(camera.id)),
            )
            Button(
                onClick = { onAction(CameraSetupUiAction.SaveCameraName) },
                enabled = cameraActionsEnabled,
                modifier = Modifier.align(Alignment.End).padding(top = 6.dp),
            ) {
                Text(stringResource(R.string.save))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = { onAction(CameraSetupUiAction.EditCameraName(camera.id)) },
                    enabled = cameraActionsEnabled,
                    modifier = Modifier.testTag(UiTestTags.editCamera(camera.id)),
                ) {
                    Text(stringResource(R.string.rename_camera))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onAction(CameraSetupUiAction.TestConnection(camera.id)) },
                    enabled =
                        camera.selected &&
                            !state.submitting &&
                            !state.selectionUpdateInProgress &&
                            !state.sharedProfileUpdateInProgress &&
                            (!state.connectionTestInProgress ||
                                camera.connectionState == ConnectionTestUiState.Testing) &&
                            state.credentialRecovery == CredentialRecoveryUiState.NotRequired,
                    modifier = Modifier.testTag(UiTestTags.testConnection(camera.id)),
                ) {
                    Text(stringResource(R.string.test_connection))
                }
            }
        }
        ConnectionStatus(camera)
    }
}

@Composable
private fun CameraStatePill(state: ConnectionTestUiState) {
    val (label, color) =
        when (state) {
            ConnectionTestUiState.NotTested -> R.string.not_tested to CamGridPalette.TextMuted
            ConnectionTestUiState.CredentialsRequired ->
                R.string.credentials_needed_short to CamGridPalette.Warning
            ConnectionTestUiState.Testing -> R.string.connecting to CamGridPalette.Primary
            ConnectionTestUiState.Connected -> R.string.live to CamGridPalette.Success
            ConnectionTestUiState.AuthenticationFailed ->
                R.string.auth_failed_short to CamGridPalette.Error
            ConnectionTestUiState.Offline -> R.string.offline to CamGridPalette.Error
            ConnectionTestUiState.Failed -> R.string.playback_failed to CamGridPalette.Error
        }
    StatusPill(text = stringResource(label), color = color)
}

@Composable
private fun ConnectionStatus(
    camera: SetupCameraUiModel,
    modifier: Modifier = Modifier,
    testTag: String = UiTestTags.connectionStatus(camera.id),
    announceChanges: Boolean = false,
) {
    val status =
        when (camera.connectionState) {
            ConnectionTestUiState.NotTested -> null
            ConnectionTestUiState.CredentialsRequired ->
                R.string.camera_account_required to CamGridPalette.Error
            ConnectionTestUiState.Testing -> R.string.connecting to CamGridPalette.Primary
            ConnectionTestUiState.Connected -> R.string.live to CamGridPalette.Success
            ConnectionTestUiState.AuthenticationFailed ->
                R.string.auth_failed to CamGridPalette.Error
            ConnectionTestUiState.Offline -> R.string.offline to CamGridPalette.Error
            ConnectionTestUiState.Failed -> R.string.playback_failed to CamGridPalette.Error
        }
    status?.let { (label, color) ->
        ConnectionStatusLabel(
            cameraId = camera.id,
            labelRes = label,
            color = color,
            modifier = modifier.padding(top = 6.dp, start = 6.dp),
            testTag = testTag,
            announceChanges = announceChanges,
        )
    }
}

@Composable
private fun SetupReadinessBanner(
    state: CameraSetupUiState,
    primaryAction: CameraSetupPrimaryActionDecision,
    modifier: Modifier = Modifier,
) {
    val selectedCount = state.cameras.count(SetupCameraUiModel::selected)
    val authenticationFailed =
        state.cameras.any {
            it.selected && it.connectionState == ConnectionTestUiState.AuthenticationFailed
        }
    val connectionFailed =
        state.cameras.any {
            it.selected &&
                (it.connectionState == ConnectionTestUiState.Offline ||
                    it.connectionState == ConnectionTestUiState.Failed)
        }
    val (message, color) =
        when {
            state.credentialRecovery != CredentialRecoveryUiState.NotRequired ->
                R.string.setup_recovery_blocked to CamGridPalette.Error
            state.connectionTestInProgress ->
                R.string.setup_testing_connection to CamGridPalette.Primary
            state.canStartWatching -> R.string.setup_ready to CamGridPalette.Success
            selectedCount == 0 -> R.string.setup_select_camera to CamGridPalette.Warning
            authenticationFailed -> R.string.setup_authentication_help to CamGridPalette.Error
            connectionFailed -> R.string.setup_connection_help to CamGridPalette.Error
            primaryAction.kind == CameraSetupPrimaryActionKind.VerifyConnection &&
                !primaryAction.verificationCredentialsAvailable ->
                R.string.setup_enter_credentials to CamGridPalette.Warning
            else -> R.string.setup_verify_before_start to CamGridPalette.Warning
        }

    Row(
        modifier =
            modifier
                .background(color.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                .border(1.dp, color.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(9.dp).background(color, RoundedCornerShape(50)))
        Text(
            text = stringResource(message),
            modifier =
                Modifier.padding(start = 10.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(UiTestTags.SetupReadiness),
            color = color,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun SharedProfileToggle(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier =
            modifier
                .alpha(if (enabled) 1f else 0.48f)
                .onFocusChanged { focused = it.isFocused }
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                )
                .background(
                    if (focused) CamGridPalette.FocusedSurface else CamGridPalette.Surface,
                    shape,
                )
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) CamGridPalette.Primary else CamGridPalette.Outline,
                    shape = shape,
                )
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .testTag(UiTestTags.SharedProfileToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(22.dp)
                    .background(
                        if (checked) CamGridPalette.Selection else CamGridPalette.SurfaceRaised,
                        RoundedCornerShape(5.dp),
                    )
                    .border(
                        1.dp,
                        if (checked) CamGridPalette.Selection else CamGridPalette.Outline,
                        RoundedCornerShape(5.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    modifier = Modifier.clearAndSetSemantics {},
                    color = CamGridPalette.OnPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = stringResource(R.string.use_for_selected),
            modifier = Modifier.padding(start = 10.dp),
            color = CamGridPalette.TextPrimary,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
