package com.example.gloveworks30.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4C7EFF),
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF062925),

    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF071A2B),

    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE5E7EB),

    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFE5E7EB),

    surfaceVariant = Color(0xFF24324A),
    onSurfaceVariant = Color(0xFFB9C3D3),

    outline = Color(0xFF3B4A66),

    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2B5BFF),
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFF2563EB),
    onTertiary = Color(0xFFFFFFFF),

    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0B1220),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1220),

    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = Color(0xFF334155),

    outline = Color(0xFF94A3B8),

    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun Gloveworks30Theme(
    darkTheme: Boolean = true,   // <-- always dark
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
){
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
