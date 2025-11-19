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
    val id: Long = 0L,
    val text: String = "",
    val selectedEmotions: Set<EmotionType> = emptySet(),
    val intensity: Int = 3,
    val category: CategoryType? = null,
    val timestamp: Long? = null,
    val isSaving: Boolean = false,
    val validationError: String? = null,
    val savedSuccessfully: Boolean = false,
    val isEditing: Boolean = false,
    val currentStep: Int = 1
)

class DecisionEditorViewModel(
    private val decisionRepository: DecisionRepository,
    private val validateDecisionUseCase: ValidateDecisionUseCase,
    private val calculateDecisionToneUseCase: CalculateDecisionToneUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecisionEditorUiState())
    val uiState: StateFlow<DecisionEditorUiState> = _uiState

    fun loadDecision(id: Long) {
        if (id <= 0L) return
        viewModelScope.launch {
            val decision = decisionRepository.getById(id) ?: return@launch
            _uiState.value = _uiState.value.copy(
                id = decision.id,
                text = decision.text,
                selectedEmotions = decision.emotions.toSet(),
                intensity = decision.intensity,
                category = decision.category,
                timestamp = decision.timestamp,
                isEditing = true,
                validationError = null,
                savedSuccessfully = false,
                currentStep = 1
            )
        }
    }

    fun onTextChange(newText: String) {
        _uiState.value = _uiState.value.copy(
            text = newText,
            validationError = null,
            savedSuccessfully = false
        )
    }

    fun onToggleEmotion(emotion: EmotionType) {
        val current = _uiState.value.selectedEmotions
        val newSet = if (current.contains(emotion)) {
            current - emotion
        } else {
            if (emotion == EmotionType.NORMAL) {
                setOf(EmotionType.NORMAL)
            } else {
                val withoutNormal = current - EmotionType.NORMAL
                if (withoutNormal.size >= 2) {
                    withoutNormal
                } else {
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

    fun goToNextStep() {
        val state = _uiState.value
        if (state.currentStep == 1 && state.text.isBlank()) {
            _uiState.value = state.copy(
                validationError = "Necesitamos una descripción para continuar"
            )
            return
        }
        if (state.currentStep < 3) {
            _uiState.value = state.copy(
                currentStep = state.currentStep + 1,
                validationError = null
            )
        }
    }

    fun goToPreviousStep() {
        val state = _uiState.value
        if (state.currentStep > 1) {
            _uiState.value = state.copy(
                currentStep = state.currentStep - 1,
                validationError = null
            )
        }
    }

    fun saveDecision() {
        val state = _uiState.value
        val timestamp = state.timestamp ?: System.currentTimeMillis()
        val decision = Decision(
            id = state.id,
            timestamp = timestamp,
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

                if (state.isEditing && state.id > 0L) {
                    decisionRepository.update(finalDecision)
                } else {
                    decisionRepository.add(finalDecision)
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true,
                    validationError = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = false,
                    validationError = "No se pudo guardar la decisión"
                )
            }
        }
    }

    fun deleteCurrentDecision() {
        val state = _uiState.value
        if (!state.isEditing || state.id <= 0L) return

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isSaving = true, validationError = null)
                val existing = decisionRepository.getById(state.id)
                    ?: run {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            validationError = "No encontramos esta decisión"
                        )
                        return@launch
                    }
                decisionRepository.delete(existing)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    validationError = "No se pudo borrar la decisión"
                )
            }
        }
    }

    fun resetSavedFlag() {
        _uiState.value = _uiState.value.copy(savedSuccessfully = false)
    }
}
