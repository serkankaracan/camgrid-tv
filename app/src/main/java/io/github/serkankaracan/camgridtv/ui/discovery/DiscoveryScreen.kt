package io.github.serkankaracan.camgridtv.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.ui.components.CamGridBackground
import io.github.serkankaracan.camgridtv.ui.components.ControlPanel
import io.github.serkankaracan.camgridtv.ui.components.ScreenHeader
import io.github.serkankaracan.camgridtv.ui.components.StatusPill
import io.github.serkankaracan.camgridtv.ui.components.TvFocusableSurface
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
import io.github.serkankaracan.camgridtv.ui.theme.CamGridDimens
import io.github.serkankaracan.camgridtv.ui.theme.CamGridPalette

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onAction: (DiscoveryUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    CamGridBackground(modifier = modifier.fillMaxSize()) {
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
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = CamGridDimens.SafeHorizontal,
                    vertical = CamGridDimens.SafeVertical,
                )
                .testTag(UiTestTags.PermissionScreen),
        contentAlignment = Alignment.Center,
    ) {
        ControlPanel(modifier = Modifier.widthIn(max = 760.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                color = CamGridPalette.Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = stringResource(R.string.permission_title),
                modifier = Modifier.padding(top = 8.dp),
                color = CamGridPalette.TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.permission_body),
                modifier = Modifier.padding(top = 12.dp, bottom = 26.dp),
                color = CamGridPalette.TextMuted,
                fontSize = 19.sp,
                lineHeight = 27.sp,
            )
            Button(
                onClick = { onAction(action) },
                modifier =
                    Modifier.focusRequester(focusRequester)
                        .testTag(UiTestTags.PermissionPrimaryAction),
            ) {
                Text(actionLabel)
            }
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
                body = stringResource(R.string.scan_ready_body),
                actionLabel = stringResource(R.string.scan_title),
                onAction = { onAction(DiscoveryUiAction.StartScan) },
                modifier = modifier,
            )
        DiscoveryContentUiState.Loading ->
            DiscoveryMessage(
                title = stringResource(R.string.scanning),
                body = stringResource(R.string.scanning_body),
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
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(actionLabel) {
        if (actionLabel != null) focusRequester.requestFocus()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = CamGridDimens.SafeHorizontal,
                    vertical = CamGridDimens.SafeVertical,
                ),
        contentAlignment = Alignment.Center,
    ) {
        ControlPanel(modifier = Modifier.widthIn(max = 760.dp)) {
            Text(
                text = stringResource(R.string.discovery_step_label),
                color = CamGridPalette.Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
            )
            Text(
                text = title,
                modifier = Modifier.padding(top = 8.dp),
                color = CamGridPalette.TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
            )
            body?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 12.dp),
                    color = CamGridPalette.TextMuted,
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                )
            }
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier =
                        Modifier.padding(top = 26.dp)
                            .focusRequester(focusRequester)
                            .testTag(UiTestTags.DiscoveryScanAction),
                ) {
                    Text(actionLabel)
                }
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
    val scanActionFocusRequester = remember { FocusRequester() }
    LaunchedEffect(scanning) {
        if (scanning || cameras.isEmpty()) scanActionFocusRequester.requestFocus()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = CamGridDimens.ScreenHorizontal,
                    vertical = CamGridDimens.ScreenVertical,
                )
    ) {
        ScreenHeader(
            title = stringResource(if (scanning) R.string.scanning else R.string.discovery_results),
            subtitle = stringResource(R.string.discovery_results_subtitle),
            eyebrow = stringResource(R.string.discovery_step_label),
            trailing = {
                StatusPill(
                    text =
                        pluralStringResource(
                            R.plurals.discovered_camera_count,
                            cameras.size,
                            cameras.size,
                        ),
                    color = if (scanning) CamGridPalette.Primary else CamGridPalette.Success,
                )
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(cameras, key = { _, camera -> camera.id }) { index, camera ->
                DiscoveryCameraCard(
                    camera = camera,
                    index = index,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    pluralStringResource(
                        R.plurals.selected_camera_count,
                        selectedCount,
                        selectedCount,
                    ),
                modifier = Modifier.testTag(UiTestTags.DiscoverySelectedCount),
                color =
                    if (selectedCount > 0) CamGridPalette.Selection else CamGridPalette.TextMuted,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = {
                        onAction(
                            if (scanning) {
                                DiscoveryUiAction.CancelScan
                            } else {
                                DiscoveryUiAction.StartScan
                            }
                        )
                    },
                    modifier =
                        Modifier.focusRequester(scanActionFocusRequester)
                            .testTag(UiTestTags.DiscoveryScanAction),
                ) {
                    Text(
                        stringResource(if (scanning) R.string.scan_cancel else R.string.scan_again)
                    )
                }
                if (!scanning && selectedCount > 0) {
                    Button(
                        onClick = { onAction(DiscoveryUiAction.ContinueToCameraSetup) },
                        modifier = Modifier.testTag(UiTestTags.DiscoveryContinueAction),
                    ) {
                        Text(stringResource(R.string.continue_to_account))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryCameraCard(
    camera: DiscoveryCameraUiModel,
    index: Int,
    requestInitialFocus: Boolean,
    onClick: () -> Unit,
) {
    TvFocusableSurface(
        onClick = onClick,
        selected = camera.selected,
        requestInitialFocus = requestInitialFocus,
        modifier = Modifier.fillMaxWidth().testTag(UiTestTags.discoveryCamera(camera.id)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.size(42.dp)
                        .background(CamGridPalette.SurfaceRaised, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    color = CamGridPalette.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    text = camera.displayName,
                    color = CamGridPalette.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = camera.detail ?: stringResource(R.string.generic_onvif_camera),
                    modifier = Modifier.padding(top = 2.dp),
                    color = CamGridPalette.TextMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(
                text =
                    stringResource(
                        if (camera.selected) R.string.camera_selected else R.string.camera_available
                    ),
                color = if (camera.selected) CamGridPalette.Selection else CamGridPalette.TextMuted,
            )
        }
    }
}
