package com.ultimatetool.app.domain.usecase

import com.ultimatetool.app.core.tools.ToolRegistry
import com.ultimatetool.app.core.tools.ToolSpec
import com.ultimatetool.app.core.util.Hashing
import com.ultimatetool.app.domain.model.AuthSession
import com.ultimatetool.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Use Cases — тонкий слой бизнес-логики между экранами и репозиториями. */

class GetToolsUseCase @Inject constructor() {
    operator fun invoke(): List<ToolSpec> = ToolRegistry.all
    operator fun invoke(query: String): List<ToolSpec> = ToolRegistry.search(query)
}

class RequestLoginCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(telegramUsername: String): Result<Unit> =
        authRepository.requestCode(telegramUsername)
}

class VerifyLoginCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(telegramUsername: String, code: String): Result<AuthSession> =
        authRepository.verifyCode(telegramUsername, code)
}

class HashTextUseCase @Inject constructor() {
    /** Все хэши строки разом; неизвестный алгоритм пропускается. */
    operator fun invoke(input: String): Map<String, String> =
        Hashing.ALGORITHMS.associateWith { algo ->
            try {
                Hashing.hash(algo, input)
            } catch (_: Exception) {
                "—"
            }
        }
}
