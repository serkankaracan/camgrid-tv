package io.github.serkankaracan.camgridtv.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import io.github.serkankaracan.camgridtv.app.AppContainer
import io.github.serkankaracan.camgridtv.discovery.CameraDiscoveryMergePolicy
import io.github.serkankaracan.camgridtv.discovery.DiscoveredOnvifDevice
import io.github.serkankaracan.camgridtv.discovery.DiscoveryIssue
import io.github.serkankaracan.camgridtv.discovery.LocalNetworkPermissionState
import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import io.github.serkankaracan.camgridtv.model.CameraDevice
import io.github.serkankaracan.camgridtv.model.CredentialProfile
import io.github.serkankaracan.camgridtv.playback.Media3PlaybackEngine
import io.github.serkankaracan.camgridtv.playback.PlaybackCoordinator
import io.github.serkankaracan.camgridtv.playback.PlaybackRequest
import io.github.serkankaracan.camgridtv.playback.PlaybackState
import io.github.serkankaracan.camgridtv.playback.RtspStream
import io.github.serkankaracan.camgridtv.security.CredentialSecret
import io.github.serkankaracan.camgridtv.security.CredentialValidationResult
import io.github.serkankaracan.camgridtv.security.CredentialValidator
import io.github.serkankaracan.camgridtv.security.SecretRecoveryRequiredException
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryCameraUiModel
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryContentUiState
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryErrorUiState
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryUiAction
import io.github.serkankaracan.camgridtv.ui.discovery.DiscoveryUiState
import io.github.serkankaracan.camgridtv.ui.discovery.LocalNetworkPermissionUiState
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenUiAction
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenUiState
import io.github.serkankaracan.camgridtv.ui.fullscreen.FullscreenViewMode
import io.github.serkankaracan.camgridtv.ui.navigation.CamGridRoute
import io.github.serkankaracan.camgridtv.ui.navigation.LocalNetworkAccessDecision
import io.github.serkankaracan.camgridtv.ui.navigation.LocalNetworkAccessPolicy
import io.github.serkankaracan.camgridtv.ui.navigation.LocalRouteActionPolicy
import io.github.serkankaracan.camgridtv.ui.navigation.LocalRouteSurface
import io.github.serkankaracan.camgridtv.ui.navigation.SavedCameraBootstrapAttemptGate
import io.github.serkankaracan.camgridtv.ui.navigation.SavedCameraBootstrapDecision
import io.github.serkankaracan.camgridtv.ui.navigation.SavedCameraBootstrapPolicy
import io.github.serkankaracan.camgridtv.ui.setup.CameraSetupReadiness
import io.github.serkankaracan.camgridtv.ui.setup.CameraSetupUiAction
import io.github.serkankaracan.camgridtv.ui.setup.CameraSetupUiState
import io.github.serkankaracan.camgridtv.ui.setup.ConnectionTestCredentialProfilePolicy
import io.github.serkankaracan.camgridtv.ui.setup.ConnectionTestOperation
import io.github.serkankaracan.camgridtv.ui.setup.ConnectionTestOperationGate
import io.github.serkankaracan.camgridtv.ui.setup.ConnectionTestPlaybackAction
import io.github.serkankaracan.camgridtv.ui.setup.ConnectionTestPlaybackPolicy
import io.github.serkankaracan.camgridtv.ui.setup.ConnectionTestPreviewGate
import io.github.serkankaracan.camgridtv.ui.setup.ConnectionTestUiState
import io.github.serkankaracan.camgridtv.ui.setup.CredentialRecoveryUiState
import io.github.serkankaracan.camgridtv.ui.setup.SetupCameraUiModel
import io.github.serkankaracan.camgridtv.ui.setup.SharedProfileUpdateOperationGate
import io.github.serkankaracan.camgridtv.ui.wall.CameraWallUiAction
import io.github.serkankaracan.camgridtv.ui.wall.CameraWallUiState
import io.github.serkankaracan.camgridtv.ui.wall.WallCameraUiModel
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class CamGridAppUiState(
    val route: CamGridRoute,
    val discovery: DiscoveryUiState,
    val cameraSetup: CameraSetupUiState,
    val wall: CameraWallUiState,
    val fullscreen: FullscreenUiState? = null,
    val playbackEngineGenerations: Map<String, Long> = emptyMap(),
)

class CamGridViewModel(private val container: AppContainer) : ViewModel() {
    private val cameraDiscoveryMergePolicy = CameraDiscoveryMergePolicy()
    private val playbackCoordinator =
        PlaybackCoordinator(
            engineFactory = container.playbackEngineFactory,
            scope = viewModelScope,
            initiallyForeground = false,
        )

    private var route: CamGridRoute = CamGridRoute.Discovery
    private var permission =
        permissionDecision(shouldShowRationale = false, requestCompleted = false).permissionUiState
    private var permissionRequestCompleted = false
    private var discoveryContent: DiscoveryContentUiState = DiscoveryContentUiState.Ready
    private var configuration = CameraConfiguration()
    private var configurationLoaded = false
    private val savedCameraBootstrapGate = SavedCameraBootstrapAttemptGate()
    private var playbackStates: Map<String, PlaybackState> = emptyMap()
    private var playbackEngineGenerations: Map<String, Long> = emptyMap()
    private var fullscreenViewMode: FullscreenViewMode = FullscreenViewMode.SAFE
    private var connectionStates: Map<String, ConnectionTestUiState> = emptyMap()
    private var credentialRecovery = CredentialRecoveryUiState.NotRequired
    private var selectionUpdateCameraId: String? = null
    private val sharedProfileUpdateGate = SharedProfileUpdateOperationGate()
    private val connectionTestGate = ConnectionTestOperationGate()
    private val connectionTestPreviewGate = ConnectionTestPreviewGate()
    private var discoveryJob: Job? = null
    private var discoveryFinalizationJob: Job? = null
    private var latestDiscoveryDevices: List<DiscoveredOnvifDevice> = emptyList()
    private var discoverySelectionOverrides: Map<String, Boolean> = emptyMap()
    private var connectionTestJob: Job? = null
    private var connectionPreviewReleaseJob: Job? = null
    private var routeJob: Job? = null
    private var playbackRefreshJob: Job? = null
    private var routeOperationGeneration = 0L
    private var setupDraft = SetupDraft()
    private var foreground = false

    private val mutableUiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<CamGridAppUiState> = mutableUiState.asStateFlow()

    val permissionName: String
        get() = container.permissionCoordinator.permissionName

    fun permissionToRequest(): String? =
        container.permissionCoordinator.permissionsToRequest().firstOrNull()

