package com.ultimatetool.app.core.util

import java.security.MessageDigest
import java.util.zip.CRC32

/**
 * Чистая логика хэширования (без Android-зависимостей) —
 * легко тестируется юнит-тестами и переиспользуется утилитами.
 */
object Hashing {

    val ALGORITHMS = listOf("MD5", "SHA-1", "SHA-256", "SHA-384", "SHA-512", "CRC32")

    /** Возвращает дайджест (hex) или бросает исключение при неизвестном алгоритме. */
    fun hash(algorithm: String, input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        if (algorithm == "CRC32") {
            val crc = CRC32()
            crc.update(bytes)
            return crc.value.toString(16).uppercase().padStart(8, '0')
        }
        val digest = MessageDigest.getInstance(algorithm)
        return digest.digest(bytes).toHex()
    }
}
