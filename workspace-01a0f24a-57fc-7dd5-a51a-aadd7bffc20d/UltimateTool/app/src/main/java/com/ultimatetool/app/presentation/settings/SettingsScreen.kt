package com.ultimatetool.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ultimatetool.app.core.security.SessionManager
import com.ultimatetool.app.core.util.toast
import com.ultimatetool.app.data.local.AppPreferences
import com.ultimatetool.app.data.local.ThemeMode
import com.ultimatetool.app.domain.repository.ChatRepository
import com.ultimatetool.app.presentation.components.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val sessionManager: SessionManager,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val biometricLock: StateFlow<Boolean> = preferences.biometricLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoLockMinutes: StateFlow<String> = preferences.autoLockMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "5")

    private val _openAiKey = MutableStateFlow(sessionManager.getOpenAiKey().orEmpty())
    val openAiKey: StateFlow<String> = _openAiKey.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { preferences.setBiometricLock(enabled) }
    }

    fun setAutoLock(value: String) {
        viewModelScope.launch { preferences.setAutoLockMinutes(value) }
    }

    fun onOpenAiKeyChange(value: String) {
        _openAiKey.value = value.take(200)
    }

    fun saveOpenAiKey() {
        sessionManager.setOpenAiKey(_openAiKey.value.trim())
    }

    fun clearChatHistory() {
        viewModelScope.launch { chatRepository.clearHistory() }
    }
}

private val THEME_LABELS = mapOf(
    ThemeMode.SYSTEM to "Система",
    ThemeMode.LIGHT to "Светлая",
    ThemeMode.DARK to "Тёмная",
    ThemeMode.AMOLED to "AMOLED"
)

/** Настройки: тема, AI-ключ, безопасность, данные, информация. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val biometricLock by viewModel.biometricLock.collectAsStateWithLifecycle()
    val autoLock by viewModel.autoLockMinutes.collectAsStateWithLifecycle()
    val openAiKey by viewModel.openAiKey.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ScreenScaffold("Настройки", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Оформление", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setTheme(ThemeMode.SYSTEM) },
                    label = { Text(THEME_LABELS.getValue(ThemeMode.SYSTEM)) }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setTheme(ThemeMode.LIGHT) },
                    label = { Text(THEME_LABELS.getValue(ThemeMode.LIGHT)) }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setTheme(ThemeMode.DARK) },
                    label = { Text(THEME_LABELS.getValue(ThemeMode.DARK)) }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.AMOLED,
                    onClick = { viewModel.setTheme(ThemeMode.AMOLED) },
                    label = { Text(THEME_LABELS.getValue(ThemeMode.AMOLED)) }
                )
            }

            Text("AI", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = openAiKey,
                onValueChange = viewModel::onOpenAiKeyChange,
                label = { Text("OpenAI API-ключ") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                viewModel.saveOpenAiKey()
                context.toast("Ключ сохранён")
            }) { Text("Сохранить ключ") }

            Text("Безопасность", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Биометрический замок", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Требовать отпечаток при входе",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = biometricLock, onCheckedChange = viewModel::setBiometricLock)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Автоблокировка", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "При бездействии: $autoLock мин",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    TextButton(onClick = { expanded = true }) { Text("Изменить") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("1", "5", "15", "60").forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text("$minutes мин") },
                                onClick = {
                                    viewModel.setAutoLock(minutes)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text("Данные", style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                viewModel.clearChatHistory()
                context.toast("История чата очищена")
            }) { Text("Очистить историю чата") }

            Text("О приложении", style = MaterialTheme.typography.titleMedium)
            Text(
                "UltimateTool 1.0.0 • Kotlin, Jetpack Compose, Hilt, Room, LuaJ.\n" +
                    "Плагины выполняются в песочнице: без доступа к файловой системе и системным модулям.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
