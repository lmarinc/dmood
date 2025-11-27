package com.dmood.app.ui.screen.summary

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.CalculateWeeklyScheduleUseCase
import com.dmood.app.domain.usecase.ExtractWeeklyHighlightsUseCase
import com.dmood.app.domain.usecase.GenerateInsightRulesUseCase
import com.dmood.app.domain.usecase.InsightRuleResult
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WeeklyHistoryItem(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val summary: WeeklySummary,
    val highlight: WeeklyHighlight?,
    val insights: List<InsightRuleResult>
) {
    val label: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("dd MMM", java.util.Locale("es", "ES"))
            return "${startDate.format(formatter)} - ${endDate.format(formatter)}"
        }
}

data class WeeklyHistoryUiState(
    val items: List<WeeklyHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val exportMessage: String? = null
)

class WeeklyHistoryViewModel(
    private val decisionRepository: DecisionRepository,
    private val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase,
    private val extractWeeklyHighlightsUseCase: ExtractWeeklyHighlightsUseCase,
    private val calculateWeeklyScheduleUseCase: CalculateWeeklyScheduleUseCase,
    private val generateInsightRulesUseCase: GenerateInsightRulesUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private var firstUseDate: LocalDate = LocalDate.now()
    private var weekStartDay: DayOfWeek = DayOfWeek.MONDAY

    private val _uiState = MutableStateFlow(WeeklyHistoryUiState())
    val uiState: StateFlow<WeeklyHistoryUiState> = _uiState

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.firstUseLocalDate(zoneId).collect { stored ->
                stored?.let {
                    firstUseDate = it
                    loadHistory()
                }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weekStartDayFlow.collect { day ->
                weekStartDay = day
                loadHistory()
            }
        }
        viewModelScope.launch {
            val stored = userPreferencesRepository.ensureFirstUseDate()
            firstUseDate = java.time.Instant.ofEpochMilli(stored).atZone(zoneId).toLocalDate()
            loadHistory()
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, exportMessage = null)
            runCatching {
                val today = LocalDate.now()
                val schedule = calculateWeeklyScheduleUseCase(
                    firstUseDate = firstUseDate,
                    weekStartDay = weekStartDay,
                    today = today
                )
                val items = mutableListOf<WeeklyHistoryItem>()
                var anchor = schedule.anchorDate
                val earliestAnchor = schedule.eligibleAnchor

                while (!anchor.isBefore(earliestAnchor)) {
                    val startDate = anchor.minusDays(7)
                    val endDate = anchor.minusDays(1)
                    val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

                    val decisions = decisionRepository.getByRange(startMillis, endMillis)
                    if (decisions.isNotEmpty()) {
                        val summary = buildWeeklySummaryUseCase(
                            decisions = decisions,
                            startDate = startMillis,
                            endDate = endMillis
                        )
                        val highlight = extractWeeklyHighlightsUseCase(summary, decisions)
                        val insights = generateInsightRulesUseCase(decisions, summary)
                        items += WeeklyHistoryItem(
                            startDate = startDate,
                            endDate = endDate,
                            summary = summary,
                            highlight = highlight,
                            insights = insights
                        )
                    }
                    anchor = anchor.minusWeeks(1)
                }
                _uiState.value = WeeklyHistoryUiState(
                    items = items.sortedByDescending { it.endDate },
                    isLoading = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "No se pudo cargar el histórico"
                )
            }
        }
    }

    fun exportToPdf(context: Context, item: WeeklyHistoryItem) {
        viewModelScope.launch {
            runCatching {
                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val titlePaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val bodyPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 14f
                }

                var y = 40f
                canvas.drawText("Resumen semanal D-Mood", 40f, y, titlePaint)
                y += 28f
                canvas.drawText("Rango: ${item.label}", 40f, y, bodyPaint)
                y += 22f
                canvas.drawText("Total decisiones: ${item.summary.totalDecisions}", 40f, y, bodyPaint)
                y += 22f
                canvas.drawText(
                    "Calmadas: ${item.summary.calmPercentage.toInt()}% · Impulsivas: ${item.summary.impulsivePercentage.toInt()}%",
                    40f,
                    y,
                    bodyPaint
                )
                y += 22f
                canvas.drawText("Tendencia: ${item.highlight?.emotionalTrend ?: "-"}", 40f, y, bodyPaint)
                y += 32f

                canvas.drawText("Categorías principales:", 40f, y, titlePaint)
                y += 22f
                item.summary.categoryDistribution.entries
                    .sortedByDescending { it.value }
                    .take(4)
                    .forEach { entry ->
                        canvas.drawText("• ${entry.key.displayName}: ${entry.value}", 48f, y, bodyPaint)
                        y += 18f
                    }

                if (item.insights.isNotEmpty()) {
                    y += 18f
                    canvas.drawText("Insights:", 40f, y, titlePaint)
                    y += 22f
                    item.insights.take(5).forEach { insight ->
                        canvas.drawText("• ${insight.title}", 48f, y, bodyPaint)
                        y += 18f
                    }
                }

                document.finishPage(page)
                val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val file = File(outputDir, "dmood_${item.startDate}_${item.endDate}.pdf")
                file.outputStream().use { out -> document.writeTo(out) }
                document.close()

                _uiState.value = _uiState.value.copy(exportMessage = "PDF guardado en ${file.absolutePath}")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(exportMessage = error.message ?: "No se pudo crear el PDF")
            }
        }
    }
}
