package com.ultimatetool.app.presentation.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ultimatetool.app.core.util.ColorConvert
import com.ultimatetool.app.core.util.Generators
import com.ultimatetool.app.core.util.copyToClipboard
import com.ultimatetool.app.presentation.components.CopyableRow
import com.ultimatetool.app.presentation.components.ScreenScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ---------- UUID ----------

@Composable
fun UuidToolScreen(onBack: () -> Unit) {
    var results by rememberSaveable { mutableStateOf(listOf<String>()) }

    ScreenScaffold("UUID", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { results = List(5) { Generators.uuid() } }) {
                Text("Сгенерировать 5 шт.")
            }
            if (results.isNotEmpty()) {
                val context = LocalContext.current
                Button(onClick = { context.copyToClipboard(results.joinToString("\n")) }) {
                    Text("Скопировать все")
                }
                results.forEach { CopyableRow("UUID v4", it) }
            }
        }
    }
}

// ---------- Генератор паролей ----------

@Composable
fun PasswordToolScreen(onBack: () -> Unit) {
    var length by rememberSaveable { mutableFloatStateOf(16f) }
    var useLower by rememberSaveable { mutableStateOf(true) }
    var useUpper by rememberSaveable { mutableStateOf(true) }
    var useDigits by rememberSaveable { mutableStateOf(true) }
    var useSymbols by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }

    ScreenScaffold("Пароль", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Длина: ${length.toInt()}")
            Slider(value = length, onValueChange = { length = it }, valueRange = 6f..64f, steps = 57)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = useLower, onCheckedChange = { useLower = it }); Text("строчные")
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = useUpper, onCheckedChange = { useUpper = it }); Text("ПРОПИСНЫЕ")
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = useDigits, onCheckedChange = { useDigits = it }); Text("цифры (без 0/1)")
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = useSymbols, onCheckedChange = { useSymbols = it }); Text("символы")
            }
            Button(onClick = {
                password = Generators.password(
                    Generators.PasswordOptions(
                        length = length.toInt(),
                        lower = useLower,
                        upper = useUpper,
                        digits = useDigits,
                        symbols = useSymbols
                    )
                )
            }) { Text("Сгенерировать") }
            if (password.isNotEmpty()) CopyableRow("Пароль", password)
        }
    }
}

// ---------- Unix Timestamp ----------

@Composable
fun TimestampToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }

    fun convert(value: String) {
        output = try {
            val num = value.trim().toLong()
            val millis = if (num > 10_000_000_000L) num else num * 1000L
            val utc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val local = SimpleDateFormat("dd.MM.yyyy HH:mm:ss z", Locale.getDefault())
            "UTC:   ${utc.format(Date(millis))}\nЛокально: ${local.format(Date(millis))}\nМиллисекунды: $millis"
        } catch (_: Exception) {
            "Введите число (секунды или миллисекунды)"
        }
    }

    ScreenScaffold("Timestamp", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(30) },
                label = { Text("Unix timestamp") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { convert(input) }) { Text("Преобразовать") }
                Button(onClick = {
                    val nowSec = (System.currentTimeMillis() / 1000L).toString()
                    input = nowSec
                    convert(nowSec)
                }) { Text("Сейчас") }
            }
            if (output.isNotEmpty()) {
                Text(output, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// ---------- Конвертер цветов ----------

@Composable
fun ColorToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("#0B57D0") }
    val info = remember(input) { ColorConvert.parse(input) }

    ScreenScaffold("Цвет", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(9) },
                label = { Text("HEX (#RRGGBB)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (info == null) {
                Text(
                    "Некорректный формат. Примеры: #0B57D0, #F00",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(info.r, info.g, info.b), RoundedCornerShape(12.dp))
                )
                CopyableRow("HEX", info.hex)
                CopyableRow("RGB", "rgb(${info.r}, ${info.g}, ${info.b})")
                CopyableRow("HSL", "hsl(${info.h}, ${info.s}%, ${info.l}%)")
            }
        }
    }
}