    fun shouldShowPermissionRationale(activity: Activity): Boolean =
        container.permissionCoordinator.shouldShowRationale(activity)

    init {
        viewModelScope.launch {
            container.cameraSelectionRepository.configuration
                .catch { emit(configuration) }
                .collect { latest ->
                    val firstConfiguration = !configurationLoaded
                    configuration = latest
                    configurationLoaded = true
                    synchronizeDiscoverySelections()
                    if (firstConfiguration && shouldStartDiscovery()) startDiscovery()
                    if (firstConfiguration) {
                        maybeStartSavedCameraBootstrap()
                    } else {
                        refreshActivePlaybackPlan(latest)
                    }
                    publish()
                }
        }
        viewModelScope.launch {
            playbackCoordinator.states.collect { states ->
                playbackStates = states
                updateConnectionTest(states)
                publish()
            }
        }
        viewModelScope.launch {
            playbackCoordinator.engineGenerations.collect { generations ->
                playbackEngineGenerations = generations
                publish()
            }
        }
        viewModelScope.launch {
            container.connectivityMonitor
                .observe()
                .catch {
                    emit(io.github.serkankaracan.camgridtv.discovery.LocalConnectivityState())
                }
                .collect { connectivity ->
                    playbackCoordinator.onConnectivityChanged(connectivity.isLocalNetworkAvailable)
                }
        }
    }

    fun refreshPermission(shouldShowRationale: Boolean, requestCompleted: Boolean = false) {
        permissionRequestCompleted = permissionRequestCompleted || requestCompleted
        val decision = permissionDecision(shouldShowRationale, permissionRequestCompleted)
        applyPermissionDecision(decision)
        if (!decision.allowsLocalWork) {
            publish()
            return
        }
        if (foreground) playbackCoordinator.onForeground()
        if (shouldStartDiscovery()) {
            startDiscovery()
        } else {
            publish()
        }
        maybeStartSavedCameraBootstrap()
    }

    fun onDiscoveryAction(action: DiscoveryUiAction) {
        if (!allowsRouteAction(LocalRouteSurface.Discovery)) return
        when (action) {
            DiscoveryUiAction.RequestPermission,
            DiscoveryUiAction.OpenAppSettings -> Unit
            DiscoveryUiAction.StartScan -> startDiscovery()
            DiscoveryUiAction.CancelScan -> cancelDiscovery()
            DiscoveryUiAction.ContinueToCameraSetup -> continueToCameraSetup()
            is DiscoveryUiAction.CameraSelectionChanged -> {
                discoverySelectionOverrides =
                    discoverySelectionOverrides + (action.cameraId to action.selected)
                discoveryContent = discoveryContent.mapCameras { camera ->
                    if (camera.id == action.cameraId) {
                        camera.copy(selected = action.selected)
                    } else {
                        camera
                    }
                }
                publish()
            }
        }
    }

    fun onCameraSetupAction(action: CameraSetupUiAction) {
        if (!allowsRouteAction(LocalRouteSurface.CameraSetup)) return
        if (selectionUpdateCameraId != null || sharedProfileUpdateGate.active != null) return
        when (action) {
            is CameraSetupUiAction.CameraSelectionChanged -> {
                if (connectionTestGate.active != null) return
                stopConnectionPreviewIfActive()
                updateCameraSelection(action.cameraId, action.selected)
            }
            is CameraSetupUiAction.EditCameraName -> {
                if (connectionTestGate.active != null) return
                val camera = configuration.cameras.firstOrNull { it.id == action.cameraId }
                if (camera != null) {
                    setupDraft =
                        setupDraft.copy(
                            editingCameraId = camera.id,
                            editedCameraName = camera.displayName,
                        )
                    publish()
                }
            }
            is CameraSetupUiAction.CameraNameChanged -> {
                setupDraft =
                    setupDraft.copy(
                        editedCameraName = action.value.take(CameraDevice.MAX_DISPLAY_NAME_LENGTH)
                    )
                publish()
            }
            CameraSetupUiAction.SaveCameraName -> saveCameraName()
            is CameraSetupUiAction.UsernameChanged -> {
                if (connectionTestGate.active != null) return
                stopConnectionPreviewIfActive()
                setupDraft =
                    setupDraft.copy(
                        username = action.value.take(CredentialValidator.MAX_USERNAME_LENGTH)
                    )
                invalidateSuccessfulConnectionTests()
                publish()
            }
            is CameraSetupUiAction.PasswordChanged -> {
                if (connectionTestGate.active != null) return
                stopConnectionPreviewIfActive()
                setupDraft =
                    setupDraft.copy(
                        password = action.value.take(CredentialValidator.MAX_PASSWORD_LENGTH)
                    )
                invalidateSuccessfulConnectionTests()
                publish()
            }
            is CameraSetupUiAction.SharedProfileChanged -> {
                if (connectionTestGate.active == null) {
                    stopConnectionPreviewIfActive()
                    changeSharedProfile(action.enabled)
                }
            }
            is CameraSetupUiAction.TestConnection -> testConnection(action.cameraId)
            CameraSetupUiAction.StartWatching -> openWall()
            CameraSetupUiAction.ClearStoredCredentials -> clearStoredCredentials()
        }
    }

    fun onCameraWallAction(action: CameraWallUiAction) {
        if (!allowsRouteAction(LocalRouteSurface.Wall)) return
        when (action) {
            is CameraWallUiAction.OpenFullscreen -> openFullscreen(action.cameraId)
            CameraWallUiAction.RescanCameras -> returnToDiscoveryAndScan()
            CameraWallUiAction.BackToCameraSetup -> {
                stopPlaybackAndTransientWork()
                route = CamGridRoute.CameraSetup
                publish()
            }
        }
    }

    fun onFullscreenAction(action: FullscreenUiAction) {
        if (!allowsRouteAction(LocalRouteSurface.Fullscreen)) return
        when (action) {
            FullscreenUiAction.BackToWall -> restoreWall()
            FullscreenUiAction.PreviousViewMode -> {
                fullscreenViewMode = fullscreenViewMode.previous()
                publish()
            }
            FullscreenUiAction.NextViewMode -> {
                fullscreenViewMode = fullscreenViewMode.next()
                publish()
            }
        }
    }

    fun onForeground(shouldShowRationale: Boolean) {
        foreground = true
        val decision =
            permissionDecision(
                shouldShowRationale = shouldShowRationale,
                requestCompleted = permissionRequestCompleted,
            )
        applyPermissionDecision(decision)
        if (!decision.allowsLocalWork) {
            publish()
            return
        }
        playbackCoordinator.onForeground()
        if (shouldStartDiscovery()) startDiscovery()
        maybeStartSavedCameraBootstrap()
    }

