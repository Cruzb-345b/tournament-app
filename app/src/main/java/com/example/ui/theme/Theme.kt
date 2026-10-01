package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = Indigo60,
    onPrimary = Color.White,
    primaryContainer = Indigo40,
    onPrimaryContainer = Indigo95,
    secondary = GoldBright,
    onSecondary = Color.Black,
    secondaryContainer = GoldDark,
    onSecondaryContainer = GoldLight,
    tertiary = WinnerGreen,
    onTertiary = Color.White,
    background = ArenaDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = ArenaDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = ArenaDarkCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = ArenaBorder,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Indigo60,
    onPrimary = Color.White,
    primaryContainer = Indigo95,
    onPrimaryContainer = Indigo40,
    secondary = GoldPrimary,
    onSecondary = Color.Black,
    secondaryContainer = GoldLight,
    onSecondaryContainer = GoldDark,
    tertiary = WinnerGreenDark,
    onTertiary = Color.White,
    background = ArenaLightBg,
    onBackground = Color(0xFF0F172A),
    surface = ArenaLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = ArenaLightCard,
    onSurfaceVariant = Color(0xFF475569),
    outline = ArenaLightBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to sleek tournament dark arena look
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
