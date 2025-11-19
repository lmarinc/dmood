package com.dmood.app.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false
)

class OnboardingViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun onNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(
            name = newName,
            errorMessage = null
        )
    }

    fun completeOnboarding() {
        val trimmedName = _uiState.value.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Necesitamos tu nombre para personalizar D-Mood"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
                userPreferencesRepository.setUserName(trimmedName)
                userPreferencesRepository.setHasSeenOnboarding(true)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    completed = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "No pudimos guardar tu información. Intenta de nuevo."
                )
            }
        }
    }
}
