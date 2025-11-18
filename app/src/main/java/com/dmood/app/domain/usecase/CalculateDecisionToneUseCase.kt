package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import com.dmood.app.domain.model.EmotionType

class CalculateDecisionToneUseCase {
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
