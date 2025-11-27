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
    val isDemo: Boolean = false
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
    private var hasPreferencesLoaded: Boolean = false

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
                    triggerReloadIfReady()
                }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weekStartDayFlow.collect { stored ->
                weekStartDay = stored
                triggerReloadIfReady()
            }
        }
        viewModelScope.launch {
            val stored = userPreferencesRepository.ensureFirstUseDate()
            firstUseDate = Instant.ofEpochMilli(stored).atZone(zoneId).toLocalDate()
            hasPreferencesLoaded = true
            triggerReloadIfReady()
        }
    }

    private fun triggerReloadIfReady() {
        if (hasPreferencesLoaded && !_uiState.value.isLoading) {
            loadWeeklySummary()
        }
    }

    fun loadWeeklySummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isDemo = false
            )

            try {
                val today = LocalDate.now()
                val schedule = calculateWeeklyScheduleUseCase(
                    firstUseDate = firstUseDate,
                    weekStartDay = weekStartDay,
                    today = today
                )

                val start = schedule.windowStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endExclusive = schedule.windowEnd.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endInclusive = schedule.windowEnd.atStartOfDay(zoneId).toInstant().toEpochMilli()

                if (!schedule.isSummaryAvailable) {
                    _uiState.value = WeeklySummaryUiState(
                        isLoading = false,
                        summary = null,
                        highlight = null,
                        errorMessage = "Tu próximo resumen estará listo el ${schedule.nextSummaryDate}",
                        nextSummaryDate = schedule.nextSummaryDate,
                        isSummaryAvailable = false,
                        isDemo = false
                    )
                    return@launch
                }

                val decisions = decisionRepository.getByRange(start, endExclusive)

                val summary = buildWeeklySummaryUseCase(
                    decisions = decisions,
                    startDate = start,
                    endDate = endInclusive
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
                    isSummaryAvailable = schedule.isSummaryAvailable,
                    isDemo = false
                )
            } catch (e: Exception) {
                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = null,
                    highlight = null,
                    errorMessage = "No se pudo cargar el resumen semanal.",
                    nextSummaryDate = null,
                    isSummaryAvailable = false,
                    isDemo = false
                )
            }
        }
    }

    fun loadDemoSummary() {
        val now = LocalDate.now()
        val startDateMillis = now.minusDays(7).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endDateMillis = now.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val demoSummary = WeeklySummary(
            startDate = startDateMillis,
            endDate = endDateMillis,
            totalDecisions = 8,
            calmPercentage = 62f,
            impulsivePercentage = 25f,
            neutralPercentage = 13f,
            dailyMoods = mapOf(
                "Lunes" to com.dmood.app.domain.usecase.DailyMood.POSITIVO,
                "Miércoles" to com.dmood.app.domain.usecase.DailyMood.NEUTRO,
                "Viernes" to com.dmood.app.domain.usecase.DailyMood.NEGATIVO
            ),
            categoryDistribution = mapOf(
                com.dmood.app.domain.model.CategoryType.RELACIONES_SOCIAL to 4,
                com.dmood.app.domain.model.CategoryType.TRABAJO_ESTUDIOS to 3,
                com.dmood.app.domain.model.CategoryType.RELACIONES_SOCIAL to 1
            ),
            emotionDistribution = emptyMap(),
            categoryEmotionMatrix = emptyMap()
        )

        val demoHighlight = WeeklyHighlight(
            strongestPositiveDay = "Lunes",
            strongestNegativeDay = "Viernes",
            mostFrequentCategory = com.dmood.app.domain.model.CategoryType.OCIO_TIEMPO_LIBRE,
            emotionalTrend = "Ejemplo de semana equilibrada"
        )

        _uiState.value = WeeklySummaryUiState(
            isLoading = false,
            summary = demoSummary,
            highlight = demoHighlight,
            errorMessage = null,
            userName = _uiState.value.userName,
            insights = emptyList(),
            nextSummaryDate = _uiState.value.nextSummaryDate,
            isSummaryAvailable = true,
            isDemo = true
        )
    }
}
