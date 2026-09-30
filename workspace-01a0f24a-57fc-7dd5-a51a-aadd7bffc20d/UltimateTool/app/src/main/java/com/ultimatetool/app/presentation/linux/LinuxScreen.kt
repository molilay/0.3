package com.ultimatetool.app.presentation.linux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ultimatetool.app.domain.model.LinuxInstance
import com.ultimatetool.app.domain.repository.LinuxRepository
import com.ultimatetool.app.linux.DistroDef
import com.ultimatetool.app.linux.LinuxCatalog
import com.ultimatetool.app.linux.ProotEngine
import com.ultimatetool.app.presentation.components.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DistroUiState(
    val def: DistroDef,
    val installed: Boolean,
    val running: Boolean,
    val installing: Boolean,
    val progress: ProotEngine.Progress?,
    val customUrl: String
)

@HiltViewModel
class LinuxViewModel @Inject constructor(
    private val engine: ProotEngine,
    private val linuxRepository: LinuxRepository
) : ViewModel() {

    private val instances: StateFlow<List<LinuxInstance>> = linuxRepository.observeInstances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _progress = MutableStateFlow<Pair<String, ProotEngine.Progress>?>(null)
    private val _busy = MutableStateFlow(false)
    private val _log = MutableStateFlow("Linux-виртуализация работает через proot (без root).\n")

    val log: StateFlow<String> = _log.asStateFlow()
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val distros: StateFlow<List<DistroUiState>> =
        combine(instances, _progress, _busy) { inst, prog, busy ->
            LinuxCatalog.all.map { def ->
                val instance = inst.firstOrNull { it.distroId == def.id }
                DistroUiState(
                    def = def,
                    installed = engine.isInstalled(def.id),
                    running = engine.isRunning(def.id),
                    installing = busy && prog?.first == def.id,
                    progress = if (prog?.first == def.id) prog.second else null,
                    customUrl = instance?.customUrl.orEmpty()
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun appendLog(line: String) {
        _log.update { (it + line + "\n").takeLast(5_000) }
    }

    /** Полная установка: бинарник proot → rootfs → распаковка. */
    fun install(distroId: String) {
        if (_busy.value) return
        val def = LinuxCatalog.byId(distroId) ?: return
        _busy.value = true
        appendLog("→ Установка ${def.name} (ABI: ${engine.abi})")
        viewModelScope.launch {
            val customUrl = linuxRepository.getInstance(distroId)?.customUrl
            val ok = withContext(Dispatchers.IO) {
                if (!engine.ensureProot { p -> _progress.value = distroId to p }) {
                    appendLog("✗ Не удалось скачать бинарник proot. Проверьте сеть.")
                    false
                } else {
                    engine.installDistro(def, customUrl) { p ->
                        _progress.value = distroId to p
                    }
                }
            }
            if (ok) {
                linuxRepository.upsert(
                    LinuxInstance(distroId, "installed", customUrl.orEmpty(), engine.vncPort(), System.currentTimeMillis())
                )
                appendLog("✓ ${def.name} установлен. Можно запускать терминал.")
            } else {
                appendLog("✗ Ошибка установки ${def.name}. Для экспериментальных дистрибутивов укажите свой URL.")
            }
            _progress.value = null
            _busy.value = false
        }
    }

    fun start(distroId: String) {
        val def = LinuxCatalog.byId(distroId) ?: return
        val started = engine.startShell(distroId) != null
        appendLog(
            if (started) "▶ ${def.name}: оболочка запущена"
            else "✗ Не удалось запустить ${def.name}"
        )
    }

    fun stop(distroId: String) {
        engine.stop(distroId)
        appendLog("■ Остановлено")
    }

    fun uninstall(distroId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { engine.uninstallDistro(distroId) }
            linuxRepository.delete(distroId)
            appendLog("→ Дистрибутив удалён")
        }
    }

    fun setCustomUrl(distroId: String, url: String) {
        viewModelScope.launch {
            val existing = linuxRepository.getInstance(distroId)
            linuxRepository.upsert(
                LinuxInstance(
                    distroId = distroId,
                    state = existing?.state ?: "pending",
                    customUrl = url.trim(),
                    vncPort = engine.vncPort(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** Отправляет команду запуска VNC-сервера в уже работающую оболочку. */
    fun startVnc(distroId: String) {
        val def = LinuxCatalog.byId(distroId) ?: return
        val instance = engine.runningInstance(distroId)
        if (instance == null) {
            appendLog("✗ Сначала запустите дистрибутив")
            return
        }
        instance.sendLine(engine.vncStartCommand(def.family))
        appendLog("→ VNC: установка/запуск сервера в госте (порт ${engine.vncPort()})…")
    }
}

/** Центр управления Linux-дистрибутивами: установка, запуск, терминал, GUI. */
@Composable
fun LinuxScreen(
    onBack: () -> Unit,
    onTerminal: (String) -> Unit,
    onGui: (String) -> Unit,
    viewModel: LinuxViewModel = hiltViewModel()
) {
    val distros by viewModel.distros.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()

    ScreenScaffold("Linux (proot)", onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Каталог: ${distros.size} дистрибутивов. Установка и запуск полностью " +
                        "внутри приложения, без root и сторонних приложений.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(distros, key = { it.def.id }) { state ->
                DistroCard(
                    state = state,
                    onInstall = { viewModel.install(state.def.id) },
                    onStart = { viewModel.start(state.def.id) },
                    onStop = { viewModel.stop(state.def.id) },
                    onTerminal = { onTerminal(state.def.id) },
                    onGui = { onGui(state.def.id) },
                    onUninstall = { viewModel.uninstall(state.def.id) },
                    onCustomUrl = { viewModel.setCustomUrl(state.def.id, it) }
                )
            }
            item {
                Text("Журнал", style = MaterialTheme.typography.titleMedium)
                Text(
                    log,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DistroCard(
    state: DistroUiState,
    onInstall: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onTerminal: () -> Unit,
    onGui: () -> Unit,
    onUninstall: () -> Unit,
    onCustomUrl: (String) -> Unit
) {
    val def = state.def
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(def.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append("${def.packageManager} • ~${def.sizeHintMb} МБ")
                            if (def.experimental) append(" • экспериментальный")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.running) Text("▶ запущен", color = MaterialTheme.colorScheme.primary)
            }
            Text(
                def.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.installing && state.progress != null) {
                LinearProgressIndicator(
                    progress = { state.progress.fraction },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${state.progress.stage} — ${(state.progress.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (def.experimental) {
                var url by remember { mutableStateOf(state.customUrl) }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.take(2_000) },
                    label = { Text("Свой URL rootfs (.tar.gz/.tar.xz)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { onCustomUrl(url) }) { Text("Сохранить URL") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.installed) {
                    Button(onClick = onInstall, enabled = !state.installing) {
                        Text(if (state.installing) "Установка…" else "Установить")
                    }
                } else {
                    if (!state.running) {
                        Button(onClick = onStart) { Text("Запустить") }
                    } else {
                        OutlinedButton(onClick = onStop) { Text("Стоп") }
                    }
                    OutlinedButton(onClick = onTerminal, enabled = state.running) { Text("Терминал") }
                    OutlinedButton(onClick = onGui) { Text("GUI") }
                    OutlinedButton(onClick = onUninstall) { Text("Удалить") }
                }
            }
        }
    }
}
