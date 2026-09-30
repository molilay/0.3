package com.ultimatetool.app.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Копирует текст в буфер обмена и показывает короткий тост. */
fun Context.copyToClipboard(text: String, label: String = "UltimateTool") {
    try {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, "Скопировано", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        // Буфер обмена может быть недоступен в некоторых ограничениях профиля.
    }
}

fun Context.toast(message: String) {
    try {
        Toast.makeText(this, message.take(300), Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
    }
}

fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }

fun formatTimestamp(ms: Long): String {
    return try {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ms))
    } catch (_: Exception) {
        ms.toString()
    }
}

/** Человекочитаемый размер в байтах. */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Б"
    val units = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return "%.1f %s".format(value, units[index])
}
