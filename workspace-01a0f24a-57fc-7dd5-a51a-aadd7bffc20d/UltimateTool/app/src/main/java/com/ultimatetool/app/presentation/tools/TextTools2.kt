package com.ultimatetool.app.presentation.tools

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ultimatetool.app.core.util.DiffEngine
import com.ultimatetool.app.presentation.components.ErrorBanner
import com.ultimatetool.app.presentation.components.ScreenScaffold

// ---------- Diff Viewer ----------

@Composable
fun DiffToolScreen(onBack: () -> Unit) {
    var textA by rememberSaveable { mutableStateOf("") }
    var textB by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf(emptyList<Pair<String, String>>()) }
    var summary by rememberSaveable { mutableStateOf("") }

    ScreenScaffold("Diff", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = textA,
                onValueChange = { textA = it.take(50_000) },
                label = { Text("Текст A (было)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = textB,
                onValueChange = { textB = it.take(50_000) },
                label = { Text("Текст B (стало)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val diff = DiffEngine.diff(textA, textB)
                result = diff.take(1_000).map {
                    val prefix = when (it.kind) {
                        DiffEngine.Kind.ADDED -> "+"
                        DiffEngine.Kind.REMOVED -> "-"
                        DiffEngine.Kind.SAME -> " "
                    }
                    it.kind.name to "$prefix ${it.text}"
                }
                val added = diff.count { it.kind == DiffEngine.Kind.ADDED }
                val removed = diff.count { it.kind == DiffEngine.Kind.REMOVED }
                summary = "+$added / −$removed"
            }) { Text("Сравнить") }

            if (summary.isNotEmpty()) {
                Text(summary, style = MaterialTheme.typography.titleSmall)
                result.forEach { (kind, line) ->
                    Text(
                        line,
                        color = when (kind) {
                            "ADDED" -> Color(0xFF2E7D32)
                            "REMOVED" -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ---------- CSV Viewer ----------

@Composable
fun CsvToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var rows by rememberSaveable { mutableStateOf(emptyList<List<String>>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenScaffold("CSV", onBack) { padding ->
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
                    input = it.take(100_000)
                    error = null
                },
                label = { Text("CSV-текст (разделитель — запятая)") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val parsed = input.lineSequence()
                    .filter { it.isNotBlank() }
                    .take(100)
                    .map { line -> line.split(",").take(20).map { it.trim() } }
                    .toList()
                if (parsed.isEmpty()) {
                    error = "Пустой или некорректный CSV"
                    rows = emptyList()
                } else {
                    error = null
                    rows = parsed
                }
            }) { Text("Показать таблицей") }

            error?.let { ErrorBanner(it) }

            if (rows.isNotEmpty()) {
                Text("Строк: ${rows.size}", style = MaterialTheme.typography.titleSmall)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    rows.forEachIndexed { index, row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            row.forEach { cell ->
                                Text(
                                    cell,
                                    style = if (index == 0) MaterialTheme.typography.labelLarge
                                    else MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Текстовый инспектор ----------

@Composable
fun TextInspectorScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }

    val stats = remember(input) {
        val words = if (input.isBlank()) 0 else input.trim().split(Regex("\\s+")).size
        "Символов: ${input.length} • Слов: $words • Строк: ${input.lines().size} • " +
            "Байт UTF-8: ${input.toByteArray(Charsets.UTF_8).size}"
    }

    ScreenScaffold("Текст", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(100_000) },
                label = { Text("Текст") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Text(stats, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { output = input.uppercase() }) { Text("UPPER") }
                OutlinedButton(onClick = { output = input.lowercase() }) { Text("lower") }
                OutlinedButton(onClick = {
                    output = input.split(Regex("(?<=[.!?]\\s+)")).joinToString("") {
                        it.replaceFirstChar { c -> c.uppercaseChar() }
                    }
                }) { Text("Предложения") }
                OutlinedButton(onClick = { output = input.reversed() }) { Text("Реверс") }
            }
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
