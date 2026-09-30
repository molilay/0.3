package com.ultimatetool.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Точка входа приложения. @HiltAndroidApp запускает генерацию DI-графа.
 */
@HiltAndroidApp
class UltimateToolApp : Application()
