package com.dmood.app.domain.model

/**
 * Resumen de estado emocional diario en D-Mood basado en las decisiones registradas.
 */
data class DailyMood(
    /**
     * Fecha del resumen en formato ISO `yyyy-MM-dd`.
     */
    val date: String,
    /**
     * Emoción predominante del día; puede ser `null` si no hay decisiones registradas.
     */
    val dominantEmotion: EmotionType?,
    /**
     * Número total de decisiones registradas en la fecha.
     */
    val decisionCount: Int,
    /**
     * Porcentaje (0..100) de decisiones clasificadas como calmadas.
     */
    val calmPercentage: Int,
    /**
     * Porcentaje (0..100) de decisiones clasificadas como impulsivas.
     */
    val impulsivePercentage: Int,
)
