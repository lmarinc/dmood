package com.dmood.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.data.preferences.WeekStartChangeResult
import com.dmood.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class SettingsUiState(
    val currentName: String = "",
    val editedName: String = "",
    val isSaving: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val dailyReminderEnabled: Boolean = false,
    val weeklyReminderEnabled: Boolean = false
)

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data class ConfirmWeekStartChange(val dayOfWeek: DayOfWeek) : SettingsEvent()
}

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events

    private var lastWeekStartChangeDate: LocalDate? = null
    private val zoneId = ZoneId.systemDefault()
    private val locale = Locale("es", "ES")
    private val dateFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", locale)

    init {
        loadCurrentName()
        observeWeekStart()
        observeReminders()
        observeLastWeekStartChange()
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

    private fun observeLastWeekStartChange() {
        viewModelScope.launch {
            userPreferencesRepository.weekStartLastChangeFlow.collect { millis ->
                lastWeekStartChangeDate = millis?.let { stored ->
                    java.time.Instant.ofEpochMilli(stored).atZone(zoneId).toLocalDate()
                }
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

    fun onWeekStartClick(dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            if (dayOfWeek == _uiState.value.weekStartDay) return@launch

            val now = LocalDate.now(zoneId)
            val lastChange = lastWeekStartChangeDate
            if (lastChange != null) {
                val daysSinceChange = java.time.temporal.ChronoUnit.DAYS.between(lastChange, now)
                if (daysSinceChange < 30) {
                    val nextAllowed = lastChange.plusDays(30)
                    _events.emit(
                        SettingsEvent.ShowToast(
                            "Solo puedes cambiarlo una vez al mes. Próximo cambio desde ${nextAllowed.format(dateFormatter)}"
                        )
                    )
                    return@launch
                }
            }

            _events.emit(SettingsEvent.ConfirmWeekStartChange(dayOfWeek))
        }
    }

    fun confirmWeekStartChange(dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            when (
                val result = userPreferencesRepository.updateWeekStartDay(
                    dayOfWeek = dayOfWeek,
                    now = LocalDate.now(zoneId),
                    enforceMonthlyLimit = true,
                    zoneId = zoneId
                )
            ) {
                is WeekStartChangeResult.Updated -> {
                    lastWeekStartChangeDate = LocalDate.now(zoneId)
                    _uiState.update { it.copy(weekStartDay = dayOfWeek, feedbackMessage = null) }
                    _events.emit(
                        SettingsEvent.ShowToast(
                            "Cambiaremos tus resúmenes para empezar la semana en ${formatDayName(dayOfWeek)}"
                        )
                    )
                }

                is WeekStartChangeResult.TooSoon -> {
                    _events.emit(
                        SettingsEvent.ShowToast(
                            "Solo puedes cambiarlo una vez al mes. Próximo cambio desde ${result.nextAllowedDate.format(dateFormatter)}"
                        )
                    )
                }

                WeekStartChangeResult.Unchanged -> Unit
            }
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
                // PRODUCCIÓN: 21:00
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

    private fun formatDayName(dayOfWeek: DayOfWeek): String {
        val raw = dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        return raw.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }
}
