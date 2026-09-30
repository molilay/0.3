package com.ultimatetool.app.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// ---------- DTO OpenAI Chat Completions ----------

data class OpenAiMessage(val role: String, val content: String)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.7
)

data class ChatChoice(val message: OpenAiMessage?)

data class ChatCompletionResponse(val choices: List<ChatChoice>?)

/**
 * OpenAI-совместимый API. Ключ передаётся заголовком, чтобы не смешивать
 * его с interceptor'ами бэкенда приложения.
 */
interface OpenAiApi {

    @POST("v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body body: ChatCompletionRequest
    ): ChatCompletionResponse
}
