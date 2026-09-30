package com.ultimatetool.app.presentation.aichat

import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultimatetool.app.core.util.copyToClipboard
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ultimatetool.app.core.security.SessionManager
import com.ultimatetool.app.domain.model.ChatMessage
import com.ultimatetool.app.domain.repository.ChatRepository
import com.ultimatetool.app.presentation.components.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import io.noties.markwon.Markwon
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiChatUiState(
    val input: String = "",
    val model: String = "gpt-4o-mini",
    val sending: Boolean = false,
    val hasApiKey: Boolean = false
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val models = listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")

    val messages: StateFlow<List<ChatMessage>> = chatRepository.observeMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(
        AiChatUiState(hasApiKey = !sessionManager.getOpenAiKey().isNullOrBlank())
    )
    val ui: StateFlow<AiChatUiState> = _ui.asStateFlow()

    fun onInputChange(value: String) {
        _ui.update { it.copy(input = value.take(4_000)) }
    }

    fun onModelChange(model: String) {
        _ui.update { it.copy(model = model) }
    }

    /** Отправка: защита от двойного нажатия флагом sending. */
    fun send() {
        val text = _ui.value.input.trim()
        val current = _ui.value
        if (text.isEmpty() || current.sending) return

        _ui.update { it.copy(sending = true, input = "") }
        viewModelScope.launch {
            // Берём контекст ДО сохранения нового сообщения, чтобы не задублировать его.
            val history = try {
                chatRepository.lastMessages(12).reversed()
            } catch (_: Exception) {
                emptyList()
            }
            chatRepository.saveMessage("user", text)

            val apiKey = sessionManager.getOpenAiKey()
            val reply = if (apiKey.isNullOrBlank()) {
                "Демо-режим: добавьте OpenAI API-ключ в Настройках, чтобы получать реальные ответы."
            } else {
                try {
                    chatRepository.askModel(history, text, current.model, apiKey)
                } catch (e: Exception) {
                    "⚠️ Ошибка запроса к модели: ${e.message ?: "неизвестная"}"
                }
            }
            chatRepository.saveMessage("assistant", reply)
            _ui.update { it.copy(sending = false) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { chatRepository.clearHistory() }
    }

    /** Экспорт диалога в текст (для сохранения в файл/буфер обмена). */
    fun exportChatText(): String {
        val builder = StringBuilder("UltimateTool — экспорт диалога (модель: ${_ui.value.model})\n\n")
        messages.value.forEach { message ->
            builder.append(if (message.isUser) "Вы: " else "Ассистент: ")
                .append(message.content)
                .append("\n\n")
        }
        return builder.toString()
    }
}

/** AI-чат: история в Room, запросы к OpenAI-совместимому API, Markdown в ответах. */
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ScreenScaffold(
        title = "QuarkAI",
        onBack = onBack,
        actions = {
            Box {
                var expanded by remember { mutableStateOf(false) }
                TextButton(onClick = { expanded = true }) { Text(ui.model) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    viewModel.models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                viewModel.onModelChange(model)
                                expanded = false
                            }
                        )
                    }
                }
            }
            val context = LocalContext.current
            IconButton(onClick = {
                context.copyToClipboard(viewModel.exportChatText())
            }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Экспорт диалога")
            }
            IconButton(onClick = viewModel::clearHistory) {
                Icon(Icons.Outlined.Delete, contentDescription = "Очистить историю")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (!ui.hasApiKey) {
                Text(
                    "API-ключ не задан — чат работает в демо-режиме. Ключ добавляется в Настройках.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            "Задайте вопрос — ответ появится здесь.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = ui.input,
                    onValueChange = viewModel::onInputChange,
                    placeholder = { Text("Сообщение…") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = viewModel::send,
                    enabled = !ui.sending && ui.input.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            if (isUser) {
                Text(message.content, modifier = Modifier.padding(12.dp))
            } else {
                MarkdownText(message.content, Modifier.padding(12.dp))
            }
        }
    }
}

/** Рендеринг Markdown через Markwon (TextView внутри AndroidView). */
@Composable
private fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply { setTextIsSelectable(true) }
        },
        update = { tv ->
            tv.setTextColor(textColor.toArgb())
            tv.textSize = 14f
            try {
                Markwon.create(tv.context).setMarkdown(tv, text)
            } catch (_: Exception) {
                tv.text = text
            }
        },
        modifier = modifier
    )
}
