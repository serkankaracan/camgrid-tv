package io.github.serkankaracan.camgridtv.discovery

import io.github.serkankaracan.camgridtv.data.CameraSelectionRepository

class CameraDiscoveryReconciler(
    private val cameraRepository: CameraSelectionRepository,
    private val identityMatcher: CameraIdentityMatcher = CameraIdentityMatcher(),
) {
    suspend fun reconcile(discoveredDevices: List<DiscoveredOnvifDevice>) {
        cameraRepository.update { configuration ->
            configuration.copy(
                cameras =
                    configuration.cameras.map { camera ->
                        val match =
                            discoveredDevices
                                .map { it to identityMatcher.match(camera, it) }
                                .filter { it.second.isMatch }
                                .minByOrNull { it.second.strength.ordinal }
                                ?.first
                        if (match == null) camera else identityMatcher.applyDiscovery(camera, match)
                    }
            )
        }
    }
}
