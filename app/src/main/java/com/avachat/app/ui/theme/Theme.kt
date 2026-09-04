package com.avachat.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ColorScheme
import com.avachat.app.core.domain.model.ThemeMode

/**
 * Centralized design tokens. No screen hardcodes colors; everything routes
 * through this theme.
 */
object AvaPalette {
    val Indigo = Color(0xFF4F46E5)
    val IndigoDark = Color(0xFF818CF8)
    val Violet = Color(0xFF7C3AED)
    val BgLight = Color(0xFFF7F7FB)
    val BgDark = Color(0xFF101318)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF191D24)
    val BubbleUserLight = Color(0xFF4F46E5)
    val BubbleUserDark = Color(0xFF6366F1)
    val BubbleAiLight = Color(0xFFEEF0F6)
    val BubbleAiDark = Color(0xFF232936)
    val SuccessGreen = Color(0xFF22C55E)
    val ErrorRed = Color(0xFFEF4444)
}

private val LightColors = lightColorScheme(
    primary = AvaPalette.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = AvaPalette.Violet,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    background = AvaPalette.BgLight,
    onBackground = Color(0xFF111827),
    surface = AvaPalette.SurfaceLight,
    onSurface = Color(0xFF111827),
    surfaceVariant = AvaPalette.BubbleAiLight,
    onSurfaceVariant = Color(0xFF4B5563),
    error = AvaPalette.ErrorRed,
    outlineVariant = Color(0xFFE5E7EB)
)

private val DarkColors = darkColorScheme(
    primary = AvaPalette.IndigoDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFA78BFA),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFEDE9FE),
    background = AvaPalette.BgDark,
    onBackground = Color(0xFFE5E7EB),
    surface = AvaPalette.SurfaceDark,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = AvaPalette.BubbleAiDark,
    onSurfaceVariant = Color(0xFF9CA3AF),
    error = Color(0xFFF87171),
    outlineVariant = Color(0xFF2A3140)
)

@Composable
fun AvaChatTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme: ColorScheme = when {
        dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AvaTypography,
        content = content
    )
}
