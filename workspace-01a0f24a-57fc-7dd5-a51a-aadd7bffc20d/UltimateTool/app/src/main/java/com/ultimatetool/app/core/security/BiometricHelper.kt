package com.ultimatetool.app.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Обёртка над BiometricPrompt. Вызывается только из UI-слоя,
 * где доступен FragmentActivity.
 */
object BiometricHelper {

    fun canAuthenticate(activity: FragmentActivity): Boolean = try {
        BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    } catch (_: Exception) {
        false
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onError(errString.toString())
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            prompt.authenticate(info)
        } catch (e: Exception) {
            onError(e.message ?: "Биометрия недоступна")
        }
    }
}
