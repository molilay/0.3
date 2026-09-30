package com.ultimatetool.app.presentation.linux

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatetool.app.presentation.components.ScreenScaffold

/**
 * Графическое окружение гостевого Linux.
 *
 * Схема (классика для rootless-подхода): внутри гостя поднимается
 * VNC-сервер (порт 5901), картинка отображается клиентом. Встроенный
 * просмотрщик открывает noVNC-совместимый веб-мост; адрес настраивается.
 */
@Composable
fun LinuxGuiScreen(
    distroId: String?,
    onBack: () -> Unit,
    viewModel: LinuxViewModel = hiltViewModel()
) {
    val log by viewModel.log.collectAsStateWithLifecycle()
    var bridgeUrl by rememberSaveable { mutableStateOf("http://127.0.0.1:6080/vnc.html") }
    var showViewer by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    ScreenScaffold("Linux GUI (VNC)", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Как это работает", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "1. Запустите дистрибутив на экране Linux.\n" +
                            "2. Нажмите «Поднять VNC» — в госте установится и стартует " +
                            "VNC-сервер на порту 5901.\n" +
                            "3. Откройте просмотрщик: встроенный веб-мост или внешний клиент.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            val target = distroId ?: "debian12"
            Button(onClick = { viewModel.startVnc(target) }) {
                Text("Поднять VNC в дистрибутиве")
            }

            OutlinedTextField(
                value = bridgeUrl,
                onValueChange = { bridgeUrl = it.take(500) },
                label = { Text("Адрес noVNC-моста") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { showViewer = !showViewer }) {
                Text(if (showViewer) "Скрыть просмотрщик" else "Открыть встроенный просмотрщик")
            }
            OutlinedButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bridgeUrl)))
                } catch (_: Exception) {
                }
            }) { Text("Открыть в браузере") }

            if (showViewer) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                        }
                    },
                    update = { web ->
                        try {
                            web.loadUrl(bridgeUrl)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp)
                )
            }

            Text("Журнал", style = MaterialTheme.typography.titleMedium)
            Text(
                log,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
