package com.ultimatetool.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

private val Context.settingsDataStore by preferencesDataStore(name = "ultimatetool_settings")

/**
 * Настройки приложения на DataStore: переживают kill процесса,
 * читаются как Flow, не требуют миграций.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val AUTO_LOCK_MINUTES = stringPreferencesKey("auto_lock_minutes")
    }

    private val dataStore get() = context.settingsDataStore

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE].orEmpty()
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        try {
            dataStore.edit { it[Keys.THEME_MODE] = mode.name }
        } catch (_: Exception) {
        }
    }

    val biometricLock: Flow<Boolean> = dataStore.data.map { it[Keys.BIOMETRIC_LOCK] ?: false }

    suspend fun setBiometricLock(enabled: Boolean) {
        try {
            dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
        } catch (_: Exception) {
        }
    }

    val autoLockMinutes: Flow<String> = dataStore.data.map { it[Keys.AUTO_LOCK_MINUTES] ?: "5" }

    suspend fun setAutoLockMinutes(value: String) {
        try {
            dataStore.edit { it[Keys.AUTO_LOCK_MINUTES] = value }
        } catch (_: Exception) {
        }
    }
}
