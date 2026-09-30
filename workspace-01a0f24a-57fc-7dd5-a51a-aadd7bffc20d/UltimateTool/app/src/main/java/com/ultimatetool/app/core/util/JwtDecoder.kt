package com.ultimatetool.app.core.util

import org.json.JSONObject
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Чистая логика работы с JWT (без Android-зависимостей).
 */
object JwtDecoder {

    data class DecodedJwt(
        val headerJson: String?,
        val payloadJson: String?,
        val signature: String,
        val isValidStructure: Boolean
    )

    /** Разбирает токен на части. Возвращает null, если это вообще не JWT-формат. */
    fun decode(token: String): DecodedJwt? {
        val trimmed = token.trim()
        val parts = trimmed.split(".")
        if (parts.size != 3) return null
        return DecodedJwt(
            headerJson = decodePartToPrettyJson(parts[0]),
            payloadJson = decodePartToPrettyJson(parts[1]),
            signature = parts[2],
            isValidStructure = true
        )
    }

    /** Декодирует base64url-часть и пытается красиво отформатировать JSON. */
    fun decodePartToPrettyJson(part: String): String? {
        val raw = decodePart(part) ?: return null
        return try {
            JSONObject(raw).toString(2)
        } catch (_: Exception) {
            raw
        }
    }

    fun decodePart(part: String): String? {
        return try {
            val padded = part.replace('-', '+').replace('_', '/')
            val withPadding = when (padded.length % 4) {
                2 -> "$padded=="
                3 -> "$padded="
                else -> padded
            }
            String(Base64.getDecoder().decode(withPadding), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Проверка подписи HMAC-SHA256 (HS256) при наличии секрета.
     * Возвращает null, если проверить невозможно (нет секрета/битый токен).
     */
    fun verifyHs256(token: String, secret: String): Boolean? {
        val parts = token.trim().split(".")
        if (parts.size != 3 || secret.isEmpty()) return null
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val calculated = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal("${parts[0]}.${parts[1]}".toByteArray(Charsets.UTF_8)))
            calculated == parts[2]
        } catch (_: Exception) {
            null
        }
    }
}
