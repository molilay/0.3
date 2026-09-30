package com.ultimatetool.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранилище сессии и секретов.
 * Основа — EncryptedSharedPreferences; если крипто-провайдер на устройстве
 * сломан (редко, но бывает), молча откатываемся на обычные SharedPreferences,
 * чтобы приложение не падало на старте.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        const val FILE_NAME = "ultimatetool_secure_prefs"
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_IS_DEMO = "is_demo"
        const val KEY_OPENAI_KEY = "openai_key"
    }

    private val prefs: SharedPreferences by lazy { createPrefs() }

    private fun createPrefs(): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Throwable) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    private inline fun <T> safe(default: T, block: () -> T): T =
        try {
            block()
        } catch (_: Throwable) {
            default
        }

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun getToken(): String? = safe(null) { prefs.getString(KEY_TOKEN, null) }

    fun getUserId(): String? = safe(null) { prefs.getString(KEY_USER_ID, null) }

    fun getUsername(): String? = safe(null) { prefs.getString(KEY_USERNAME, null) }

    fun isDemo(): Boolean = safe(true) { prefs.getBoolean(KEY_IS_DEMO, true) }

    fun saveSession(token: String, userId: String, username: String, isDemo: Boolean) {
        try {
            prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putBoolean(KEY_IS_DEMO, isDemo)
                .apply()
        } catch (_: Throwable) {
        }
    }

    /** Выход из аккаунта. API-ключи пользователя сохраняем — они не относятся к сессии. */
    fun clear() {
        try {
            prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USERNAME)
                .remove(KEY_IS_DEMO)
                .apply()
        } catch (_: Throwable) {
        }
    }

    fun getOpenAiKey(): String? = safe(null) { prefs.getString(KEY_OPENAI_KEY, null) }

    fun setOpenAiKey(key: String?) {
        try {
            prefs.edit().putString(KEY_OPENAI_KEY, key?.takeIf { it.isNotBlank() }).apply()
        } catch (_: Throwable) {
        }
    }
}