    fun onBackground() {
        foreground = false
        supersedeRouteOperations()
        savedCameraBootstrapGate.cancelActive()
        if (discoveryJob?.isActive == true) cancelDiscovery()
        val testingOperation = connectionTestGate.cancelActive()
        connectionTestJob?.cancel()
        connectionTestJob = null
        val previewSession = connectionTestPreviewGate.cancelActive()
        connectionPreviewReleaseJob?.cancel()
        connectionPreviewReleaseJob = null
        if (testingOperation != null) {
            if (connectionStates[testingOperation.cameraId] == ConnectionTestUiState.Testing) {
                connectionStates =
                    connectionStates +
                        (testingOperation.cameraId to ConnectionTestUiState.NotTested)
            }
        }
        if (testingOperation != null || previewSession != null) playbackCoordinator.leaveScreen()
        playbackCoordinator.onBackground()
        publish()
    }

    fun playerFor(cameraId: String): Player? =
        (playbackCoordinator.activeEngineFor(cameraId) as? Media3PlaybackEngine)?.player

    fun connectionPreviewPlayerFor(cameraId: String): Player? =
        if (
            route == CamGridRoute.CameraSetup &&
                connectionTestPreviewGate.active?.cameraId == cameraId
        ) {
            playerFor(cameraId)
        } else {
            null
        }

    override fun onCleared() {
        discoveryJob?.cancel()
        discoveryFinalizationJob?.cancel()
        playbackRefreshJob?.cancel()
        connectionPreviewReleaseJob?.cancel()
        stopPlaybackAndTransientWork()
    }

    private fun startDiscovery() {
        if (
            !configurationLoaded ||
                !foreground ||
                permission != LocalNetworkPermissionUiState.Granted ||
                route != CamGridRoute.Discovery
        ) {
            publish()
            return
        }
        val selectionsBeforeLoading = currentDiscoverySelections()
        discoveryJob?.cancel()
        discoveryFinalizationJob?.cancel()
        discoveryFinalizationJob = null
        discoverySelectionOverrides =
            configuration.cameras.associate { it.id to it.selected } + selectionsBeforeLoading
        latestDiscoveryDevices = emptyList()
        discoveryContent = DiscoveryContentUiState.Loading
        publish()
        discoveryJob = viewModelScope.launch {
            try {
                container.discoveryRepository.scan().collect { snapshot ->
                    latestDiscoveryDevices = snapshot.devices
                    captureLiveDiscoverySelections()
                    if (snapshot.issue == DiscoveryIssue.PERMISSION_REQUIRED) {
                        handleDiscoveryIssue(snapshot.issue)
                        return@collect
                    }
                    snapshot.issue?.let(::handleDiscoveryIssue)
                    val cameras = discoveryUiModels(snapshot.devices, discoverySelectionOverrides)
                    if (snapshot.isScanning) {
                        discoveryContent = DiscoveryContentUiState.Scanning(cameras)
                    } else {
                        val persisted =
                            persistDiscoveredDevicesSafely(
                                devices = snapshot.devices,
                                selectedById = discoverySelectionOverrides,
                            )
                        discoveryContent =
                            when {
                                !persisted -> DiscoveryContentUiState.Ready
                                cameras.isEmpty() && snapshot.issue != null ->
                                    snapshot.issue.toUiError()?.let(DiscoveryContentUiState::Error)
                                        ?: DiscoveryContentUiState.Empty
                                cameras.isEmpty() -> DiscoveryContentUiState.Empty
                                else -> DiscoveryContentUiState.Results(cameras)
                            }
                    }
                    publish()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                discoveryContent =
                    DiscoveryContentUiState.Error(DiscoveryErrorUiState.TransportUnavailable)
                publish()
            }
        }
    }

    private fun shouldStartDiscovery(): Boolean =
        configurationLoaded &&
            foreground &&
            permission == LocalNetworkPermissionUiState.Granted &&
            route == CamGridRoute.Discovery &&
            discoveryContent == DiscoveryContentUiState.Ready &&
            discoveryJob?.isActive != true &&
            discoveryFinalizationJob?.isActive != true

    private fun cancelDiscovery() {
        captureLiveDiscoverySelections()
        val devices = latestDiscoveryDevices.toList()
        val selectedById = discoverySelectionOverrides.toMap()
        val cameras = discoveryUiModels(devices, selectedById)
        discoveryJob?.cancel()
        discoveryJob = null
        discoveryFinalizationJob?.cancel()
        discoveryContent = DiscoveryContentUiState.Ready
        publish()
        if (devices.isEmpty()) return

        discoveryFinalizationJob = viewModelScope.launch {
            val persisted = persistDiscoveredDevicesSafely(devices, selectedById)
            discoveryContent =
                if (persisted) DiscoveryContentUiState.Results(cameras)
                else DiscoveryContentUiState.Ready
            publish()
        }
    }

    private fun handleDiscoveryIssue(issue: DiscoveryIssue) {
        if (issue == DiscoveryIssue.PERMISSION_REQUIRED) {
            applyPermissionDecision(
                LocalNetworkAccessPolicy.decide(
                    permissionState =
                        LocalNetworkPermissionState.Denied(shouldShowRationale = true),
                    requestCompleted = permissionRequestCompleted,
                    currentRoute = route,
                )
            )
            publish()
        }
    }

    private fun DiscoveryIssue.toUiError(): DiscoveryErrorUiState? =
        when (this) {
            DiscoveryIssue.NO_ACTIVE_LOCAL_NETWORK -> DiscoveryErrorUiState.NoActiveLocalNetwork
            DiscoveryIssue.NETWORK_LOST -> DiscoveryErrorUiState.NetworkLost
            DiscoveryIssue.TRANSPORT_UNAVAILABLE -> DiscoveryErrorUiState.TransportUnavailable
            DiscoveryIssue.PERMISSION_REQUIRED -> null
        }

    private suspend fun persistDiscoveredDevices(
        devices: List<DiscoveredOnvifDevice>,
        selectedById: Map<String, Boolean>,
    ) {
        if (devices.isEmpty()) return
        container.cameraSelectionRepository.update { current ->
            cameraDiscoveryMergePolicy.merge(
                current = current,
                discoveredDevices = devices,
                selectedByDiscoveryId = selectedById,
            )
        }
    }

    private suspend fun persistDiscoveredDevicesSafely(
        devices: List<DiscoveredOnvifDevice>,
        selectedById: Map<String, Boolean>,
    ): Boolean =
        try {
            persistDiscoveredDevices(devices, selectedById)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }

    private fun continueToCameraSetup() {
        if (!allowsRouteAction(LocalRouteSurface.Discovery)) return
        val selectedIds =
            discoveryContent.cameras().filter(DiscoveryCameraUiModel::selected).map { it.id }
        if (selectedIds.isEmpty()) return
        savedCameraBootstrapGate.skip()
        val expectedRoute = route
        val operationGeneration = supersedeRouteOperations()
        val devices = latestDiscoveryDevices.toList()
        val selectedById = currentDiscoverySelections()
        routeJob = viewModelScope.launch {
            try {
                if (
                    devices.isNotEmpty() && !persistDiscoveredDevicesSafely(devices, selectedById)
                ) {
                    publish()
                    return@launch
                }
                if (!isRouteOperationCurrent(operationGeneration, expectedRoute)) return@launch
                playbackCoordinator.leaveScreen()
                route = CamGridRoute.CameraSetup
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the actionable discovery results visible when persistence fails.
            }
            publish()
        }
    }

    private fun updateCameraSelection(cameraId: String, selected: Boolean) {
        val camera = configuration.cameras.firstOrNull { it.id == cameraId } ?: return
        if (camera.selected == selected) return
        selectionUpdateCameraId = cameraId
        connectionStates = connectionStates - cameraId
        publish()
        viewModelScope.launch {
            try {
                container.cameraSelectionRepository.update { current ->
                    if (current.cameras.none { it.id == cameraId }) return@update current
                    val selectedIds =
                        current.selectedCameras().map(CameraDevice::id).toMutableList()
                    if (selected) {
                        if (cameraId !in selectedIds) selectedIds += cameraId
                    } else {
                        selectedIds.remove(cameraId)
                    }
                    val orderById = selectedIds.withIndex().associate { it.value to it.index }
                    current.copy(
                        cameras =
                            current.cameras.map { camera ->
                                val order = orderById[camera.id]
                                camera.copy(selected = order != null, selectionOrder = order)
                            }
                    )
                }
                configuration = container.cameraSelectionRepository.current()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the last repository-backed selection when the mutation fails.
            } finally {
                selectionUpdateCameraId = null
                publish()
            }
        }
    }

    private fun saveCameraName() {
        val cameraId = setupDraft.editingCameraId ?: return
        val name = setupDraft.editedCameraName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            try {
                container.cameraSelectionRepository.renameCamera(cameraId, name)
                setupDraft = setupDraft.copy(editingCameraId = null, editedCameraName = "")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the editor open so the operation can be retried.
            }
            publish()
        }
    }

