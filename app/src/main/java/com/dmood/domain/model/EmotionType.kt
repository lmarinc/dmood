package com.dmood.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Representa los diferentes tipos de emociones soportados por D-Mood.
 */
enum class EmotionType(
    val displayName: String,
    val color: Color,
) {
    /** Emoción asociada con la alegría y el entusiasmo. */
    ALEGRE(
        displayName = "Alegre",
        color = Color(0xFFFFE59D),
    ),

    /** Sensación de confianza y tranquilidad. */
    SEGURO(
        displayName = "Seguro",
        color = Color(0xFFD1F2EB),
    ),

    /** Estado relacionado con el miedo o la preocupación. */
    MIEDO(
        displayName = "Miedo",
        color = Color(0xFFFFD1CF),
    ),

    /** Emoción vinculada a la sorpresa positiva o curiosidad. */
    SORPRENDIDO(
        displayName = "Sorprendido",
        color = Color(0xFFDDEBFF),
    ),

    /** Sentimiento de tristeza o melancolía. */
    TRISTE(
        displayName = "Triste",
        color = Color(0xFFCBD5F0),
    ),

    /** Incomodidad o tensión moderada. */
    INCOMODO(
        displayName = "Incómodo",
        color = Color(0xFFE8D4FF),
    ),

    /** Emoción asociada al enfado o la frustración. */
    ENFADADO(
        displayName = "Enfadado",
        color = Color(0xFFFFCFCB),
    ),

    /** Motivación, energía y enfoque. */
    MOTIVADO(
        displayName = "Motivado",
        color = Color(0xFFC8F7C5),
    ),

    /** Estado neutro o equilibrado. */
    NORMAL(
        displayName = "Normal",
        color = Color(0xFFE6E8EB),
    );

    fun isPositive(): Boolean = when (this) {
        ALEGRE, SEGURO, SORPRENDIDO, MOTIVADO -> true
        NORMAL -> false
        else -> false
    }

    fun isNegative(): Boolean = when (this) {
        MIEDO, TRISTE, INCOMODO, ENFADADO -> true
        NORMAL -> false
        else -> false
    }
}
