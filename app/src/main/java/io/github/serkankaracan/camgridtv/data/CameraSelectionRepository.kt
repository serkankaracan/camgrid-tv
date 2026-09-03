package io.github.serkankaracan.camgridtv.data

import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import io.github.serkankaracan.camgridtv.model.CameraDevice
import io.github.serkankaracan.camgridtv.model.CredentialProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface CameraSelectionRepository {
    val configuration: Flow<CameraConfiguration>

    suspend fun update(transform: (CameraConfiguration) -> CameraConfiguration)

    suspend fun current(): CameraConfiguration = configuration.first()

    suspend fun upsertCamera(camera: CameraDevice) {
        update { current ->
            val index = current.cameras.indexOfFirst { it.id == camera.id }
            val cameras = current.cameras.toMutableList()
            if (index >= 0) cameras[index] = camera else cameras += camera
            current.copy(cameras = cameras)
        }
    }

    suspend fun removeCamera(cameraId: String) {
        update { current ->
            current.copy(cameras = current.cameras.filterNot { it.id == cameraId })
        }
    }

    suspend fun renameCamera(cameraId: String, displayName: String) {
        val safeName = displayName.trim()
        require(safeName.isNotEmpty()) { "Camera display name is required" }
        update { current ->
            require(current.cameras.any { it.id == cameraId }) { "Unknown camera id" }
            current.copy(
                cameras =
                    current.cameras.map {
                        if (it.id == cameraId) it.copy(displayName = safeName) else it
                    }
            )
        }
    }

    suspend fun setSelectedCameraIds(cameraIdsInOrder: List<String>) {
        require(cameraIdsInOrder.distinct().size == cameraIdsInOrder.size) {
            "Camera selection contains duplicates"
        }
        update { current ->
            val existingIds = current.cameras.mapTo(mutableSetOf(), CameraDevice::id)
            require(cameraIdsInOrder.all(existingIds::contains)) {
                "Camera selection contains an unknown id"
            }
            val orderById = cameraIdsInOrder.withIndex().associate { it.value to it.index }
            current.copy(
                cameras =
                    current.cameras.map { camera ->
                        val order = orderById[camera.id]
                        camera.copy(selected = order != null, selectionOrder = order)
                    }
            )
        }
    }

    suspend fun upsertCredentialProfile(profile: CredentialProfile) {
        update { current ->
            val index = current.credentialProfiles.indexOfFirst { it.id == profile.id }
            val profiles = current.credentialProfiles.toMutableList()
            if (index >= 0) profiles[index] = profile else profiles += profile
            current.copy(credentialProfiles = profiles)
        }
    }

    suspend fun assignCredentialProfile(cameraId: String, profileId: String?) {
        update { current ->
            require(current.cameras.any { it.id == cameraId }) { "Unknown camera id" }
            require(profileId == null || current.credentialProfiles.any { it.id == profileId }) {
                "Unknown credential profile id"
            }
            current.copy(
                cameras =
                    current.cameras.map {
                        if (it.id == cameraId) it.copy(credentialProfileId = profileId) else it
                    }
            )
        }
    }

    suspend fun removeCredentialProfile(profileId: String) {
        update { current ->
            current.copy(
                cameras =
                    current.cameras.map {
                        if (it.credentialProfileId == profileId) it.copy(credentialProfileId = null)
                        else it
                    },
                credentialProfiles = current.credentialProfiles.filterNot { it.id == profileId },
            )
        }
    }
}
