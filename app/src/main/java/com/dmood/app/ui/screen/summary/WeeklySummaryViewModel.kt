package com.dmood.app.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.CalculateSummaryScheduleUseCase
import com.dmood.app.domain.usecase.ExtractWeeklyHighlightsUseCase
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WeeklySummaryUiState(
    val isLoading: Boolean = false,
    val summary: WeeklySummary? = null,
    val highlight: WeeklyHighlight? = null,
    val errorMessage: String? = null,
    val userName: String? = null,
    val startOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val nextSummaryDate: LocalDate? = null,
    val summaryAvailableToday: Boolean = false
)

class WeeklySummaryViewModel(
    private val decisionRepository: DecisionRepository,
    private val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase,
    private val extractWeeklyHighlightsUseCase: ExtractWeeklyHighlightsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateSummaryScheduleUseCase: CalculateSummaryScheduleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklySummaryUiState())
    val uiState: StateFlow<WeeklySummaryUiState> = _uiState

    init {
        observeUserName()
        observeStartOfWeek()
        observeFirstUsage()
    }

    private fun observeUserName() {
        viewModelScope.launch {
            userPreferencesRepository.userNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(userName = name)
            }
        }
    }

    private fun observeStartOfWeek() {
        viewModelScope.launch {
            userPreferencesRepository.startOfWeekFlow.collect { day ->
                refreshSchedule(startDay = day)
            }
        }
    }

    private fun observeFirstUsage() {
        viewModelScope.launch {
            userPreferencesRepository.firstUsageDateFlow.collect { firstUsage ->
                refreshSchedule(firstUsageDate = firstUsage)
            }
        }
    }

    private suspend fun refreshSchedule(
        startDay: DayOfWeek? = null,
        firstUsageDate: LocalDate? = null
    ) {
        val usage = firstUsageDate ?: userPreferencesRepository.firstUsageDateFlow.first()
        val day = startDay ?: _uiState.value.startOfWeek
        val schedule = calculateSummaryScheduleUseCase(
            startOfWeek = day,
            firstUsageDate = usage,
            today = LocalDate.now()
        )
        _uiState.value = _uiState.value.copy(
            startOfWeek = day,
            nextSummaryDate = schedule.nextSummaryDate,
            summaryAvailableToday = schedule.isAvailableToday
        )
    }

    fun loadWeeklySummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val firstUsage = userPreferencesRepository.firstUsageDateFlow.first()
                val schedule = calculateSummaryScheduleUseCase(
                    startOfWeek = _uiState.value.startOfWeek,
                    firstUsageDate = firstUsage,
                    today = LocalDate.now()
                )

                if (!schedule.isAvailableToday) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        summary = null,
                        highlight = null,
                        nextSummaryDate = schedule.nextSummaryDate,
                        summaryAvailableToday = false
                    )
                    return@launch
                }

                val now = System.currentTimeMillis()
                val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
                val start = now - sevenDaysMillis
                val end = now

                // 1) Decisiones de los últimos 7 días
                val decisions = decisionRepository.getByRange(start, end)

                // 2) Construir resumen
                val summary = buildWeeklySummaryUseCase(
                    decisions = decisions,
                    startDate = start,
                    endDate = end
                )

                // 3) Extraer hallazgos
                val highlight = extractWeeklyHighlightsUseCase(summary)

                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = summary,
                    highlight = highlight,
                    errorMessage = null,
                    userName = _uiState.value.userName,
                    startOfWeek = _uiState.value.startOfWeek,
                    nextSummaryDate = schedule.nextSummaryDate,
                    summaryAvailableToday = true
                )
            } catch (e: Exception) {
                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = null,
                    highlight = null,
                    errorMessage = "No se pudo cargar el resumen semanal.",
                    userName = _uiState.value.userName,
                    startOfWeek = _uiState.value.startOfWeek,
                    nextSummaryDate = _uiState.value.nextSummaryDate,
                    summaryAvailableToday = false
                )
            }
        }
    }
}
