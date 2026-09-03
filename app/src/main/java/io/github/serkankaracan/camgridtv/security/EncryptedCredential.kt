package io.github.serkankaracan.camgridtv.security

class EncryptedCredential(
    val schemaVersion: Int,
    initializationVector: ByteArray,
    ciphertext: ByteArray,
) {
    private val storedInitializationVector = initializationVector.copyOf()
    private val storedCiphertext = ciphertext.copyOf()

    init {
        require(schemaVersion > 0) { "Encrypted credential schema is invalid" }
        require(storedInitializationVector.size in 12..32) { "Encryption IV is invalid" }
        require(storedCiphertext.isNotEmpty()) { "Encrypted credential is empty" }
    }

    val initializationVector: ByteArray
        get() = storedInitializationVector.copyOf()

    val ciphertext: ByteArray
        get() = storedCiphertext.copyOf()

    override fun equals(other: Any?): Boolean =
        other is EncryptedCredential &&
            schemaVersion == other.schemaVersion &&
            storedInitializationVector.contentEquals(other.storedInitializationVector) &&
            storedCiphertext.contentEquals(other.storedCiphertext)

    override fun hashCode(): Int {
        var result = schemaVersion
        result = 31 * result + storedInitializationVector.contentHashCode()
        result = 31 * result + storedCiphertext.contentHashCode()
        return result
    }

    override fun toString(): String =
        "EncryptedCredential(schemaVersion=$schemaVersion, initializationVector=***, ciphertext=***)"
}
