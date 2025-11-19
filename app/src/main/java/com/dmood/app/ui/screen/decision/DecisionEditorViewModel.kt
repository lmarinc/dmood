package com.dmood.app.ui.screen.decision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.CalculateDecisionToneUseCase
import com.dmood.app.domain.usecase.ValidateDecisionUseCase
import com.dmood.app.domain.usecase.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DecisionEditorUiState(
    val text: String = "",
    val selectedEmotions: Set<EmotionType> = emptySet(),
    val intensity: Int = 3,
    val category: CategoryType? = null,
    val isSaving: Boolean = false,
    val validationError: String? = null,
    val savedSuccessfully: Boolean = false
)

class DecisionEditorViewModel(
    private val decisionRepository: DecisionRepository,
    private val validateDecisionUseCase: ValidateDecisionUseCase,
    private val calculateDecisionToneUseCase: CalculateDecisionToneUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecisionEditorUiState())
    val uiState: StateFlow<DecisionEditorUiState> = _uiState

    fun onTextChange(newText: String) {
        _uiState.value = _uiState.value.copy(
            text = newText,
            validationError = null,
            savedSuccessfully = false
        )
    }

    fun onToggleEmotion(emotion: EmotionType) {
        val current = _uiState.value.selectedEmotions

        val newSet: Set<EmotionType> = if (current.contains(emotion)) {
            // Si ya estaba seleccionada, la quitamos
            current - emotion
        } else {
            if (emotion == EmotionType.NORMAL) {
                // Si el usuario elige NORMAL, va siempre sola
                setOf(EmotionType.NORMAL)
            } else {
                // Emoción distinta de NORMAL
                val withoutNormal = current - EmotionType.NORMAL

                if (withoutNormal.size >= 2) {
                    // Ya hay 2 emociones seleccionadas → ignoramos el nuevo click
                    withoutNormal
                } else {
                    // Aún hay hueco (0 o 1 emoción) → añadimos la nueva
                    withoutNormal + emotion
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedEmotions = newSet,
            validationError = null,
            savedSuccessfully = false
        )
    }

    fun onIntensityChange(newIntensity: Int) {
        _uiState.value = _uiState.value.copy(
            intensity = newIntensity.coerceIn(1, 5),
            validationError = null,
            savedSuccessfully = false
        )
    }

    fun onCategoryChange(newCategory: CategoryType) {
        _uiState.value = _uiState.value.copy(
            category = newCategory,
            validationError = null,
            savedSuccessfully = false
        )
    }

    fun saveDecision() {
        val state = _uiState.value

        val decision = Decision(
            id = 0L,
            timestamp = System.currentTimeMillis(),
            text = state.text,
            emotions = state.selectedEmotions.toList(),
            intensity = state.intensity,
            category = state.category ?: CategoryType.OTRO,
            tone = DecisionTone.NEUTRA
        )

        when (val validation = validateDecisionUseCase(decision)) {
            is ValidationResult.Error -> {
                _uiState.value = state.copy(
                    validationError = validation.message,
                    savedSuccessfully = false,
                    isSaving = false
                )
                return
            }

            ValidationResult.Valid -> Unit
        }

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isSaving = true, validationError = null)

                val tone = calculateDecisionToneUseCase(decision)
                val finalDecision = decision.copy(tone = tone)

                decisionRepository.add(finalDecision)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true,
                    validationError = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = false,
                    validationError = "No se pudo guardar la decisión."
                )
            }
        }
    }

    fun resetSavedFlag() {
        _uiState.value = _uiState.value.copy(savedSuccessfully = false)
    }
}
