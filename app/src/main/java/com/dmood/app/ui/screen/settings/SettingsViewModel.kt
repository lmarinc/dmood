package com.dmood.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val currentName: String = "",
    val editedName: String = "",
    val isSaving: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadCurrentName()
    }

    private fun loadCurrentName() {
        viewModelScope.launch {
            val name = userPreferencesRepository.getUserName().orEmpty()
            _uiState.value = _uiState.value.copy(
                currentName = name,
                editedName = name
            )
        }
    }

    fun onNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(
            editedName = newName,
            errorMessage = null,
            feedbackMessage = null
        )
    }

    fun saveName() {
        val trimmed = _uiState.value.editedName.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El nombre no puede quedar vacío"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
                userPreferencesRepository.setUserName(trimmed)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    currentName = trimmed,
                    editedName = trimmed,
                    feedbackMessage = "Nombre actualizado"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "No pudimos guardar los cambios"
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }
}
