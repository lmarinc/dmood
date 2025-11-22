package com.dmood.app.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.usecase.DailyMood
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
import kotlinx.coroutines.flow.first
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
    val developerModeEnabled: Boolean = false
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
        observeDeveloperMode()
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

    private fun observeDeveloperMode() {
        viewModelScope.launch {
            userPreferencesRepository.developerModeFlow.collect { enabled ->
                val previous = _uiState.value.developerModeEnabled
                _uiState.value = _uiState.value.copy(developerModeEnabled = enabled)
                if (enabled != previous) {
                    loadWeeklySummary()
                }
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
                val devEnabled = _uiState.value.developerModeEnabled
                val today = LocalDate.now()
                val storedWeekStart = userPreferencesRepository.weekStartDayFlow.first()
                val storedFirstUse = userPreferencesRepository.ensureFirstUseDate()
                weekStartDay = storedWeekStart
                firstUseDate = Instant.ofEpochMilli(storedFirstUse).atZone(zoneId).toLocalDate()

                val schedule = calculateWeeklyScheduleUseCase(
                    firstUseDate = firstUseDate,
                    weekStartDay = weekStartDay,
                    today = today
                )

                val start = schedule.windowStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = (if (devEnabled) minOf(schedule.windowEnd, today) else schedule.windowEnd)
                    .plusDays(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()

                if (!schedule.isSummaryAvailable && !devEnabled) {
                    _uiState.value = WeeklySummaryUiState(
                        isLoading = false,
                        summary = null,
                        highlight = null,
                        errorMessage = "Tu próximo resumen estará listo el ${schedule.nextSummaryDate}",
                        nextSummaryDate = schedule.nextSummaryDate,
                        isSummaryAvailable = false,
                        developerModeEnabled = devEnabled
                    )
                    return@launch
                }

                val decisions = decisionRepository.getByRange(start, end)

                val (summary, highlight, insights) = buildSummaryBundle(
                    decisions = decisions,
                    startDate = start,
                    endDate = end,
                    devEnabled = devEnabled
                )

                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = summary,
                    highlight = highlight,
                    errorMessage = null,
                    insights = insights,
                    nextSummaryDate = schedule.nextSummaryDate,
                    isSummaryAvailable = schedule.isSummaryAvailable || devEnabled,
                    developerModeEnabled = devEnabled
                )
            } catch (e: Exception) {
                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = null,
                    highlight = null,
                    errorMessage = "No se pudo cargar el resumen semanal.",
                    nextSummaryDate = null,
                    isSummaryAvailable = false,
                    developerModeEnabled = _uiState.value.developerModeEnabled
                )
            }
        }
    }

    fun forceBuildSummary() {
        if (!_uiState.value.developerModeEnabled) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val today = LocalDate.now()
                val storedWeekStart = userPreferencesRepository.weekStartDayFlow.first()
                val storedFirstUse = userPreferencesRepository.ensureFirstUseDate()
                weekStartDay = storedWeekStart
                firstUseDate = Instant.ofEpochMilli(storedFirstUse).atZone(zoneId).toLocalDate()

                val schedule = calculateWeeklyScheduleUseCase(
                    firstUseDate = firstUseDate,
                    weekStartDay = weekStartDay,
                    today = today
                )

                val start = schedule.windowStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = minOf(schedule.windowEnd, today)
                    .plusDays(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()

                val decisions = decisionRepository.getByRange(start, end)
                val (summary, highlight, insights) = buildSummaryBundle(
                    decisions = decisions,
                    startDate = start,
                    endDate = end,
                    devEnabled = true
                )

                _uiState.value = WeeklySummaryUiState(
                    isLoading = false,
                    summary = summary,
                    highlight = highlight,
                    errorMessage = null,
                    insights = insights,
                    nextSummaryDate = schedule.nextSummaryDate,
                    isSummaryAvailable = true,
                    developerModeEnabled = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No pudimos generar el resumen de prueba."
                )
            }
        }
    }

    private fun buildSummaryBundle(
        decisions: List<com.dmood.app.domain.model.Decision>,
        startDate: Long,
        endDate: Long,
        devEnabled: Boolean
    ): Triple<WeeklySummary, WeeklyHighlight, List<com.dmood.app.domain.usecase.InsightRuleResult>> {
        if (devEnabled && decisions.isEmpty()) {
            val demoSummary = createDemoSummary(startDate, endDate)
            val demoHighlight = extractWeeklyHighlightsUseCase(demoSummary)
            val demoInsights = buildDemoInsights()
            return Triple(demoSummary, demoHighlight, demoInsights)
        }

        val summary = buildWeeklySummaryUseCase(
            decisions = decisions,
            startDate = startDate,
            endDate = endDate
        )

        val highlight = extractWeeklyHighlightsUseCase(summary)
        val insights = generateInsightRulesUseCase(decisions).let { generated ->
            if (devEnabled && generated.isEmpty()) buildDemoInsights() else generated
        }
        return Triple(summary, highlight, insights)
    }

    private fun createDemoSummary(startDate: Long, endDate: Long): WeeklySummary {
        val demoDailyMoods = mapOf(
            "Lunes" to DailyMood.POSITIVO,
            "Martes" to DailyMood.POSITIVO,
            "Miércoles" to DailyMood.NEUTRO,
            "Jueves" to DailyMood.NEGATIVO,
            "Viernes" to DailyMood.POSITIVO,
            "Sábado" to DailyMood.NEUTRO,
            "Domingo" to DailyMood.NEGATIVO
        )
        val categoryDistribution = mapOf(
            CategoryType.TRABAJO_ESTUDIOS to 4,
            CategoryType.RELACIONES_SOCIAL to 3,
            CategoryType.SALUD_BIENESTAR to 2,
            CategoryType.FINANZAS_COMPRAS to 1
        )

        return WeeklySummary(
            startDate = startDate,
            endDate = endDate,
            totalDecisions = categoryDistribution.values.sum(),
            calmPercentage = 62f,
            impulsivePercentage = 24f,
            neutralPercentage = 14f,
            dailyMoods = demoDailyMoods,
            categoryDistribution = categoryDistribution
        )
    }

    private fun buildDemoInsights(): List<com.dmood.app.domain.usecase.InsightRuleResult> = listOf(
        com.dmood.app.domain.usecase.InsightRuleResult(
            title = "Tu semana fue serena",
            description = "La mayoría de las decisiones se tomaron con calma y foco. Sigue protegiendo esos espacios de claridad.",
            tag = "Serenidad"
        ),
        com.dmood.app.domain.usecase.InsightRuleResult(
            title = "Predominio profesional",
            description = "Casi la mitad de tus decisiones se relacionaron con el trabajo. Reserva tiempo de desconexión.",
            tag = "Prioridad"
        ),
        com.dmood.app.domain.usecase.InsightRuleResult(
            title = "Picos emocionales",
            description = "Detectamos dos días retadores. Considera planificar actividades ligeras esos días.",
            tag = "Prevención"
        )
    )
}
