package com.ultimatetool.app.domain.model

/** Доменные модели: только данные, без Android-зависимостей. */

data class AuthSession(
    val userId: String,
    val username: String,
    val token: String,
    val isDemo: Boolean
)

data class ChatMessage(
    val id: String,
    val role: String,       // "user" | "assistant"
    val content: String,
    val createdAt: Long
) {
    val isUser: Boolean get() = role == "user"
}

data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val source: String,
    val enabled: Boolean,
    val updatedAt: Long
)

/** Установленный экземпляр Linux-дистрибутива (proot-виртуализация). */
data class LinuxInstance(
    val distroId: String,
    val state: String,
    val customUrl: String,
    val vncPort: Int,
    val updatedAt: Long
)
