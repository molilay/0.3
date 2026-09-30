package com.ultimatetool.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ultimatetool.app.core.tools.ToolSpec
import com.ultimatetool.app.domain.repository.PluginsRepository
import com.ultimatetool.app.domain.usecase.GetToolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    getToolsUseCase: GetToolsUseCase,
    pluginsRepository: PluginsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val tools: StateFlow<List<ToolSpec>> = _query
        .map { getToolsUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), getToolsUseCase(""))

    val pluginCount: StateFlow<Int> = pluginsRepository.countPlugins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onQueryChange(value: String) {
        _query.value = value.take(100)
    }
}

/** Домашний дашборд: поиск, быстрые разделы, сетка инструментов. */
@Composable
fun HomeScreen(
    onToolClick: (String) -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenPlugins: () -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenLinux: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val tools by viewModel.tools.collectAsStateWithLifecycle()
    val pluginCount by viewModel.pluginCount.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("UltimateTool", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Мультитул для всего",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenProfile) {
                    Icon(Icons.Outlined.Person, contentDescription = "Профиль")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Настройки")
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Поиск инструментов…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickCard("AI-чат", Icons.Outlined.Chat, Modifier.weight(1f), onOpenAiChat)
                QuickCard("Плагины ($pluginCount)", Icons.Outlined.Extension, Modifier.weight(1f), onOpenPlugins)
                QuickCard("Сообщество", Icons.Outlined.Forum, Modifier.weight(1f), onOpenCommunity)
            }
            Spacer(Modifier.height(8.dp))
            QuickCard(
                "Linux без root — дистрибутивы, терминал и GUI (proot/VNC)",
                Icons.Outlined.Terminal,
                Modifier.fillMaxWidth(),
                onOpenLinux
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "Инструменты (${tools.size})",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tools, key = { it.id }) { tool ->
                    ToolCard(tool = tool, onClick = { onToolClick(tool.id) })
                }
            }
        }
    }
}

@Composable
private fun QuickCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ToolCard(tool: ToolSpec, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Icon(
                tool.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(tool.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

