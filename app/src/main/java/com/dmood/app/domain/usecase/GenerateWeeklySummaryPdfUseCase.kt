package com.dmood.app.domain.usecase

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class GenerateWeeklySummaryPdfUseCase(private val context: Context) {

    operator fun invoke(
        summary: WeeklySummary,
        highlight: WeeklyHighlight?,
        insights: List<InsightRuleResult>,
        userName: String?,
        fileName: String
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1B4F72")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1F2933")
            textSize = 14f
            isAntiAlias = true
        }
        val highlightPaint = Paint().apply {
            color = Color.parseColor("#5C6AC4")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var currentY = 60f
        val startDate = Instant.ofEpochMilli(summary.startDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val endDate = Instant.ofEpochMilli(summary.endDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))

        canvas.drawText("Resumen semanal de ${userName ?: "D-Mood"}", 40f, currentY, titlePaint)
        currentY += 28f
        canvas.drawText("Periodo: ${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}", 40f, currentY, bodyPaint)
        currentY += 26f

        canvas.drawLine(40f, currentY, pageInfo.pageWidth - 40f, currentY, highlightPaint)
        currentY += 30f

        canvas.drawText("Tono emocional", 40f, currentY, highlightPaint)
        currentY += 22f
        canvas.drawText("Calmadas: ${summary.calmPercentage.toInt()}%", 40f, currentY, bodyPaint)
        currentY += 18f
        canvas.drawText("Impulsivas: ${summary.impulsivePercentage.toInt()}%", 40f, currentY, bodyPaint)
        currentY += 18f
        canvas.drawText("Neutras: ${summary.neutralPercentage.toInt()}%", 40f, currentY, bodyPaint)
        currentY += 30f

        highlight?.let {
            canvas.drawText("Destacados", 40f, currentY, highlightPaint)
            currentY += 22f
            it.strongestPositiveDay?.let { day ->
                canvas.drawText("Día más luminoso: $day", 40f, currentY, bodyPaint)
                currentY += 18f
            }
            it.strongestNegativeDay?.let { day ->
                canvas.drawText("Día más retador: $day", 40f, currentY, bodyPaint)
                currentY += 18f
            }
            it.mostFrequentCategory?.let { category ->
                canvas.drawText("Categoría líder: ${category.displayName}", 40f, currentY, bodyPaint)
                currentY += 18f
            }
            canvas.drawText(it.emotionalTrend, 40f, currentY, bodyPaint)
            currentY += 30f
        }

        if (insights.isNotEmpty()) {
            canvas.drawText("Ideas rápidas", 40f, currentY, highlightPaint)
            currentY += 22f
            insights.take(4).forEach { insight ->
                canvas.drawText("• ${insight.title}", 40f, currentY, bodyPaint)
                currentY += 16f
                canvas.drawText(insight.description, 60f, currentY, bodyPaint)
                currentY += 22f
            }
            currentY += 10f
        }

        canvas.drawText(
            "Total decisiones: ${summary.totalDecisions}",
            40f,
            (pageInfo.pageHeight - 40).toFloat(),
            highlightPaint
        )

        document.finishPage(page)

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val outputFile = File(directory, "$fileName.pdf")
        FileOutputStream(outputFile).use { output ->
            document.writeTo(output)
        }
        document.close()
        return outputFile
    }
}
