package com.dmood.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class SettingsUiState(
    val currentName: String = "",
    val editedName: String = "",
    val isSaving: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val dailyReminderEnabled: Boolean = true,
    val weeklyReminderEnabled: Boolean = false
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadCurrentName()
        observeWeekStart()
        observeReminders()
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

    private fun observeWeekStart() {
        viewModelScope.launch {
            userPreferencesRepository.weekStartDayFlow.collect { stored ->
                _uiState.value = _uiState.value.copy(weekStartDay = stored)
            }
        }
    }

    private fun observeReminders() {
        viewModelScope.launch {
            userPreferencesRepository.dailyReminderEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(dailyReminderEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weeklyReminderEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(weeklyReminderEnabled = enabled) }
            }
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

    fun onWeekStartChange(dayOfWeek: DayOfWeek) {
        _uiState.value = _uiState.value.copy(weekStartDay = dayOfWeek, feedbackMessage = null)
        viewModelScope.launch {
            userPreferencesRepository.setWeekStartDay(dayOfWeek)
        }
    }

    /**
     * Activa / desactiva el recordatorio diario.
     * Cuando se activa, programa la alarma con AlarmManager.
     */
    fun setDailyReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(dailyReminderEnabled = enabled) }
        viewModelScope.launch {
            userPreferencesRepository.setDailyReminderEnabled(enabled)
            if (enabled) {
                // PRODUCCIÓN: 22:00
                reminderScheduler.scheduleDailyReminder(
                    ReminderScheduler.DAILY_REMINDER_HOUR_DEFAULT,
                    ReminderScheduler.DAILY_REMINDER_MINUTE_DEFAULT
                )

                // PARA PRUEBA RÁPIDA (por ejemplo 13:30), comenta lo de arriba y descomenta esto:
                // reminderScheduler.scheduleDailyReminder(
                //     ReminderScheduler.DAILY_REMINDER_HOUR_TEST,
                //     ReminderScheduler.DAILY_REMINDER_MINUTE_TEST
                // )
            } else {
                reminderScheduler.cancelDailyReminder()
            }
        }
    }

    /**
     * Activa / desactiva el recordatorio de resumen semanal.
     * Seguimos usando WorkManager para esta parte (no necesita hora exacta).
     */
    fun setWeeklyReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(weeklyReminderEnabled = enabled) }
        viewModelScope.launch {
            userPreferencesRepository.setWeeklyReminderEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleWeeklySummaryReminder()
            } else {
                reminderScheduler.cancelWeeklySummaryReminder()
            }
        }
    }
}
