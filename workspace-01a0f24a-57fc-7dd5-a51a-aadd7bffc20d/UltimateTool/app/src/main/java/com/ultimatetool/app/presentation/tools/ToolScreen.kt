package com.ultimatetool.app.presentation.tools

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ultimatetool.app.presentation.components.EmptyState
import com.ultimatetool.app.presentation.components.ScreenScaffold

/** Диспетчер утилит: по идентификатору показывает конкретный экран. */
@Composable
fun ToolScreen(toolId: String, onBack: () -> Unit) {
    when (toolId) {
        // Крипто / кодирование
        "base64" -> Base64ToolScreen(onBack)
        "hash" -> HashToolScreen(onBack)
        "jwt" -> JwtToolScreen(onBack)
        "aes" -> AesToolScreen(onBack)
        "totp" -> TotpToolScreen(onBack)
        // Текст / данные
        "json" -> JsonFormatterScreen(onBack)
        "urlcodec" -> UrlCodecScreen(onBack)
        "regex" -> RegexTesterScreen(onBack)
        "diff" -> DiffToolScreen(onBack)
        "csv" -> CsvToolScreen(onBack)
        "textinsp" -> TextInspectorScreen(onBack)
        // Генераторы / конвертеры
        "uuid" -> UuidToolScreen(onBack)
        "password" -> PasswordToolScreen(onBack)
        "timestamp" -> TimestampToolScreen(onBack)
        "color" -> ColorToolScreen(onBack)
        // Сеть
        "httptool" -> HttpToolScreen(onBack)
        "tcping" -> TcpPingScreen(onBack)
        "iplookup" -> IpLookupScreen(onBack)
        "dns" -> DnsLookupScreen(onBack)
        // Система
        "qr" -> QrGeneratorScreen(onBack)
        "deviceinfo" -> DeviceInfoScreen(onBack)
        else -> UnknownToolScreen(onBack)
    }
}

@Composable
private fun UnknownToolScreen(onBack: () -> Unit) {
    ScreenScaffold("Инструмент", onBack) { padding ->
        EmptyState("Инструмент не найден", Modifier.padding(padding))
    }
}
