package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.CategoryType

/**
 * Hallazgos cualitativos destacados sobre una semana.
 */
data class WeeklyHighlight(
    val strongestPositiveDay: String?,
    val strongestNegativeDay: String?,
    val mostFrequentCategory: CategoryType?,
    val emotionalTrend: String
)

/**
 * Caso de uso para extraer los puntos más relevantes de un WeeklySummary.
 */
class ExtractWeeklyHighlightsUseCase {

    operator fun invoke(summary: WeeklySummary): WeeklyHighlight {
        val strongestPositiveDay = findDayWithMood(summary, DailyMood.POSITIVO)
        val strongestNegativeDay = findDayWithMood(summary, DailyMood.NEGATIVO)

        val mostFrequentCategory = summary.categoryDistribution
            .maxByOrNull { it.value }
            ?.key

        val emotionalTrend = calculateTrend(summary)

        return WeeklyHighlight(
            strongestPositiveDay = strongestPositiveDay,
            strongestNegativeDay = strongestNegativeDay,
            mostFrequentCategory = mostFrequentCategory,
            emotionalTrend = emotionalTrend
        )
    }

    /**
     * Devuelve el primer día que presenta el estado de ánimo indicado.
     * (Simplificación: día representativo, no el “más fuerte” a nivel cuantitativo.)
     */
    private fun findDayWithMood(summary: WeeklySummary, mood: DailyMood): String? {
        return summary.dailyMoods.entries.firstOrNull { it.value == mood }?.key
    }

    private fun calculateTrend(summary: WeeklySummary): String {
        val positiveDays = summary.dailyMoods.count { it.value == DailyMood.POSITIVO }
        val negativeDays = summary.dailyMoods.count { it.value == DailyMood.NEGATIVO }

        return when {
            positiveDays > negativeDays -> "Semana predominantemente positiva"
            negativeDays > positiveDays -> "Semana predominantemente negativa"
            else -> "Semana equilibrada"
        }
    }
}
