package io.github.serkankaracan.camgridtv.model

/** Metadata for encrypted credentials. This type never contains a username or password. */
data class CredentialProfile(
    val id: String,
    val displayName: String,
    val secretId: String = id,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
) {
    init {
        require(id.isNotBlank() && id.length <= MAX_ID_LENGTH) {
            "Credential profile id is invalid"
        }
        require(secretId.isNotBlank() && secretId.length <= MAX_ID_LENGTH) {
            "Credential secret id is invalid"
        }
        require(displayName.isNotBlank() && displayName.length <= MAX_DISPLAY_NAME_LENGTH) {
            "Credential profile display name is invalid"
        }
        require(createdAtEpochMillis >= 0L) { "Created time is invalid" }
        require(updatedAtEpochMillis >= createdAtEpochMillis) { "Updated time is invalid" }
    }

    companion object {
        const val MAX_ID_LENGTH = 256
        const val MAX_DISPLAY_NAME_LENGTH = 120
    }
}
