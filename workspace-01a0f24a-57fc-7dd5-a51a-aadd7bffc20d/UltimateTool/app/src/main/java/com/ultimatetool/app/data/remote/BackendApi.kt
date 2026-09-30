package com.ultimatetool.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

// ---------- DTO авторизации ----------

data class RequestCodeRequest(val telegramUsername: String)

data class RequestCodeResponse(val ok: Boolean, val error: String?)

data class VerifyCodeRequest(val telegramUsername: String, val code: String)

data class VerifyCodeResponse(
    val ok: Boolean,
    val token: String?,
    val userId: String?,
    val username: String?,
    val error: String?
)

/**
 * REST-контракт бэкенда (авторизация через Telegram-бот).
 * Если бэкенд недоступен — приложение уходит в демо-режим.
 */
interface BackendApi {

    @POST("auth/telegram/request-code")
    suspend fun requestCode(@Body body: RequestCodeRequest): RequestCodeResponse

    @POST("auth/telegram/verify")
    suspend fun verifyCode(@Body body: VerifyCodeRequest): VerifyCodeResponse
}
