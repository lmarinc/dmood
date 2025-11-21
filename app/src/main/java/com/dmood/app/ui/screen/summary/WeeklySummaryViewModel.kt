package com.dmood.app.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.CalculateWeeklySummaryScheduleUseCase
import com.dmood.app.domain.usecase.DecisionInsight
import com.dmood.app.domain.usecase.ExtractWeeklyHighlightsUseCase
import com.dmood.app.domain.usecase.GenerateDecisionInsightsUseCase
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class WeeklySummaryUiState(
    val isLoading: Boolean = false,
    val summary: WeeklySummary? = null,
    val highlight: WeeklyHighlight? = null,
    val errorMessage: String? = null,
    val userName: String? = null,
    val nextReleaseDate: LocalDate? = null,
    val availableToday: Boolean = false,
    val insights: List<DecisionInsight> = emptyList()
)

class WeeklySummaryViewModel(
    private val decisionRepository: DecisionRepository,
    private val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase,
    private val extractWeeklyHighlightsUseCase: ExtractWeeklyHighlightsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateWeeklySummaryScheduleUseCase: CalculateWeeklySummaryScheduleUseCase,
    private val generateDecisionInsightsUseCase: GenerateDecisionInsightsUseCase
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()

    private val _uiState = MutableStateFlow(WeeklySummaryUiState())
    val uiState: StateFlow<WeeklySummaryUiState> = _uiState

    init {
        observeUserName()
    }

    private fun observeUserName() {
        viewModelScope.launch {
            userPreferencesRepository.userNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(userName = name)
            }
        }
    }

    fun loadWeeklySummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val today = LocalDate.now()
                val weekStartDay = userPreferencesRepository.weekStartDayFlow.first()
                val firstUseDate = userPreferencesRepository.ensureFirstUseDate(today)

                val schedule = calculateWeeklySummaryScheduleUseCase(
                    today = today,
                    startDay = weekStartDay,
                    firstUseDate = firstUseDate
                )

                val windowStart = schedule.windowStart
                val windowEnd = schedule.windowEnd

                if (windowStart == null || windowEnd == null || schedule.latestAvailableDate == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        summary = null,
                        highlight = null,
                        availableToday = false,
                        nextReleaseDate = schedule.nextReleaseDate,
                        insights = emptyList(),
                        errorMessage = "Aún necesitamos más días para crear tu primer resumen."
                    )
                    return@launch
                }

                val startMillis = windowStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endMillis = windowEnd.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

                val decisions = decisionRepository.getByRange(startMillis, endMillis)

                val summary = buildWeeklySummaryUseCase(
                    decisions = decisions,
                    startDate = startMillis,
                    endDate = endMillis
                )

                val highlight = extractWeeklyHighlightsUseCase(summary)
                val insights = generateDecisionInsightsUseCase(decisions)

                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = summary,
                    highlight = highlight,
                    errorMessage = null,
                    userName = _uiState.value.userName,
                    nextReleaseDate = schedule.nextReleaseDate,
                    availableToday = schedule.availableToday,
                    insights = insights
                )
            } catch (e: Exception) {
                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = null,
                    highlight = null,
                    errorMessage = "No se pudo cargar el resumen semanal.",
                    userName = _uiState.value.userName
                )
            }
        }
    }
}
