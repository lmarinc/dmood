package com.dmood.app.ui.screen.summary

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import com.dmood.app.domain.usecase.InsightRuleResult
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary

class WeeklySummaryPdfGenerator(private val context: Context) {

    private val headerColor = Color.parseColor("#5C6BFF")
    private val accentSoft = Color.parseColor("#F4F6FF")
    private val accentText = Color.parseColor("#1B1C33")

    fun generate(
        summary: WeeklySummary,
        highlight: WeeklyHighlight?,
        insights: List<InsightRuleResult>,
        rangeLabel: String
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)
        drawHeader(canvas, rangeLabel)

        var currentY = 320f
        currentY = drawSummaryBlock(canvas, summary, currentY)
        currentY = drawHighlightBlock(canvas, highlight, currentY + 20f)
        currentY = drawCategoryBlock(canvas, summary, currentY + 20f)
        drawInsightsBlock(canvas, insights, currentY + 20f)

        document.finishPage(page)

        val file = File(context.cacheDir, "dmood_resumen_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    private fun drawHeader(canvas: Canvas, rangeLabel: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = headerColor
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), 260f, paint)

        paint.color = Color.WHITE
        paint.textSize = 54f
        paint.isFakeBoldText = true
        canvas.drawText("Resumen semanal D-Mood", 64f, 140f, paint)

        paint.textSize = 32f
        paint.isFakeBoldText = false
        canvas.drawText(rangeLabel, 64f, 190f, paint)
    }

    private fun drawCardBackground(canvas: Canvas, top: Float, height: Float): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = accentSoft
        val bottom = top + height
        canvas.drawRoundRect(48f, top, canvas.width - 48f, bottom, 28f, 28f, paint)
        return bottom
    }

    private fun drawSummaryBlock(canvas: Canvas, summary: WeeklySummary, top: Float): Float {
        val blockHeight = 280f
        drawCardBackground(canvas, top, blockHeight)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = accentText
        paint.textSize = 40f
        paint.isFakeBoldText = true
        canvas.drawText("Pulso de tu semana", 80f, top + 70f, paint)

        paint.textSize = 30f
        paint.isFakeBoldText = false
        val totals = "${summary.totalDecisions} decisiones"
        canvas.drawText(totals, 80f, top + 120f, paint)

        val calm = "Calmadas: ${summary.calmPercentage.toInt()}%"
        val impulsive = "Impulsivas: ${summary.impulsivePercentage.toInt()}%"
        val neutral = "Neutras: ${summary.neutralPercentage.toInt()}%"

        canvas.drawText(calm, 80f, top + 170f, paint)
        canvas.drawText(impulsive, 80f, top + 210f, paint)
        canvas.drawText(neutral, 80f, top + 250f, paint)

        return top + blockHeight
    }

    private fun drawHighlightBlock(canvas: Canvas, highlight: WeeklyHighlight?, top: Float): Float {
        val blockHeight = 220f
        drawCardBackground(canvas, top, blockHeight)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = accentText
        paint.textSize = 38f
        paint.isFakeBoldText = true
        canvas.drawText("Días destacados", 80f, top + 70f, paint)

        paint.textSize = 28f
        paint.isFakeBoldText = false
        val positive = "Más luminoso: ${highlight?.strongestPositiveDay ?: "-"}"
        val negative = "Más retador: ${highlight?.strongestNegativeDay ?: "-"}"
        val area = "Área presente: ${highlight?.mostFrequentCategory?.displayName ?: "-"}"

        canvas.drawText(positive, 80f, top + 120f, paint)
        canvas.drawText(negative, 80f, top + 160f, paint)
        canvas.drawText(area, 80f, top + 200f, paint)

        return top + blockHeight
    }

    private fun drawCategoryBlock(canvas: Canvas, summary: WeeklySummary, top: Float): Float {
        val blockHeight = 260f
        drawCardBackground(canvas, top, blockHeight)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = accentText
        paint.textSize = 38f
        paint.isFakeBoldText = true
        canvas.drawText("Top categorías", 80f, top + 70f, paint)

        paint.textSize = 28f
        paint.isFakeBoldText = false
        val sorted = summary.categoryDistribution.entries.sortedByDescending { it.value }.take(3)
        var currentY = top + 120f
        if (sorted.isEmpty()) {
            canvas.drawText("Aún no registras áreas destacadas.", 80f, currentY, paint)
        } else {
            sorted.forEach { entry ->
                val weight = ((entry.value / summary.totalDecisions.toFloat()) * 100).toInt()
                canvas.drawText("• ${entry.key.displayName}: $weight%", 80f, currentY, paint)
                currentY += 40f
            }
        }
        return top + blockHeight
    }

    private fun drawInsightsBlock(canvas: Canvas, insights: List<InsightRuleResult>, top: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = accentText
        paint.textSize = 38f
        paint.isFakeBoldText = true
        canvas.drawText("Claves para ti", 80f, top + 50f, paint)

        paint.textSize = 26f
        paint.isFakeBoldText = false
        var currentY = top + 100f
        val maxItems = if (insights.isEmpty()) 1 else insights.size.coerceAtMost(4)
        if (insights.isEmpty()) {
            canvas.drawText("Registra más decisiones para descubrir patrones.", 80f, currentY, paint)
            return
        }
        insights.take(maxItems).forEach { insight ->
            canvas.drawText("• ${insight.title}", 80f, currentY, paint)
            currentY += 34f
            canvas.drawText(insight.description, 100f, currentY, paint)
            currentY += 46f
        }
    }
}
