package com.dmood.app.ui.screen.summary.history

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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WeeklyHistoryItem(
    val label: String,
    val summary: WeeklySummary,
    val highlight: WeeklyHighlight?,
    val insights: List<InsightRuleResult>
)

data class WeeklyHistoryUiState(
    val isLoading: Boolean = false,
    val items: List<WeeklyHistoryItem> = emptyList(),
    val errorMessage: String? = null,
    val downloadMessage: String? = null,
    val userName: String? = null
)

class WeeklyHistoryViewModel(
    private val decisionRepository: DecisionRepository,
    private val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase,
    private val extractWeeklyHighlightsUseCase: ExtractWeeklyHighlightsUseCase,
    private val calculateWeeklyScheduleUseCase: CalculateWeeklyScheduleUseCase,
    private val generateWeeklySummaryPdfUseCase: GenerateWeeklySummaryPdfUseCase,
    private val generateInsightRulesUseCase: GenerateInsightRulesUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyHistoryUiState(isLoading = true))
    val uiState: StateFlow<WeeklyHistoryUiState> = _uiState

    private val zoneId = ZoneId.systemDefault()
    private var firstUseDate: LocalDate = LocalDate.now()
    private var weekStartDay: DayOfWeek = DayOfWeek.MONDAY

    init {
        observeUser()
        loadHistory()
    }

    private fun observeUser() {
        viewModelScope.launch {
            userPreferencesRepository.userNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(userName = name)
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, downloadMessage = null)
            try {
                refreshPreferences()
                val today = LocalDate.now()
                val schedule = calculateWeeklyScheduleUseCase(
                    firstUseDate = firstUseDate,
                    weekStartDay = weekStartDay,
                    today = today
                )
                val currentAnchor = today.with(TemporalAdjusters.previousOrSame(weekStartDay))
                val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("es", "ES"))

                val items = buildList {
                    var anchor = currentAnchor
                    while (!anchor.isBefore(schedule.eligibleAnchor)) {
                        val startDate = anchor.minusDays(7)
                        val endDate = anchor.minusDays(1)
                        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                        val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                        val decisions = decisionRepository.getByRange(startMillis, endMillis)
                        if (decisions.isNotEmpty()) {
                            val summary = buildWeeklySummaryUseCase(decisions, startMillis, endMillis)
                            val highlight = extractWeeklyHighlightsUseCase(summary)
                            val insights = generateInsightRulesUseCase(decisions)
                            val label = "${startDate.format(formatter)} - ${endDate.format(formatter)}"
                            add(
                                WeeklyHistoryItem(
                                    label = label,
                                    summary = summary,
                                    highlight = highlight,
                                    insights = insights
                                )
                            )
                        }
                        anchor = anchor.minusWeeks(1)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items,
                    errorMessage = if (items.isEmpty()) "Aún no hay resúmenes semanales generados." else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No pudimos cargar tu histórico semanal"
                )
            }
        }
    }

    fun download(item: WeeklyHistoryItem) {
        viewModelScope.launch {
            try {
                val safeName = item.label.replace(" ", "_")
                val file = generateWeeklySummaryPdfUseCase(
                    summary = item.summary,
                    highlight = item.highlight,
                    insights = item.insights,
                    userName = _uiState.value.userName,
                    fileName = safeName
                )
                _uiState.value = _uiState.value.copy(
                    downloadMessage = "PDF guardado en ${file.name}",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No se pudo generar el PDF",
                    downloadMessage = null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(downloadMessage = null, errorMessage = null)
    }

    private suspend fun refreshPreferences() {
        val storedFirstUse = userPreferencesRepository.firstUseLocalDate(zoneId).first()
        storedFirstUse?.let { firstUseDate = it }
        weekStartDay = userPreferencesRepository.weekStartDayFlow.first()
        if (storedFirstUse == null) {
            val ensured = userPreferencesRepository.ensureFirstUseDate()
            firstUseDate = Instant.ofEpochMilli(ensured).atZone(zoneId).toLocalDate()
        }
    }
}
