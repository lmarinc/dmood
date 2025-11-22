package com.dmood.app.ui.screen.summary

import android.content.Context
import android.graphics.Paint
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
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WeeklyHistoryItem(
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val summary: WeeklySummary?,
    val highlight: WeeklyHighlight?,
    val insights: List<InsightRuleResult>,
    val hasData: Boolean
)

data class WeeklyHistoryUiState(
    val isLoading: Boolean = false,
    val weeks: List<WeeklyHistoryItem> = emptyList(),
    val error: String? = null,
    val userName: String? = null
)

class WeeklyHistoryViewModel(
    private val decisionRepository: DecisionRepository,
    private val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase,
    private val extractWeeklyHighlightsUseCase: ExtractWeeklyHighlightsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateWeeklyScheduleUseCase: CalculateWeeklyScheduleUseCase,
    private val generateInsightRulesUseCase: GenerateInsightRulesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyHistoryUiState())
    val uiState: StateFlow<WeeklyHistoryUiState> = _uiState

    private val zoneId: ZoneId = ZoneId.systemDefault()

    init {
        loadUserName()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            val name = userPreferencesRepository.getUserName()
            _uiState.value = _uiState.value.copy(userName = name)
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val weekStart = userPreferencesRepository.weekStartDayFlow.first()
                val firstUseMillis = userPreferencesRepository.ensureFirstUseDate()
                val firstUseDate = Instant.ofEpochMilli(firstUseMillis).atZone(zoneId).toLocalDate()
                val schedule = calculateWeeklyScheduleUseCase(
                    firstUseDate = firstUseDate,
                    weekStartDay = weekStart,
                    today = LocalDate.now()
                )

                var anchor = schedule.anchorDate
                val eligibleAnchor = schedule.eligibleAnchor
                val historyItems = mutableListOf<WeeklyHistoryItem>()

                while (!anchor.isBefore(eligibleAnchor)) {
                    val start = anchor.minusDays(7)
                    val end = anchor.minusDays(1)
                    val startMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val endMillis = end.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

                    val decisions = decisionRepository.getByRange(startMillis, endMillis)

                    if (decisions.isEmpty()) {
                        historyItems.add(
                            WeeklyHistoryItem(
                                title = "Semana del ${start.format(shortFormatter())}",
                                startDate = start,
                                endDate = end,
                                summary = null,
                                highlight = null,
                                insights = emptyList(),
                                hasData = false
                            )
                        )
                    } else {
                        val summary = buildWeeklySummaryUseCase(
                            decisions = decisions,
                            startDate = startMillis,
                            endDate = endMillis
                        )
                        val highlight = extractWeeklyHighlightsUseCase(summary)
                        val insights = generateInsightRulesUseCase(decisions)
                        historyItems.add(
                            WeeklyHistoryItem(
                                title = "Semana del ${start.format(shortFormatter())}",
                                startDate = start,
                                endDate = end,
                                summary = summary,
                                highlight = highlight,
                                insights = insights,
                                hasData = true
                            )
                        )
                    }

                    anchor = anchor.minusWeeks(1)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weeks = historyItems
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No se pudo cargar el histórico semanal"
                )
            }
        }
    }

    fun exportWeekToPdf(context: Context, item: WeeklyHistoryItem): File? {
        val summary = item.summary ?: return null
        val highlight = item.highlight

        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val document = PdfDocument()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 22f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 14f
        }
        val accentPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#DDE9FF")
        }

        canvas.drawRect(0f, 0f, pageInfo.pageWidth.toFloat(), 120f, accentPaint)
        canvas.drawText("D-Mood", 32f, 50f, titlePaint)
        _uiState.value.userName?.let { name ->
            canvas.drawText("Hola, $name", 32f, 80f, bodyPaint)
        }
        canvas.drawText(item.title, 32f, 110f, bodyPaint)

        var currentY = 150f
        canvas.drawText("Decisiones: ${summary.totalDecisions}", 32f, currentY, bodyPaint)
        currentY += 22f
        canvas.drawText("Calmadas: ${summary.calmPercentage.toInt()}%", 32f, currentY, bodyPaint)
        currentY += 22f
        canvas.drawText("Impulsivas: ${summary.impulsivePercentage.toInt()}%", 32f, currentY, bodyPaint)
        currentY += 32f

        highlight?.let {
            canvas.drawText("Día más luminoso: ${it.strongestPositiveDay ?: "-"}", 32f, currentY, bodyPaint)
            currentY += 22f
            canvas.drawText("Día más retador: ${it.strongestNegativeDay ?: "-"}", 32f, currentY, bodyPaint)
            currentY += 22f
            canvas.drawText("Área protagonista: ${it.mostFrequentCategory?.displayName ?: "-"}", 32f, currentY, bodyPaint)
            currentY += 32f
            canvas.drawText(it.emotionalTrend, 32f, currentY, bodyPaint)
            currentY += 32f
        }

        if (item.insights.isNotEmpty()) {
            canvas.drawText("Pistas rápidas", 32f, currentY, titlePaint)
            currentY += 24f
            item.insights.take(3).forEach { insight ->
                canvas.drawText("• ${insight.title}", 32f, currentY, bodyPaint)
                currentY += 18f
                canvas.drawText(insight.description, 48f, currentY, bodyPaint)
                currentY += 22f
            }
        }

        document.finishPage(page)

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val formatter = DateTimeFormatter.ofPattern("ddMMyyyy", Locale.getDefault())
        val fileName = "dmood_resumen_${item.startDate.format(formatter)}_${item.endDate.format(formatter)}.pdf"
        val file = File(directory, fileName)

        try {
            FileOutputStream(file).use { output ->
                document.writeTo(output)
            }
        } catch (e: Exception) {
            document.close()
            return null
        }

        document.close()
        return file
    }

    private fun shortFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM", Locale("es", "ES"))
}
