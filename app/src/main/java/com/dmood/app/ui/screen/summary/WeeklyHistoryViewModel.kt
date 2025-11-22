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
    val decisions: List<Decision>
)

data class WeeklyHistoryUiState(
    val isLoading: Boolean = false,
    val entries: List<WeeklyHistoryEntry> = emptyList(),
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class WeeklyHistoryViewModel(
    private val decisionRepository: DecisionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
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
                    WeeklyHistoryEntry(
                        windowStart = anchor.minusDays(7).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                        windowEnd = anchor.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                        decisions = items
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

                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint().apply { textSize = 16f }

                canvas.drawText("Resumen semanal", 40f, 40f, paint)
                canvas.drawText("Período: ${startDate.format(formatter)} - ${endDate.format(formatter)}", 40f, 70f, paint)
                canvas.drawText("Decisiones registradas: ${entry.decisions.size}", 40f, 100f, paint)

                var y = 140f
                entry.decisions.take(10).forEachIndexed { index, decision ->
                    canvas.drawText("${index + 1}. ${decision.text}", 40f, y, paint)
                    y += 24f
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
}
