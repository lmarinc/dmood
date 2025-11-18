package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.CategoryType

data class WeeklyHighlight(
    val strongestPositiveDay: String?,
    val strongestNegativeDay: String?,
    val mostFrequentCategory: CategoryType?,
    val emotionalTrend: String
)

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
