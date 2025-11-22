package com.dmood.app.ui.screen.summary

import android.content.Context
import android.content.ContentValues
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.DailyMood
import com.dmood.app.domain.usecase.WeeklySummary
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WeeklyHistoryEntry(
    val windowStart: Long,
    val windowEnd: Long,
    val decisions: List<Decision>,
    val summary: WeeklySummary?
)

data class WeeklyHistoryUiState(
    val isLoading: Boolean = false,
    val entries: List<WeeklyHistoryEntry> = emptyList(),
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class WeeklyHistoryViewModel(
    private val decisionRepository: DecisionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyHistoryUiState())
    val uiState: StateFlow<WeeklyHistoryUiState> = _uiState

    private val zoneId = ZoneId.systemDefault()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val weekStartDay = userPreferencesRepository.weekStartDayFlow.first()
                val decisions = decisionRepository.getAll()
                val grouped = decisions.groupBy { decision ->
                    decisionWeekAnchor(decision.timestamp, weekStartDay)
                }.map { (anchor, items) ->
                    val start = anchor.minusDays(7).atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val end = anchor.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val summary = runCatching {
                        buildWeeklySummaryUseCase(
                            decisions = items,
                            startDate = start,
                            endDate = end
                        )
                    }.getOrNull()

                    WeeklyHistoryEntry(
                        windowStart = start,
                        windowEnd = end,
                        decisions = items,
                        summary = summary
                    )
                }.sortedByDescending { it.windowStart }

                _uiState.value = WeeklyHistoryUiState(
                    isLoading = false,
                    entries = grouped
                )
            } catch (e: Exception) {
                _uiState.value = WeeklyHistoryUiState(
                    isLoading = false,
                    entries = emptyList(),
                    errorMessage = "No pudimos cargar el histórico."
                )
            }
        }
    }

    fun generatePdf(context: Context, entry: WeeklyHistoryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                val startDate = Instant.ofEpochMilli(entry.windowStart).atZone(zoneId).toLocalDate()
                val endDate = Instant.ofEpochMilli(entry.windowEnd).atZone(zoneId).toLocalDate()
                val summary = entry.summary ?: buildWeeklySummaryUseCase(
                    decisions = entry.decisions,
                    startDate = entry.windowStart,
                    endDate = entry.windowEnd
                )

                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val titlePaint = Paint().apply {
                    textSize = 20f
                    isFakeBoldText = true
                }
                val bodyPaint = Paint().apply {
                    textSize = 15f
                }
                val accentPaint = Paint().apply {
                    textSize = 14f
                    isFakeBoldText = true
                }

                canvas.drawText("Resumen semanal", 40f, 60f, titlePaint)
                canvas.drawText(
                    "Período: ${startDate.format(formatter)} - ${endDate.format(formatter)}",
                    40f,
                    88f,
                    bodyPaint
                )

                var y = 130f
                canvas.drawLine(40f, y, 560f, y, bodyPaint)
                y += 30f

                canvas.drawText("Tono emocional", 40f, y, accentPaint)
                y += 24f
                canvas.drawText("Calmadas: ${summary.calmPercentage.toInt()}%", 40f, y, bodyPaint)
                y += 20f
                canvas.drawText("Impulsivas: ${summary.impulsivePercentage.toInt()}%", 40f, y, bodyPaint)
                y += 34f

                canvas.drawText("Áreas presentes", 40f, y, accentPaint)
                y += 24f
                summary.categoryDistribution
                    .entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .forEach { (category, count) ->
                        canvas.drawText("${category.displayName}: ${((count / summary.totalDecisions.toFloat()) * 100).toInt()}%", 40f, y, bodyPaint)
                        y += 20f
                    }

                if (summary.dailyMoods.isNotEmpty()) {
                    y += 24f
                    canvas.drawText("Estado por día", 40f, y, accentPaint)
                    y += 24f
                    summary.dailyMoods.entries.take(5).forEach { (day, mood) ->
                        canvas.drawText("$day · ${mood.toDisplayName()}", 40f, y, bodyPaint)
                        y += 20f
                    }
                }

                document.finishPage(page)

                val displayName = "dmood_resumen_${startDate}_${endDate}.pdf"
                val savedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS + "/Dmood"
                        )
                    }

                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    ) ?: throw IllegalStateException("No se pudo crear el archivo de destino")

                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        document.writeTo(output)
                    } ?: throw IllegalStateException("No se pudo abrir la ruta de descarga")

                    uri.toString()
                } else {
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloads.exists()) downloads.mkdirs()
                    val targetFile = File(downloads, displayName)
                    targetFile.outputStream().use { output ->
                        document.writeTo(output)
                    }
                    targetFile.absolutePath
                }

                document.close()

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = "PDF guardado en $savedPath",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "No pudimos generar el PDF",
                        feedbackMessage = null
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    private fun decisionWeekAnchor(timestamp: Long, weekStartDay: DayOfWeek) =
        Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
            .with(TemporalAdjusters.nextOrSame(weekStartDay))

    private fun DailyMood.toDisplayName(): String = when (this) {
        DailyMood.POSITIVO -> "positivo"
        DailyMood.NEGATIVO -> "negativo"
        DailyMood.NEUTRO -> "neutro"
        DailyMood.NORMAL -> "normal"
    }
}
