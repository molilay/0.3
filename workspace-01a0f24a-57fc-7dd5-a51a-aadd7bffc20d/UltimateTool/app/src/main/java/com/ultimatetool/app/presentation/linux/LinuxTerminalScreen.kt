package com.ultimatetool.app.presentation.linux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ultimatetool.app.linux.ProotEngine
import com.ultimatetool.app.presentation.components.EmptyState
import com.ultimatetool.app.presentation.components.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Терминал запущенного дистрибутива: линейный ввод/вывод поверх потоков
 * процесса проот. Буфер ограничен 40 КБ, чтение — корутина на IO,
 * отменяется вместе с ViewModel.
 */
@HiltViewModel
class LinuxTerminalViewModel @Inject constructor(
    engine: ProotEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val distroId: String = savedStateHandle.get<String>("distroId").orEmpty()
    private val instance: ProotEngine.RunningInstance? = engine.runningInstance(distroId)

    val isAttached: Boolean = instance != null

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    init {
        val running = instance
        if (running != null) {
            // Пока процесс жив, поток завершится по EOF (после остановки дистрибутива).
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val reader = BufferedReader(InputStreamReader(running.output, Charsets.UTF_8))
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        _output.update { (it + line + "\n").takeLast(40_000) }
                    }
                    _output.update { it + "\n[процесс завершён]\n" }
                } catch (_: Exception) {
                    _output.update { it + "\n[процесс завершён]\n" }
                }
            }
        }
    }

    fun onInputChange(value: String) {
        _input.value = value.take(2_000)
    }

    fun send() {
        val line = _input.value
        _input.value = ""
        instance?.sendLine(line)
        _output.update { (it + "$ $line\n").takeLast(40_000) }
    }
}

@Composable
fun LinuxTerminalScreen(
    onBack: () -> Unit,
    viewModel: LinuxTerminalViewModel = hiltViewModel()
) {
    val output by viewModel.output.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(output.length) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    ScreenScaffold("Терминал", onBack) { padding ->
        if (!viewModel.isAttached) {
            EmptyState(
                "Дистрибутив не запущен.\nЗапустите его на экране Linux и вернитесь.",
                Modifier.padding(padding)
            )
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(12.dp)
                    .fillMaxSize()
            ) {
                SelectionContainer(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        output.ifBlank { "[подключено — введите команду]\n" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = viewModel::onInputChange,
                        placeholder = { Text("команда…") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(onClick = viewModel::send, enabled = input.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                    }
                }
            }
        }
    }
}
