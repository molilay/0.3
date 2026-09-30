package com.ultimatetool.app.core.util

/** Простой построчный diff на основе LCS (без внешних библиотек). */
object DiffEngine {

    enum class Kind { SAME, ADDED, REMOVED }

    data class Line(val kind: Kind, val text: String)

    /**
     * Сравнивает два текста построчно. Для очень больших текстов
     * обрезает вход, чтобы не построить квадратную таблицу памяти.
     */
    fun diff(a: String, b: String, maxLines: Int = 2_000): List<Line> {
        val left = a.lines().take(maxLines)
        val right = b.lines().take(maxLines)
        val n = left.size
        val m = right.size

        // Таблица LCS: (n+1) * (m+1) int. Ограничение сверху защищает память.
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (left[i] == right[j]) {
                    dp[i + 1][j + 1] + 1
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val result = ArrayList<Line>(n + m)
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                left[i] == right[j] -> {
                    result.add(Line(Kind.SAME, left[i])); i++; j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> {
                    result.add(Line(Kind.REMOVED, left[i])); i++
                }
                else -> {
                    result.add(Line(Kind.ADDED, right[j])); j++
                }
            }
        }
        while (i < n) result.add(Line(Kind.REMOVED, left[i++]))
        while (j < m) result.add(Line(Kind.ADDED, right[j++]))
        return result
    }
}

/** Конвертер систем счисления и цветов. */
object ColorConvert {

    data class ColorInfo(
        val hex: String,
        val r: Int, val g: Int, val b: Int,
        val h: Int, val s: Int, val l: Int
    )

    /** Парсит #RGB/#RRGGBB без падений; возвращает null при ошибке. */
    fun parse(hexInput: String): ColorInfo? {
        var hex = hexInput.trim().removePrefix("#")
        if (hex.length == 3) hex = hex.map { "$it$it" }.joinToString("")
        if (hex.length != 6) return null
        return try {
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            val (h, s, l) = rgbToHsl(r, g, b)
            ColorInfo("#${hex.uppercase()}", r, g, b, h, s, l)
        } catch (_: Exception) {
            null
        }
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf)
        val l = (max + min) / 2f
        if (max == min) return Triple(0, 0, Math.round(l * 100).toInt())
        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            rf -> ((gf - bf) / d + (if (gf < bf) 6 else 0))
            gf -> ((bf - rf) / d + 2)
            else -> ((rf - gf) / d + 4)
        }
        return Triple(Math.round(h * 60).toInt(), Math.round(s * 100).toInt(), Math.round(l * 100).toInt())
    }
}
