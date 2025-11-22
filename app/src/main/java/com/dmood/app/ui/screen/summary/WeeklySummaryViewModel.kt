package com.dmood.app.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.CalculateWeeklyScheduleUseCase
import com.dmood.app.domain.usecase.ExtractWeeklyHighlightsUseCase
import com.dmood.app.domain.usecase.GenerateInsightRulesUseCase
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    val insights: List<com.dmood.app.domain.usecase.InsightRuleResult> = emptyList(),
    val nextSummaryDate: LocalDate? = null,
    val isSummaryAvailable: Boolean = false,
    val isDeveloperMode: Boolean = false
)

class WeeklySummaryViewModel(
    private val decisionRepository: DecisionRepository,
    private val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase,
    private val extractWeeklyHighlightsUseCase: ExtractWeeklyHighlightsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateWeeklyScheduleUseCase: CalculateWeeklyScheduleUseCase,
    private val generateInsightRulesUseCase: GenerateInsightRulesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklySummaryUiState())
    val uiState: StateFlow<WeeklySummaryUiState> = _uiState

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private var firstUseDate: LocalDate = LocalDate.now()
    private var weekStartDay: DayOfWeek = DayOfWeek.MONDAY

    init {
        observeUserName()
        observeUserPreferences()
    }

    private fun observeUserName() {
        viewModelScope.launch {
            userPreferencesRepository.userNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(userName = name)
            }
        }
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.firstUseLocalDate(zoneId).collect { stored ->
                stored?.let { firstUseDate = it }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weekStartDayFlow.collect { stored ->
                weekStartDay = stored
            }
        }
        viewModelScope.launch {
            val stored = userPreferencesRepository.ensureFirstUseDate()
            firstUseDate = Instant.ofEpochMilli(stored).atZone(zoneId).toLocalDate()
        }
        viewModelScope.launch {
            userPreferencesRepository.developerModeFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isDeveloperMode = enabled)
            }
        }
    }

    fun loadWeeklySummary(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val today = LocalDate.now()
                val developerMode = _uiState.value.isDeveloperMode
                val latestFirstUse = userPreferencesRepository.firstUseLocalDate(zoneId).first()
                    ?: firstUseDate
                val latestWeekStart = userPreferencesRepository.weekStartDayFlow.first()
                firstUseDate = latestFirstUse
                weekStartDay = latestWeekStart

                val schedule = calculateWeeklyScheduleUseCase(
                    firstUseDate = latestFirstUse,
                    weekStartDay = latestWeekStart,
                    today = today
                )

                val start = schedule.windowStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = schedule.windowEnd.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

                val allowForce = force || developerMode

                if (!schedule.isSummaryAvailable && !allowForce) {
                    _uiState.value = WeeklySummaryUiState(
                        isLoading = false,
                        summary = null,
                        highlight = null,
                        errorMessage = "Tu próximo resumen estará listo el ${schedule.nextSummaryDate}",
                        nextSummaryDate = schedule.nextSummaryDate,
                        isSummaryAvailable = false,
                        isDeveloperMode = developerMode
                    )
                    return@launch
                }

                val decisions = decisionRepository.getByRange(start, end)

                val summary = buildWeeklySummaryUseCase(
                    decisions = decisions,
                    startDate = start,
                    endDate = end
                )

                val highlight = extractWeeklyHighlightsUseCase(summary)
                val insights = generateInsightRulesUseCase(decisions)

                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = summary,
                    highlight = highlight,
                    errorMessage = null,
                    insights = insights,
                    nextSummaryDate = schedule.nextSummaryDate,
                    isSummaryAvailable = schedule.isSummaryAvailable || allowForce,
                    isDeveloperMode = developerMode
                )
            } catch (e: Exception) {
                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = null,
                    highlight = null,
                    errorMessage = "No se pudo cargar el resumen semanal.",
                    nextSummaryDate = null,
                    isSummaryAvailable = false,
                    isDeveloperMode = _uiState.value.isDeveloperMode
                )
            }
        }
    }
}
