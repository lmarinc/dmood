package com.dmood.app.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.CalculateWeeklyScheduleUseCase
import com.dmood.app.domain.usecase.ExtractWeeklyHighlightsUseCase
import com.dmood.app.domain.usecase.GenerateInsightRulesUseCase
import com.dmood.app.domain.usecase.GenerateWeeklySummaryPdfUseCase
import com.dmood.app.domain.usecase.InsightRuleResult
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
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

data class WeeklyHistoryItem(
    val windowStart: LocalDate,
    val windowEnd: LocalDate,
    val summary: WeeklySummary?,
    val highlight: WeeklyHighlight?,
    val insights: List<InsightRuleResult>
) {
    val label: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("es", "ES"))
            return "${windowStart.format(formatter)} - ${windowEnd.format(formatter)}"
        }
}

data class WeeklyHistoryUiState(
    val isLoading: Boolean = true,
    val weeks: List<WeeklyHistoryItem> = emptyList(),
    val errorMessage: String? = null,
    val exportMessage: String? = null
)

class WeeklyHistoryViewModel(
    private val decisionRepository: DecisionRepository,
    private val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase,
    private val extractWeeklyHighlightsUseCase: ExtractWeeklyHighlightsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateWeeklyScheduleUseCase: CalculateWeeklyScheduleUseCase,
    private val generateInsightRulesUseCase: GenerateInsightRulesUseCase,
    private val generateWeeklySummaryPdfUseCase: GenerateWeeklySummaryPdfUseCase
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val _uiState = MutableStateFlow(WeeklyHistoryUiState())
    val uiState: StateFlow<WeeklyHistoryUiState> = _uiState

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, exportMessage = null) }
            try {
                val firstUseDateMillis = userPreferencesRepository.ensureFirstUseDate()
                val firstUseDate = Instant.ofEpochMilli(firstUseDateMillis).atZone(zoneId).toLocalDate()
                val weekStartDay = userPreferencesRepository.weekStartDayFlow.first()
                val schedule = calculateWeeklyScheduleUseCase(firstUseDate, weekStartDay, LocalDate.now())

                val items = mutableListOf<WeeklyHistoryItem>()
                var anchor = schedule.anchorDate
                while (!anchor.isBefore(schedule.eligibleAnchor)) {
                    val windowStart = anchor.minusDays(7)
                    val windowEnd = anchor.minusDays(1)
                    val startMillis = windowStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val endMillis = anchor.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val decisions = decisionRepository.getByRange(startMillis, endMillis)
                    if (decisions.isNotEmpty()) {
                        val summary = buildWeeklySummaryUseCase(decisions, startMillis, endMillis)
                        val highlight = extractWeeklyHighlightsUseCase(summary)
                        val insights = generateInsightRulesUseCase(decisions)
                        items.add(
                            WeeklyHistoryItem(
                                windowStart = windowStart,
                                windowEnd = windowEnd,
                                summary = summary,
                                highlight = highlight,
                                insights = insights
                            )
                        )
                    }
                    anchor = anchor.minusWeeks(1)
                }

                _uiState.value = WeeklyHistoryUiState(
                    isLoading = false,
                    weeks = items
                )
            } catch (e: Exception) {
                _uiState.value = WeeklyHistoryUiState(
                    isLoading = false,
                    weeks = emptyList(),
                    errorMessage = "No se pudo cargar el histórico semanal."
                )
            }
        }
    }

    fun exportWeek(item: WeeklyHistoryItem) {
        val summary = item.summary ?: return
        viewModelScope.launch {
            try {
                val file = generateWeeklySummaryPdfUseCase(
                    summary = summary,
                    highlight = item.highlight,
                    insights = item.insights,
                    fileName = "resumen_${item.windowStart}_${item.windowEnd}.pdf"
                )
                _uiState.update {
                    it.copy(exportMessage = "PDF generado en ${file.absolutePath}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportMessage = "No se pudo generar el PDF.") }
            }
        }
    }
}
