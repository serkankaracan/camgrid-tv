package io.github.serkankaracan.camgridtv.model

/**
 * Persisted, non-secret camera configuration.
 *
 * Credentials are deliberately referenced by identifier and are stored separately by the credential
 * secret store. [displayName] is user-facing and is preserved when discovery updates an address.
 */
data class CameraDevice(
    val id: String,
    val endpointUuid: String? = null,
    val onvifXAddr: String? = null,
    val displayName: String,
    val discoveredName: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val host: String,
    val onvifPort: Int = DEFAULT_ONVIF_PORT,
    val rtspPort: Int = DEFAULT_RTSP_PORT,
    val credentialProfileId: String? = null,
    val selected: Boolean = false,
    val selectionOrder: Int? = null,
    val lastSeenEpochMillis: Long,
) {
    init {
        require(id.isNotBlank() && id.length <= MAX_ID_LENGTH) { "Camera id is invalid" }
        require(displayName.isNotBlank() && displayName.length <= MAX_DISPLAY_NAME_LENGTH) {
            "Camera display name is invalid"
        }
        require(host.isNotBlank() && host.length <= MAX_HOST_LENGTH) { "Camera host is invalid" }
        require(onvifPort in VALID_PORTS) { "ONVIF port is invalid" }
        require(rtspPort in VALID_PORTS) { "RTSP port is invalid" }
        require(lastSeenEpochMillis >= 0L) { "Last-seen time is invalid" }
        require(selectionOrder == null || selectionOrder >= 0) { "Selection order is invalid" }
        require(selected || selectionOrder == null) {
            "An unselected camera cannot have a selection order"
        }
        endpointUuid?.let {
            require(it.isNotBlank() && it.length <= MAX_ENDPOINT_LENGTH) {
                "Endpoint identifier is invalid"
            }
        }
        credentialProfileId?.let {
            require(it.isNotBlank() && it.length <= MAX_ID_LENGTH) {
                "Credential profile identifier is invalid"
            }
        }
    }

    companion object {
        const val DEFAULT_ONVIF_PORT = 2020
        const val DEFAULT_RTSP_PORT = 554
        const val MAX_DISPLAY_NAME_LENGTH = 120
        const val MAX_HOST_LENGTH = 253
        const val MAX_ID_LENGTH = 256
        const val MAX_ENDPOINT_LENGTH = 512
        val VALID_PORTS: IntRange = 1..65535
    }
}
