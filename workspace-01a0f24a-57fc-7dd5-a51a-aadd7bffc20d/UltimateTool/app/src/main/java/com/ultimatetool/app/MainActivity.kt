package com.ultimatetool.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatetool.app.core.navigation.UltimateToolNavHost
import com.ultimatetool.app.data.local.AppPreferences
import com.ultimatetool.app.data.local.ThemeMode
import com.ultimatetool.app.presentation.theme.UltimateToolTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Единственная Activity. FragmentActivity нужна для BiometricPrompt.
 * Edge-to-edge + тема из настроек пользователя.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by preferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            UltimateToolTheme(themeMode = themeMode) {
                UltimateToolNavHost()
            }
        }
    }
}
