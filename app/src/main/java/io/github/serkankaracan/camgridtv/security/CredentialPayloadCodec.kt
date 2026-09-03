package io.github.serkankaracan.camgridtv.security

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Arrays

internal object CredentialPayloadCodec {
    private const val MAGIC = 0x43524544 // CRED
    private const val FORMAT_VERSION = 1
    private const val MAX_USERNAME_BYTES = 1_024
    private const val MAX_PASSWORD_BYTES = 8_192

    fun encode(secret: CredentialSecret): ByteArray {
        val usernameBytes = secret.username.toByteArray(StandardCharsets.UTF_8)
        val passwordChars = secret.copyPassword()
        val passwordBytes = passwordChars.toUtf8Bytes()
        try {
            require(usernameBytes.size <= MAX_USERNAME_BYTES) { "Username is too long" }
            require(passwordBytes.size <= MAX_PASSWORD_BYTES) { "Password is too long" }
            return ByteArrayOutputStream().use { byteStream ->
                DataOutputStream(byteStream).use { output ->
                    output.writeInt(MAGIC)
                    output.writeInt(FORMAT_VERSION)
                    output.writeInt(usernameBytes.size)
                    output.write(usernameBytes)
                    output.writeInt(passwordBytes.size)
                    output.write(passwordBytes)
                }
                byteStream.toByteArray()
            }
        } finally {
            Arrays.fill(passwordChars, '\u0000')
            Arrays.fill(passwordBytes, 0)
            Arrays.fill(usernameBytes, 0)
        }
    }

    fun decode(payload: ByteArray): CredentialSecret {
        require(payload.size <= MAX_USERNAME_BYTES + MAX_PASSWORD_BYTES + 32) {
            "Credential payload is too large"
        }
        return DataInputStream(ByteArrayInputStream(payload)).use { input ->
            require(input.readInt() == MAGIC) { "Credential payload header is invalid" }
            require(input.readInt() == FORMAT_VERSION) {
                "Credential payload version is unsupported"
            }
            val usernameBytes = input.readBytes(MAX_USERNAME_BYTES)
            val passwordBytes = input.readBytes(MAX_PASSWORD_BYTES)
            require(input.read() == -1) { "Credential payload has trailing data" }
            try {
                val username =
                    StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(usernameBytes))
                        .toString()
                val passwordBuffer =
                    StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(passwordBytes))
                val password = CharArray(passwordBuffer.remaining())
                passwordBuffer.get(password)
                CredentialSecret(username, password).also { Arrays.fill(password, '\u0000') }
            } finally {
                Arrays.fill(usernameBytes, 0)
                Arrays.fill(passwordBytes, 0)
            }
        }
    }

    private fun DataInputStream.readBytes(maximum: Int): ByteArray {
        val length = readInt()
        require(length in 0..maximum && length <= available()) {
            "Credential field length is invalid"
        }
        return ByteArray(length).also { readFully(it) }
    }

    private fun CharArray.toUtf8Bytes(): ByteArray {
        val encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(this))
        return ByteArray(encoded.remaining()).also { encoded.get(it) }
    }
}
