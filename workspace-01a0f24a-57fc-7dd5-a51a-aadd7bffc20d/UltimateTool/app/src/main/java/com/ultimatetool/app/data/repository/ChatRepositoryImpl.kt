package com.ultimatetool.app.data.repository

import com.ultimatetool.app.data.local.MessageDao
import com.ultimatetool.app.data.local.MessageEntity
import com.ultimatetool.app.data.remote.ChatCompletionRequest
import com.ultimatetool.app.data.remote.OpenAiApi
import com.ultimatetool.app.data.remote.OpenAiMessage
import com.ultimatetool.app.domain.model.ChatMessage
import com.ultimatetool.app.domain.repository.ChatRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val openAiApi: OpenAiApi
) : ChatRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> =
        messageDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun lastMessages(limit: Int): List<ChatMessage> =
        try {
            messageDao.last(limit).map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }

    override suspend fun saveMessage(role: String, content: String): ChatMessage {
        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            role = role,
            content = content.take(50_000),
            createdAt = System.currentTimeMillis()
        )
        try {
            messageDao.insert(entity)
        } catch (_: Exception) {
            // Потеря одного сообщения не критичнее падения экрана.
        }
        return entity.toDomain()
    }

    override suspend fun askModel(
        history: List<ChatMessage>,
        prompt: String,
        model: String,
        apiKey: String
    ): String {
        val messages = buildList {
            add(OpenAiMessage("system", SYSTEM_PROMPT))
            history.takeLast(CONTEXT_LIMIT).forEach { add(OpenAiMessage(it.role, it.content)) }
            add(OpenAiMessage("user", prompt))
        }
        val response = openAiApi.chatCompletions(
            authorization = "Bearer $apiKey",
            body = ChatCompletionRequest(model = model, messages = messages)
        )
        return response.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: "(пустой ответ модели)"
    }

    override suspend fun clearHistory() {
        try {
            messageDao.deleteAll()
        } catch (_: Exception) {
        }
    }

    private fun MessageEntity.toDomain() =
        ChatMessage(id = id, role = role, content = content, createdAt = createdAt)

    private companion object {
        const val SYSTEM_PROMPT =
            "Ты — встроенный ассистент приложения UltimateTool. Отвечай кратко, по делу, " +
                "используй Markdown, если это улучшает читаемость."
        const val CONTEXT_LIMIT = 12
    }
}
