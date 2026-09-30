package com.ultimatetool.app.data.repository

import com.ultimatetool.app.data.remote.BackendApi
import com.ultimatetool.app.data.remote.RequestCodeRequest
import com.ultimatetool.app.data.remote.VerifyCodeRequest
import com.ultimatetool.app.domain.model.AuthSession
import com.ultimatetool.app.domain.repository.AuthRepository
import java.util.UUID
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: BackendApi
) : AuthRepository {

    override suspend fun requestCode(telegramUsername: String): Result<Unit> = try {
        val response = api.requestCode(RequestCodeRequest(telegramUsername.trim()))
        if (response.ok) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.error ?: "Сервер отклонил запрос"))
        }
    } catch (e: Exception) {
        Result.failure(Exception("Бэкенд недоступен: ${e.message ?: "сеть"}", e))
    }

    override suspend fun verifyCode(
        telegramUsername: String,
        code: String
    ): Result<AuthSession> = try {
        val response = api.verifyCode(VerifyCodeRequest(telegramUsername.trim(), code.trim()))
        if (response.ok && !response.token.isNullOrBlank()) {
            Result.success(
                AuthSession(
                    userId = response.userId ?: "unknown",
                    username = response.username ?: telegramUsername.trim(),
                    token = response.token,
                    isDemo = false
                )
            )
        } else {
            Result.failure(Exception(response.error ?: "Неверный код"))
        }
    } catch (e: Exception) {
        Result.failure(Exception("Бэкенд недоступен: ${e.message ?: "сеть"}", e))
    }

    override fun demoSession(): AuthSession = AuthSession(
        userId = "demo-${UUID.randomUUID().toString().take(8)}",
        username = "demo_user",
        token = UUID.randomUUID().toString(),
        isDemo = true
    )
}
