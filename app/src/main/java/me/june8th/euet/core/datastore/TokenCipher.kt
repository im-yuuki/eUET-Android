package me.june8th.euet.core.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts bearer tokens at rest using an AES-GCM key held in the Android Keystore, so the key
 * material never enters the app process and the ciphertext in DataStore is useless on its own.
 *
 * Deliberately avoids `androidx.security:security-crypto` — `EncryptedSharedPreferences` is
 * deprecated, and the platform Keystore covers this without an extra dependency.
 *
 * Values are stored as Base64 of `IV || ciphertext`. A fresh random IV is generated per encryption,
 * as required for GCM.
 */
class TokenCipher {

    /** Returns Base64(`IV || ciphertext`), or null if encryption is unavailable. */
    fun encrypt(plaintext: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val ciphertext = cipher.doFinal(plaintext.toByteArray())
        Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to encrypt token", e)
        null
    }

    /**
     * Reverses [encrypt]. Returns null when the value can't be decrypted — a legacy plaintext
     * value, a rotated/invalidated key, or tampering. Callers treat null as "no session" and
     * re-authenticate, which fails closed.
     */
    fun decrypt(stored: String): String? = try {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        if (bytes.size <= IV_LENGTH) return null
        val iv = bytes.copyOfRange(0, IV_LENGTH)
        val ciphertext = bytes.copyOfRange(IV_LENGTH, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        }
        String(cipher.doFinal(ciphertext))
    } catch (e: Exception) {
        Log.w(TAG, "Failed to decrypt token; treating as no session")
        null
    }

    /** Fetches the existing Keystore key, generating one on first use. */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    // Readable while the device is locked so background refresh keeps working;
                    // the key is still non-exportable and device-bound.
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val TAG = "TokenCipher"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "euet_session_token_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
    }
}
