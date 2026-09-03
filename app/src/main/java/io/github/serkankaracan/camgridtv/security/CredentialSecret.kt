package io.github.serkankaracan.camgridtv.security

import java.util.Arrays

/**
 * Short-lived credential material. Call [close] as soon as the caller has finished with it. Its
 * string representation is intentionally always redacted.
 */
class CredentialSecret(
    val username: String,
    password: CharArray,
) : AutoCloseable {
    private val passwordChars = password.copyOf()
    private var closed = false

    val passwordLength: Int
        get() {
            check(!closed) { "Credential secret is closed" }
            return passwordChars.size
        }

    fun copyPassword(): CharArray {
        check(!closed) { "Credential secret is closed" }
        return passwordChars.copyOf()
    }

    override fun close() {
        if (!closed) {
            Arrays.fill(passwordChars, '\u0000')
            closed = true
        }
    }

    override fun toString(): String = "CredentialSecret(username=***, password=***)"
}
