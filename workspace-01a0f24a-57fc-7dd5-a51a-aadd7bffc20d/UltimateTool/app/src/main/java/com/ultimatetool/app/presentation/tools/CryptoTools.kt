package com.ultimatetool.app.presentation.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultimatetool.app.core.util.Hashing
import com.ultimatetool.app.core.util.JwtDecoder
import com.ultimatetool.app.presentation.components.CopyableRow
import com.ultimatetool.app.presentation.components.ErrorBanner
import com.ultimatetool.app.presentation.components.ScreenScaffold
import java.util.Base64

// ---------- Base64 ----------

@Composable
fun Base64ToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var urlSafe by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold("Base64", onBack) { padding ->
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
                label = { Text("Входные данные") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = urlSafe, onCheckedChange = { urlSafe = it })
                Text("URL-safe (без +/=)")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    output = try {
                        val encoder = if (urlSafe) Base64.getUrlEncoder().withoutPadding()
                        else Base64.getEncoder()
                        encoder.encodeToString(input.toByteArray(Charsets.UTF_8))
                    } catch (e: Exception) {
                        "Ошибка: ${e.message}"
                    }
                }) { Text("Кодировать") }
                OutlinedButton(onClick = {
                    output = try {
                        val normalized = input.trim().replace('-', '+').replace('_', '/')
                        val padded = when (normalized.length % 4) {
                            2 -> "$normalized=="
                            3 -> "$normalized="
                            else -> normalized
                        }
                        String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
                    } catch (e: Exception) {
                        "Ошибка: некорректный Base64"
                    }
                }) { Text("Декодировать") }
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

// ---------- Хэши ----------

@Composable
fun HashToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var results by rememberSaveable { mutableStateOf(emptyMap<String, String>()) }

    ScreenScaffold("Хэши", onBack) { padding ->
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
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                results = Hashing.ALGORITHMS.associateWith { algo ->
                    try {
                        Hashing.hash(algo, input)
                    } catch (_: Exception) {
                        "—"
                    }
                }
            }) { Text("Рассчитать все хэши") }

            results.forEach { (algo, value) ->
                CopyableRow(label = algo, value = value)
            }
        }
    }
}

// ---------- JWT ----------

@Composable
fun JwtToolScreen(onBack: () -> Unit) {
    var token by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    // DecodedJwt не является Parcelable/Serializable, поэтому обычный remember.
    var decoded by remember { mutableStateOf<JwtDecoder.DecodedJwt?>(null) }
    var verification by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenScaffold("JWT Decoder", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = token,
                onValueChange = {
                    token = it.take(10_000)
                    error = null
                    verification = null
                },
                label = { Text("JWT-токен") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it.take(500) },
                label = { Text("Секрет (опционально, для HS256)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val result = JwtDecoder.decode(token)
                if (result == null) {
                    decoded = null
                    error = "Это не похоже на JWT (ожидаются 3 части через точку)"
                } else {
                    decoded = result
                    error = null
                    verification = JwtDecoder.verifyHs256(token, secret)
                }
            }) { Text("Разобрать токен") }

            error?.let { ErrorBanner(it) }

            when (verification) {
                true -> Text("✅ Подпись HS256 корректна", color = MaterialTheme.colorScheme.primary)
                false -> ErrorBanner("Подпись НЕ совпадает с указанным секретом")
                null -> Unit
            }

            decoded?.let { jwt ->
                Text("Header", style = MaterialTheme.typography.titleSmall)
                SelectionContainer {
                    Text(
                        jwt.headerJson ?: "—",
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }
                Text("Payload", style = MaterialTheme.typography.titleSmall)
                SelectionContainer {
                    Text(
                        jwt.payloadJson ?: "—",
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }
                CopyableRow("Signature (base64url)", jwt.signature)
            }
        }
    }
}
