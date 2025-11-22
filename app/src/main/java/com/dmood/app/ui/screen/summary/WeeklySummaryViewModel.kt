package com.dmood.app.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.CalculateWeeklyScheduleUseCase
import com.dmood.app.domain.usecase.DailyMood
import com.dmood.app.domain.usecase.ExtractWeeklyHighlightsUseCase
import com.dmood.app.domain.usecase.GenerateInsightRulesUseCase
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
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
    val isDemoMode: Boolean = false
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
    }

    fun loadWeeklySummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isDemoMode = false
            )

            try {
                refreshPreferencesSnapshot()

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
                        isSummaryAvailable = false,
                        isDemoMode = false
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
                    isSummaryAvailable = schedule.isSummaryAvailable,
                    isDemoMode = false
                )
            } catch (e: Exception) {
                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = null,
                    highlight = null,
                    errorMessage = "No se pudo cargar el resumen semanal.",
                    nextSummaryDate = null,
                    isSummaryAvailable = false,
                    isDemoMode = false
                )
            }
        }
    }

    fun showDemoSummary() {
        viewModelScope.launch {
            val demo = buildDemoSummary()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                summary = demo.summary,
                highlight = demo.highlight,
                insights = demo.insights,
                errorMessage = null,
                isSummaryAvailable = false,
                isDemoMode = true
            )
        }
    }

    private suspend fun refreshPreferencesSnapshot() {
        val storedFirstUse = userPreferencesRepository.firstUseLocalDate(zoneId).first()
        storedFirstUse?.let { firstUseDate = it }
        weekStartDay = userPreferencesRepository.weekStartDayFlow.first()
        if (storedFirstUse == null) {
            val ensured = userPreferencesRepository.ensureFirstUseDate()
            firstUseDate = Instant.ofEpochMilli(ensured).atZone(zoneId).toLocalDate()
        }
    }

    private fun buildDemoSummary(): DemoSummaryData {
        val anchor = LocalDate.now().with(TemporalAdjusters.previousOrSame(weekStartDay))
        val startDate = anchor.minusDays(7)
        val endDate = anchor.minusDays(1)
        val summary = WeeklySummary(
            startDate = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endDate = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            totalDecisions = 8,
            calmPercentage = 62f,
            impulsivePercentage = 25f,
            neutralPercentage = 13f,
            dailyMoods = mapOf(
                "Lunes" to DailyMood.POSITIVO,
                "Martes" to DailyMood.POSITIVO,
                "Miércoles" to DailyMood.NEUTRO,
                "Jueves" to DailyMood.NEGATIVO,
                "Viernes" to DailyMood.POSITIVO,
                "Sábado" to DailyMood.POSITIVO,
                "Domingo" to DailyMood.NEUTRO
            ),
            categoryDistribution = mapOf(
                CategoryType.TRABAJO to 3,
                CategoryType.SALUD to 2,
                CategoryType.FAMILIA to 2,
                CategoryType.OCIO to 1
            )
        )

        val highlight = WeeklyHighlight(
            strongestPositiveDay = "Viernes",
            strongestNegativeDay = "Jueves",
            mostFrequentCategory = CategoryType.TRABAJO,
            emotionalTrend = "Semana con buen equilibrio emocional"
        )

        val insights = listOf(
            com.dmood.app.domain.usecase.InsightRuleResult(
                title = "Predominio sereno",
                description = "Más de la mitad de tus decisiones surgieron con calma y claridad.",
                tag = "Serenidad"
            ),
            com.dmood.app.domain.usecase.InsightRuleResult(
                title = "Jueves retador",
                description = "El jueves concentró la mayor tensión emocional. Agenda descansos ese día.",
                tag = "Prevención"
            ),
            com.dmood.app.domain.usecase.InsightRuleResult(
                title = "Enfoque laboral",
                description = "Trabajo y familia lideran tus prioridades esta semana.",
                tag = "Prioridad"
            )
        )

        return DemoSummaryData(summary = summary, highlight = highlight, insights = insights)
    }
}

private data class DemoSummaryData(
    val summary: WeeklySummary,
    val highlight: WeeklyHighlight?,
    val insights: List<com.dmood.app.domain.usecase.InsightRuleResult>
)
