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
    val isDemoPreview: Boolean = false
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
    private var hasLoadedOnce: Boolean = false

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
                stored?.let {
                    firstUseDate = it
                    refreshIfLoaded()
                }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weekStartDayFlow.collect { stored ->
                weekStartDay = stored
                refreshIfLoaded()
            }
        }
        viewModelScope.launch {
            val stored = userPreferencesRepository.ensureFirstUseDate()
            firstUseDate = Instant.ofEpochMilli(stored).atZone(zoneId).toLocalDate()
            refreshIfLoaded()
        }
    }

    fun loadWeeklySummary() {
        viewModelScope.launch {
            hasLoadedOnce = true
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isDemoPreview = false
            )

            try {
                val today = LocalDate.now()
                val schedule = calculateWeeklyScheduleUseCase(
                    firstUseDate = firstUseDate,
                    weekStartDay = weekStartDay,
                    today = today
                )

                val start = schedule.windowStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = schedule.windowEnd.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

                if (!schedule.isSummaryAvailable) {
                    _uiState.value = WeeklySummaryUiState(
                        isLoading = false,
                        summary = null,
                        highlight = null,
                        errorMessage = "Tu próximo resumen estará listo el ${schedule.nextSummaryDate}",
                        nextSummaryDate = schedule.nextSummaryDate,
                        isSummaryAvailable = false
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
                    isSummaryAvailable = schedule.isSummaryAvailable
                )
            } catch (e: Exception) {
                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = null,
                    highlight = null,
                    errorMessage = "No se pudo cargar el resumen semanal.",
                    nextSummaryDate = null,
                    isSummaryAvailable = false
                )
            }
        }
    }

    fun startDemoPreview() {
        val today = LocalDate.now()
        val startDate = today.minusDays(7)
        val endDate = today.minusDays(1)
        val summary = WeeklySummary(
            startDate = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endDate = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            totalDecisions = 8,
            calmPercentage = 55f,
            impulsivePercentage = 30f,
            neutralPercentage = 15f,
            dailyMoods = mapOf(
                "Lunes" to com.dmood.app.domain.usecase.DailyMood.POSITIVO,
                "Martes" to com.dmood.app.domain.usecase.DailyMood.POSITIVO,
                "Miércoles" to com.dmood.app.domain.usecase.DailyMood.NEUTRO,
                "Jueves" to com.dmood.app.domain.usecase.DailyMood.NEGATIVO,
                "Viernes" to com.dmood.app.domain.usecase.DailyMood.POSITIVO
            ),
            categoryDistribution = mapOf(
                com.dmood.app.domain.model.CategoryType.RELACIONES to 3,
                com.dmood.app.domain.model.CategoryType.SALUD to 2,
                com.dmood.app.domain.model.CategoryType.PROYECTOS to 3
            )
        )
        val highlight = extractWeeklyHighlightsUseCase(summary)
        val insights = generateInsightRulesUseCase(emptyList())

        _uiState.value = WeeklySummaryUiState(
            isLoading = false,
            summary = summary,
            highlight = highlight,
            errorMessage = null,
            userName = _uiState.value.userName,
            insights = if (insights.isEmpty()) listOf(
                com.dmood.app.domain.usecase.InsightRuleResult(
                    title = "Ejemplo de insight",
                    description = "Así verás tus pistas semanales cuando registres decisiones a diario.",
                    tag = "Demo"
                )
            ) else insights,
            nextSummaryDate = _uiState.value.nextSummaryDate,
            isSummaryAvailable = true,
            isDemoPreview = true
        )
    }

    private fun refreshIfLoaded() {
        if (hasLoadedOnce) {
            loadWeeklySummary()
        }
    }
}
