package com.a2heng.opencode.mobile.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Accent = Color(0xFF4CC38A)
val AccentDim = Color(0xFF2E7D57)
val Sky = Color(0xFF7DD3FC)
val Warn = Color(0xFFF5A524)
val Danger = Color(0xFFF0616D)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF06281B),
    primaryContainer = Color(0xFF143A2B),
    onPrimaryContainer = Color(0xFFB7F5D6),
    secondary = Sky,
    onSecondary = Color(0xFF04202C),
    tertiary = Warn,
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE6EAF0),
    surface = Color(0xFF141A21),
    onSurface = Color(0xFFE6EAF0),
    surfaceVariant = Color(0xFF1E2732),
    onSurfaceVariant = Color(0xFFA7B2C0),
    outline = Color(0xFF39424E),
    error = Danger,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F8A5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F5E4),
    onPrimaryContainer = Color(0xFF06301E),
    secondary = Color(0xFF1D7FA6),
    onSecondary = Color.White,
    tertiary = Color(0xFF9A6400),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF14181D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14181D),
    surfaceVariant = Color(0xFFEDF0F4),
    onSurfaceVariant = Color(0xFF5B6572),
    outline = Color(0xFFC7CDD6),
    error = Color(0xFFC0323D),
)

fun rateColor(rate: Double, dark: Boolean): Color = when {
    rate >= 95.0 -> if (dark) Color(0xFF4ADE80) else Color(0xFF17864A)
    rate >= 90.0 -> if (dark) Color(0xFFA3E635) else Color(0xFF4D7C0F)
    rate >= 80.0 -> if (dark) Color(0xFFFACC15) else Color(0xFF9A6700)
    rate >= 70.0 -> if (dark) Color(0xFFFB923C) else Color(0xFFB45309)
    rate >= 50.0 -> if (dark) Color(0xFFF87171) else Color(0xFFC0323D)
    else -> if (dark) Color(0xFFEF4444) else Color(0xFF991B1B)
}

@Composable
fun OcTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}
