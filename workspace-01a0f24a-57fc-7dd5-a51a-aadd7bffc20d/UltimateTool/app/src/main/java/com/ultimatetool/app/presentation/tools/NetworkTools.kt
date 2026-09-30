package com.ultimatetool.app.presentation.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultimatetool.app.presentation.components.CopyableRow
import com.ultimatetool.app.presentation.components.ErrorBanner
import com.ultimatetool.app.presentation.components.LoadingRow
import com.ultimatetool.app.presentation.components.ScreenScaffold
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Все сетевые вызовы — на Dispatchers.IO, обновление состояния — на Main.
 * Таймауты обязательны: сеть может быть недоступна.
 */
private fun httpGetText(url: String, timeoutMs: Int = 10_000, headers: Map<String, String> = emptyMap()): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
        requestMethod = "GET"
        headers.forEach { (k, v) -> setRequestProperty(k, v) }
    }
    return try {
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("HTTP $code: ${body.take(200)}")
        body
    } finally {
        connection.disconnect()
    }
}

// ---------- HTTP-тестер ----------

@Composable
fun HttpToolScreen(onBack: () -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("") }
    var headersText by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("HTTP", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it.take(2_000)
                    error = null
                },
                label = { Text("https://example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = !loading && url.isNotBlank(),
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                val connection = (URL(url.trim()).openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 10_000
                                    readTimeout = 15_000
                                    requestMethod = "GET"
                                }
                                try {
                                    val code = connection.responseCode
                                    val headerLines = connection.headerFields
                                        .filterKeys { it != null }
                                        .map { (k, v) -> "$k: ${v.joinToString()}" }
                                        .joinToString("\n")
                                    val responseBody = try {
                                        connection.inputStream.bufferedReader().use { it.readText() }
                                    } catch (_: Exception) {
                                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                                    }
                                    Triple(code, headerLines, responseBody.take(2_000))
                                } finally {
                                    connection.disconnect()
                                }
                            }
                            withContext(Dispatchers.Main) {
                                status = "HTTP ${result.first}"
                                headersText = result.second
                                body = result.third
                                loading = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                error = "Ошибка: ${e.message ?: "сеть недоступна"}"
                                loading = false
                            }
                        }
                    }
                }
            ) { Text(if (loading) "Запрос…" else "Отправить GET") }

            if (loading) LoadingRow()
            error?.let { ErrorBanner(it) }
            if (status.isNotEmpty()) {
                CopyableRow("Статус", status)
                if (headersText.isNotEmpty()) CopyableRow("Заголовки", headersText)
                Text("Тело (первые 2 КБ):", style = MaterialTheme.typography.titleSmall)
                Text(body.ifBlank { "(пусто)" }, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------- TCP Ping ----------

@Composable
fun TcpPingScreen(onBack: () -> Unit) {
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("443") }
    var loading by remember { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("TCP Ping", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.take(200) },
                    label = { Text("Хост") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit).take(5) },
                    label = { Text("Порт") },
                    singleLine = true,
                    modifier = Modifier.weight(0.5f)
                )
            }
            Button(
                enabled = !loading && host.isNotBlank() && port.isNotBlank(),
                onClick = {
                    loading = true
                    error = null
                    result = null
                    scope.launch {
                        try {
                            val report = withContext(Dispatchers.IO) {
                                val portNumber = port.toIntOrNull() ?: throw IllegalArgumentException("Неверный порт")
                                val socket = Socket()
                                val start = System.currentTimeMillis()
                                try {
                                    socket.connect(InetSocketAddress(host.trim(), portNumber), 5_000)
                                    "Порт $portNumber открыт. Подключение за ${System.currentTimeMillis() - start} мс."
                                } finally {
                                    try { socket.close() } catch (_: Exception) {}
                                }
                            }
                            withContext(Dispatchers.Main) { result = report; loading = false }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                error = "Недоступно: ${e.message ?: "таймаут или отказ"}"
                                loading = false
                            }
                        }
                    }
                }
            ) { Text(if (loading) "Проверка…" else "Проверить порт") }

            if (loading) LoadingRow()
            result?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            error?.let { ErrorBanner(it) }
            Text(
                "Используйте только для диагностики собственных сервисов.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- IP Lookup ----------

@Composable
fun IpLookupScreen(onBack: () -> Unit) {
    var loading by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("IP Lookup", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                enabled = !loading,
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            val parsed = withContext(Dispatchers.IO) {
                                val body = httpGetText("https://ipwho.is/")
                                val json = JSONObject(body)
                                listOf(
                                    "IP" to json.optString("ip", "—"),
                                    "Страна" to json.optString("country", "—"),
                                    "Город" to json.optString("city", "—"),
                                    "Провайдер" to (json.optJSONObject("connection")?.optString("org", "—") ?: "—"),
                                    "Часовой пояс" to (json.optJSONObject("timezone")?.optString("id", "—") ?: "—"),
                                    "Широта/Долгота" to
                                        "${json.optDouble("latitude", 0.0)} / ${json.optDouble("longitude", 0.0)}"
                                )
                            }
                            withContext(Dispatchers.Main) { rows = parsed; loading = false }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                error = "Не удалось получить данные: ${e.message ?: "сеть"}"
                                loading = false
                            }
                        }
                    }
                }
            ) { Text(if (loading) "Запрос…" else "Определить мой IP") }

            if (loading) LoadingRow()
            error?.let { ErrorBanner(it) }
            rows.forEach { (label, value) -> CopyableRow(label, value) }
        }
    }
}

// ---------- DNS over HTTPS ----------

private val DNS_TYPES = listOf("A", "AAAA", "MX", "TXT", "NS", "CNAME")

@Composable
fun DnsLookupScreen(onBack: () -> Unit) {
    var domain by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("A") }
    var loading by remember { mutableStateOf(false) }
    var answers by remember { mutableStateOf(emptyList<String>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("DNS (DoH)", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = {
                        domain = it.take(200)
                        error = null
                    },
                    label = { Text("example.com") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { value ->
                        type = value.uppercase().take(6).let { v ->
                            DNS_TYPES.firstOrNull { it.startsWith(v) } ?: v
                        }
                    },
                    label = { Text("Тип") },
                    singleLine = true,
                    modifier = Modifier.weight(0.5f)
                )
            }
            Button(
                enabled = !loading && domain.isNotBlank(),
                onClick = {
                    loading = true
                    error = null
                    answers = emptyList()
                    scope.launch {
                        try {
                            val parsed = withContext(Dispatchers.IO) {
                                val url = "https://cloudflare-dns.com/dns-query?name=${domain.trim()}&type=$type"
                                val body = httpGetText(url, headers = mapOf("Accept" to "application/dns-json"))
                                val json = JSONObject(body)
                                val array = json.optJSONArray("Answer")
                                if (array == null || array.length() == 0) {
                                    throw RuntimeException("Записей не найдено (Status: ${json.optInt("Status", -1)})")
                                }
                                (0 until array.length()).map { i ->
                                    val item = array.getJSONObject(i)
                                    "${item.optString("name")} • TTL ${item.optInt("TTL")} • ${item.optString("data")}"
                                }
                            }
                            withContext(Dispatchers.Main) { answers = parsed; loading = false }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                error = e.message ?: "Ошибка DNS-запроса"
                                loading = false
                            }
                        }
                    }
                }
            ) { Text(if (loading) "Запрос…" else "Запросить записи") }

            if (loading) LoadingRow()
            error?.let { ErrorBanner(it) }
            answers.forEach { CopyableRow(type, it) }
        }
    }
}
