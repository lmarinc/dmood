package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.EmotionType

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

class ValidateDecisionUseCase {
    operator fun invoke(decision: Decision): ValidationResult {
        if (decision.text.isBlank()) {
            return ValidationResult.Error("El texto no puede estar vacío.")
        }

        if (decision.emotions.isEmpty()) {
            return ValidationResult.Error("Debes seleccionar al menos una emoción.")
        }

        val containsNormal = decision.emotions.contains(EmotionType.NORMAL)
        if (containsNormal && decision.emotions.size > 1) {
            return ValidationResult.Error("La emoción NORMAL no puede combinarse con otras emociones.")
        }

        if (decision.intensity !in 1..5) {
            return ValidationResult.Error("La intensidad debe estar entre 1 y 5.")
        }

        val category: CategoryType? = decision.category
        if (category == null) {
            return ValidationResult.Error("Debes seleccionar una categoría válida.")
        }

        return ValidationResult.Valid
    }
}
