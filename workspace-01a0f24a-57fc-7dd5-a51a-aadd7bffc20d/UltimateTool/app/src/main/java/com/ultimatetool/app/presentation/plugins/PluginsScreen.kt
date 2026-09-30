package com.ultimatetool.app.presentation.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ultimatetool.app.domain.model.PluginInfo
import com.ultimatetool.app.domain.repository.PluginsRepository
import com.ultimatetool.app.plugin_engine.LuaPluginEngine
import com.ultimatetool.app.presentation.components.EmptyState
import com.ultimatetool.app.presentation.components.ErrorBanner
import com.ultimatetool.app.presentation.components.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PluginRunUi(
    val pluginName: String,
    val output: String,
    val error: String?,
    val durationMs: Long
)

@HiltViewModel
class PluginsViewModel @Inject constructor(
    private val pluginsRepository: PluginsRepository,
    private val engine: LuaPluginEngine
) : ViewModel() {

    val plugins: StateFlow<List<PluginInfo>> = pluginsRepository.observePlugins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _runResult = MutableStateFlow<PluginRunUi?>(null)
    val runResult: StateFlow<PluginRunUi?> = _runResult.asStateFlow()

    /** Запуск в песочнице на IO-потоке; защита от повторного нажатия — флаг. */
    fun runPlugin(plugin: PluginInfo) {
        if (_running.value || !plugin.enabled) return
        _running.value = true
        _runResult.value = null
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { engine.run(plugin.source) }
            _runResult.value = PluginRunUi(
                pluginName = plugin.name,
                output = result.output,
                error = result.error,
                durationMs = result.durationMs
            )
            _running.value = false
        }
    }

    fun toggleEnabled(plugin: PluginInfo) {
        viewModelScope.launch {
            pluginsRepository.upsert(
                plugin.copy(enabled = !plugin.enabled, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun deletePlugin(plugin: PluginInfo) {
        viewModelScope.launch { pluginsRepository.delete(plugin.id) }
    }

    fun installSample() {
        viewModelScope.launch {
            pluginsRepository.upsert(
                PluginInfo(
                    id = UUID.randomUUID().toString(),
                    name = "Пример плагина",
                    version = "1.0",
                    source = LuaPluginEngine.SAMPLE_PLUGIN,
                    enabled = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun dismissRunResult() {
        _runResult.value = null
    }
}

/** Платформа плагинов: список, запуск в Lua-песочнице, управление. */
@Composable
fun PluginsScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: PluginsViewModel = hiltViewModel()
) {
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    val runResult by viewModel.runResult.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "Плагины",
        onBack = onBack,
        actions = {
            TextButton(onClick = viewModel::installSample) { Text("Пример") }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Новый плагин") }
            )
        }
    ) { padding ->
        if (plugins.isEmpty()) {
            EmptyState(
                "Плагинов пока нет.\nСоздайте свой или нажмите «Пример».",
                Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(plugins, key = { it.id }) { plugin ->
                    PluginCard(
                        plugin = plugin,
                        running = running,
                        onRun = { viewModel.runPlugin(plugin) },
                        onEdit = { onEdit(plugin.id) },
                        onToggle = { viewModel.toggleEnabled(plugin) },
                        onDelete = { viewModel.deletePlugin(plugin) }
                    )
                }
            }
        }
    }

    runResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRunResult,
            title = { Text("Результат: ${result.pluginName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.error?.let { ErrorBanner(it) }
                    Text(
                        result.output.ifBlank { "(плагин ничего не вывел)" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Выполнено за ${result.durationMs} мс",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissRunResult) { Text("OK") }
            }
        )
    }
}

@Composable
private fun PluginCard(
    plugin: PluginInfo,
    running: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(plugin.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "v${plugin.version} • Lua",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = plugin.enabled, onCheckedChange = { onToggle() })
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Редактировать")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRun,
                enabled = plugin.enabled && !running,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (running) "Выполняется…" else "Запустить")
            }
        }
    }
}
