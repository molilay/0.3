package com.ultimatetool.app.core.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * TOTP (RFC 6238). Чистая реализация без Android-зависимостей,
 * покрыта юнит-тестами по векторам из RFC.
 */
object Totp {

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /** Текущий код для секрета в base32. */
    fun currentCode(
        base32Secret: String,
        digits: Int = 6,
        periodSeconds: Long = 30,
        timeMillis: Long = System.currentTimeMillis()
    ): String? {
        val key = try {
            decodeBase32(base32Secret)
        } catch (_: Exception) {
            return null
        }
        if (key.isEmpty()) return null
        return codeAt(timeMillis / 1000L, key, digits, periodSeconds)
    }

    /** Секунд до смены кода. */
    fun secondsRemaining(periodSeconds: Long = 30, timeMillis: Long = System.currentTimeMillis()): Long {
        val seconds = timeMillis / 1000L
        return periodSeconds - (seconds % periodSeconds)
    }

    private fun codeAt(epochSeconds: Long, key: ByteArray, digits: Int, periodSeconds: Long): String? {
        return try {
            val counter = epochSeconds / periodSeconds
            val message = ByteArray(8)
            var value = counter
            for (i in 7 downTo 0) {
                message[i] = (value and 0xFF).toByte()
                value = value shr 8
            }
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(key, "HmacSHA1"))
            val hash = mac.doFinal(message)
            val offset = hash[hash.size - 1].toInt() and 0x0F
            var binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
            val otp = binary % pow10(digits)
            otp.toString().padStart(digits, '0')
        } catch (_: Exception) {
            null
        }
    }

    private fun pow10(n: Int): Int {
        var result = 1
        repeat(n) { result *= 10 }
        return result
    }

    /** Декодер base32 без падений: игнорирует пробелы/дефисы, отбрасывает паддинг. */
    fun decodeBase32(input: String): ByteArray {
        val cleaned = input.uppercase().filter { !it.isWhitespace() && it != '-' && it != '=' }
        val out = ArrayList<Byte>(cleaned.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        for (ch in cleaned) {
            val index = BASE32_ALPHABET.indexOf(ch)
            if (index < 0) throw IllegalArgumentException("Некорректный символ base32: $ch")
            buffer = (buffer shl 5) or index
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }
}
