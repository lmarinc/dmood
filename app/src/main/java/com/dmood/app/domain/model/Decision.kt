package com.dmood.app.domain.model

/**
 * Representa una decisión registrada en D-Mood, incluyendo su contexto y estado emocional asociado.
 */
data class Decision(
    /** Identificador único de la decisión. */
    val id: Long,
    /** Momento en el que se tomó la decisión, en milisegundos desde epoch. */
    val timestamp: Long,
    /** Descripción breve del contenido o motivo de la decisión. */
    val text: String,
    /** Emociones predominantes asociadas a la decisión. */
    val emotions: List<EmotionType>,
    /** Nivel de intensidad percibido por la persona usuaria, en un rango esperado de 1..5. */
    val intensity: Int,
    /** Categoría temática a la que pertenece la decisión. */
    val category: CategoryType,
    /** Tono interno clasificado externamente mediante el CalculateDecisionToneUseCase. */
    val tone: DecisionTone,
)
