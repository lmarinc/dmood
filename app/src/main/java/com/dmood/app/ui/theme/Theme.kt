package com.dmood.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------
// NUEVA PALETA D-MOOD (Inspirada exactamente en tu referencia)
// ---------------------------------------------------------

private val DmoodLightColors = lightColorScheme(

    // Naranja cálido como color principal
    primary = Color(0xFFEA580C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE7D7),
    onPrimaryContainer = Color(0xFF622100),

    // Acento suave
    secondary = Color(0xFFF97316),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF5C2E00),

    // Tercario opcional melocotón
    tertiary = Color(0xFFFFA76C),
    onTertiary = Color.Black,

    // Fondo general suave beige
    background = Color(0xFFFEF7ED),
    onBackground = Color(0xFF1F1F1F),

    // Cards blanco puro
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),

    // Cards secundarias color crema
    surfaceVariant = Color(0xFFFFF1E6),
    onSurfaceVariant = Color(0xFF4B4B4B),

    outline = Color(0xFFE2E2E2),

    error = Color(0xFFB3261E),
    onError = Color.White
)

// Tema oscuro equivalente cálido
private val DmoodDarkColors = darkColorScheme(
    primary = Color(0xFFFF8A4C),
    onPrimary = Color.Black,

    secondary = Color(0xFFFFB38A),
    onSecondary = Color.Black,

    background = Color(0xFF201A17),
    onBackground = Color(0xFFFCEBDD),

    surface = Color(0xFF2A221E),
    onSurface = Color(0xFFF3E6DB),

    surfaceVariant = Color(0xFF3B302C),
    onSurfaceVariant = Color(0xFFEAD7C1),

    outline = Color(0xFF5C4A41)
)

@Composable
fun DmoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors: ColorScheme =
        if (darkTheme) DmoodDarkColors else DmoodLightColors

    MaterialTheme(
        colorScheme = colors,
        typography = DmoodTypography,
        content = content
    )
}
