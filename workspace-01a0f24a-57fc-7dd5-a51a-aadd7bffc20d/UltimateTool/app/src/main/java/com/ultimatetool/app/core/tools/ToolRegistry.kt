package com.ultimatetool.app.core.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/** Описание встроенной утилиты для реестра/поиска/домашней сетки. */
data class ToolSpec(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

/**
 * Реестр встроенных утилит. Добавление новой утилиты:
 * 1) запись здесь; 2) экран в presentation/tools; 3) ветка в ToolScreen.
 */
object ToolRegistry {

    val all: List<ToolSpec> = listOf(
        // Крипто / кодирование
        ToolSpec("base64", "Base64", "Кодирование и декодирование", Icons.Outlined.Lock),
        ToolSpec("hash", "Хэши", "MD5 / SHA-1/256/384/512 / CRC32", Icons.Outlined.Fingerprint),
        ToolSpec("jwt", "JWT Decoder", "Разбор и проверка токенов", Icons.Outlined.Key),
        ToolSpec("aes", "AES", "Шифрование AES-256-GCM", Icons.Outlined.Lock),
        ToolSpec("totp", "TOTP / 2FA", "Генератор одноразовых кодов", Icons.Outlined.Timer),
        // Текст / данные
        ToolSpec("json", "JSON", "Форматирование и валидация", Icons.Outlined.DataObject),
        ToolSpec("urlcodec", "URL Codec", "URL encode / decode", Icons.Outlined.Link),
        ToolSpec("regex", "Regex", "Проверка регулярных выражений", Icons.Outlined.TextFields),
        ToolSpec("diff", "Diff", "Сравнение двух текстов", Icons.Outlined.CompareArrows),
        ToolSpec("csv", "CSV", "Просмотр CSV таблицей", Icons.Outlined.TableChart),
        ToolSpec("textinsp", "Текст", "Регистр, счётчики, реверс", Icons.Outlined.TextFields),
        // Генераторы / конвертеры
        ToolSpec("uuid", "UUID", "Генератор UUID v4", Icons.Outlined.Tag),
        ToolSpec("password", "Пароль", "Генератор надёжных паролей", Icons.Outlined.Key),
        ToolSpec("timestamp", "Timestamp", "Unix-время ↔ дата", Icons.Outlined.AccessTime),
        ToolSpec("color", "Цвет", "Конвертер HEX/RGB/HSL", Icons.Outlined.Palette),
        // Сеть
        ToolSpec("httptool", "HTTP", "Запрос: статус/заголовки/тело", Icons.Outlined.Http),
        ToolSpec("tcping", "TCP Ping", "Проверка хост:порт", Icons.Outlined.NetworkCheck),
        ToolSpec("iplookup", "IP Lookup", "Информация по IP", Icons.Outlined.Public),
        ToolSpec("dns", "DNS", "DoH-запрос записей", Icons.Outlined.Dns),
        // Система
        ToolSpec("qr", "QR-код", "Генератор QR-кодов", Icons.Outlined.QrCode),
        ToolSpec("deviceinfo", "Устройство", "Информация об устройстве", Icons.Outlined.Memory)
    )

    fun byId(id: String): ToolSpec? = all.firstOrNull { it.id == id }

    fun search(query: String): List<ToolSpec> {
        if (query.isBlank()) return all
        val q = query.trim().lowercase()
        return all.filter {
            it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }
}
