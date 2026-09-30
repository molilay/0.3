package com.ultimatetool.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ultimatetool.app.data.local.ThemeMode

// Базовая палитра (используется, когда Dynamic Color недоступен — Android < 12).
private val SeedPrimary = Color(0xFF0B57D0)
private val SeedSecondary = Color(0xFF00639B)
private val SeedTertiary = Color(0xFF6B4FA3)

private val LightColors = lightColorScheme(
    primary = SeedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = SeedSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE5FF),
    onSecondaryContainer = Color(0xFF001D33),
    tertiary = SeedTertiary,
    background = Color(0xFFFAF9FD),
    surface = Color(0xFFFAF9FD),
    surfaceVariant = Color(0xFFE7E8EC),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF9BCCFF),
    onSecondary = Color(0xFF003353),
    secondaryContainer = Color(0xFF004A75),
    onSecondaryContainer = Color(0xFFCDE5FF),
    tertiary = Color(0xFFCDBBFF),
    background = Color(0xFF131318),
    surface = Color(0xFF131318),
    surfaceVariant = Color(0xFF24262B),
    error = Color(0xFFFFB4AB)
)

/** AMOLED-вариант: чисто чёрный фон для экранов с соответствующей матрицей. */
private val AmoledColors = DarkColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF141414),
    outlineVariant = Color(0xFF2A2A2A)
)

val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp)
)

@Composable
fun UltimateToolTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = when {
        themeMode == ThemeMode.AMOLED -> AmoledColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
