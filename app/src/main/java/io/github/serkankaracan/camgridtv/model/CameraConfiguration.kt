package io.github.serkankaracan.camgridtv.model

data class CameraConfiguration(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val cameras: List<CameraDevice> = emptyList(),
    val credentialProfiles: List<CredentialProfile> = emptyList(),
) {
    init {
        require(schemaVersion in 1..CURRENT_SCHEMA_VERSION) {
            "Unsupported camera configuration schema"
        }
        require(cameras.size <= MAX_CAMERAS) { "Too many camera records" }
        require(credentialProfiles.size <= MAX_CREDENTIAL_PROFILES) {
            "Too many credential profiles"
        }
        require(cameras.map(CameraDevice::id).distinct().size == cameras.size) {
            "Duplicate camera id"
        }
        require(
            credentialProfiles.map(CredentialProfile::id).distinct().size == credentialProfiles.size
        ) {
            "Duplicate credential profile id"
        }
        val profileIds = credentialProfiles.mapTo(mutableSetOf(), CredentialProfile::id)
        require(
            cameras.all { it.credentialProfileId == null || it.credentialProfileId in profileIds }
        ) {
            "Camera references an unknown credential profile"
        }
        val selectedOrders =
            cameras.filter(CameraDevice::selected).mapNotNull(CameraDevice::selectionOrder)
        require(selectedOrders.distinct().size == selectedOrders.size) {
            "Duplicate camera selection order"
        }
    }

    fun selectedCameras(): List<CameraDevice> =
        cameras
            .asSequence()
            .filter(CameraDevice::selected)
            .sortedWith(
                compareBy<CameraDevice> { it.selectionOrder ?: Int.MAX_VALUE }.thenBy { it.id }
            )
            .toList()

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val MAX_CAMERAS = 1_000
        const val MAX_CREDENTIAL_PROFILES = 1_000
    }
}
