package com.scall.phone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF061C43)
val Blue = Color(0xFF186EFF)
val Cyan = Color(0xFF20BDFF)
val Mint = Color(0xFF32C58B)
val Ink = Color(0xFF122039)

private val Light = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    background = Color(0xFFF8FAFE),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDF2FA),
    outlineVariant = Color(0xFFE5EAF3)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF6DA5FF),
    onPrimary = Navy,
    background = Color(0xFF101725),
    onBackground = Color(0xFFF1F5FC),
    surface = Color(0xFF1B2434),
    onSurface = Color(0xFFF1F5FC),
    surfaceVariant = Color(0xFF273248)
)

private val Amoled = darkColorScheme(
    primary = Color(0xFF6DA5FF),
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0F1116),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF191D24)
)

@Composable
fun SCallTheme(mode: String = "System", content: @Composable () -> Unit) {
    val dark = mode == "Dark" || mode == "AMOLED" || (mode == "System" && isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (mode == "AMOLED") Amoled else if (dark) Dark else Light,
        content = content
    )
}
