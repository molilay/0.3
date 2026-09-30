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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ultimatetool.app.core.util.AesGcm
import com.ultimatetool.app.core.util.Totp
import com.ultimatetool.app.presentation.components.ErrorBanner
import com.ultimatetool.app.presentation.components.ScreenScaffold
import kotlinx.coroutines.delay

// ---------- AES-256-GCM ----------

@Composable
fun AesToolScreen(onBack: () -> Unit) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenScaffold("AES-256-GCM", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it.take(200) },
                label = { Text("Парольная фраза") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it.take(50_000)
                    error = null
                },
                label = { Text("Текст / шифртекст (base64)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    error = null
                    output = try {
                        if (passphrase.isEmpty()) throw IllegalArgumentException("Нужна парольная фраза")
                        AesGcm.encrypt(input, passphrase)
                    } catch (e: Exception) {
                        error = e.message ?: "Ошибка шифрования"; ""
                    }
                }) { Text("Зашифровать") }
                OutlinedButton(onClick = {
                    error = null
                    output = try {
                        if (passphrase.isEmpty()) throw IllegalArgumentException("Нужна парольная фраза")
                        AesGcm.decrypt(input, passphrase)
                    } catch (e: Exception) {
                        error = "Не удалось расшифровать: неверный пароль или повреждённые данные"; ""
                    }
                }) { Text("Расшифровать") }
            }
            error?.let { ErrorBanner(it) }
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
            Text(
                "Ключ выводится из фразы через PBKDF2 (60 000 итераций). " +
                    "Формат: base64(salt|iv|ciphertext+tag).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- TOTP / 2FA ----------

@Composable
fun TotpToolScreen(onBack: () -> Unit) {
    var secret by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Тикаем раз в секунду для обратного отсчёта и смены кода.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    val code = remember(secret, now) { Totp.currentCode(secret, digits = 6, timeMillis = now) }
    val remaining = Totp.secondsRemaining(timeMillis = now)

    ScreenScaffold("TOTP / 2FA", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it.take(128) },
                label = { Text("Секрет в base32") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            when {
                secret.isBlank() -> Text(
                    "Введите секрет из сервиса (например, отсканированный ранее в формате base32).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                code == null -> ErrorBanner("Некорректный base32-секрет")
                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        code,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Смена через $remaining с",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "Коды считаются локально по RFC 6238 (HMAC-SHA1, шаг 30 с) — сеть не нужна.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
