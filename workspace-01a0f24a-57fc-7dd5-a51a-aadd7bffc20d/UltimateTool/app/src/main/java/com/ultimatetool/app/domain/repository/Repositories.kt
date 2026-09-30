package com.ultimatetool.app.domain.repository

import com.ultimatetool.app.domain.model.AuthSession
import com.ultimatetool.app.domain.model.ChatMessage
import com.ultimatetool.app.domain.model.LinuxInstance
import com.ultimatetool.app.domain.model.PluginInfo
import kotlinx.coroutines.flow.Flow

/** Контракты репозиториев: слой presentation зависит только от них. */

interface AuthRepository {
    suspend fun requestCode(telegramUsername: String): Result<Unit>
    suspend fun verifyCode(telegramUsername: String, code: String): Result<AuthSession>
    fun demoSession(): AuthSession
}

interface ChatRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun lastMessages(limit: Int): List<ChatMessage>
    suspend fun saveMessage(role: String, content: String): ChatMessage
    suspend fun askModel(history: List<ChatMessage>, prompt: String, model: String, apiKey: String): String
    suspend fun clearHistory()
}

interface PluginsRepository {
    fun observePlugins(): Flow<List<PluginInfo>>
    fun countPlugins(): Flow<Int>
    suspend fun getById(id: String): PluginInfo?
    suspend fun upsert(plugin: PluginInfo)
    suspend fun delete(id: String)
}

interface LinuxRepository {
    fun observeInstances(): Flow<List<LinuxInstance>>
    suspend fun getInstance(distroId: String): LinuxInstance?
    suspend fun upsert(instance: LinuxInstance)
    suspend fun delete(distroId: String)
}
