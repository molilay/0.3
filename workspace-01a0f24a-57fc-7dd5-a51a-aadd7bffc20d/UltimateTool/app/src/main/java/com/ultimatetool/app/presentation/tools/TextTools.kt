package com.ultimatetool.app.presentation.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultimatetool.app.presentation.components.ErrorBanner
import com.ultimatetool.app.presentation.components.ScreenScaffold
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.PatternSyntaxException
import org.json.JSONArray
import org.json.JSONObject

// ---------- JSON ----------

@Composable
fun JsonFormatterScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun parse(text: String, indent: Int): String {
        val trimmed = text.trim()
        return when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(indent)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(indent)
            else -> throw IllegalArgumentException("Ожидается объект {...} или массив [...]")
        }
    }

    ScreenScaffold("JSON", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it.take(200_000)
                    error = null
                },
                label = { Text("JSON") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    try {
                        output = parse(input, 2)
                        error = null
                    } catch (e: Exception) {
                        output = ""
                        error = "Неверный JSON: ${e.message}"
                    }
                }) { Text("Форматировать") }
                OutlinedButton(onClick = {
                    try {
                        output = parse(input, 0)
                        error = null
                    } catch (e: Exception) {
                        output = ""
                        error = "Неверный JSON: ${e.message}"
                    }
                }) { Text("Минифицировать") }
            }
            error?.let { ErrorBanner(it) }
            if (output.isNotEmpty()) {
                Text("Результат", style = MaterialTheme.typography.titleSmall)
                SelectionContainer {
                    Text(
                        output,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

// ---------- URL Codec ----------

@Composable
fun UrlCodecScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }

    ScreenScaffold("URL Codec", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(50_000) },
                label = { Text("Текст или URL") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    output = try {
                        URLEncoder.encode(input, "UTF-8")
                    } catch (e: Exception) {
                        "Ошибка: ${e.message}"
                    }
                }) { Text("Encode") }
                OutlinedButton(onClick = {
                    output = try {
                        URLDecoder.decode(input, "UTF-8")
                    } catch (e: Exception) {
                        "Ошибка: некорректная escape-последовательность"
                    }
                }) { Text("Decode") }
            }
            Text(
                "Примечание: пробел кодируется как «+» (формат form-encoding).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (output.isNotEmpty()) {
                OutlinedTextField(
                    value = output,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Результат") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ---------- Regex ----------

@Composable
fun RegexTesterScreen(onBack: () -> Unit) {
    var pattern by rememberSaveable { mutableStateOf("") }
    var input by rememberSaveable { mutableStateOf("") }
    var matches by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var searched by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold("Regex", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = pattern,
                onValueChange = {
                    pattern = it.take(1_000)
                    error = null
                },
                label = { Text("Регулярное выражение") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(100_000) },
                label = { Text("Тестовый текст") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                searched = true
                try {
                    matches = pattern.toRegex()
                        .findAll(input)
                        .take(200)
                        .map { it.value }
                        .toList()
                    error = null
                } catch (e: PatternSyntaxException) {
                    matches = emptyList()
                    error = "Синтаксис: ${e.description}"
                } catch (e: Exception) {
                    matches = emptyList()
                    error = "Ошибка: ${e.message}"
                }
            }) { Text("Найти совпадения") }

            error?.let { ErrorBanner(it) }

            if (searched && error == null) {
                Text("Совпадений: ${matches.size}", style = MaterialTheme.typography.titleSmall)
                matches.forEachIndexed { index, match ->
                    SelectionContainer {
                        Text("$index. $match", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
