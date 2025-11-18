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
        color = Color(0xFFFFE59D),
        description = "Sensación de felicidad, energía emocional y disfrute."
    ),

    /** Sensación de autoconfianza, estabilidad y tranquilidad interior. */
    SEGURO(
        displayName = "Seguro",
        color = Color(0xFFD1F2EB),
        description = "Estado de confianza personal, calma y seguridad emocional."
    ),

    /** Reacción emocional ante incertidumbre, amenaza o preocupación. */
    MIEDO(
        displayName = "Miedo",
        color = Color(0xFFFFD1CF),
        description = "Sensación de alerta, nerviosismo o anticipación negativa."
    ),

    /** Reacción ante lo inesperado, novedoso o fuera de expectativas. */
    SORPRENDIDO(
        displayName = "Sorprendido",
        color = Color(0xFFDDEBFF),
        description = "Sensación provocada por un evento inesperado o novedoso."
    ),

    /** Estado emocional asociado a tristeza, vacío o pérdida temporal. */
    TRISTE(
        displayName = "Triste",
        color = Color(0xFFCBD5F0),
        description = "Sensación emocional melancólica, apagada o con baja energía."
    ),

    /** Disconfort emocional que puede ser social, personal o situacional. */
    INCOMODO(
        displayName = "Incómodo",
        color = Color(0xFFE8D4FF),
        description = "Sensación de tensión emocional, desalineación o inseguridad situacional."
    ),

    /** Respuesta emocional de frustración ante obstáculos o conflictos. */
    ENFADADO(
        displayName = "Enfadado",
        color = Color(0xFFFFCFCB),
        description = "Estado de irritación, enojo o saturación emocional ante un evento."
    ),

    /** Estado orientado a objetivos, acción y determinación. */
    MOTIVADO(
        displayName = "Motivado",
        color = Color(0xFFC8F7C5),
        description = "Sensación de foco, intención, impulso y propósito personal."
    ),

    /** Estado emocional equilibrado sin carga positiva o negativa. */
    NORMAL(
        displayName = "Normal",
        color = Color(0xFFE6E8EB),
        description = "Estado neutral, estable y sin activación emocional destacable."
    );

    /** Indica si la emoción tiene predominancia positiva a nivel emocional. */
    fun isPositive(): Boolean = this in listOf(ALEGRE, SEGURO, SORPRENDIDO, MOTIVADO)

    /** Indica si la emoción tiene predominancia negativa a nivel emocional. */
    fun isNegative(): Boolean = this in listOf(MIEDO, TRISTE, INCOMODO, ENFADADO)

    /** Representación numérica útil para análisis y resúmenes. */
    fun valence(): Int = when {
        isPositive() -> 1
        isNegative() -> -1
        else -> 0
    }
}
