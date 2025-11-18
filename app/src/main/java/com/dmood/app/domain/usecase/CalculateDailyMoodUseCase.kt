package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.Decision

enum class DailyMood { POSITIVO, NEGATIVO, NEUTRO, NORMAL }

class CalculateDailyMoodUseCase {
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
            }
        }

        return when {
            positiveDecisions > negativeDecisions -> DailyMood.POSITIVO
            negativeDecisions > positiveDecisions -> DailyMood.NEGATIVO
            else -> DailyMood.NEUTRO
        }
    }
}
