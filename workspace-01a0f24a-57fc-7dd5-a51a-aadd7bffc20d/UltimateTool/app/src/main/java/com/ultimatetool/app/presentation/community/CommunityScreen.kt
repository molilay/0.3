package com.ultimatetool.app.presentation.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultimatetool.app.presentation.components.ScreenScaffold

/**
 * Раздел «Сообщество». В MVP — локальная витрина; при подключении бэкенда
 * данные заменяются на ленту с сервера (тот же UI-контракт).
 */
private data class CommunityPost(
    val title: String,
    val author: String,
    val text: String,
    val likes: Int
)

private val SAMPLE_POSTS = listOf(
    CommunityPost(
        "Добро пожаловать в UltimateTool",
        "Команда",
        "Мультитул с AI-чатом, плагинами и набором утилит. Делитесь плагинами и идеями!",
        128
    ),
    CommunityPost(
        "Обновление 1.0.0",
        "Команда",
        "Первый публичный релиз: 8 встроенных утилит, Lua-плагины и AI-чат.",
        96
    ),
    CommunityPost(
        "Пишем свой первый Lua-плагин",
        "DCA",
        "Создайте плагин кнопкой «Новый плагин», используйте tool.log() и on_run().",
        54
    )
)

@Composable
fun CommunityScreen(onBack: () -> Unit) {
    ScreenScaffold("Сообщество", onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Лента", style = MaterialTheme.typography.titleMedium)
            }
            items(SAMPLE_POSTS) { post ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(post.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            post.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                post.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Outlined.ThumbUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                " ${post.likes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Топ плагинов недели и форум станут доступны после подключения бэкенда.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
