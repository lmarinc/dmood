package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import com.dmood.app.domain.model.EmotionType

/**
 * Caso de uso responsable de calcular el tono de una decisión
 * a partir de sus emociones y nivel de intensidad.
 */
class CalculateDecisionToneUseCase {

    /**
     * Calcula el tono de una decisión según las reglas:
     *
     * - CALMADA:
     *   - Todas las emociones son positivas
     *   - Intensidad <= 3
     *
     * - IMPULSIVA:
     *   - Existe al menos una emoción negativa
     *   - Intensidad >= 4
     *
     * - NEUTRA:
     *   - Solo hay NORMAL, o mezcla positiva/negativa, o no encaja
     */
    operator fun invoke(decision: Decision): DecisionTone {
        val emotions = decision.emotions
        val intensity = decision.intensity

        val onlyNormal = emotions.isNotEmpty() && emotions.all { it == EmotionType.NORMAL }
        val allPositive = emotions.isNotEmpty() && emotions.all { it.isPositive() }
        val hasNegative = emotions.any { it.isNegative() }

        return when {
            allPositive && intensity <= 3 -> DecisionTone.CALMADA
            hasNegative && intensity >= 4 -> DecisionTone.IMPULSIVA
            onlyNormal -> DecisionTone.NEUTRA
            else -> DecisionTone.NEUTRA
        }
    }
}
