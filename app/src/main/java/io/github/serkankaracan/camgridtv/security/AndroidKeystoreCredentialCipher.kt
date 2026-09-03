package io.github.serkankaracan.camgridtv.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidKeystoreCredentialCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CredentialCipher {
    override suspend fun encrypt(secret: CredentialSecret): EncryptedCredential =
        withContext(ioDispatcher) {
            val payload = CredentialPayloadCodec.encode(secret)
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
                EncryptedCredential(
                    schemaVersion = ENCRYPTED_SCHEMA_VERSION,
                    initializationVector = cipher.iv,
                    ciphertext = cipher.doFinal(payload),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                throw CredentialEncryptionException("Camera credentials could not be encrypted")
            } finally {
                payload.fill(0)
            }
        }

    override suspend fun decrypt(encryptedCredential: EncryptedCredential): CredentialSecret =
        withContext(ioDispatcher) {
            if (encryptedCredential.schemaVersion != ENCRYPTED_SCHEMA_VERSION) {
                throw SecretRecoveryRequiredException(
                    "Stored camera credentials use an unsupported format"
                )
            }
            val ciphertext = encryptedCredential.ciphertext
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    getExistingKey(),
                    GCMParameterSpec(GCM_TAG_LENGTH_BITS, encryptedCredential.initializationVector),
                )
                val payload = cipher.doFinal(ciphertext)
                try {
                    CredentialPayloadCodec.decode(payload)
                } finally {
                    payload.fill(0)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecretRecoveryRequiredException) {
                throw SecretRecoveryRequiredException("Stored camera credentials require recovery")
            } catch (_: Exception) {
                throw SecretRecoveryRequiredException("Stored camera credentials require recovery")
            } finally {
                ciphertext.fill(0)
            }
        }

    private fun getExistingKey(): SecretKey =
        synchronized(KEY_LOCK) {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            (keyStore.getKey(keyAlias, null) as? SecretKey)
                ?: throw SecretRecoveryRequiredException(
                    "Stored camera credentials require recovery"
                )
        }

    private fun getOrCreateKey(): SecretKey =
        synchronized(KEY_LOCK) {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            (keyStore.getKey(keyAlias, null) as? SecretKey)
                ?: run {
                    val keyGenerator =
                        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
                    keyGenerator.init(
                        KeyGenParameterSpec.Builder(
                                keyAlias,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                            )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(KEY_SIZE_BITS)
                            .setRandomizedEncryptionRequired(true)
                            .build()
                    )
                    keyGenerator.generateKey()
                }
        }

    companion object {
        const val DEFAULT_KEY_ALIAS = "camgrid_tv_camera_credentials_v1"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256
        private const val ENCRYPTED_SCHEMA_VERSION = 1
        private val KEY_LOCK = Any()
    }
}
