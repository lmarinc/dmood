package com.dmood.app.domain.model

/**
 * Representa el "tono" interno de una decisión, usado para clasificar decisiones
 * según su intensidad emocional y las emociones predominantes.
 */
enum class DecisionTone {
    /** Decisiones tomadas con baja intensidad emocional y emociones predominantemente positivas. */
    CALMADA,

    /** Decisiones tomadas con alta intensidad emocional y presencia de emociones negativas. */
    IMPULSIVA,

    /** Resto de decisiones, incluida la emoción NORMAL o mezclas que no encajan claramente en calmada o impulsiva. */
    NEUTRA,
}
