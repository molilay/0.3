package com.ultimatetool.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.ultimatetool.app.core.security.SessionManager
import com.ultimatetool.app.core.util.copyToClipboard
import com.ultimatetool.app.presentation.components.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.json.JSONObject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    val username: String = sessionManager.getUsername() ?: "unknown"
    val userId: String = sessionManager.getUserId() ?: "—"
    val isDemo: Boolean = sessionManager.isDemo()

    /** Экспорт данных пользователя (мини-реализация в духе GDPR). */
    fun exportData(): String = try {
        JSONObject()
            .put("userId", userId)
            .put("username", username)
            .put("isDemo", isDemo)
            .put("exportedAt", System.currentTimeMillis())
            .toString(2)
    } catch (_: Exception) {
        "{}"
    }

    fun referralCode(): String = "UT-" + userId.takeLast(6).uppercase()

    fun signOut() {
        sessionManager.clear()
    }
}

/** Личный кабинет: профиль, статус, экспорт данных, выход. */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    ScreenScaffold("Профиль", onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    viewModel.username.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(viewModel.username, style = MaterialTheme.typography.titleLarge)
            Text(
                if (viewModel.isDemo) "Статус: демо-аккаунт" else "Статус: активен",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileAction("Реферальный код") {
                        context.copyToClipboard(viewModel.referralCode())
                    }
                    ProfileAction("Экспорт моих данных (JSON)") {
                        context.copyToClipboard(viewModel.exportData())
                    }
                    ProfileAction("История активности") {
                        context.copyToClipboard("История активности появится с подключением бэкенда.")
                    }
                    ProfileAction("Устройства и сессии") {
                        context.copyToClipboard("Список сессий появится с подключением бэкенда.")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.signOut()
                    onSignedOut()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выйти из аккаунта")
            }
        }
    }
}

@Composable
private fun ProfileAction(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title)
    }
}