    private fun changeSharedProfile(enabled: Boolean) {
        val previousValue = setupDraft.useSharedProfile
        if (previousValue == enabled) return
        val operation = sharedProfileUpdateGate.tryStart(enabled) ?: return
        setupDraft = setupDraft.copy(useSharedProfile = enabled)
        invalidateSuccessfulConnectionTests()
        publish()
        viewModelScope.launch {
            var persistenceSucceeded = false
            try {
                val current = container.cameraSelectionRepository.current()
                if (enabled && current.credentialProfiles.any { it.id == SHARED_PROFILE_ID }) {
                    assignProfileToSelected(SHARED_PROFILE_ID)
                } else if (!enabled) {
                    container.cameraSelectionRepository.update { latest ->
                        latest.copy(
                            cameras =
                                latest.cameras.map { camera ->
                                    if (
                                        camera.selected &&
                                            camera.credentialProfileId == SHARED_PROFILE_ID
                                    ) {
                                        camera.copy(credentialProfileId = null)
                                    } else {
                                        camera
                                    }
                                }
                        )
                    }
                }
                val latest = container.cameraSelectionRepository.current()
                if (sharedProfileUpdateGate.isCurrent(operation)) {
                    configuration = latest
                    persistenceSucceeded = true
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Restore the prior mode so the UI never claims a write that did not complete.
            } finally {
                if (sharedProfileUpdateGate.finish(operation)) {
                    if (!persistenceSucceeded) {
                        setupDraft = setupDraft.copy(useSharedProfile = previousValue)
                    }
                    publish()
                }
            }
        }
    }

    private fun testConnection(cameraId: String) {
        if (
            credentialRecovery != CredentialRecoveryUiState.NotRequired ||
                route != CamGridRoute.CameraSetup ||
                !foreground ||
                selectionUpdateCameraId != null ||
                sharedProfileUpdateGate.active != null ||
                permission != LocalNetworkPermissionUiState.Granted
        ) {
            return
        }
        val camera =
            configuration.cameras.firstOrNull { it.id == cameraId && it.selected } ?: return
        val operation = connectionTestGate.tryStart(cameraId) ?: return
        stopConnectionPreviewIfActive()
        playbackCoordinator.leaveScreen()
        connectionStates = connectionStates + (cameraId to ConnectionTestUiState.Testing)
        publish()
        connectionTestJob = viewModelScope.launch {
            try {
                val selectedCamera =
                    container.cameraSelectionRepository.current().cameras.firstOrNull {
                        it.id == cameraId && it.selected
                    }
                if (selectedCamera == null) {
                    completeConnectionTest(operation, ConnectionTestUiState.NotTested)
                    return@launch
                }
                val profileId = credentialProfileForTest(selectedCamera)
                if (profileId == null) {
                    completeConnectionTest(
                        operation,
                        ConnectionTestUiState.CredentialsRequired,
                    )
                    return@launch
                }
                val current = container.cameraSelectionRepository.current()
                val currentCamera = current.cameras.firstOrNull { it.id == cameraId && it.selected }
                val request = currentCamera?.let {
                    playbackRequest(
                        camera = it,
                        profileId = profileId,
                        credentialProfiles = current.credentialProfiles,
                        stream = RtspStream.SECONDARY,
                    )
                }
                if (request == null) {
                    completeConnectionTest(operation, ConnectionTestUiState.Failed)
                    return@launch
                }
                if (!connectionTestGate.isCurrent(operation)) return@launch
                connectionTestPreviewGate.replace(cameraId)
                publish()
                playbackCoordinator.showGrid(listOf(request))
                delay(CONNECTION_TEST_TIMEOUT_MILLIS)
                if (connectionTestGate.isCurrent(operation)) {
                    completeConnectionTest(operation, ConnectionTestUiState.Failed)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (!connectionTestGate.isCurrent(operation)) return@launch
                if (failure.requiresCredentialRecovery()) {
                    showCredentialRecoveryRequired()
                } else {
                    completeConnectionTest(operation, ConnectionTestUiState.Failed)
                }
            }
        }
    }

    private fun clearStoredCredentials() {
        if (
            credentialRecovery == CredentialRecoveryUiState.NotRequired ||
                credentialRecovery == CredentialRecoveryUiState.Clearing
        ) {
            return
        }

        stopPlaybackAndTransientWork()
        connectionStates = emptyMap()
        credentialRecovery = CredentialRecoveryUiState.Clearing
        route = CamGridRoute.CameraSetup
        publish()

        viewModelScope.launch {
            try {
                container.credentialRecovery.clearStoredCredentials()
                credentialRecovery = CredentialRecoveryUiState.NotRequired
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                credentialRecovery = CredentialRecoveryUiState.ClearFailed
            }
            publish()
        }
    }

    private fun showCredentialRecoveryRequired() {
        if (permission != LocalNetworkPermissionUiState.Granted) return
        supersedeRouteOperations()
        discoveryJob?.cancel()
        discoveryJob = null
        discoveryFinalizationJob?.cancel()
        discoveryFinalizationJob = null
        connectionTestGate.cancelActive()
        connectionTestJob?.cancel()
        connectionTestJob = null
        connectionTestPreviewGate.cancelActive()
        connectionPreviewReleaseJob?.cancel()
        connectionPreviewReleaseJob = null
        playbackCoordinator.leaveScreen()
        playbackStates = emptyMap()
        connectionStates = emptyMap()
        setupDraft = setupDraft.copy(username = "", password = "", submitting = false)
        credentialRecovery = CredentialRecoveryUiState.Required
        route = CamGridRoute.CameraSetup
        publish()
    }

    private suspend fun credentialProfileForTest(camera: CameraDevice): String? {
        if (setupDraft.password.isNotEmpty() || setupDraft.username.isNotEmpty()) {
            return persistCredential(camera)
        }
        return ConnectionTestCredentialProfilePolicy.storedProfileForTest(
            assignedProfileId = camera.credentialProfileId,
            useSharedProfile = setupDraft.useSharedProfile,
            sharedProfileId = SHARED_PROFILE_ID,
        )
    }

    private suspend fun persistCredential(camera: CameraDevice): String? {
        val draft = setupDraft
        val passwordChars = draft.password.toCharArray()
        try {
            if (
                CredentialValidator.validate(draft.username, passwordChars)
                    !is CredentialValidationResult.Valid
            ) {
                return null
            }
            val profileId =
                if (draft.useSharedProfile) SHARED_PROFILE_ID else cameraProfileId(camera.id)
            val currentConfiguration = container.cameraSelectionRepository.current()
            val secretId =
                CredentialSecretIdResolver.resolve(
                    profileId = profileId,
                    profiles = currentConfiguration.credentialProfiles,
                ) ?: profileId
            CredentialSecret(draft.username, passwordChars).use { secret ->
                container.credentialSecretStore.put(secretId, secret)
            }
            val now = System.currentTimeMillis()
            container.cameraSelectionRepository.update { current ->
                val oldProfile = current.credentialProfiles.firstOrNull { it.id == profileId }
                val profile =
                    CredentialProfile(
                        id = profileId,
                        displayName = profileId,
                        secretId = oldProfile?.secretId ?: secretId,
                        createdAtEpochMillis = oldProfile?.createdAtEpochMillis ?: now,
                        updatedAtEpochMillis = maxOf(now, oldProfile?.createdAtEpochMillis ?: now),
                    )
                val profiles = current.credentialProfiles.filterNot { it.id == profileId } + profile
                val targetIds =
                    if (draft.useSharedProfile) {
                        current.cameras
                            .filter(CameraDevice::selected)
                            .mapTo(mutableSetOf(), CameraDevice::id)
                    } else {
                        setOf(camera.id)
                    }
                current.copy(
                    cameras =
                        current.cameras.map { item ->
                            if (item.id in targetIds) item.copy(credentialProfileId = profileId)
                            else item
                        },
                    credentialProfiles = profiles,
                )
            }
            setupDraft = setupDraft.copy(username = "", password = "")
            publish()
            return profileId
        } finally {
            passwordChars.fill('\u0000')
        }
    }

    private suspend fun assignProfileToSelected(profileId: String) {
        container.cameraSelectionRepository.update { current ->
            current.copy(
                cameras =
                    current.cameras.map { camera ->
                        if (camera.selected) camera.copy(credentialProfileId = profileId)
                        else camera
                    }
            )
        }
    }

    private fun updateConnectionTest(states: Map<String, PlaybackState>) {
        val operation = connectionTestGate.active
        if (operation != null) {
            val decision = ConnectionTestPlaybackPolicy.duringTest(states[operation.cameraId])
            val connectionState = decision.connectionState ?: return
            connectionStates = connectionStates + (operation.cameraId to connectionState)
            if (decision.action == ConnectionTestPlaybackAction.ContinueTesting) return
            if (!connectionTestGate.finish(operation)) return

            connectionTestJob?.cancel()
            connectionTestJob = null
            when (decision.action) {
                ConnectionTestPlaybackAction.ContinueTesting -> Unit
                ConnectionTestPlaybackAction.HoldPreview -> scheduleConnectionPreviewRelease()
                ConnectionTestPlaybackAction.ReleasePreview -> stopConnectionPreviewIfActive()
            }
            return
        }

        val preview = connectionTestPreviewGate.active ?: return
        val decision =
            ConnectionTestPlaybackPolicy.whileHoldingPreview(states[preview.cameraId]) ?: return
        val previewFailure = decision.connectionState ?: return
        connectionStates = connectionStates + (preview.cameraId to previewFailure)
        stopConnectionPreviewIfActive()
    }

    private fun completeConnectionTest(
        operation: ConnectionTestOperation,
        state: ConnectionTestUiState,
    ) {
        if (!connectionTestGate.finish(operation)) return
        connectionStates = connectionStates + (operation.cameraId to state)
        connectionTestJob = null
        stopConnectionPreviewIfActive()
        playbackCoordinator.leaveScreen()
        publish()
    }

    private fun scheduleConnectionPreviewRelease() {
        val preview = connectionTestPreviewGate.active ?: return
        connectionPreviewReleaseJob?.cancel()
        connectionPreviewReleaseJob = viewModelScope.launch {
            delay(CONNECTION_PREVIEW_HOLD_MILLIS)
            if (!connectionTestPreviewGate.finish(preview)) return@launch
            connectionPreviewReleaseJob = null
            playbackCoordinator.leaveScreen()
            playbackStates = emptyMap()
            publish()
        }
    }

    private fun stopConnectionPreviewIfActive() {
        if (connectionTestPreviewGate.active == null && connectionPreviewReleaseJob == null) return
        connectionTestPreviewGate.cancelActive()
        connectionPreviewReleaseJob?.cancel()
        connectionPreviewReleaseJob = null
        playbackCoordinator.leaveScreen()
        playbackStates = emptyMap()
    }

    private fun maybeStartSavedCameraBootstrap() {
        if (!foreground || route != CamGridRoute.Discovery) return

        when (
            SavedCameraBootstrapPolicy.decide(
                configuration = configuration.takeIf { configurationLoaded },
                permissionGranted = permission == LocalNetworkPermissionUiState.Granted,
                alreadyHandled = savedCameraBootstrapGate.handled,
            )
        ) {
            SavedCameraBootstrapDecision.AwaitConfiguration,
            SavedCameraBootstrapDecision.AwaitPermission -> Unit
            SavedCameraBootstrapDecision.Skip -> savedCameraBootstrapGate.skip()
            is SavedCameraBootstrapDecision.OpenWall -> openSavedCameraWall()
        }
    }

    private fun openSavedCameraWall() {
        val bootstrapToken = savedCameraBootstrapGate.tryStart() ?: return
        val expectedRoute = route
        val operationGeneration = supersedeRouteOperations()
        routeJob = viewModelScope.launch {
            try {
                val current = container.cameraSelectionRepository.current()
                val selected = current.selectedCameras()
                if (
                    !isRouteOperationCurrent(operationGeneration, expectedRoute) ||
                        expectedRoute != CamGridRoute.Discovery ||
                        permission != LocalNetworkPermissionUiState.Granted ||
                        !foreground ||
                        !canOpenWall(
                            selected = selected,
                            requireSuccessfulTest = false,
                            submitting = false,
                        )
                ) {
                    return@launch
                }

                val requests = gridPlaybackRequests(current)
                if (
                    !isRouteOperationCurrent(operationGeneration, expectedRoute) ||
                        permission != LocalNetworkPermissionUiState.Granted ||
                        !foreground
                ) {
                    return@launch
                }
                connectionTestGate.cancelActive()
                connectionTestJob?.cancel()
                connectionTestJob = null
                playbackCoordinator.leaveScreen()
                playbackCoordinator.showGrid(requests)
                route = CamGridRoute.Wall()
                savedCameraBootstrapGate.complete(bootstrapToken)
                publish()

                // Discovery may have persisted a newer endpoint while secrets were being read.
                // Reapplying that authoritative snapshot replaces only the engine whose endpoint
                // actually changed; an undiscovered camera remains on its saved address.
                val refreshed = container.cameraSelectionRepository.current()
                if (refreshed != current) refreshActivePlaybackPlan(refreshed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (failure.requiresCredentialRecovery()) {
                    showCredentialRecoveryRequired()
                } else {
                    // URI construction failures are not authentication failures. A valid stored
                    // endpoint remains eligible for the playback coordinator's Offline/retry path.
                    publish()
                }
            } finally {
                savedCameraBootstrapGate.cancel(bootstrapToken)
            }
        }
    }

    private fun refreshActivePlaybackPlan(latest: CameraConfiguration) {
        val expectedRoute = route
        val expectedGeneration = routeOperationGeneration
        if (
            !foreground ||
                permission != LocalNetworkPermissionUiState.Granted ||
                (expectedRoute !is CamGridRoute.Wall && expectedRoute !is CamGridRoute.Fullscreen)
        ) {
            return
        }

        playbackRefreshJob?.cancel()
        playbackRefreshJob = viewModelScope.launch {
            try {
                when (expectedRoute) {
                    is CamGridRoute.Wall -> {
                        val requests = gridPlaybackRequests(latest)
                        if (!isRouteOperationCurrent(expectedGeneration, expectedRoute)) {
                            return@launch
                        }
                        playbackCoordinator.showGrid(requests)
                    }
                    is CamGridRoute.Fullscreen -> {
                        val camera =
                            latest.selectedCameras().firstOrNull {
                                it.id == expectedRoute.cameraId
                            } ?: return@launch
                        val request = fullscreenPlaybackRequest(camera, latest)
                        if (!isRouteOperationCurrent(expectedGeneration, expectedRoute)) {
                            return@launch
                        }
                        playbackCoordinator.showFullscreen(request)
                    }
                    CamGridRoute.Discovery,
                    CamGridRoute.CameraSetup -> return@launch
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (failure.requiresCredentialRecovery()) {
                    showCredentialRecoveryRequired()
                } else {
                    // Retain the last valid plan; generic endpoint failures are not auth failures.
                    publish()
                }
            }
        }
    }

    private fun openWall() {
        if (!allowsRouteAction(LocalRouteSurface.CameraSetup)) return
        if (connectionTestGate.active != null) return
        if (!canOpenWall(requireSuccessfulTest = true)) return
        stopConnectionPreviewIfActive()
        val expectedRoute = route
        val operationGeneration = supersedeRouteOperations()
        routeJob = viewModelScope.launch {
            setupDraft = setupDraft.copy(submitting = true)
            publish()
            try {
                val current = container.cameraSelectionRepository.current()
                val selected = current.selectedCameras()
                if (
                    !isRouteOperationCurrent(operationGeneration, expectedRoute) ||
                        expectedRoute != CamGridRoute.CameraSetup ||
                        !foreground ||
                        !canOpenWall(
                            selected = selected,
                            requireSuccessfulTest = true,
                            submitting = false,
                        )
                ) {
                    return@launch
                }
                connectionTestGate.cancelActive()
                connectionTestJob?.cancel()
                connectionTestJob = null
                val requests = gridPlaybackRequests(current)
                if (!isRouteOperationCurrent(operationGeneration, expectedRoute) || !foreground) {
                    return@launch
                }
                playbackCoordinator.leaveScreen()
                playbackCoordinator.showGrid(requests)
                route = CamGridRoute.Wall()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (failure.requiresCredentialRecovery()) {
                    showCredentialRecoveryRequired()
                }
                // Keep the setup route active when local persistence or secret lookup fails.
            } finally {
                setupDraft = setupDraft.copy(submitting = false, password = "")
                publish()
            }
        }
    }

    private fun openFullscreen(cameraId: String) {
        if (!allowsRouteAction(LocalRouteSurface.Wall)) return
        val expectedRoute = route
        val operationGeneration = supersedeRouteOperations()
        routeJob = viewModelScope.launch {
            try {
                val current = container.cameraSelectionRepository.current()
                val camera =
                    current.selectedCameras().firstOrNull { it.id == cameraId } ?: return@launch
                val request = fullscreenPlaybackRequest(camera, current)
                if (
                    !isRouteOperationCurrent(operationGeneration, expectedRoute) ||
                        expectedRoute !is CamGridRoute.Wall ||
                        !foreground
                ) {
                    return@launch
                }
                playbackCoordinator.showFullscreen(request)
                route = CamGridRoute.Fullscreen(cameraId)
                publish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (failure.requiresCredentialRecovery()) {
                    showCredentialRecoveryRequired()
                }
                // Route/URI failures remain local playback failures; never present them as auth.
                publish()
            }
        }
    }

    private fun restoreWall() {
        if (!allowsRouteAction(LocalRouteSurface.Fullscreen)) return
        val cameraId = (route as? CamGridRoute.Fullscreen)?.cameraId ?: return
        val expectedRoute = route
        val operationGeneration = supersedeRouteOperations()
        routeJob = viewModelScope.launch {
            try {
                val current = container.cameraSelectionRepository.current()
                val requests = gridPlaybackRequests(current)
                if (!isRouteOperationCurrent(operationGeneration, expectedRoute) || !foreground) {
                    return@launch
                }
                playbackCoordinator.showGrid(requests)
                route = CamGridRoute.Wall(restoreFocusCameraId = cameraId)
                publish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (failure.requiresCredentialRecovery()) {
                    showCredentialRecoveryRequired()
                }
                // Keep fullscreen active if the grid cannot be reconstructed safely.
            }
        }
    }

    private suspend fun playbackRequest(
        camera: CameraDevice,
        profileId: String?,
        credentialProfiles: List<CredentialProfile>,
        stream: RtspStream,
        fallbackStream: RtspStream? = null,
    ): PlaybackRequest {
        val secretId =
            CredentialSecretIdResolver.resolve(profileId, credentialProfiles)
                ?: throw SecretRecoveryRequiredException(
                    "Stored camera credentials require recovery"
                )
        val secret =
            container.credentialSecretStore.get(secretId)
                ?: throw SecretRecoveryRequiredException(
                    "Stored camera credentials require recovery"
                )
        try {
            val passwordChars = secret.copyPassword()
            try {
                val password = String(passwordChars)
                val uri =
                    container.rtspUriFactory.create(
                        username = secret.username,
                        password = password,
                        host = camera.host,
                        port = camera.rtspPort,
                        stream = stream,
                    )
                val fallbackUri = fallbackStream?.let { fallback ->
                    container.rtspUriFactory.create(
                        username = secret.username,
                        password = password,
                        host = camera.host,
                        port = camera.rtspPort,
                        stream = fallback,
                    )
                }
                return PlaybackRequest(
                    slotId = camera.id,
                    uri = uri,
                    fallbackUri = fallbackUri,
                )
            } finally {
                passwordChars.fill('\u0000')
            }
        } finally {
            secret.close()
        }
    }

    private suspend fun playbackRequest(
        camera: CameraDevice,
        configuration: CameraConfiguration,
        stream: RtspStream,
    ): PlaybackRequest =
        playbackRequest(
            camera = camera,
            profileId = camera.credentialProfileId,
            credentialProfiles = configuration.credentialProfiles,
            stream = stream,
        )

    private suspend fun gridPlaybackRequests(
        configuration: CameraConfiguration
    ): List<PlaybackRequest> =
        configuration.selectedCameras().map { camera ->
            playbackRequest(camera, configuration, RtspStream.SECONDARY)
        }

    private suspend fun fullscreenPlaybackRequest(
        camera: CameraDevice,
        configuration: CameraConfiguration,
    ): PlaybackRequest =
        playbackRequest(
            camera = camera,
            profileId = camera.credentialProfileId,
            credentialProfiles = configuration.credentialProfiles,
            stream = RtspStream.PRIMARY,
            fallbackStream = RtspStream.SECONDARY,
        )

    private fun returnToDiscoveryAndScan() {
        stopPlaybackAndTransientWork()
        savedCameraBootstrapGate.skip()
        discoveryContent = DiscoveryContentUiState.Ready
        route = CamGridRoute.Discovery
        publish()
        startDiscovery()
    }

    private fun stopPlaybackAndTransientWork() {
        supersedeRouteOperations()
        val testingOperation = connectionTestGate.cancelActive()
        if (
            testingOperation != null &&
                connectionStates[testingOperation.cameraId] == ConnectionTestUiState.Testing
        ) {
            connectionStates =
                connectionStates + (testingOperation.cameraId to ConnectionTestUiState.NotTested)
        }
        connectionTestJob?.cancel()
        connectionTestJob = null
        connectionTestPreviewGate.cancelActive()
        connectionPreviewReleaseJob?.cancel()
        connectionPreviewReleaseJob = null
        playbackCoordinator.leaveScreen()
        playbackStates = emptyMap()
        setupDraft = setupDraft.copy(username = "", password = "", submitting = false)
    }

    private fun supersedeRouteOperations(): Long {
        routeOperationGeneration += 1
        playbackRefreshJob?.cancel()
        playbackRefreshJob = null
        routeJob?.cancel()
        routeJob = null
        return routeOperationGeneration
    }

    private fun isRouteOperationCurrent(
        generation: Long,
        expectedRoute: CamGridRoute,
    ): Boolean =
        generation == routeOperationGeneration &&
            route == expectedRoute &&
            foreground &&
            permission == LocalNetworkPermissionUiState.Granted &&
            credentialRecovery == CredentialRecoveryUiState.NotRequired

    private fun allowsRouteAction(expectedSurface: LocalRouteSurface): Boolean =
        LocalRouteActionPolicy.allows(
            foreground = foreground,
            permissionUiState = permission,
            currentRoute = route,
            expectedSurface = expectedSurface,
        )

    private fun permissionDecision(
        shouldShowRationale: Boolean,
        requestCompleted: Boolean,
    ): LocalNetworkAccessDecision =
        LocalNetworkAccessPolicy.decide(
            permissionState = container.permissionCoordinator.state(shouldShowRationale),
            requestCompleted = requestCompleted,
            currentRoute = route,
        )

    private fun applyPermissionDecision(decision: LocalNetworkAccessDecision) {
        permission = decision.permissionUiState
        if (decision.stopLocalWork) {
            savedCameraBootstrapGate.cancelActive()
            discoveryJob?.cancel()
            discoveryJob = null
            discoveryFinalizationJob?.cancel()
            discoveryFinalizationJob = null
            stopPlaybackAndTransientWork()
            connectionStates = emptyMap()
            discoveryContent = DiscoveryContentUiState.Ready
            route = decision.route
        }
    }

    private fun canOpenWall(
        selected: List<CameraDevice> = configuration.selectedCameras(),
        requireSuccessfulTest: Boolean,
        submitting: Boolean = setupDraft.submitting,
    ): Boolean =
        CameraSetupReadiness.canStartWatching(
            selectedCameraIds = selected.mapTo(mutableSetOf(), CameraDevice::id),
            camerasWithCredentialProfiles =
                selected
                    .filter { it.credentialProfileId != null }
                    .mapTo(mutableSetOf(), CameraDevice::id),
            connectionStates = connectionStates,
            submitting = submitting,
            credentialRecovery = credentialRecovery,
            requireSuccessfulTest = requireSuccessfulTest,
        )

    private fun buildUiState(): CamGridAppUiState {
        val selected = configuration.selectedCameras()
        val effectivePlaybackStates = selected.associate { camera ->
            camera.id to (playbackStates[camera.id] ?: PlaybackState.Idle)
        }
        val wall =
            CameraWallUiState(
                cameras =
                    selected.map { camera ->
                        WallCameraUiModel(
                            id = camera.id,
                            displayName = camera.displayName,
                            playbackState = effectivePlaybackStates.getValue(camera.id),
                        )
                    },
                restoreFocusCameraId = (route as? CamGridRoute.Wall)?.restoreFocusCameraId,
            )
        val fullscreenCameraId = (route as? CamGridRoute.Fullscreen)?.cameraId
        val fullscreenCamera = selected.firstOrNull { it.id == fullscreenCameraId }
        return CamGridAppUiState(
            route = route,
            discovery = DiscoveryUiState(permission = permission, content = discoveryContent),
            cameraSetup =
                CameraSetupUiState(
                    cameras =
                        configuration.cameras.map { camera ->
                            SetupCameraUiModel(
                                id = camera.id,
                                displayName = camera.displayName,
                                detail = camera.model ?: camera.manufacturer,
                                selected = camera.selected,
                                hasCredentialProfile = camera.credentialProfileId != null,
                                connectionState =
                                    connectionStates[camera.id] ?: ConnectionTestUiState.NotTested,
                            )
                        },
                    username = setupDraft.username,
                    password = setupDraft.password,
                    useSharedProfile = setupDraft.useSharedProfile,
                    editingCameraId = setupDraft.editingCameraId,
                    editedCameraName = setupDraft.editedCameraName,
                    credentialRecovery = credentialRecovery,
                    canStartWatching = canOpenWall(selected, requireSuccessfulTest = true),
                    submitting = setupDraft.submitting,
                    selectionUpdateCameraId = selectionUpdateCameraId,
                    sharedProfileUpdateInProgress = sharedProfileUpdateGate.active != null,
                    connectionPreviewCameraId = connectionTestPreviewGate.active?.cameraId,
                ),
            wall = wall,
            fullscreen =
                fullscreenCamera?.let { camera ->
                    FullscreenUiState(
                        cameraId = camera.id,
                        displayName = camera.displayName,
                        playbackState = effectivePlaybackStates.getValue(camera.id),
                        viewMode = fullscreenViewMode,
                    )
                },
            playbackEngineGenerations = playbackEngineGenerations,
        )
    }

    private fun publish() {
        mutableUiState.value = buildUiState()
    }

    private fun currentDiscoverySelections(): Map<String, Boolean> =
        discoveryContent.cameras().associate { it.id to it.selected }

    private fun captureLiveDiscoverySelections() {
        discoverySelectionOverrides = discoverySelectionOverrides + currentDiscoverySelections()
    }

    private fun discoveryUiModels(
        devices: List<DiscoveredOnvifDevice>,
        selectedById: Map<String, Boolean>,
    ): List<DiscoveryCameraUiModel> = devices.map { device ->
        val persistedCamera =
            cameraDiscoveryMergePolicy.matchingCamera(configuration.cameras, device)
        device.toUiModel(
            selected =
                selectedById[device.id]
                    ?: persistedCamera?.let { selectedById[it.id] }
                    ?: persistedCamera?.selected
                    ?: false
        )
    }

    private fun synchronizeDiscoverySelections() {
        val selectedById = configuration.cameras.associate { it.id to it.selected }
        discoveryContent = discoveryContent.mapCameras { camera ->
            camera.copy(
                selected =
                    discoverySelectionOverrides[camera.id]
                        ?: selectedById[camera.id]
                        ?: camera.selected
            )
        }
    }

    private fun DiscoveredOnvifDevice.toUiModel(selected: Boolean): DiscoveryCameraUiModel =
        DiscoveryCameraUiModel(
            id = id,
            displayName = discoveredName,
            detail = model ?: manufacturer,
            selected = selected,
        )

    private fun DiscoveryContentUiState.cameras(): List<DiscoveryCameraUiModel> =
        when (this) {
            is DiscoveryContentUiState.Results -> cameras
            is DiscoveryContentUiState.Scanning -> camerasFound
            is DiscoveryContentUiState.Error -> emptyList()
            DiscoveryContentUiState.Ready,
            DiscoveryContentUiState.Loading,
            DiscoveryContentUiState.Empty -> emptyList()
        }

    private fun DiscoveryContentUiState.mapCameras(
        transform: (DiscoveryCameraUiModel) -> DiscoveryCameraUiModel
    ): DiscoveryContentUiState =
        when (this) {
            is DiscoveryContentUiState.Results -> copy(cameras = cameras.map(transform))
            is DiscoveryContentUiState.Scanning -> copy(camerasFound = camerasFound.map(transform))
            is DiscoveryContentUiState.Error -> this
            DiscoveryContentUiState.Ready,
            DiscoveryContentUiState.Loading,
            DiscoveryContentUiState.Empty -> this
        }

    private fun cameraProfileId(cameraId: String): String {
        val digest =
            MessageDigest.getInstance("SHA-256")
                .digest(cameraId.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return "camera-$digest"
    }

    private fun invalidateSuccessfulConnectionTests() {
        connectionStates = connectionStates.mapValues { (_, state) ->
            if (state == ConnectionTestUiState.Connected) {
                ConnectionTestUiState.NotTested
            } else {
                state
            }
        }
    }

    private data class SetupDraft(
        val username: String = "",
        val password: String = "",
        val useSharedProfile: Boolean = true,
        val editingCameraId: String? = null,
        val editedCameraName: String = "",
        val submitting: Boolean = false,
    )

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CamGridViewModel::class.java)) {
                "Unsupported ViewModel class"
            }
            return CamGridViewModel(container) as T
        }
    }

    private companion object {
        const val SHARED_PROFILE_ID = "shared-camera-account"
        const val CONNECTION_TEST_TIMEOUT_MILLIS = 25_000L
        const val CONNECTION_PREVIEW_HOLD_MILLIS = 5_000L
    }
}

internal fun Throwable.requiresCredentialRecovery(): Boolean =
    this is SecretRecoveryRequiredException
