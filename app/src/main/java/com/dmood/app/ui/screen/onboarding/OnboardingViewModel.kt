package com.dmood.app.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.data.preferences.WeekStartChangeResult
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false,
    val weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val selectedWeekStartDay: DayOfWeek? = null
)

class OnboardingViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    init {
        observeWeekStartDay()
    }

    fun onNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(
            name = newName,
            errorMessage = null
        )
    }

    fun onWeekStartSelected(dayOfWeek: DayOfWeek) {
        _uiState.update { current ->
            current.copy(
                selectedWeekStartDay = dayOfWeek,
                errorMessage = null
            )
        }
    }

    fun completeOnboarding() {
        val trimmedName = _uiState.value.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Necesitamos tu nombre para personalizar D-Mood"
            )
            return
        }

        val selectedWeekStart = _uiState.value.selectedWeekStartDay
        if (selectedWeekStart == null) {
            _uiState.update {
                it.copy(errorMessage = "Elige el día en el que arranca tu semana")
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
                userPreferencesRepository.setUserName(trimmedName)
                when (
                    userPreferencesRepository.updateWeekStartDay(
                        selectedWeekStart,
                        enforceMonthlyLimit = false
                    )
                ) {
                    is WeekStartChangeResult.Updated -> Unit
                    WeekStartChangeResult.Unchanged -> Unit
                    is WeekStartChangeResult.TooSoon -> Unit
                }
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

    private fun observeWeekStartDay() {
        viewModelScope.launch {
            userPreferencesRepository.weekStartDayFlow.collect { day ->
                _uiState.update { state ->
                    state.copy(
                        weekStartDay = day,
                        selectedWeekStartDay = state.selectedWeekStartDay ?: day
                    )
                }
            }
        }
    }
}
