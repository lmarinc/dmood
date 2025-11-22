package com.dmood.app.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.CalculateWeeklyScheduleUseCase
import com.dmood.app.domain.usecase.DailyMood
import com.dmood.app.domain.usecase.ExtractWeeklyHighlightsUseCase
import com.dmood.app.domain.usecase.GenerateInsightRulesUseCase
import com.dmood.app.domain.usecase.InsightRuleResult
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
                val storedWeekStart = userPreferencesRepository.weekStartDayFlow.first()
                val storedFirstUse = userPreferencesRepository.ensureFirstUseDate()
                val firstUseDate = Instant.ofEpochMilli(storedFirstUse).atZone(zoneId).toLocalDate()
                val schedule = calculateWeeklyScheduleUseCase(
                    firstUseDate = firstUseDate,
                    weekStartDay = storedWeekStart,
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
                        userName = _uiState.value.userName,
                        isDemo = false
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
                    userName = _uiState.value.userName,
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
                    userName = _uiState.value.userName,
                    isDemo = false
                )
            }
        }
    }

    fun loadDemoSummary() {
        val today = LocalDate.now()
        val startDate = today.minusDays(7)
        val endDate = today.minusDays(1)

        val summary = WeeklySummary(
            startDate = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endDate = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            totalDecisions = 8,
            calmPercentage = 62f,
            impulsivePercentage = 24f,
            neutralPercentage = 14f,
            dailyMoods = mapOf(
                "Lunes" to DailyMood.POSITIVO,
                "Martes" to DailyMood.NEUTRO,
                "Miércoles" to DailyMood.POSITIVO,
                "Jueves" to DailyMood.NEGATIVO,
                "Viernes" to DailyMood.POSITIVO
            ),
            categoryDistribution = mapOf(
                CategoryType.RELACIONES_SOCIAL to 3,
                CategoryType.SALUD_BIENESTAR to 2,
                CategoryType.TRABAJO_ESTUDIOS to 2,
                CategoryType.OCIO_TIEMPO_LIBRE to 1
            )
        )

        val highlight = WeeklyHighlight(
            emotionalTrend = "Semana equilibrada con momentos luminosos y aprendizajes.",
            strongestPositiveDay = "Miércoles",
            strongestNegativeDay = "Jueves",
            mostFrequentCategory = CategoryType.RELACIONES_SOCIAL
        )

        val insights = listOf(
            InsightRuleResult(
                title = "Fortalece tus relaciones", 
                description = "La mayor parte de tus decisiones estuvieron ligadas a tu entorno social.",
                tag = "Conexiones"
            ),
            InsightRuleResult(
                title = "Gestiona la energía en jueves",
                description = "El jueves se percibe más pesado. Programa una pausa consciente ese día.",
                tag = "Rutinas"
            )
        )

        _uiState.update { state ->
            WeeklySummaryUiState(
                isLoading = false,
                summary = summary,
                highlight = highlight,
                errorMessage = null,
                userName = state.userName,
                insights = insights,
                nextSummaryDate = today.plusWeeks(1),
                isSummaryAvailable = true,
                isDemo = true
            )
        }
    }
}
