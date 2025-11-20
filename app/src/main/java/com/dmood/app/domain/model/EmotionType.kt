package com.dmood.app.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Representa los diferentes tipos de emociones soportados por D-Mood.
 * Cada emoción incluye:
 * - displayName: texto visible para usuario
 * - color: referencia visual para UI
 * - description: explicación breve para onboarding, ayuda o interpretación
 */
enum class EmotionType(
    val displayName: String,
    val color: Color,
    val description: String,
) {

    /** Emoción asociada a entusiasmo, energía y bienestar emocional. */
    ALEGRE(
        displayName = "Alegre",
        color = Color(0xFFFFEB3B), // Amarillo
        description = "Sensación de felicidad, energía emocional y disfrute."
    ),

    /** Sensación de autoconfianza, estabilidad y tranquilidad interior. */
    SEGURO(
        displayName = "Seguro",
        color = Color(0xFF4CAF50), // Verde esperanza
        description = "Estado de confianza personal, calma y seguridad emocional."
    ),

    /** Reacción emocional ante incertidumbre, amenaza o preocupación. */
    MIEDO(
        displayName = "Miedo",
        color = Color(0xFF212121), // Negro / muy oscuro
        description = "Sensación de alerta, nerviosismo o anticipación negativa."
    ),

    /** Reacción ante lo inesperado, novedoso o fuera de expectativas. */
    SORPRENDIDO(
        displayName = "Sorprendido",
        color = Color(0xFF9C27B0), // Morado
        description = "Sensación provocada por un evento inesperado o novedoso."
    ),

    /** Estado emocional asociado a tristeza, vacío o pérdida temporal. */
    TRISTE(
        displayName = "Triste",
        color = Color(0xFF2196F3), // Azul
        description = "Sensación emocional melancólica, apagada o con baja energía."
    ),

    /** Disconfort emocional que puede ser social, personal o situacional. */
    INCOMODO(
        displayName = "Incómodo",
        color = Color(0xFF8D6E63), // Marrón medio
        description = "Sensación de tensión emocional, desalineación o inseguridad situacional."
    ),

    /** Respuesta emocional de frustración ante obstáculos o conflictos. */
    ENFADADO(
        displayName = "Enfadado",
        color = Color(0xFFF44336), // Rojo
        description = "Estado de irritación, enojo o saturación emocional ante un evento."
    ),

    /** Estado orientado a objetivos, acción y determinación. */
    MOTIVADO(
        displayName = "Motivado",
        color = Color(0xFFFF9800), // Naranja
        description = "Sensación de foco, intención, impulso y propósito personal."
    ),

    /** Estado emocional equilibrado sin carga positiva o negativa. */
    NORMAL(
        displayName = "Normal",
        color = Color(0xFFBDBDBD), // Gris neutro
        description = "Estado neutral, estable y sin activación emocional destacable."
    );

    fun isPositive(): Boolean = this in listOf(ALEGRE, SEGURO, SORPRENDIDO, MOTIVADO)

    fun isNegative(): Boolean = this in listOf(MIEDO, TRISTE, INCOMODO, ENFADADO)

    fun valence(): Int = when {
        isPositive() -> 1
        isNegative() -> -1
        else -> 0
    }
}
