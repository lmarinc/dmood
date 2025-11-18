package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.EmotionType

/**
 * Resultado de la validación de una decisión antes de guardarla.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

/**
 * Caso de uso para validar que una decisión cumple las reglas mínimas
 * antes de ser persistida.
 */
class ValidateDecisionUseCase {

    /**
     * Reglas:
     * - El texto no puede estar vacío.
     * - Debe haber al menos una emoción.
     * - NORMAL no puede combinarse con otras emociones.
     * - La intensidad debe estar entre 1 y 5 (ambos incluidos).
     */
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

        // La categoría en Decision no es nula por diseño, por lo que no es necesario validar null aquí.

        return ValidationResult.Valid
    }
}
