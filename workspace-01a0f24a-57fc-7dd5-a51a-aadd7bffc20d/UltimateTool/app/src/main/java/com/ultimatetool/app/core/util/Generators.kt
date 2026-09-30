package com.ultimatetool.app.core.util

import java.security.SecureRandom

/** Генерация паролей и UUID — чистая логика. */
object Generators {

    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#\$%^&*-_=+"

    data class PasswordOptions(
        val length: Int = 16,
        val lower: Boolean = true,
        val upper: Boolean = true,
        val digits: Boolean = true,
        val symbols: Boolean = false
    )

    /**
     * Генерирует пароль. Гарантирует по одному символу из каждого выбранного
     * класса (перемешивание — через SecureRandom), если длина позволяет.
     */
    fun password(options: PasswordOptions, random: SecureRandom = SecureRandom()): String {
        val pools = buildList {
            if (options.lower) add(LOWER)
            if (options.upper) add(UPPER)
            if (options.digits) add(DIGITS)
            if (options.symbols) add(SYMBOLS)
        }
        if (pools.isEmpty()) return ""
        val length = options.length.coerceIn(4, 128)
        val chars = CharArray(length)
        // По одному обязательному символу из каждого пула.
        pools.forEachIndexed { index, pool ->
            if (index < length) chars[index] = pool[random.nextInt(pool.length)]
        }
        val all = pools.joinToString("")
        for (i in pools.size until length) {
            chars[i] = all[random.nextInt(all.length)]
        }
        // Перемешивание Фишера–Йетса.
        for (i in length - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp
        }
        return String(chars)
    }

    fun uuid(): String = java.util.UUID.randomUUID().toString()
}
