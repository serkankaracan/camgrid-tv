package io.github.serkankaracan.camgridtv.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.ui.components.ConnectionStatusLabel
import io.github.serkankaracan.camgridtv.ui.components.TvFocusableSurface
import io.github.serkankaracan.camgridtv.ui.components.TvTextField
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags

@Composable
fun CameraSetupScreen(
    state: CameraSetupUiState,
    onAction: (CameraSetupUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = state.cameras.count(SetupCameraUiModel::selected)
    val credentialControlsEnabled =
        !state.submitting && state.credentialRecovery == CredentialRecoveryUiState.NotRequired
    val initialFocusRequester = remember { FocusRequester() }
    val recoveryFocusRequester = remember { FocusRequester() }
    LaunchedEffect(state.credentialRecovery) {
        if (state.credentialRecovery == CredentialRecoveryUiState.NotRequired) {
            initialFocusRequester.requestFocus()
        } else {
            recoveryFocusRequester.requestFocus()
        }
    }

    Surface(modifier = modifier.fillMaxSize().testTag(UiTestTags.SetupScreen)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 32.dp)) {
            Text(text = stringResource(R.string.camera_account_required), fontSize = 32.sp)
            Text(
                text = stringResource(R.string.camera_account_explanation),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                fontSize = 18.sp,
            )
            CredentialRecoveryPanel(
                recovery = state.credentialRecovery,
                clearActionFocusRequester = recoveryFocusRequester,
                onAction = onAction,
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.selected_camera_count,
                        selectedCount,
                        selectedCount,
                    ),
                fontSize = 18.sp,
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.cameras, key = SetupCameraUiModel::id) { camera ->
                    SetupCameraCard(camera = camera, state = state, onAction = onAction)
                }
            }
            TvTextField(
                label = stringResource(R.string.username),
                value = state.username,
                onValueChange = { onAction(CameraSetupUiAction.UsernameChanged(it)) },
                enabled = credentialControlsEnabled,
                modifier =
                    Modifier.focusRequester(initialFocusRequester)
                        .testTag(UiTestTags.UsernameField),
            )
            TvTextField(
                label = stringResource(R.string.password),
                value = state.password,
                onValueChange = { onAction(CameraSetupUiAction.PasswordChanged(it)) },
                password = true,
                enabled = credentialControlsEnabled,
                modifier = Modifier.padding(top = 10.dp).testTag(UiTestTags.PasswordField),
            )
            SharedProfileToggle(
                checked = state.useSharedProfile,
                enabled = credentialControlsEnabled,
                onCheckedChange = { onAction(CameraSetupUiAction.SharedProfileChanged(it)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = { onAction(CameraSetupUiAction.StartWatching) },
                    enabled = state.canStartWatching && !state.submitting,
                    modifier = Modifier.testTag(UiTestTags.StartWatchingAction),
                ) {
                    Text(stringResource(R.string.start_watching))
                }
            }
        }
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
                .padding(bottom = 16.dp)
                .border(2.dp, MaterialTheme.colorScheme.error)
                .padding(16.dp)
                .testTag(UiTestTags.CredentialRecoveryPanel),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.credential_recovery_required),
            color = MaterialTheme.colorScheme.error,
            fontSize = 18.sp,
        )
        if (recovery == CredentialRecoveryUiState.ClearFailed) {
            Text(
                text = stringResource(R.string.credential_recovery_clear_failed),
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp,
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
private fun SetupCameraCard(
    camera: SetupCameraUiModel,
    state: CameraSetupUiState,
    onAction: (CameraSetupUiAction) -> Unit,
) {
    val editorFocusRequester = remember(camera.id) { FocusRequester() }
    LaunchedEffect(state.editingCameraId) {
        if (state.editingCameraId == camera.id) editorFocusRequester.requestFocus()
    }
    TvFocusableSurface(
        onClick = {
            onAction(CameraSetupUiAction.CameraSelectionChanged(camera.id, !camera.selected))
        },
        selected = camera.selected,
        modifier = Modifier.fillMaxWidth().testTag(UiTestTags.setupCamera(camera.id)),
    ) {
        Column {
            if (state.editingCameraId == camera.id) {
                TvTextField(
                    label = stringResource(R.string.edit_camera_name),
                    value = state.editedCameraName,
                    onValueChange = { onAction(CameraSetupUiAction.CameraNameChanged(it)) },
                    modifier =
                        Modifier.focusRequester(editorFocusRequester)
                            .testTag(UiTestTags.editCamera(camera.id)),
                )
                Button(
                    onClick = { onAction(CameraSetupUiAction.SaveCameraName) },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.save))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(text = camera.displayName, fontSize = 22.sp)
                        camera.detail?.let { Text(text = it, fontSize = 15.sp) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onAction(CameraSetupUiAction.EditCameraName(camera.id)) },
                            modifier = Modifier.testTag(UiTestTags.editCamera(camera.id)),
                        ) {
                            Text(stringResource(R.string.edit_camera_name))
                        }
                        Button(
                            onClick = { onAction(CameraSetupUiAction.TestConnection(camera.id)) },
                            enabled =
                                camera.selected &&
                                    !state.submitting &&
                                    state.credentialRecovery ==
                                        CredentialRecoveryUiState.NotRequired,
                            modifier = Modifier.testTag(UiTestTags.testConnection(camera.id)),
                        ) {
                            Text(stringResource(R.string.test_connection))
                        }
                    }
                }
            }
            ConnectionStatus(camera)
        }
    }
}

@Composable
private fun ConnectionStatus(camera: SetupCameraUiModel) {
    val status =
        when (camera.connectionState) {
            ConnectionTestUiState.NotTested -> null
            ConnectionTestUiState.Testing -> R.string.connecting to false
            ConnectionTestUiState.Connected -> R.string.live to false
            ConnectionTestUiState.AuthenticationFailed -> R.string.auth_failed to true
            ConnectionTestUiState.Offline -> R.string.offline to true
            ConnectionTestUiState.Failed -> R.string.playback_failed to true
        }
    status?.let { (label, error) ->
        ConnectionStatusLabel(
            cameraId = camera.id,
            labelRes = label,
            isError = error,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SharedProfileToggle(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = 10.dp)
                .onFocusChanged { focused = it.isFocused }
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                )
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color =
                        if (focused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    shape = shape,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(UiTestTags.SharedProfileToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(26.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .background(
                        if (checked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
        )
        Text(
            text = stringResource(R.string.use_for_selected),
            modifier = Modifier.padding(start = 12.dp),
            fontSize = 18.sp,
        )
    }
}
