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

// Brand palette aligns with the user's selected bottle-green calling icon.
// Contrast stays high in Light, Dark and AMOLED modes.
val Navy = Color(0xFF062F20)
val Blue = Color(0xFF176FBC)  // Preserve blue for messaging actions.
val Cyan = Color(0xFF23BD9D)
val Mint = Color(0xFF24B67A)
val Ink = Color(0xFF142A21)
val BottleGreen = Color(0xFF087347)

private val Light = lightColorScheme(
    primary = BottleGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F4E5),
    onPrimaryContainer = Color(0xFF06462D),
    secondary = Color(0xFF316B52),
    onSecondary = Color.White,
    background = Color(0xFFF5F8F6),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8F1EB),
    onSurfaceVariant = Color(0xFF50665A),
    surfaceContainerLow = Color(0xFFF0F6F2),
    surfaceContainerHigh = Color(0xFFE8F0EA),
    outline = Color(0xFF81988B),
    outlineVariant = Color(0xFFD9E7DD)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF4DD49A),
    onPrimary = Color(0xFF003D24),
    primaryContainer = Color(0xFF17583C),
    onPrimaryContainer = Color(0xFFCBF5DC),
    secondary = Color(0xFF8BDCB4),
    background = Color(0xFF0B1712),
    onBackground = Color(0xFFE9F6EE),
    surface = Color(0xFF14251C),
    onSurface = Color(0xFFE9F6EE),
    surfaceVariant = Color(0xFF22372B),
    onSurfaceVariant = Color(0xFFBACBBF),
    surfaceContainerLow = Color(0xFF112018),
    surfaceContainerHigh = Color(0xFF253A2D),
    outlineVariant = Color(0xFF385443)
)

private val Amoled = darkColorScheme(
    primary = Color(0xFF4DD49A),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF174F36),
    onPrimaryContainer = Color(0xFFD8F5E2),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0B130E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF15251B),
    onSurfaceVariant = Color(0xFFB5CCBD),
    surfaceContainerLow = Color(0xFF08120C),
    surfaceContainerHigh = Color(0xFF203126),
    outlineVariant = Color(0xFF314739)
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
