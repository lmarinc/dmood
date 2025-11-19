package com.dmood.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta base D-Mood: verde suave + naranja acento

private val LightDmoodColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),          // Verde principal (app bar, botones)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF003314),

    secondary = Color(0xFFFFB74D),        // Naranja acento
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF442B00),

    tertiary = Color(0xFF039BE5),         // Azul suave opcional
    onTertiary = Color.White,

    // OJO: esto manda en el fondo general de las pantallas
    background = Color(0xFFE8F3EC),       // Verde-gris claro (NO es blanco)
    onBackground = Color(0xFF102017),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212),

    surfaceVariant = Color(0xFFD4E4D9),   // Para Cards, etc.
    onSurfaceVariant = Color(0xFF223326),

    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkDmoodColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),

    secondary = Color(0xFFFFCC80),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF8D6E63),
    onSecondaryContainer = Color(0xFFFFE0B2),

    tertiary = Color(0xFF4FC3F7),
    onTertiary = Color.Black,

    background = Color(0xFF101412),
    onBackground = Color(0xFFE0E3E1),

    surface = Color(0xFF181C1A),
    onSurface = Color(0xFFE0E3E1),
    surfaceVariant = Color(0xFF2A322D),
    onSurfaceVariant = Color(0xFFCED9D2),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

@Composable
fun DmoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme =
        if (darkTheme) DarkDmoodColorScheme else LightDmoodColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DmoodTypography,
        content = content
    )
}
