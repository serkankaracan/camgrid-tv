package io.github.serkankaracan.camgridtv.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.ui.components.TvFocusableSurface
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onAction: (DiscoveryUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (state.permission) {
            LocalNetworkPermissionUiState.Granted ->
                DiscoveryContent(
                    content = state.content,
                    onAction = onAction,
                    modifier = Modifier.testTag(UiTestTags.DiscoveryScreen),
                )
            LocalNetworkPermissionUiState.RationaleRequired ->
                PermissionContent(
                    action = DiscoveryUiAction.RequestPermission,
                    actionLabel = stringResource(R.string.permission_allow),
                    onAction = onAction,
                )
            LocalNetworkPermissionUiState.Denied ->
                PermissionContent(
                    action = DiscoveryUiAction.OpenAppSettings,
                    actionLabel = stringResource(R.string.permission_settings),
                    onAction = onAction,
                )
        }
    }
}

@Composable
private fun PermissionContent(
    action: DiscoveryUiAction,
    actionLabel: String,
    onAction: (DiscoveryUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp, vertical = 56.dp)
                .testTag(UiTestTags.PermissionScreen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.permission_title), fontSize = 36.sp)
        Text(
            text = stringResource(R.string.permission_body),
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
            fontSize = 20.sp,
        )
        Button(
            onClick = { onAction(action) },
            modifier =
                Modifier.focusRequester(focusRequester).testTag(UiTestTags.PermissionPrimaryAction),
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun DiscoveryContent(
    content: DiscoveryContentUiState,
    onAction: (DiscoveryUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (content) {
        DiscoveryContentUiState.Ready ->
            DiscoveryMessage(
                title = stringResource(R.string.scan_title),
                actionLabel = stringResource(R.string.scan_title),
                onAction = { onAction(DiscoveryUiAction.StartScan) },
                modifier = modifier,
            )
        DiscoveryContentUiState.Loading ->
            DiscoveryMessage(
                title = stringResource(R.string.scanning),
                modifier = modifier,
            )
        is DiscoveryContentUiState.Scanning ->
            DiscoveryResults(
                cameras = content.camerasFound,
                scanning = true,
                onAction = onAction,
                modifier = modifier,
            )
        DiscoveryContentUiState.Empty ->
            DiscoveryMessage(
                title = stringResource(R.string.no_cameras_title),
                body = stringResource(R.string.no_cameras_body),
                actionLabel = stringResource(R.string.scan_again),
                onAction = { onAction(DiscoveryUiAction.StartScan) },
                modifier = modifier,
            )
        is DiscoveryContentUiState.Error ->
            DiscoveryMessage(
                title = stringResource(R.string.discovery_error_title),
                body =
                    stringResource(
                        when (content.reason) {
                            DiscoveryErrorUiState.NoActiveLocalNetwork ->
                                R.string.discovery_no_local_network
                            DiscoveryErrorUiState.NetworkLost -> R.string.discovery_network_lost
                            DiscoveryErrorUiState.TransportUnavailable ->
                                R.string.discovery_transport_unavailable
                        }
                    ),
                actionLabel = stringResource(R.string.scan_again),
                onAction = { onAction(DiscoveryUiAction.StartScan) },
                modifier = modifier,
            )
        is DiscoveryContentUiState.Results ->
            DiscoveryResults(
                cameras = content.cameras,
                scanning = false,
                onAction = onAction,
                modifier = modifier,
            )
    }
}

@Composable
private fun DiscoveryMessage(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    LaunchedEffect(actionLabel) {
        if (actionLabel != null) focusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 80.dp, vertical = 56.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, fontSize = 34.sp)
        body?.let {
            Text(text = it, modifier = Modifier.padding(top = 16.dp), fontSize = 19.sp)
        }
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier =
                    Modifier.padding(top = 28.dp)
                        .focusRequester(focusRequester)
                        .testTag(UiTestTags.DiscoveryScanAction),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun DiscoveryResults(
    cameras: List<DiscoveryCameraUiModel>,
    scanning: Boolean,
    onAction: (DiscoveryUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = cameras.count(DiscoveryCameraUiModel::selected)
    val scanActionFocusRequester = androidx.compose.runtime.remember { FocusRequester() }
    LaunchedEffect(scanning) {
        if (scanning || cameras.isEmpty()) scanActionFocusRequester.requestFocus()
    }
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 36.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    if (scanning) {
                        stringResource(R.string.scanning)
                    } else {
                        stringResource(R.string.scan_title)
                    },
                fontSize = 32.sp,
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.selected_camera_count,
                        selectedCount,
                        selectedCount,
                    ),
                modifier = Modifier.testTag(UiTestTags.DiscoverySelectedCount),
                fontSize = 20.sp,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cameras, key = DiscoveryCameraUiModel::id) { camera ->
                DiscoveryCameraCard(
                    camera = camera,
                    requestInitialFocus = !scanning && camera.id == cameras.firstOrNull()?.id,
                    onClick = {
                        onAction(
                            DiscoveryUiAction.CameraSelectionChanged(
                                cameraId = camera.id,
                                selected = !camera.selected,
                            )
                        )
                    },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    onAction(
                        if (scanning) DiscoveryUiAction.CancelScan else DiscoveryUiAction.StartScan
                    )
                },
                modifier =
                    Modifier.focusRequester(scanActionFocusRequester)
                        .testTag(UiTestTags.DiscoveryScanAction),
            ) {
                Text(stringResource(if (scanning) R.string.scan_cancel else R.string.scan_again))
            }
            if (!scanning && selectedCount > 0) {
                Button(
                    onClick = { onAction(DiscoveryUiAction.ContinueToCameraSetup) },
                    modifier = Modifier.testTag(UiTestTags.DiscoveryContinueAction),
                ) {
                    Text(stringResource(R.string.camera_account_required))
                }
            }
        }
    }
}

@Composable
private fun DiscoveryCameraCard(
    camera: DiscoveryCameraUiModel,
    requestInitialFocus: Boolean,
    onClick: () -> Unit,
) {
    TvFocusableSurface(
        onClick = onClick,
        selected = camera.selected,
        requestInitialFocus = requestInitialFocus,
        modifier = Modifier.fillMaxWidth().testTag(UiTestTags.discoveryCamera(camera.id)),
    ) {
        Column {
            Text(text = camera.displayName, fontSize = 23.sp)
            camera.detail?.let {
                Spacer(Modifier.height(4.dp))
                Text(text = it, fontSize = 16.sp)
            }
        }
    }
}
