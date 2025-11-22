package com.dmood.app.ui.screen.summary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.CalculateWeeklyScheduleUseCase
import com.dmood.app.domain.usecase.ExtractWeeklyHighlightsUseCase
import com.dmood.app.domain.usecase.GenerateInsightRulesUseCase
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val WEEK_LABEL_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM", Locale("es", "ES"))

internal data class WeeklyHistoryWeek(
    val anchorDate: LocalDate,
    val windowStart: LocalDate,
    val windowEnd: LocalDate,
    val label: String,
    val isReady: Boolean,
    val isGenerating: Boolean = false
)

data class WeeklyHistoryUiState(
    val isLoading: Boolean = true,
    val weeks: List<WeeklyHistoryWeek> = emptyList(),
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val generatedFilePath: String? = null
)

class WeeklyHistoryViewModel(
    private val decisionRepository: DecisionRepository,
    private val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase,
    private val extractWeeklyHighlightsUseCase: ExtractWeeklyHighlightsUseCase,
    private val generateInsightRulesUseCase: GenerateInsightRulesUseCase,
    private val calculateWeeklyScheduleUseCase: CalculateWeeklyScheduleUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private var firstUseDate: LocalDate = LocalDate.now()
    private var weekStartDay: DayOfWeek = DayOfWeek.MONDAY

    private val _uiState = MutableStateFlow(WeeklyHistoryUiState())
    val uiState: StateFlow<WeeklyHistoryUiState> = _uiState

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null,
                generatedFilePath = null
            )

            runCatching {
                syncPreferences()
                buildWeeks()
            }.onSuccess { weeks ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weeks = weeks,
                    errorMessage = if (weeks.isEmpty()) "Todavía no hay semanas listas para exportar." else null
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No pudimos cargar el histórico semanal."
                )
            }
        }
    }

    fun generatePdfForWeek(anchorDate: LocalDate, context: Context) {
        viewModelScope.launch {
            updateWeek(anchorDate) { it.copy(isGenerating = true) }
            val windowStart = anchorDate.minusDays(7)
            val startMillis = windowStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMillis = anchorDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val rangeLabel = "${windowStart.format(WEEK_LABEL_FORMATTER)} - ${anchorDate.minusDays(1).format(WEEK_LABEL_FORMATTER)}"

            runCatching {
                syncPreferences()
                val decisions = decisionRepository.getByRange(startMillis, endMillis)
                if (decisions.isEmpty()) {
                    error("No hay decisiones registradas en ese intervalo.")
                }
                val summary = buildWeeklySummaryUseCase(decisions, startMillis, endMillis)
                val highlight = extractWeeklyHighlightsUseCase(summary)
                val insights = generateInsightRulesUseCase(decisions)
                val generator = WeeklySummaryPdfGenerator(context)
                generator.generate(
                    summary = summary,
                    highlight = highlight,
                    insights = insights,
                    rangeLabel = rangeLabel
                )
            }.onSuccess { file ->
                _uiState.update { state ->
                    state.copy(
                        weeks = state.weeks.map { week ->
                            if (week.anchorDate == anchorDate) week.copy(isGenerating = false) else week
                        },
                        infoMessage = "PDF generado: ${file.name}",
                        generatedFilePath = file.absolutePath,
                        errorMessage = null
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        weeks = state.weeks.map { week ->
                            if (week.anchorDate == anchorDate) week.copy(isGenerating = false) else week
                        },
                        errorMessage = "No se pudo crear el PDF de esa semana."
                    )
                }
            }
        }
    }

    private suspend fun syncPreferences() {
        val storedFirstUse = userPreferencesRepository.ensureFirstUseDate()
        firstUseDate = Instant.ofEpochMilli(storedFirstUse).atZone(zoneId).toLocalDate()
        weekStartDay = userPreferencesRepository.weekStartDayFlow.first()
    }

    private suspend fun buildWeeks(): List<WeeklyHistoryWeek> {
        val today = LocalDate.now()
        val schedule = calculateWeeklyScheduleUseCase(firstUseDate, weekStartDay, today)
        if (!schedule.isSummaryAvailable && today.isBefore(schedule.eligibleAnchor)) {
            return emptyList()
        }

        val anchors = generateSequence(schedule.anchorDate) { it.minusWeeks(1) }
            .takeWhile { !it.isBefore(schedule.eligibleAnchor) }
            .toList()

        return anchors.map { anchor ->
            val start = anchor.minusDays(7)
            val end = anchor.minusDays(1)
            WeeklyHistoryWeek(
                anchorDate = anchor,
                windowStart = start,
                windowEnd = end,
                label = "${start.format(WEEK_LABEL_FORMATTER)} - ${end.format(WEEK_LABEL_FORMATTER)}",
                isReady = !today.isBefore(anchor)
            )
        }
    }

    private fun updateWeek(anchorDate: LocalDate, transform: (WeeklyHistoryWeek) -> WeeklyHistoryWeek) {
        _uiState.update { state ->
            state.copy(
                weeks = state.weeks.map { week ->
                    if (week.anchorDate == anchorDate) transform(week) else week
                }
            )
        }
    }
}
