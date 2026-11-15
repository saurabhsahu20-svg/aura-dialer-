package com.scall.phone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

/** Carefully tuned Android system sans typography: lightweight, offline, and Hindi-compatible. */
private val SCallTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.75).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.55).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.35).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.35).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = (-0.25).sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = (-0.18).sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 17.sp)
)

@Composable
fun SCallTheme(mode: String = "System", content: @Composable () -> Unit) {
    val dark = mode == "Dark" || mode == "AMOLED" || (mode == "System" && isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (mode == "AMOLED") Amoled else if (dark) Dark else Light,
        typography = SCallTypography,
        content = content
    )
}
