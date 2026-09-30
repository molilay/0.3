package com.ultimatetool.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ultimatetool.app.core.security.BiometricHelper
import com.ultimatetool.app.core.security.SessionManager
import com.ultimatetool.app.core.util.toast
import com.ultimatetool.app.domain.repository.AuthRepository
import com.ultimatetool.app.domain.usecase.RequestLoginCodeUseCase
import com.ultimatetool.app.domain.usecase.VerifyLoginCodeUseCase
import com.ultimatetool.app.presentation.components.ErrorBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginPhase { USERNAME, CODE }

data class LoginUiState(
    val phase: LoginPhase = LoginPhase.USERNAME,
    val username: String = "",
    val code: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val authed: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val requestLoginCode: RequestLoginCodeUseCase,
    private val verifyLoginCode: VerifyLoginCodeUseCase,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value.take(64), error = null) }
    }

    fun onCodeChange(value: String) {
        _state.update { it.copy(code = value.filter(Char::isDigit).take(16), error = null) }
    }

    /** Защита от повторных нажатий — через флаг loading. */
    fun requestCode() {
        val username = _state.value.username.trim()
        if (username.isEmpty() || _state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            requestLoginCode(username)
                .onSuccess {
                    _state.update { it.copy(loading = false, phase = LoginPhase.CODE) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Ошибка запроса кода") }
                }
        }
    }

    fun verifyCode() {
        val current = _state.value
        if (current.loading || current.code.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            verifyLoginCode(current.username.trim(), current.code)
                .onSuccess { session ->
                    sessionManager.saveSession(session.token, session.userId, session.username, session.isDemo)
                    _state.update { it.copy(loading = false, authed = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Неверный код") }
                }
        }
    }

    fun demoLogin() {
        if (_state.value.loading) return
        val session = authRepository.demoSession()
        sessionManager.saveSession(session.token, session.userId, session.username, true)
        _state.update { it.copy(authed = true) }
    }

    /** Биометрия: если сохранённая сессия есть — просто пропускаем, иначе демо-вход. */
    fun biometricLogin() {
        if (sessionManager.isLoggedIn()) {
            _state.update { it.copy(authed = true) }
        } else {
            demoLogin()
        }
    }

    fun backToUsername() {
        _state.update { it.copy(phase = LoginPhase.USERNAME, code = "", error = null) }
    }
}

/** Экран входа без пароля: Telegram-код / биометрия / демо-режим. */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context as? FragmentActivity }
    val biometricAvailable = remember(activity) {
        activity?.let { BiometricHelper.canAuthenticate(it) } == true
    }

    LaunchedEffect(state.authed) {
        if (state.authed) onLoggedIn()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("UltimateTool", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (state.phase == LoginPhase.USERNAME) "Вход через Telegram" else "Введите код из бота",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            when (state.phase) {
                LoginPhase.USERNAME -> OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    label = { Text("Telegram @username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LoginPhase.CODE -> OutlinedTextField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    label = { Text("Одноразовый код") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
            state.error?.let { ErrorBanner(it) }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (state.phase == LoginPhase.USERNAME) viewModel.requestCode()
                    else viewModel.verifyCode()
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        state.loading -> "Подождите…"
                        state.phase == LoginPhase.USERNAME -> "Получить код"
                        else -> "Войти"
                    }
                )
            }

            if (state.phase == LoginPhase.CODE) {
                TextButton(onClick = viewModel::backToUsername) {
                    Text("Изменить аккаунт")
                }
            }

            if (biometricAvailable) {
                TextButton(
                    enabled = !state.loading,
                    onClick = {
                        activity?.let { fragmentActivity ->
                            BiometricHelper.authenticate(
                                activity = fragmentActivity,
                                title = "Вход в UltimateTool",
                                onSuccess = { viewModel.biometricLogin() },
                                onError = { msg -> context.toast(msg) }
                            )
                        }
                    }
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Биометрический вход")
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            TextButton(onClick = viewModel::demoLogin, enabled = !state.loading) {
                Text("Войти в демо-режиме (без сервера)")
            }
        }
    }
}
