package com.ultimatetool.app.presentation.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ultimatetool.app.domain.model.PluginInfo
import com.ultimatetool.app.domain.repository.PluginsRepository
import com.ultimatetool.app.plugin_engine.LuaPluginEngine
import com.ultimatetool.app.presentation.components.ErrorBanner
import com.ultimatetool.app.presentation.components.LoadingRow
import com.ultimatetool.app.presentation.components.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PluginEditorUiState(
    val loading: Boolean = true,
    val name: String = "",
    val version: String = "1.0",
    val source: String = "",
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class PluginEditorViewModel @Inject constructor(
    private val pluginsRepository: PluginsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val pluginId: String? = savedStateHandle.get<String>("pluginId")

    private val _state = MutableStateFlow(PluginEditorUiState())
    val state: StateFlow<PluginEditorUiState> = _state.asStateFlow()

    init {
        val id = pluginId
        if (id == null) {
            _state.update {
                it.copy(loading = false, name = "Мой плагин", source = LuaPluginEngine.SAMPLE_PLUGIN)
            }
        } else {
            viewModelScope.launch {
                val plugin = pluginsRepository.getById(id)
                _state.update {
                    it.copy(
                        loading = false,
                        name = plugin?.name ?: "",
                        version = plugin?.version ?: "1.0",
                        source = plugin?.source ?: LuaPluginEngine.SAMPLE_PLUGIN
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value.take(100), error = null) }
    }

    fun onVersionChange(value: String) {
        _state.update { it.copy(version = value.take(20), error = null) }
    }

    fun onSourceChange(value: String) {
        _state.update { it.copy(source = value.take(200_000), error = null) }
    }

    fun insertSample() {
        _state.update { it.copy(source = LuaPluginEngine.SAMPLE_PLUGIN, error = null) }
    }

    fun save() {
        val current = _state.value
        if (current.loading) return
        if (current.name.isBlank()) {
            _state.update { it.copy(error = "Введите название плагина") }
            return
        }
        if (current.source.isBlank()) {
            _state.update { it.copy(error = "Код плагина пуст") }
            return
        }
        viewModelScope.launch {
            pluginsRepository.upsert(
                PluginInfo(
                    id = pluginId ?: UUID.randomUUID().toString(),
                    name = current.name.trim(),
                    version = current.version.trim().ifEmpty { "1.0" },
                    source = current.source,
                    enabled = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _state.update { it.copy(saved = true) }
        }
    }
}

/** Встроенный редактор Lua-плагинов (моноширинный редактор + метаданные). */
@Composable
fun PluginEditorScreen(
    pluginId: String?,
    onDone: () -> Unit,
    viewModel: PluginEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    ScreenScaffold(
        title = if (pluginId == null) "Новый плагин" else "Редактор плагина",
        onBack = onDone,
        actions = {
            TextButton(onClick = viewModel::save, enabled = !state.loading) {
                Text("Сохранить")
            }
        }
    ) { padding ->
        if (state.loading) {
            LoadingRow(Modifier.padding(padding))
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.version,
                        onValueChange = viewModel::onVersionChange,
                        label = { Text("Версия") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = viewModel::insertSample) { Text("Пример") }
                }
                state.error?.let { ErrorBanner(it) }

                Text(
                    "Код плагина (Lua). Точка входа — функция on_run(). " +
                        "API: tool.log, tool.toast, tool.version, tool.httpGet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.source,
                    onValueChange = viewModel::onSourceChange,
                    label = { Text("main.lua") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    minLines = 14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp)
                )
            }
        }
    }
}
