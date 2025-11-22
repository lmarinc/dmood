package com.dmood.app.domain.usecase

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
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
        fileName: String
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 20f
            color = 0xFF1A1C1E.toInt()
        }
        val bodyPaint = Paint().apply {
            textSize = 14f
            color = 0xFF1A1C1E.toInt()
        }
        val accentPaint = Paint().apply {
            color = 0xFFEEF2FF.toInt()
        }
        val dividerPaint = Paint().apply {
            color = 0xFF4F46E5.toInt()
            strokeWidth = 3f
        }

        canvas.drawRect(0f, 0f, pageInfo.pageWidth.toFloat(), 120f, accentPaint)
        canvas.drawText("Resumen semanal D-Mood", 40f, 70f, titlePaint)

        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(summary.startDate).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(summary.endDate).atZone(zone).toLocalDate()

        var yPosition = 150f
        canvas.drawLine(40f, yPosition, pageInfo.pageWidth - 40f, yPosition, dividerPaint)
        yPosition += 24f

        canvas.drawText("Periodo: ${start.format(formatter)} - ${end.format(formatter)}", 40f, yPosition, bodyPaint)
        yPosition += 24f
        canvas.drawText("Decisiones registradas: ${summary.totalDecisions}", 40f, yPosition, bodyPaint)
        yPosition += 24f
        canvas.drawText("Calmadas: ${summary.calmPercentage.toInt()}% | Impulsivas: ${summary.impulsivePercentage.toInt()}% | Neutras: ${summary.neutralPercentage.toInt()}%", 40f, yPosition, bodyPaint)

        highlight?.let {
            yPosition += 36f
            canvas.drawLine(40f, yPosition, pageInfo.pageWidth - 40f, yPosition, dividerPaint)
            yPosition += 28f
            canvas.drawText("Día más positivo: ${it.strongestPositiveDay ?: "-"}", 40f, yPosition, bodyPaint)
            yPosition += 22f
            canvas.drawText("Día más retador: ${it.strongestNegativeDay ?: "-"}", 40f, yPosition, bodyPaint)
            yPosition += 22f
            it.mostFrequentCategory?.let { category ->
                canvas.drawText("Área protagonista: ${category.displayName}", 40f, yPosition, bodyPaint)
                yPosition += 22f
            }
            canvas.drawText(it.emotionalTrend, 40f, yPosition, bodyPaint)
        }

        if (insights.isNotEmpty()) {
            yPosition += 36f
            canvas.drawLine(40f, yPosition, pageInfo.pageWidth - 40f, yPosition, dividerPaint)
            yPosition += 28f
            canvas.drawText("Insights destacados", 40f, yPosition, titlePaint)
            yPosition += 12f
            insights.take(4).forEach { insight ->
                yPosition += 28f
                canvas.drawText("• ${insight.title}", 40f, yPosition, bodyPaint)
                yPosition += 20f
                canvas.drawText(insight.description, 60f, yPosition, bodyPaint)
            }
        }

        pdfDocument.finishPage(page)

        val safeFileName = fileName.ifBlank { "dmood_resumen.pdf" }
        val outputFile = File(context.cacheDir, safeFileName)
        FileOutputStream(outputFile).use { output ->
            pdfDocument.writeTo(output)
        }
        pdfDocument.close()
        return outputFile
    }
}
