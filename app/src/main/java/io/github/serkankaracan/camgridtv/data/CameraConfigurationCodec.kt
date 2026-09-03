package io.github.serkankaracan.camgridtv.data

import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import io.github.serkankaracan.camgridtv.model.CameraDevice
import io.github.serkankaracan.camgridtv.model.CredentialProfile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64

internal object CameraConfigurationCodec {
    private const val MAGIC = 0x43475456 // CGTV
    private const val MAX_ENCODED_BYTES = 2 * 1024 * 1024
    private const val MAX_STRING_BYTES = 64 * 1024

    fun encode(configuration: CameraConfiguration): String {
        val bytes =
            ByteArrayOutputStream().use { byteStream ->
                DataOutputStream(byteStream).use { output ->
                    output.writeInt(MAGIC)
                    output.writeInt(configuration.schemaVersion)
                    output.writeInt(configuration.cameras.size)
                    configuration.cameras.forEach { camera ->
                        output.writeString(camera.id)
                        output.writeNullableString(camera.endpointUuid)
                        output.writeNullableString(camera.onvifXAddr)
                        output.writeString(camera.displayName)
                        output.writeNullableString(camera.discoveredName)
                        output.writeNullableString(camera.manufacturer)
                        output.writeNullableString(camera.model)
                        output.writeString(camera.host)
                        output.writeInt(camera.onvifPort)
                        output.writeInt(camera.rtspPort)
                        output.writeNullableString(camera.credentialProfileId)
                        output.writeBoolean(camera.selected)
                        output.writeNullableInt(camera.selectionOrder)
                        output.writeLong(camera.lastSeenEpochMillis)
                    }
                    output.writeInt(configuration.credentialProfiles.size)
                    configuration.credentialProfiles.forEach { profile ->
                        output.writeString(profile.id)
                        output.writeString(profile.displayName)
                        output.writeString(profile.secretId)
                        output.writeLong(profile.createdAtEpochMillis)
                        output.writeLong(profile.updatedAtEpochMillis)
                    }
                }
                byteStream.toByteArray()
            }
        check(bytes.size <= MAX_ENCODED_BYTES) { "Camera configuration is too large" }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun decode(encoded: String): CameraConfiguration {
        val bytes =
            try {
                Base64.getUrlDecoder().decode(encoded)
            } catch (_: IllegalArgumentException) {
                throw CameraConfigurationStorageException("Stored camera configuration is invalid")
            }
        if (bytes.size > MAX_ENCODED_BYTES) {
            throw CameraConfigurationStorageException("Stored camera configuration is too large")
        }
        return try {
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                if (input.readInt() != MAGIC) {
                    throw CameraConfigurationStorageException(
                        "Stored camera configuration has an invalid header"
                    )
                }
                val schemaVersion = input.readInt()
                val cameraCount = input.readBoundedCount(CameraConfiguration.MAX_CAMERAS)
                val cameras =
                    List(cameraCount) {
                        CameraDevice(
                            id = input.readString(),
                            endpointUuid = input.readNullableString(),
                            onvifXAddr = input.readNullableString(),
                            displayName = input.readString(),
                            discoveredName = input.readNullableString(),
                            manufacturer = input.readNullableString(),
                            model = input.readNullableString(),
                            host = input.readString(),
                            onvifPort = input.readInt(),
                            rtspPort = input.readInt(),
                            credentialProfileId = input.readNullableString(),
                            selected = input.readBoolean(),
                            selectionOrder = input.readNullableInt(),
                            lastSeenEpochMillis = input.readLong(),
                        )
                    }
                val profileCount =
                    input.readBoundedCount(CameraConfiguration.MAX_CREDENTIAL_PROFILES)
                val profiles =
                    List(profileCount) {
                        CredentialProfile(
                            id = input.readString(),
                            displayName = input.readString(),
                            secretId = input.readString(),
                            createdAtEpochMillis = input.readLong(),
                            updatedAtEpochMillis = input.readLong(),
                        )
                    }
                if (input.read() != -1) {
                    throw CameraConfigurationStorageException(
                        "Stored camera configuration has trailing data"
                    )
                }
                CameraConfiguration(
                    schemaVersion = schemaVersion,
                    cameras = cameras,
                    credentialProfiles = profiles,
                )
            }
        } catch (error: CameraConfigurationStorageException) {
            throw error
        } catch (_: Exception) {
            throw CameraConfigurationStorageException(
                "Stored camera configuration could not be read"
            )
        }
    }

    private fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_STRING_BYTES) { "Stored value is too long" }
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeString(value)
    }

    private fun DataOutputStream.writeNullableInt(value: Int?) {
        writeBoolean(value != null)
        if (value != null) writeInt(value)
    }

    private fun DataInputStream.readString(): String {
        val length = readInt()
        if (length !in 0..MAX_STRING_BYTES || length > available()) {
            throw CameraConfigurationStorageException("Stored text length is invalid")
        }
        return String(ByteArray(length).also { readFully(it) }, StandardCharsets.UTF_8)
    }

    private fun DataInputStream.readNullableString(): String? =
        if (readBoolean()) readString() else null

    private fun DataInputStream.readNullableInt(): Int? = if (readBoolean()) readInt() else null

    private fun DataInputStream.readBoundedCount(maximum: Int): Int =
        readInt().also {
            if (it !in 0..maximum) {
                throw CameraConfigurationStorageException("Stored item count is invalid")
            }
        }
}

class CameraConfigurationStorageException(message: String) : IllegalStateException(message)
