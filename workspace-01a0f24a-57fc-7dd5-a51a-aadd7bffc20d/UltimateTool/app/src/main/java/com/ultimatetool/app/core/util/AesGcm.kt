package com.ultimatetool.app.core.util

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM с ключом из пароля (PBKDF2-HMAC-SHA256).
 * Формат контейнера: base64( salt[16] | iv[12] | ciphertext+tag ).
 */
object AesGcm {

    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val ITERATIONS = 60_000
    private const val KEY_BITS = 256

    fun encrypt(plainText: String, passphrase: String): String {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val container = ByteArray(SALT_LEN + IV_LEN + ciphertext.size)
        System.arraycopy(salt, 0, container, 0, SALT_LEN)
        System.arraycopy(iv, 0, container, SALT_LEN, IV_LEN)
        System.arraycopy(ciphertext, 0, container, SALT_LEN + IV_LEN, ciphertext.size)
        return Base64.getEncoder().encodeToString(container)
    }

    fun decrypt(encoded: String, passphrase: String): String {
        val container = Base64.getDecoder().decode(encoded.trim())
        require(container.size > SALT_LEN + IV_LEN) { "Повреждённый контейнер" }
        val salt = container.copyOfRange(0, SALT_LEN)
        val iv = container.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val ciphertext = container.copyOfRange(SALT_LEN + IV_LEN, container.size)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
