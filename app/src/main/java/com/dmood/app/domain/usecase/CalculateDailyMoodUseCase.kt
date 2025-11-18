package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.Decision

/**
 * Estado emocional global de un día, a nivel cualitativo.
 */
enum class DailyMood {
    POSITIVO,
    NEGATIVO,
    NEUTRO,
    NORMAL
}

/**
 * Caso de uso para calcular el "clima emocional" de un día
 * a partir de la lista de decisiones registradas.
 */
class CalculateDailyMoodUseCase {

    /**
     * Devuelve:
     * - NORMAL, si no hay decisiones.
     * - POSITIVO, si hay más decisiones con valencia positiva que negativa.
     * - NEGATIVO, si hay más decisiones con valencia negativa que positiva.
     * - NEUTRO, si están equilibradas.
     */
    operator fun invoke(decisions: List<Decision>): DailyMood {
        if (decisions.isEmpty()) {
            return DailyMood.NORMAL
        }

        var positiveDecisions = 0
        var negativeDecisions = 0

        decisions.forEach { decision ->
            val valence = decision.emotions.sumOf { it.valence() }
            when {
                valence > 0 -> positiveDecisions++
                valence < 0 -> negativeDecisions++
                // valence == 0 -> no suma a ninguna
            }
        }

        return when {
            positiveDecisions > negativeDecisions -> DailyMood.POSITIVO
            negativeDecisions > positiveDecisions -> DailyMood.NEGATIVO
            else -> DailyMood.NEUTRO
        }
    }
}
